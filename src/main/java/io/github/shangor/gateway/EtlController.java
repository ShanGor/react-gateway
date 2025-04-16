package io.github.shangor.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.uuid.Generators;
import io.github.shangor.config.CustomAiMcp;
import io.github.shangor.data.entity.UploadFileRecordEntity;
import io.github.shangor.data.repo.UploadFileRecordRepository;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import liquibase.util.MD5Util;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

@RestController
@Slf4j
public class EtlController {
    private final CustomAiMcp config;

    @Resource
    UploadFileRecordRepository uploadRepo;

    @Resource
    ObjectMapper objectMapper;
    /**
     * 512KB
     */
    private final static long BLOCK_SIZE = 8 * 1024;
    @Autowired
    private PgVectorStore vectorStore;

    public EtlController(CustomAiMcp config) {
        this.config = config;
        if (!Files.exists(Paths.get(config.getUploadDir()))) {
            try {
                Files.createDirectories(Paths.get(config.getUploadDir()));
                log.info("Created upload folder successfully!");
            } catch (IOException e) {
                log.error("Failed to create upload folder: {}", e.getMessage());
            }
        }
    }

    @Data
    public static class UploadRequest {
        private String fileName;
        private String fileType;
        private boolean ocr;
        private int size;
        private String checksum;
    }
    @Data
    public static class UploadResponse {
        private String status;
        private String message;
        private String id;
    }

    @Data
    public static class UploadPart {
        private String id;
        private String ext;
        private int sequence; // starts from 0
        private int size;
        private String contentBase64;
    }

    @PostMapping("/api/docs/convert/{id}")
    public Mono<?> convertRag(@PathVariable String id) {
        return Mono.create(sink -> Thread.ofVirtual().start(() -> {
            try {
                var opt = uploadRepo.findById(id);
                if (opt.isEmpty()) {
                    sink.success(ResponseEntity.status(404).body("Not found the file"));
                    return;
                }
                var o = opt.get();
                var path = Paths.get(o.getFilePath());
                if (!Files.exists(path)) {
                    sink.success(ResponseEntity.status(404).body("File cannot be found with the given path, might got data damage issue."));
                    return;
                }

                if (".pdf".equalsIgnoreCase(o.getFileType())) {
                    var cfg = PdfDocumentReaderConfig.builder()
                            .withPageTopMargin(0)
                            .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                    .withLeftAlignment(true)
                                    .withNumberOfTopTextLinesToDelete(0)
                                    .build())
                            .withPagesPerDocument(1)
                            .build();

                    DocumentReader pdfReader;
                    try {
                        pdfReader = new ParagraphPdfDocumentReader(new FileSystemResource(o.getFilePath()), cfg);
                    } catch (IllegalArgumentException e) {
                        pdfReader = new PagePdfDocumentReader(new FileSystemResource(o.getFilePath()), cfg);
                    }

                    var docs = pdfReader.read();
                    docs.forEach(d -> {
                        var text = d.getText();
                        if (StringUtils.isBlank(text)) return;
                        var docId = Generators.timeBasedEpochGenerator().generate().toString();
                        var meta = new HashMap<>(d.getMetadata());
                        meta.put("fileOriginalName", o.getFileName());
                        meta.put("fileRecordId", o.getId());
                        var doc = new org.springframework.ai.document.Document(docId, text.trim(), meta);
                        vectorStore.doAdd(List.of(doc));
                    });
                    o.setProcessStatus("converted");
                    sink.success(uploadRepo.save(o));
                } else {
                    sink.success(ResponseEntity.status(400).body("Unsupported file type"));
                }
            } catch (Exception e) {
                sink.error(e);
            }

        }));
    }

    @GetMapping("/api/docs")
    public Page<UploadFileRecordEntity> listDocs(@RequestParam(required = false, defaultValue = "0") int page,
                                                 @RequestParam(required = false, defaultValue = "10") int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        return uploadRepo.findAllByOrderByUpdateTimeDesc(pageRequest);
    }

    @PostMapping("/api/docs/before")
    public ResponseEntity<?> uploadFileRequest(@RequestBody String requestText) {
        try {
            var req = objectMapper.readValue(requestText, UploadRequest.class);
            var id = Generators.timeBasedEpochGenerator().generate().toString();
            var fileName = id + req.fileType;
            var filePath = Paths.get(config.getUploadDir(), fileName);
            var record = UploadFileRecordEntity.builder()
                    .id(id)
                    .fileName(req.fileName)
                    .fileType(req.fileType)
                    .filePath(filePath.toString())
                    .ocr(req.ocr)
                    .fileSize(req.size)
                    .fileChecksum(req.checksum)
                    .processStatus("beforeUpload")
                    .build();
            try {
                uploadRepo.save(record);
            } catch (DataIntegrityViolationException e) {
                var opt = uploadRepo.findByFileChecksumAndFileSize(req.checksum, req.size);
                if (opt.isPresent()) {
                    record = opt.get();
                    id = record.getId();
                } else {
                    throw e;
                }
            }

            var resp = new UploadResponse();
            resp.setId(id);
            resp.setStatus("OK");
            return ResponseEntity.ok(resp);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/api/docs/part")
    ResponseEntity<?> uploadFilePart(@RequestBody String requestText) throws IOException {
        var req = objectMapper.readValue(requestText, UploadPart.class);
        var fileName = req.getId() + req.getExt();
        var file = Paths.get(config.getUploadDir(), fileName);
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
        try(RandomAccessFile randomAccessFile = new RandomAccessFile(file.toFile(), "rw")) {
            randomAccessFile.seek(req.getSequence() * BLOCK_SIZE);
            randomAccessFile.write(Base64.getDecoder().decode(req.getContentBase64()));
        }

        return ResponseEntity.ok("OK");
    }

    @PatchMapping("/api/docs/after/{id}")
    ResponseEntity<?> uploadFileComplete(@PathVariable String id) throws IOException {
        var record = uploadRepo.findById(id);
        if (record.isEmpty()) return ResponseEntity.status(404).body("Not found the file");
        var o = record.get();
        var path = Paths.get(o.getFilePath());
        var md5 = MD5Util.computeMD5(Files.newInputStream(path, StandardOpenOption.READ));
        if (!md5.equals(o.getFileChecksum())) {
            return ResponseEntity.status(400).body("MD5 mismatch");
        }
        o.setProcessStatus("uploaded");

        return ResponseEntity.ok(uploadRepo.save(o));
    }

    @DeleteMapping("/api/docs/{id}")
    Mono<?> deleteDoc(@PathVariable String id) {
        return Mono.create(sink -> Thread.ofVirtual().start(() -> {
            var opt = uploadRepo.findById(id);
            if (opt.isEmpty()) {
                sink.success(ResponseEntity.ok("Not found the file"));
            } else {
                var o = opt.get();
                var path = Paths.get(o.getFilePath());
                if (Files.exists(path)) {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        sink.error(e);
                    }
                }

                try {
                    var filter = new FilterExpressionBuilder().eq("fileRecordId", id).build();
                    vectorStore.delete(filter);
                    uploadRepo.delete(o);
                    sink.success(ResponseEntity.ok("OK"));
                } catch (Exception e) {
                    sink.error(e);
                }

            }
        }));
    }
}

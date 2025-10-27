package io.github.shangor.service;

import com.fasterxml.uuid.Generators;
import io.github.shangor.data.dto.ConversionResponse;
import io.github.shangor.data.repo.UploadFileRecordRepository;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class RagService {

    @Resource
    UploadFileRecordRepository uploadRepo;

//    @Autowired
//    private PgVectorStore vectorStore;

//    public ConversionResponse convertPdfToText(String id) {
//        try {
//            var opt = uploadRepo.findById(id);
//            if (opt.isEmpty()) {
//                return ConversionResponse.error(404, "File not found");
//            }
//            var o = opt.get();
//            if (".pdf".equalsIgnoreCase(o.getFileType())) {
//                var cfg = PdfDocumentReaderConfig.builder()
//                        .withPageTopMargin(0)
//                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
//                                .withLeftAlignment(true)
//                                .withNumberOfTopTextLinesToDelete(0)
//                                .build())
//                        .withPagesPerDocument(1)
//                        .build();
//
//                DocumentReader pdfReader;
//                try {
//                    pdfReader = new ParagraphPdfDocumentReader(new FileSystemResource(o.getFilePath()), cfg);
//                } catch (IllegalArgumentException e) {
//                    pdfReader = new PagePdfDocumentReader(new FileSystemResource(o.getFilePath()), cfg);
//                }
//
//                var docs = pdfReader.read();
//                docs.forEach(d -> {
//                    var text = d.getText();
//                    if (StringUtils.isBlank(text)) return;
//                    var docId = Generators.timeBasedEpochGenerator().generate().toString();
//                    var meta = new HashMap<>(d.getMetadata());
//                    meta.put("fileOriginalName", o.getFileName());
//                    meta.put("fileRecordId", o.getId());
//                    var doc = new org.springframework.ai.document.Document(docId, text.trim(), meta);
//                    vectorStore.doAdd(List.of(doc));
//                });
//                o.setProcessStatus("converted");
//
//                return ConversionResponse.success(uploadRepo.save(o));
//            } else {
//                return ConversionResponse.error(400, "Unsupported file type");
//            }
//
//            // 如果成功，返回成功响应
//
//        } catch (Exception e) {
//            log.error("convert pdf to text error: {}", e.getMessage(), e);
//            return ConversionResponse.error(500, "convert pdf to text error: " + e.getMessage());
//        }
//    }
}

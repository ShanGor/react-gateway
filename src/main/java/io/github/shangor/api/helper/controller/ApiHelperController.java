package io.github.shangor.api.helper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.api.helper.db.RequestEntity;
import io.github.shangor.api.helper.db.RequestRepository;
import io.github.shangor.api.helper.pojo.ProxyRequest;
import io.github.shangor.api.helper.pojo.RequestTree;
import jakarta.annotation.Resource;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.LinkedList;

@RestController
@CrossOrigin(origins = "*")
public class ApiHelperController {

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    RequestRepository requestRepo;

    @GetMapping("/api/requests")
    public Collection<RequestTree> getRequestTrees() throws JsonProcessingException {
        var list = new LinkedList<RequestTree>();
        for (var request : requestRepo.findAll()) {
            var requestTree = objectMapper.readValue(request.getContent(), RequestTree.class);
            list.add(requestTree);
        }
        return list;
    }

    @PostMapping(path = "/api/requests")
    public RequestEntity addRequestTree(@RequestBody String body) throws IOException {
        var requestTree = objectMapper.readValue(body, RequestTree.class);
        Long id = requestTree.getId();
        if (id == null || id < 1) {
            id = System.currentTimeMillis();
            requestTree.setId(id);
        }
        var req = new RequestEntity();
        req.setId(id);
        req.setContent(objectMapper.writeValueAsString(requestTree));
        return requestRepo.save(req);
    }

    @PostMapping("/api/request-proxy")
    public void proxyRequest(@RequestBody String body, ServerHttpResponse resp) throws IOException {
        var req = objectMapper.readValue(body, ProxyRequest.class);

        try(var httpclient = HttpClients.createDefault()) {
            var builder = ClassicRequestBuilder.create(req.getMethod());
            if (req.getHeaders() != null) {
                req.getHeaders().forEach(builder::addHeader);
            }
            builder.setUri(req.getUrl());

            httpclient.execute(builder.build(), response -> {
                resp.setRawStatusCode(response.getCode());
                for(var header : response.getHeaders()) {
                    resp.getHeaders().add(header.getName(), header.getValue());
                }
                var stream = response.getEntity().getContent();
                Flux<DataBuffer> dbf = DataBufferUtils.readInputStream(()->stream, resp.bufferFactory(), 4096);
                resp.writeAndFlushWith(Flux.just(dbf)).subscribe();
                return null;
            });
        }


    }

    @DeleteMapping("/api/requests/{id}")
    public ResponseEntity<String> deleteRequestTree(@PathVariable Long id) {
        requestRepo.deleteById(id);
        return ResponseEntity.ok("Request with id " + id + " deleted");
    }

    @PutMapping("/api/requests/{id}")
    public ResponseEntity<String> updateRequestTree(@RequestBody RequestTree requestTree) throws IOException {
        Long id = requestTree.getId();
        if (id == null || id < 1) {
            return ResponseEntity.badRequest().body("Invalid request id");
        }
        var obj = requestRepo.findById(id);
        if (obj.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            obj.get().setContent(objectMapper.writeValueAsString(requestTree));
            requestRepo.save(obj.get());
            return ResponseEntity.ok("Request with id " + id + " updated");
        }
    }
}

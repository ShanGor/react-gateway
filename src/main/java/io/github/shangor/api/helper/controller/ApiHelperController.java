package io.github.shangor.api.helper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.api.helper.db.RequestEntity;
import io.github.shangor.api.helper.db.RequestRepository;
import io.github.shangor.api.helper.pojo.RequestTree;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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

    @PostMapping("/api/requests")
    public RequestEntity addRequestTree(@RequestBody RequestTree requestTree) throws IOException {
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

package io.github.shangor.gateway;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class McpClientTest {
    @Test
    void test() {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:8080").build();
        var client = McpClient.async(transport).capabilities(McpSchema.ClientCapabilities.builder().build()).build();
        var list = client.listTools().block();
        Assertions.assertNotNull(list);
        list.tools().forEach(tool -> {
            if (tool.name().equals("tellStory")) {
                var result = client.callTool(McpSchema.CallToolRequest.builder()
                        .name(tool.name())
                        .arguments(Map.of("author", "Lewis Carroll"))
                        .build()).block();
                Assertions.assertNotNull(result);
                System.out.println(result.content());
            }
        });
    }
}

package io.github.shangor.gateway;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.profiles.active=test"})
class ReactGatewayApplicationTests {
	@Autowired
	ApplicationContext ctx;
	@Autowired
	IndexController controller;
	WebTestClient client;
	@Value("${gateway.static-file-path}")
	String resourcePath;

	@BeforeEach
	void init() {
		client = WebTestClient.bindToApplicationContext(ctx).build();
	}

	@Test
	void contextLoads() {
		var bodyBytes = client.get().uri("/health").exchange().expectStatus().is2xxSuccessful()
				.expectBody().returnResult().getResponseBody();
		assertEquals("ok", new String(bodyBytes));
	}

	@Test
	void testHealth() {
		assertEquals("ok", controller.health());
	}

	@Test
	void testSpa() throws IOException{
		var testPath = Paths.get(resourcePath);
		try {
			if (!Files.exists(testPath)) {
				Files.createDirectories(testPath);
			}
			Files.writeString(testPath.resolve("index.html"), "<h1>Hello world</h1>");
			client.get().uri("/ui").exchange().expectStatus().is2xxSuccessful();
			client.get().uri("/ui/index.html").exchange().expectStatus().is2xxSuccessful();
			client.get().uri("/test.html").exchange().expectStatus().isNotFound();
		} catch (Exception e) {
			System.out.println("Fatal error: " + e.getMessage());
		} finally {
			FileUtils.deleteDirectory(testPath.toFile());
		}
	}
}

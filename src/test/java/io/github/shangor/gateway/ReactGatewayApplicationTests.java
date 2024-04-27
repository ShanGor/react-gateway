package io.github.shangor.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReactGatewayApplicationTests {
	@Autowired
	ApplicationContext ctx;
	@Autowired
	IndexController controller;
	WebTestClient client;

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
}

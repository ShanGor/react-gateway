package io.github.shangor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
@Slf4j
public class ReactGatewayApplication {

	public static void main(String[] args) {
		var userHome = System.getProperty("user.home");
		var appHome = Paths.get(userHome, ".api-helper");
		if (!Files.exists(appHome)) {
			try {
				Files.createDirectories(appHome);
				log.info("Created app home: {}", appHome);
			} catch (Exception e) {
				log.error("Failed to create app home: {}", appHome);
				System.exit(-1);
			}
		}
		SpringApplication.run(ReactGatewayApplication.class, args);
	}

}

package com.xyz.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.profiles.active=native",
        "spring.cloud.config.server.native.searchLocations=classpath:/config"
})
class ConfigApplicationTests {

	@Test
	void contextLoads() {
	}

}

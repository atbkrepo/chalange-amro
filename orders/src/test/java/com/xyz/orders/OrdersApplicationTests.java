package com.xyz.orders;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest
@Import(OrdersApplicationTests.TestJwt.class)
class OrdersApplicationTests {

    @TestConfiguration
    static class TestJwt {

        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return Mockito.mock(JwtDecoder.class);
        }
    }

    @Test
    void contextLoads() {
    }

}

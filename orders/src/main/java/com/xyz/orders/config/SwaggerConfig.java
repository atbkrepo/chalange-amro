package com.xyz.orders.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${auth-server.token-url:http://localhost:9000/oauth2/token}")
    private String tokenUrl;

    @Value("${auth-server.authorization-url:http://localhost:9000/oauth2/authorize}")
    private String authorizationUrl;

    @Bean
    public OpenAPI ordersOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Orders Service API")
                        .description("Order management, cart, inventory & product catalog — secured with OAuth2 JWT")
                        .version("1.0.0")
                        .contact(new Contact().name("Orders Team")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"))
                .addSecurityItem(new SecurityRequirement().addList("oauth2"))
                .components(new Components()
                        .addSecuritySchemes("bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows()
                                        .clientCredentials(new OAuthFlow()
                                                .tokenUrl(tokenUrl)
                                                .scopes(new Scopes()
                                                        .addString("service.read", "Read access for services")
                                                        .addString("service.write", "Write access for services")))
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl(authorizationUrl)
                                                .tokenUrl(tokenUrl)
                                                .scopes(new Scopes()
                                                        .addString("openid", "OpenID Connect")
                                                        .addString("profile", "User profile")
                                                        .addString("read", "Read access")
                                                        .addString("write", "Write access"))))));
    }
}

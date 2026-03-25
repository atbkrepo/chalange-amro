package com.xyz.auth.config;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private UserEntry admin = new UserEntry();
    private Clients clients = new Clients();
    private List<UserEntry> users = new ArrayList<>();

    @Data
    public static class Clients {
        private ClientCredentials service = new ClientCredentials();
        private ClientCredentials external = new ClientCredentials();
    }

    @Data
    public static class ClientCredentials {
        private String clientId;
        private String clientSecret;
    }

    @Data
    public static class UserEntry {
        private String username;
        private String password;
        private List<String> roles = List.of("USER");
    }
}

package com.xyz.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthServerTestConfiguration.class)
class AuthServerTokenTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegisteredClientRepository clientRepository;

    @Autowired
    private OAuth2AuthorizationConsentService consentService;

    static final String SERVICE_CLIENT_ID = "test-service";
    static final String SERVICE_CLIENT_SECRET = "test-service-secret";
    static final String EXTERNAL_CLIENT_ID = "test-external";
    static final String EXTERNAL_CLIENT_SECRET = "test-external-secret";
    static final String ADMIN_USERNAME = "admin";
    static final String EXTERNAL_REDIRECT_URI = "http://localhost:8080/login/oauth2/code/auth-server";

    private String basicAuth(String clientId, String clientSecret) {
        return "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Internal Service Client – client_credentials grant
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Internal service client – client_credentials")
    class InternalServiceClientCredentials {

        @Test
        @DisplayName("returns access token with requested scopes")
        void returnsAccessToken() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "client_credentials")
                            .param("scope", "service.read service.write")
                            .header(HttpHeaders.AUTHORIZATION, basicAuth(SERVICE_CLIENT_ID, SERVICE_CLIENT_SECRET)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.expires_in").isNumber())
                    .andExpect(jsonPath("$.scope", containsString("service.read")))
                    .andExpect(jsonPath("$.scope", containsString("service.write")));
        }

        @Test
        @DisplayName("access token is a valid JWT with correct subject")
        void tokenIsValidJwt() throws Exception {
            MvcResult result = mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "client_credentials")
                            .param("scope", "service.read")
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(SERVICE_CLIENT_ID, SERVICE_CLIENT_SECRET)))
                    .andExpect(status().isOk())
                    .andReturn();

            String accessToken = tokenField(result, "access_token");
            String[] jwtParts = accessToken.split("\\.");
            assertThat(jwtParts).hasSize(3);

            Map<String, Object> claims = decodeJwtPayload(jwtParts[1]);
            assertThat(claims.get("sub")).isEqualTo(SERVICE_CLIENT_ID);
            assertThat(claims).containsKeys("iss", "exp", "iat");
        }

        @Test
        @DisplayName("refresh token for client_credentials")
        void noRefreshToken() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "client_credentials")
                            .param("scope", "service.read")
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(SERVICE_CLIENT_ID, SERVICE_CLIENT_SECRET)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.refresh_token").doesNotExist());
        }

        @Test
        @DisplayName("grants only the requested subset of allowed scopes")
        void grantsRequestedScopeSubset() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "client_credentials")
                            .param("scope", "service.read")
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(SERVICE_CLIENT_ID, SERVICE_CLIENT_SECRET)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scope").value("service.read"));
        }

        @Test
        @DisplayName("rejects wrong client secret")
        void rejectsWrongSecret() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "client_credentials")
                            .param("scope", "service.read")
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(SERVICE_CLIENT_ID, "wrong-secret")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("rejects authorization_code grant (not registered)")
        void rejectsAuthCodeGrant() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "authorization_code")
                            .param("code", "fake")
                            .param("redirect_uri", "http://example.com/callback")
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(SERVICE_CLIENT_ID, SERVICE_CLIENT_SECRET)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("rejects scope outside the allowed set")
        void rejectsDisallowedScope() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "client_credentials")
                            .param("scope", "openid profile")
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(SERVICE_CLIENT_ID, SERVICE_CLIENT_SECRET)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  External App Client – client_credentials grant
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("External app client – client_credentials")
    class ExternalAppClientCredentials {

        @Test
        @DisplayName("returns access token via CLIENT_SECRET_BASIC")
        void returnsTokenViaBasicAuth() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "client_credentials")
                            .param("scope", "read write")
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(EXTERNAL_CLIENT_ID, EXTERNAL_CLIENT_SECRET)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.expires_in").isNumber());
        }

        @Test
        @DisplayName("returns access token via CLIENT_SECRET_POST")
        void returnsTokenViaClientSecretPost() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "client_credentials")
                            .param("client_id", EXTERNAL_CLIENT_ID)
                            .param("client_secret", EXTERNAL_CLIENT_SECRET)
                            .param("scope", "read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty());
        }

        @Test
        @DisplayName("rejects invalid secret via CLIENT_SECRET_POST")
        void rejectsInvalidSecretPost() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "client_credentials")
                            .param("client_id", EXTERNAL_CLIENT_ID)
                            .param("client_secret", "wrong-secret")
                            .param("scope", "read"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  External App Client – authorization_code grant
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("External app client – authorization_code")
    class ExternalAppAuthorizationCode {

        @BeforeEach
        void grantConsent() {
            RegisteredClient client = Objects.requireNonNull(
                    clientRepository.findByClientId(EXTERNAL_CLIENT_ID),
                    "OAuth2 clients must be registered (active profile must include dev)");
            OAuth2AuthorizationConsent consent = OAuth2AuthorizationConsent
                    .withId(client.getId(), ADMIN_USERNAME)
                    .scope("openid").scope("profile").scope("read").scope("write")
                    .build();
            consentService.save(consent);
        }

        @Test
        @DisplayName("full flow: authorize -> exchange code (with PKCE) -> access + refresh + id tokens")
        void fullFlow() throws Exception {
            String codeVerifier = generateCodeVerifier();
            String code = obtainAuthorizationCode("openid profile read", codeVerifier);

            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "authorization_code")
                            .param("code", code)
                            .param("redirect_uri", EXTERNAL_REDIRECT_URI)
                            .param("code_verifier", codeVerifier)
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(EXTERNAL_CLIENT_ID, EXTERNAL_CLIENT_SECRET)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                    .andExpect(jsonPath("$.id_token").isNotEmpty())
                    .andExpect(jsonPath("$.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.scope", containsString("openid")));
        }

        @Test
        @DisplayName("ID token contains standard OIDC claims")
        void idTokenContainsOidcClaims() throws Exception {
            String codeVerifier = generateCodeVerifier();
            String code = obtainAuthorizationCode("openid profile", codeVerifier);

            MvcResult result = mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "authorization_code")
                            .param("code", code)
                            .param("redirect_uri", EXTERNAL_REDIRECT_URI)
                            .param("code_verifier", codeVerifier)
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(EXTERNAL_CLIENT_ID, EXTERNAL_CLIENT_SECRET)))
                    .andExpect(status().isOk())
                    .andReturn();

            String idToken = tokenField(result, "id_token");
            assertThat(idToken).isNotNull();

            Map<String, Object> claims = decodeJwtPayload(idToken.split("\\.")[1]);
            assertThat(claims.get("sub")).isEqualTo(ADMIN_USERNAME);
            assertThat(claims).containsKeys("iss", "aud", "exp", "iat");
        }

        @Test
        @DisplayName("rejects invalid authorization code")
        void rejectsInvalidCode() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "authorization_code")
                            .param("code", "invalid-code")
                            .param("redirect_uri", EXTERNAL_REDIRECT_URI)
                            .param("code_verifier", "dummy-verifier")
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(EXTERNAL_CLIENT_ID, EXTERNAL_CLIENT_SECRET)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  External App Client – refresh_token grant
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("External app client – refresh_token")
    class ExternalAppRefreshToken {

        @BeforeEach
        void grantConsent() {
            RegisteredClient client = Objects.requireNonNull(
                    clientRepository.findByClientId(EXTERNAL_CLIENT_ID),
                    "OAuth2 clients must be registered (active profile must include dev)");
            OAuth2AuthorizationConsent consent = OAuth2AuthorizationConsent
                    .withId(client.getId(), ADMIN_USERNAME)
                    .scope("openid").scope("profile").scope("read").scope("write")
                    .build();
            consentService.save(consent);
        }

        @Test
        @DisplayName("exchanges refresh token for a new access token")
        void refreshTokenExchange() throws Exception {
            String codeVerifier = generateCodeVerifier();
            String code = obtainAuthorizationCode("openid read", codeVerifier);

            MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "authorization_code")
                            .param("code", code)
                            .param("redirect_uri", EXTERNAL_REDIRECT_URI)
                            .param("code_verifier", codeVerifier)
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(EXTERNAL_CLIENT_ID, EXTERNAL_CLIENT_SECRET)))
                    .andExpect(status().isOk())
                    .andReturn();

            String refreshToken = tokenField(tokenResult, "refresh_token");
            assertThat(refreshToken).isNotNull();

            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "refresh_token")
                            .param("refresh_token", refreshToken)
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(EXTERNAL_CLIENT_ID, EXTERNAL_CLIENT_SECRET)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                    .andExpect(jsonPath("$.token_type").value("Bearer"));
        }

        @Test
        @DisplayName("rotates refresh token (reuse is disabled)")
        void rotatesRefreshToken() throws Exception {
            String codeVerifier = generateCodeVerifier();
            String code = obtainAuthorizationCode("openid read", codeVerifier);

            MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "authorization_code")
                            .param("code", code)
                            .param("redirect_uri", EXTERNAL_REDIRECT_URI)
                            .param("code_verifier", codeVerifier)
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(EXTERNAL_CLIENT_ID, EXTERNAL_CLIENT_SECRET)))
                    .andExpect(status().isOk())
                    .andReturn();

            String originalRefresh = tokenField(tokenResult, "refresh_token");

            MvcResult refreshResult = mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "refresh_token")
                            .param("refresh_token", originalRefresh)
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(EXTERNAL_CLIENT_ID, EXTERNAL_CLIENT_SECRET)))
                    .andExpect(status().isOk())
                    .andReturn();

            String rotatedRefresh = tokenField(refreshResult, "refresh_token");
            assertThat(rotatedRefresh).isNotEqualTo(originalRefresh);
        }

        @Test
        @DisplayName("rejects invalid refresh token")
        void rejectsInvalidRefreshToken() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "refresh_token")
                            .param("refresh_token", "bogus-token")
                            .header(HttpHeaders.AUTHORIZATION,
                                    basicAuth(EXTERNAL_CLIENT_ID, EXTERNAL_CLIENT_SECRET)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Error cases
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Error cases")
    class ErrorCases {

        @Test
        @DisplayName("unknown client ID -> 401")
        void unknownClient() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "client_credentials")
                            .header(HttpHeaders.AUTHORIZATION, basicAuth("ghost", "secret")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("missing client authentication -> redirected to login")
        void missingAuth() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "client_credentials"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  OIDC Discovery & JWK Set
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("OIDC discovery and JWK endpoints")
    class OidcDiscovery {

        @Test
        @DisplayName("/.well-known/openid-configuration exposes server metadata")
        void oidcConfiguration() throws Exception {
            mockMvc.perform(get("/.well-known/openid-configuration"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.issuer").isNotEmpty())
                    .andExpect(jsonPath("$.authorization_endpoint").isNotEmpty())
                    .andExpect(jsonPath("$.token_endpoint").isNotEmpty())
                    .andExpect(jsonPath("$.jwks_uri").isNotEmpty())
                    .andExpect(jsonPath("$.grant_types_supported",
                            hasItems("client_credentials", "authorization_code", "refresh_token")));
        }

        @Test
        @DisplayName("/oauth2/jwks exposes RSA public key")
        void jwkSet() throws Exception {
            mockMvc.perform(get("/oauth2/jwks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.keys").isArray())
                    .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                    .andExpect(jsonPath("$.keys[0].kid").isNotEmpty())
                    .andExpect(jsonPath("$.keys[0].n").isNotEmpty())
                    .andExpect(jsonPath("$.keys[0].e").isNotEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private String obtainAuthorizationCode(String scope, String codeVerifier) throws Exception {
        String codeChallenge = computeCodeChallenge(codeVerifier);

        MvcResult result = mockMvc.perform(get("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", EXTERNAL_CLIENT_ID)
                        .queryParam("redirect_uri", EXTERNAL_REDIRECT_URI)
                        .queryParam("scope", scope)
                        .queryParam("state", "state-" + System.nanoTime())
                        .queryParam("code_challenge", codeChallenge)
                        .queryParam("code_challenge_method", "S256")
                        .with(user(ADMIN_USERNAME).roles("ADMIN", "USER")))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirect = result.getResponse().getRedirectedUrl();
        assertThat(redirect).as("redirect should contain authorization code").isNotNull().contains("code=");
        return extractQueryParam(redirect, "code");
    }

    private static String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String computeCodeChallenge(String codeVerifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    /**
     * Token responses can include {@code scope} as a string or a JSON array; deserializing the whole body
     * into {@code Map<String, Object>} fails on Jackson 3+ when lists are present. Read only the fields we need as text.
     */
    private String tokenField(MvcResult result, String field) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private Map<String, Object> decodeJwtPayload(String base64Payload) throws Exception {
        byte[] decoded = Base64.getUrlDecoder().decode(base64Payload);
        JsonNode tree = objectMapper.readTree(decoded);
        return objectMapper.convertValue(tree, new TypeReference<>() {});
    }

    private static String extractQueryParam(String url, String name) {
        String query = url.contains("?") ? url.substring(url.indexOf('?') + 1) : "";
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv[0].equals(name) && kv.length == 2) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}

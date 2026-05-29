package com.exchange.gateway;

import com.exchange.gateway.security.dto.AuthResponse;
import com.exchange.gateway.security.dto.LoginRequest;
import com.exchange.gateway.security.dto.RefreshRequest;
import com.exchange.gateway.security.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("exchange_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432)
                        + "/exchange_test?sslMode=disable");
        registry.add("spring.r2dbc.username", () -> "test");
        registry.add("spring.r2dbc.password", () -> "test");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private WebTestClient webTestClient;

    private String uniqueUser() {
        return "user" + System.nanoTime();
    }

    @Test
    void registerNewUser_returnsTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(uniqueUser());
        request.setPassword("password123");

        webTestClient.post().uri("/api/v1/auth/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assertThat(response.getAccessToken()).isNotBlank();
                    assertThat(response.getRefreshToken()).isNotBlank();
                    assertThat(response.getUsername()).isNotBlank();
                });
    }

    @Test
    void registerDuplicateUser_fails() {
        String username = uniqueUser();
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword("password123");

        webTestClient.post().uri("/api/v1/auth/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/api/v1/auth/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void loginWithValidCredentials_returnsTokens() {
        String username = uniqueUser();

        RegisterRequest register = new RegisterRequest();
        register.setUsername(username);
        register.setPassword("password123");

        webTestClient.post().uri("/api/v1/auth/register")
                .bodyValue(register)
                .exchange()
                .expectStatus().isCreated();

        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword("password123");

        webTestClient.post().uri("/api/v1/auth/login")
                .bodyValue(login)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assertThat(response.getAccessToken()).isNotBlank();
                    assertThat(response.getRefreshToken()).isNotBlank();
                });
    }

    @Test
    void loginWithWrongPassword_fails() {
        String username = uniqueUser();

        RegisterRequest register = new RegisterRequest();
        register.setUsername(username);
        register.setPassword("password123");

        webTestClient.post().uri("/api/v1/auth/register")
                .bodyValue(register)
                .exchange()
                .expectStatus().isCreated();

        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword("wrongpassword");

        webTestClient.post().uri("/api/v1/auth/login")
                .bodyValue(login)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void refreshToken_returnsNewTokenPair() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername(uniqueUser());
        register.setPassword("password123");

        AuthResponse authResponse = webTestClient.post().uri("/api/v1/auth/register")
                .bodyValue(register)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        RefreshRequest refresh = new RefreshRequest();
        refresh.setRefreshToken(authResponse.getRefreshToken());

        webTestClient.post().uri("/api/v1/auth/refresh")
                .bodyValue(refresh)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assertThat(response.getAccessToken()).isNotBlank();
                    assertThat(response.getRefreshToken()).isNotBlank();
                    assertThat(response.getRefreshToken())
                            .isNotEqualTo(authResponse.getRefreshToken());
                });
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        webTestClient.get().uri("/api/v1/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpoint_withValidToken_passesAuth() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername(uniqueUser());
        register.setPassword("password123");

        AuthResponse authResponse = webTestClient.post().uri("/api/v1/auth/register")
                .bodyValue(register)
                .exchange()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        webTestClient.get().uri("/api/v1/orders")
                .header("Authorization", "Bearer " + authResponse.getAccessToken())
                .exchange()
                .expectStatus().isEqualTo(405);
    }
}

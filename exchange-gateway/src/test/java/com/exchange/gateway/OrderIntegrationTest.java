package com.exchange.gateway;

import com.exchange.gateway.security.dto.AuthResponse;
import com.exchange.gateway.security.dto.RegisterRequest;
import com.exchange.common.command.PlaceOrderCommand;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
public class OrderIntegrationTest {

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

    private String accessToken;

    @BeforeEach
    void setUp() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("orderuser" + System.nanoTime());
        register.setPassword("password123");

        AuthResponse response = webTestClient.post().uri("/api/v1/auth/register")
                .bodyValue(register)
                .exchange()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        accessToken = response.getAccessToken();
    }

    @Test
    void placeOrder_withoutAuth_returns401() {
        PlaceOrderCommand order = new PlaceOrderCommand();
        order.setTradingPair("BTC-USD");
        order.setSide(com.exchange.common.enums.OrderSide.BUY);
        order.setType(com.exchange.common.enums.OrderType.LIMIT);
        order.setPrice(new BigDecimal("40000"));
        order.setQuantity(new BigDecimal("0.5"));

        webTestClient.post().uri("/api/v1/orders")
                .bodyValue(order)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void placeValidLimitOrder_returns202() {
        PlaceOrderCommand order = new PlaceOrderCommand();
        order.setTradingPair("BTC-USD");
        order.setSide(com.exchange.common.enums.OrderSide.BUY);
        order.setType(com.exchange.common.enums.OrderType.LIMIT);
        order.setPrice(new BigDecimal("40000"));
        order.setQuantity(new BigDecimal("0.5"));

        webTestClient.post().uri("/api/v1/orders")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(order)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(PlaceOrderCommand.class)
                .value(response -> {
                    assertThat(response.getOrderId()).isNotBlank();
                    assertThat(response.getTradingPair()).isEqualTo("BTC-USD");
                    assertThat(response.getUserId()).isNotNull();
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    @Test
    void placeMarketOrder_withoutPrice_returns202() {
        PlaceOrderCommand order = new PlaceOrderCommand();
        order.setTradingPair("ETH-USD");
        order.setSide(com.exchange.common.enums.OrderSide.BUY);
        order.setType(com.exchange.common.enums.OrderType.MARKET);
        order.setQuantity(new BigDecimal("1.0"));

        webTestClient.post().uri("/api/v1/orders")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(order)
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void placeLimitOrder_withoutPrice_returns500() {
        PlaceOrderCommand order = new PlaceOrderCommand();
        order.setTradingPair("BTC-USD");
        order.setSide(com.exchange.common.enums.OrderSide.BUY);
        order.setType(com.exchange.common.enums.OrderType.LIMIT);
        order.setQuantity(new BigDecimal("0.5"));

        webTestClient.post().uri("/api/v1/orders")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(order)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void placeOrder_serverSetsUserIdAndOrderId() {
        PlaceOrderCommand order = new PlaceOrderCommand();
        order.setTradingPair("BTC-USD");
        order.setSide(com.exchange.common.enums.OrderSide.SELL);
        order.setType(com.exchange.common.enums.OrderType.LIMIT);
        order.setPrice(new BigDecimal("50000"));
        order.setQuantity(new BigDecimal("1.0"));
        order.setOrderId("client-should-not-set-this");
        order.setUserId(999L);

        webTestClient.post().uri("/api/v1/orders")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(order)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(PlaceOrderCommand.class)
                .value(response -> {
                    assertThat(response.getOrderId()).isNotEqualTo("client-should-not-set-this");
                    assertThat(response.getUserId()).isNotEqualTo(999L);
                });
    }
}
package com.example.city;



import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


/**
 * This is a skeleton for implementing integration tests for a Kalix application built with the Spring SDK.
 *
 * This test will initiate a Kalix Proxy using testcontainers and therefore it's required to have Docker installed
 * on your machine. This test will also start your Spring Boot application.
 *
 * Since this is an integration tests, it interacts with the application using a WebClient
 * (already configured and provided automatically through injection).
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
@Profile("test")
public class IntegrationTest extends KalixIntegrationTestKitSupport {

  private static Logger logger = LoggerFactory.getLogger(IntegrationTest.class);
  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(5, ChronoUnit.SECONDS);

  @Test
  public void aggregateByQuantity() throws Exception {
    var cityId = UUID.randomUUID().toString();
    var aggregationLimit = 3;
    var aggregationTimeWindowSeconds = 10;

    //create
    var emptyRes =
            webClient.post()
                    .uri("/city/%s/create".formatted(cityId))
                    .bodyValue(new Model.CreateRequest("name",aggregationLimit,aggregationTimeWindowSeconds))
                    .retrieve()
                    .toEntity(String.class)
                    .block(timeout);

    assertEquals(HttpStatus.OK,emptyRes.getStatusCode());

    var city = get(cityId);
    assertEquals(city.recordsSize(),0);


    add(cityId,10);
    city = get(cityId);
    assertEquals(1, city.recordsSize());
    var aggregationId = city.aggregationId();
    assertNotNull(aggregationId);

    add(cityId,10);
    city = get(cityId);
    assertEquals(2,city.recordsSize());
    assertEquals(aggregationId,city.aggregationId());

    add(cityId,20);
    city = get(cityId);
    assertEquals(0,city.recordsSize());
    assertNull(city.aggregationId());

  }

  @Test
  public void aggregateByTime() throws Exception {
    var cityId = UUID.randomUUID().toString();
    var aggregationLimit = 3;
    var aggregationTimeWindowSeconds = 1;

    //create
    var emptyRes =
            webClient.post()
                    .uri("/city/%s/create".formatted(cityId))
                    .bodyValue(new Model.CreateRequest("name",aggregationLimit,aggregationTimeWindowSeconds))
                    .retrieve()
                    .toEntity(String.class)
                    .block(timeout);

    assertEquals(HttpStatus.OK,emptyRes.getStatusCode());

    var city = get(cityId);
    assertEquals(city.recordsSize(),0);


    add(cityId,10);
    city = get(cityId);
    assertEquals(1, city.recordsSize());
    var aggregationId = city.aggregationId();
    assertNotNull(aggregationId);

    add(cityId,10);
    city = get(cityId);
    assertEquals(2,city.recordsSize());
    assertEquals(aggregationId,city.aggregationId());

    //waiting for timer to trigger
    Thread.sleep(3000);

    city = get(cityId);
    assertEquals(0,city.recordsSize());
    assertNull(city.aggregationId());

  }

  private ResponseEntity<String> add(String cityId, Integer temperature){
    return webClient.post()
            .uri("/city/%s/add-temperature".formatted(cityId))
            .bodyValue(new Model.AddTemperatureRequest(UUID.randomUUID().toString(),temperature))
            .retrieve()
            .toEntity(String.class)
            .block(timeout);
  }
  private Model.GetResponse get(String cityId){
    return webClient.get()
            .uri("/city/%s".formatted(cityId))
            .retrieve()
            .bodyToMono(Model.GetResponse.class)
            .block(timeout);
  }
}
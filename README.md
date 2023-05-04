# Kalix Demo - City Temperature Aggregator - Spring
Not supported by Lightbend in any conceivable way, not open for contributions.<br>
## Prerequisite
Java 17<br>
Apache Maven 3.6 or higher<br>
[Kalix CLI](https://docs.kalix.io/kalix/install-kalix.html) <br>
Docker 20.10.8 or higher (client and daemon)<br>
Container registry with public access (like Docker Hub)<br>
Access to the `gcr.io/kalix-public` container registry<br>
cURL<br>
IDE / editor<br>

## Create Kalix Java/Spring maven project

```
mvn \
archetype:generate \
-DarchetypeGroupId=io.kalix \
-DarchetypeArtifactId=kalix-spring-boot-archetype \
-DarchetypeVersion=LATEST
```
Define value for property 'groupId': `com.example`<br>
Define value for property 'artifactId': `city-temperature-aggregation-spring`<br>
Define value for property 'version' 1.0-SNAPSHOT: :<br>
Define value for property 'package' io.kx: : `com.example.city`<br>

## Import generated project in your IDE/editor

## Define data model/structure
1. Create `com.example.city.Model` interface
2. Add `TemperatureRecord` Java Record
3. Add `City` Java Record
4. Add all helper methods to `City` Java Record (excluding event handler helper methods)
5. Create `com.example.city.input.InputTemperatureMessage` Java Record
6. Create `com.example.city.output.AggregatedTemperatureMessage` Java Record
7. Create interface `Event` in `com.example.city.Model` that extends Model
8. Add all events to `com.example.city.Model` that implement `Event`
9. Add event handler helper methods to `City` Java Record
10. Add external api data model
11. Add internal api data model

## Implement entity
Entity is modeled per one instance (in our case city).<br>
1. Create class `com.example.city.CityEntity`
2. Class needs to extend `EventSourcedEntity<Model.City, Model.Event>` to get support for event sourcing
3. Add class level annotation: `@RequestMapping("/city/{cityId}")`
4. Add `private final String cityId` and constructor for injecting `EventSourcedEntityContext` 
5. Add `EntityType` and `EntityKey` annotations
6. Add `emptyState` helper method
7. Add entity endpoints without the body: `create`, `addTemperature`, `get` and `aggregationTimeWindowTimerTrigger`
8. Annotate each method with spring web bind annotation
9. Add body to each method

## Implement Subscriber Action
1. Create class `com.example.city.input.InputTopicSubscriberAction`
2. Class needs to extend `Action`
3. Add `@Subscribe.Topic("input")` annotation (exclude `@Profile("prod")`)
4. Add `onTemperature` method without body
5. Add `private KalixClient kalixClient;` and inject via constructor 
6. Implement `onTemperature` method body

## Implement Publisher Action
1. Create class `com.example.city.output.OutputTopicPublisherAction`
2. Class needs to extend `Action`
3. Add `@Subscribe.EventSourcedEntity(value = CityEntity.class,ignoreUnknown = true)` annotation (exclude `@Profile("prod")`)
4. Add `onTemperatureAggregatedEvent` method with body
5. Add method annotation `@Publish.Topic("output")`

## Implement Time Window Timer
1. Create class `com.example.city.AggregationTimeWindowTimerAction`
2. Class needs to extend `Action`
3. Add `@Subscribe.EventSourcedEntity(value = CityEntity.class,ignoreUnknown = true)` annotation
4. Add `onTemperature` and `onAggregationFinishedEvent` methods without body
5. Add `private KalixClient kalixClient;` and inject via constructor
6. Add helper method `getTimerName`
7. Implement `onTemperature` and `onAggregationFinishedEvent` methods body

## Unit tests
1. Create `src/test` folder
2. Create Java package `com.example.city`
3. Create class `UnitTest`
4. Add `aggregateByQuantity`
5. Add `aggregateByTime`
6. Runt the unit test with ```mvn test```

## Integration test
1. In `src/it/java/` edit class `com.example.city.IntegrationTest`
2. Add `private Duration timeout = Duration.of(5, ChronoUnit.SECONDS);`
3. Add `add` and `get` helper methods
4. Add `aggregateByQuantity`
5. Add `aggregateByTime`
6. Add class level annotation `@Profile("test")`
7. Action to subscribe and publish to message broker are not supported, for now, by Kalix Integratiokn test so Actions need to be disabled by adding class level annotation `@Profile("prod")` to `com.example.city.input.InputTopicSubscriberAction` and `com.example.city.output.OutputTopicPublisherAction`
6. Runt the unit test with ```mvn -Pit verify```


# Local test
## Prune docker (optional)
```
docker system prune 
```

## Configure local access to Aiven Kafka. 
1. Aiven portal: Create Aiven project and provision Apache Kafka service
2. Aiven portal: Create Kafka topics: `input` and `output`
3. Aiven portal - Services - Kafka - Connection information - Apache Kafka : Enable `SASL` and Switch to `SASL` Authentication Method
3. Aiven portal - Services - Kafka - Connection information - Apache Kafka - SASL: Download CA Certificate to `ca.crt`
4. Import CA Certificate to Java Keystore (set password `pass123`) in the project root: <br>
```
keytool -import -file ca.crt -alias aivenCA -keystore aivenCA.jks
```
6. Create `aiven-kafka.properties` file, in the project root, with this content (replace all `<>` with configuration from Aiven portal - Services - Kafka - Connection information - Apache Kafka - SASL):
```
bootstrap.servers=<copy Service URI>
security.protocol=SASL_SSL
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username='<copy User>' password='<copy Password>';
sasl.mechanism=PLAIN
```
## Setup Kalix Proxy
1. Copy `docker-compose.yaml` from the reference project
2. Start Kalix proxy
```
docker-compose up
```
## Start user function
```
mvn clean compile exec:exec
```

## Test
1. Create one city
```
curl -XPOST -d '{ 
  "name": "Rotterdam",
  "aggregationLimit": 2,
  "aggregationTimeWindowSeconds": 60000
}' http://localhost:9000/city/rotterdam/create -H "Content-Type: application/json"
```
Note: `aggregationLimit` is 2 and `aggregationTimeWindowSeconds` is 1min

2. Add temperature manually
```
curl -XPOST -d '{ 
  "recordId": "11111",
  "temperature": 20
}' http://localhost:9000/city/rotterdam/add-temperature -H "Content-Type: application/json"
```
`Note:` To trigger aggregation send 2 requests and for each increment the recordId
2. Get city state (optional for test):
```
curl -XGET http://localhost:9000/city/rotterdam -H "Content-Type: application/json"
```
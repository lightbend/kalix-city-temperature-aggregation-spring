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

##Define data model/structure
1. Create `com.example.city.Model` interface
2. Add `TemperatureRecord` Java Record
3. Add `City` Java Record
4. Add all helper methods to `City` Java Record (excluding event handler helper methods)
5. Create `com.example.city.input.InputTemperatureMessage` Java Record
6. Create `com.example.city.output.AggregatedTemperatureMessage` Java Record
7. Add all events to `com.example.city.Model`
8. Add event handler helper methods to `City` Java Record
9. Add external api data model
10. Add internal api data model

##Implement entity
Entity is modeled per one instance (in our case city).<br>
1. Create class `com.example.city.CityEntity`
2. Class needs to extend `EventSourcedEntity<Model.City>` to get support for event sourcing
3. Add class level annotation: `@RequestMapping("/city/{cityId}")`
4. Add `private final String cityId` and constructor for injecting `EventSourcedEntityContext` 
5. Add `EntityType` and `EntityKey` annotations
6. Add `emptyState` helper method
7. Add entity endpoints without the body: `create`, `addTemperature`, `get` and `aggregationTimeWindowTimerTrigger`
8. Annotate each method with spring web bind annotation
9. Add body to each method

##Implement Subscriber Action
1. Create class `com.example.city.input.InputTopicSubscriberAction`
2. Class needs to extend `Action`
3. Add `@Subscribe.Topic("input")` annotation (exclude `@Profile("prod")`)
4. Add `onTemperature` method without body
5. Add `private KalixClient kalixClient;` and inject via constructor 
6. Implement `onTemperature` method body

##Implement Publisher Action
1. Create class `com.example.city.output.OutputTopicPublisherAction`
2. Class needs to extend `Action`
3. Add `@Subscribe.EventSourcedEntity(value = CityEntity.class,ignoreUnknown = true)` annotation (exclude `@Profile("prod")`)
4. Add `onTemperatureAggregatedEvent` method with body
5. Add method annotation `@Publish.Topic("output")`

##Implement Time Window Timer
1. Create class `com.example.city.AggregationTimeWindowTimerAction`
2. Class needs to extend `Action`
3. Add `@Subscribe.EventSourcedEntity(value = CityEntity.class,ignoreUnknown = true)` annotation
4. Add `onTemperature` and `onAggregationFinishedEvent` methods without body
5. Add `private KalixClient kalixClient;` and inject via constructor
6. Add helper method `getTimerName`
7. Implement `onTemperature` and `onAggregationFinishedEvent` methods body

##Unit tests
1. Create `src/test` folder
2. Create Java package `com.example.city`
3. Create class `UnitTest`
4. Add `aggregateByQuantity`
5. Add `aggregateByTime`
6. Runt the unit test with ```mvn test```

##Integration test
1. In `src/it/java/` edit class `com.example.city.IntegrationTest`
2. Add `private Duration timeout = Duration.of(5, ChronoUnit.SECONDS);`
3. Add `add` and `get` helper methods
4. Add `aggregateByQuantity`
5. Add `aggregateByTime`
6. Add class level annotation `@Profile("test")`
7. Action to subscribe and publish to message broker are not supported, for now, by Kalix Integratiokn test so Actions need to be disabled by adding class level annotation `@Profile("prod")` to `com.example.city.input.InputTopicSubscriberAction` and `com.example.city.output.OutputTopicPublisherAction`
6. Runt the unit test with ```mvn -Pit verify```


#Local test
##Prune docker (optional)
```
docker system prune 
```

##Setup local Zookeeper and Kafka
1. Copy `docker-compose-kafka.yaml` from the reference project
2. Start kafka and zookeeper <br>
```
docker-compose -f docker-compose-kafka.yaml up
```
3. Create Kafka topics <br>
[Open Kafka UI](http://localhost:8081/)
   1. Create input topic: `input` (number of partitions: 2, Min Sync replicas: 1, Replication factor: 1, Time to retain data: 10000)
   2. Create output topic: `output`(number of partitions: 2, Min Sync replicas: 1, Replication factor: 1, Time to retain data: 60000)
   
##Setup Kalix Proxy
1. Copy `docker-compose.yaml` from the reference project
2. Start Kalix proxy
```
docker-compose up
```
##Start user function
```
mvn clean compile exec:exec
```
##Create one city
```
curl -XPOST -d '{ 
  "name": "Rotterdam",
  "aggregationLimit": 2,
  "aggregationTimeWindowSeconds": 60000
}' http://localhost:9000/city/rotterdam/create -H "Content-Type: application/json"
```
Note: `aggregationLimit` is 2 and `aggregationTimeWindowSeconds` is 1min

##Optional CURLs
1. Add temperature manually (optional for test):
```
curl -XPOST -d '{ 
  "recordId": "11111",
  "temperature": 20
}' http://localhost:9000/city/rotterdam/add-temperature -H "Content-Type: application/json"
```
2. Get city (optional for test):
```
curl -XGET http://localhost:9000/city/rotterdam -H "Content-Type: application/json"
```

##Test
[Open Kafka UI](http://localhost:8081/)
<br>
Produce message to `input` topic
- key does not need to be populated
- value: 
```
{
	"cityId": "rotterdam",
	"recordId": "22226",
	"temperature": "10",
	"timestamp": "2023-02-16T20:00:00.000Z"
}
```
- header:
```
{
	"ce-source": "manual",
	"ce-datacontenttype": "application/json",
	"ce-specversion": "1.0",
	"ce-type": "InputTemperatureMessage",
	"ce-id": "22226",
	"ce-time": "2023-02-16T20:00:00.000Z",
	"Content-Type": "application/json"
}
```

#Kalix deployment & test
##Confluent Cloud setup (using free tier)
1. Register: https://www.confluent.io/confluent-cloud/tryfree/
2. Add cluster:
   1. Cluster type: `Basic`
   2. Region/zones: `Google Cloud`, `N.Virginia (us-east4)`, Availability: `Single zone`
   3. Skip payment
   4. Cluster name: `kalix`
3. Create topics:
   1. `input` topic:
      1. Topic name: `input`
      2. Partitions: `2`
      3. Show advanced settings - Retention time: `1 hour` (just to have for testing)
   2. `output` topic:
      4. Topic name: `output`
      5. Partitions: `2`
      6. Show advanced settings - Retention time: `1 hour` (just to have for testing)
4. Export connection configuration
   1. Clients - `New client`
   2. Choose language: `Java`
   3. `Create Kafka cluster API key`
   4. `Copy`
   5. Create file `confluent-kafka.properties` and paste copied content in

##Kalix project setup
1. Register for free
2. Create Kalix project:
```
kalix projects new city-temperature-aggregation --region gcp-us-east1
```
3. Set Kalix project in Kalix CLI
```
kalix config set project city-temperature-aggregation
```
4. Configure confluent kafka message broker in the Kalix project
```
kalix projects config set broker --broker-service kafka --broker-config-file confluent-kafka.properties
```
##Deploy to Kalix
###Configure container registry
`Note`: The most simple setup is to use public `hub.docker.com`. You just need to replace in `pom.xml`, `my-docker-repo` with your `dockerId`<br>
<br>
More options can be found here: <br>
https://docs.kalix.io/projects/container-registries.html
###Deploy
```
mvn deploy
```
`Note`: First deployment take few minutes for all required resources to be provisioned
##Kalix connection proxy
All services by default are not exposed to Internet and only local/private access is allowed. For local/private access Kalix connection proxy is used.<br>
Create Kalix connection proxy:
```
kalix service proxy city-temperature-aggregation-spring
```

##Create one city
```
curl -XPOST -d '{ 
  "name": "Rotterdam",
  "aggregationLimit": 2,
  "aggregationTimeWindowSeconds": 60000
}' http://localhost:8080/city/rotterdam/create -H "Content-Type: application/json"
```
Note: `aggregationLimit` is 2 and `aggregationTimeWindowSeconds` is 1min

##Optional CURLs
1. Add temperature manually (optional for test):
```
curl -XPOST -d '{ 
  "recordId": "11111",
  "temperature": 20
}' http://localhost:8080/city/rotterdam/add-temperature -H "Content-Type: application/json"
```
2. Get city (optional for test):
```
curl -XGET http://localhost:8080/city/rotterdam -H "Content-Type: application/json"
```

##Test
Unfortunately Confluent Cloud Console does not have an option to produce a message to a topic with headers so that feature can not be used.<>
So we are going to use REST API to produce an input message.

1. [Open Confluent Cloud](https://confluent.cloud/environments)
2. Client - `New client`
3. Choose your language: `REST API`
4. `Produce records using the Confluent Cloud REST API`
   1. Topic name: `input`
   2. Mode: `Non-streaming mode`
   3. `Create Kafka cluster API key`
   4. `Copy`
5. Produce message:
From copied CURL command replace set `-d @input_message.json`
```
 curl \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic XXXXXX" \
  https://<URL>:443/kafka/v3/clusters/lkc-nw619k/topics/input/records \
  -d @input_message.json 

```
`Note`: Header values need to be base64 encoded but you only need to change the `value` in json structure.<br>
6. Validate<br>
Check in Confluent cloud messages in `output` topic (You need to search by offset to be able to see all messages because consumer is configured with ). <br>
Check the service logs:
```
kalix service logs city-temperature-aggregation-spring
```
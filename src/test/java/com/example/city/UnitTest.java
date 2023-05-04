package com.example.city;


import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UnitTest {

    @Test
    public void aggregateByQuantity(){
        var cityId = UUID.randomUUID().toString();
        var aggregationLimit = 3;
        var aggregationTimeWindowSeconds = 10;
        EventSourcedTestKit<Model.City, Model.Event,CityEntity> testKit = EventSourcedTestKit.of(cityId,CityEntity::new);

        var createRequest = new Model.CreateRequest("CITY-%s".formatted(cityId),aggregationLimit,aggregationTimeWindowSeconds);
        var createResult = testKit.call(service -> service.create(createRequest));
        createResult.getNextEventOfType(Model.CreatedEvent.class);

        var addTemperatureResult = testKit.call(service -> service.addTemperature(new Model.AddTemperatureRequest(UUID.randomUUID().toString(),10)));
        var temperatureAddedEvent = addTemperatureResult.getNextEventOfType(Model.TemperatureAddedEvent.class);
        assertTrue(temperatureAddedEvent.firstRecordInAggregation());
        var city = (Model.City)addTemperatureResult.getUpdatedState();
        assertEquals(1,city.records().size());
        var aggregationId = city.aggregationId();
        assertNotNull(aggregationId);

        addTemperatureResult = testKit.call(service -> service.addTemperature(new Model.AddTemperatureRequest(UUID.randomUUID().toString(),10)));
        temperatureAddedEvent = addTemperatureResult.getNextEventOfType(Model.TemperatureAddedEvent.class);
        assertFalse(temperatureAddedEvent.firstRecordInAggregation());
        city = (Model.City)addTemperatureResult.getUpdatedState();
        assertEquals(2,city.records().size());
        assertEquals(aggregationId,city.aggregationId());

        addTemperatureResult = testKit.call(service -> service.addTemperature(new Model.AddTemperatureRequest(UUID.randomUUID().toString(),20)));
        var temperatureAggregatedEvent = addTemperatureResult.getNextEventOfType(Model.TemperatureAggregatedEvent.class);
        assertEquals(13,temperatureAggregatedEvent.avgTemperature());
        assertEquals(10,temperatureAggregatedEvent.minTemperature());
        assertEquals(20,temperatureAggregatedEvent.maxTemperature());
        assertEquals(3,temperatureAggregatedEvent.numberOfRecordsAggregated());

        city = (Model.City)addTemperatureResult.getUpdatedState();
        assertEquals(0,city.records().size());
        assertNull(city.aggregationId());
    }

    @Test
    public void aggregateByTime()throws Exception{
        var cityId = UUID.randomUUID().toString();
        var aggregationLimit = 3;
        var aggregationTimeWindowSeconds = 2;
        EventSourcedTestKit<Model.City, Model.Event,CityEntity> testKit = EventSourcedTestKit.of(cityId,CityEntity::new);

        var createRequest = new Model.CreateRequest("CITY-%s".formatted(cityId),aggregationLimit,aggregationTimeWindowSeconds);
        var createResult = testKit.call(service -> service.create(createRequest));
        createResult.getNextEventOfType(Model.CreatedEvent.class);

        var addTemperatureResult = testKit.call(service -> service.addTemperature(new Model.AddTemperatureRequest(UUID.randomUUID().toString(),10)));
        var temperatureAddedEvent = addTemperatureResult.getNextEventOfType(Model.TemperatureAddedEvent.class);
        assertTrue(temperatureAddedEvent.firstRecordInAggregation());
        var city = (Model.City)addTemperatureResult.getUpdatedState();
        assertEquals(1,city.records().size());
        var aggregationId = city.aggregationId();
        assertNotNull(aggregationId);

        Thread.sleep(3000);

        addTemperatureResult = testKit.call(service -> service.addTemperature(new Model.AddTemperatureRequest(UUID.randomUUID().toString(),20)));
        var temperatureAggregatedEvent = addTemperatureResult.getNextEventOfType(Model.TemperatureAggregatedEvent.class);
        assertEquals(15,temperatureAggregatedEvent.avgTemperature());
        assertEquals(10,temperatureAggregatedEvent.minTemperature());
        assertEquals(20,temperatureAggregatedEvent.maxTemperature());
        assertEquals(2,temperatureAggregatedEvent.numberOfRecordsAggregated());

        city = (Model.City)addTemperatureResult.getUpdatedState();
        assertEquals(0,city.records().size());
        assertNull(city.aggregationId());
    }
}

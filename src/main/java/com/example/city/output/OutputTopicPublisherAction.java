package com.example.city.output;

import com.example.city.CityEntity;
import com.example.city.Model;
import kalix.javasdk.action.Action;
import kalix.springsdk.annotations.Publish;
import kalix.springsdk.annotations.Subscribe;
import org.springframework.context.annotation.Profile;

@Subscribe.EventSourcedEntity(value = CityEntity.class,ignoreUnknown = true)
@Profile("prod")
public class OutputTopicPublisherAction extends Action {

    @Publish.Topic("output")
    public Effect<AggregatedTemperatureMessage> onTemperatureAggregatedEvent(Model.TemperatureAggregatedEvent event){
        return effects().reply(
                new AggregatedTemperatureMessage(event.cityId(),event.aggregationId(),event.avgTemperature(),event.maxTemperature(),event.minTemperature(),event.numberOfRecordsAggregated(),event.aggregationStartTime(),event.aggregationStartTime(),event.timestamp())
        );
    }
}

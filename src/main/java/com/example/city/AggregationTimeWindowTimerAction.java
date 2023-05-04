package com.example.city;

import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.spring.KalixClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Subscribe.EventSourcedEntity(value = CityEntity.class,ignoreUnknown = true)
public class AggregationTimeWindowTimerAction extends Action {

    private static Logger logger = LoggerFactory.getLogger(AggregationTimeWindowTimerAction.class);

    private KalixClient kalixClient;

    public AggregationTimeWindowTimerAction(KalixClient kalixClient) {
        this.kalixClient = kalixClient;
    }

    private String getTimerName (String cityId){
        return  "TIME-WINDOW-TIMER/" + cityId;
    }

    public Effect<String> onTemperatureAddedEvent(Model.TemperatureAddedEvent event){
        logger.info("onTemperatureAddedEvent: {}",event);
        if(event.firstRecordInAggregation()) {
            var timerCreate =
                    timers().cancel(getTimerName(event.cityId()))
                    .thenCompose(done -> {
                        var deferredCall = kalixClient.post("/city/"+event.cityId()+"/aggregation-time-window-timer-trigger",new Model.AggregationTimeWindowDoneRequest(event.aggregationId()),String.class);
                        return timers().startSingleTimer(getTimerName(event.cityId()), Duration.of(event.aggregationTimeWindowSeconds(), ChronoUnit.SECONDS),deferredCall);
                    })
                    .thenApply(done -> Model.RESPONSE_OK);
            return effects().asyncReply(timerCreate);
        }else{
            return effects().reply(Model.RESPONSE_OK);
        }
    }

    public Effect<String> onAggregationFinishedEvent(Model.TemperatureAggregatedEvent event){
        logger.info("onAggregationFinishedEvent: {}",event);
        return effects().asyncReply(timers().cancel(getTimerName(event.cityId())).thenApply(d -> Model.RESPONSE_OK));
    }




}

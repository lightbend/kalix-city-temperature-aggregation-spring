package com.example.city;

import io.grpc.Status;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.util.UUID;

@EntityType("city")
@EntityKey("cityId")
@RequestMapping("/city/{cityId}")
public class CityEntity extends EventSourcedEntity<Model.City> {

    private static Logger logger = LoggerFactory.getLogger(CityEntity.class);

    private final String cityId;

    public CityEntity(EventSourcedEntityContext context) {

        this.cityId = context.entityId();
    }

    @Override
    public Model.City emptyState() {
        return Model.City.empty();
    }

    @PostMapping("/create")
    public Effect<String> create(@RequestBody Model.CreateRequest request){
        logger.info("create [{}]: {}", cityId,request);
        if(currentState().isEmpty()){
            Model.CreatedEvent event = new Model.CreatedEvent(cityId,request.aggregationLimit(), request.aggregationTimeWindowSeconds(),  Instant.now());
            return effects().emitEvent(event).thenReply(updateState -> Model.RESPONSE_OK);
        }else{
            return effects().reply(Model.RESPONSE_OK);
        }
    }

    @PostMapping("/add-temperature")
    public Effect<String> addTemperature(@RequestBody Model.AddTemperatureRequest request){
        logger.info("addTemperature [{}]: {}", cityId,request);
        if (currentState().isEmpty()) {
            //if city not added yet ignore
            return effects().reply(Model.RESPONSE_OK);
        }else if(currentState().isDuplicate(request.recordId())){
            //if duplicate skip
            return effects().reply(Model.RESPONSE_OK);
        }else{
            var firstRecordInAggregation = false;
            var aggregationId = currentState().aggregationId();
            if(aggregationId == null){
                firstRecordInAggregation = true;
                aggregationId = UUID.randomUUID().toString();
            }
            var temperatureAddedEvent =
                    new Model.TemperatureAddedEvent(
                            cityId,
                            aggregationId,
                            request.recordId(),
                            request.temperature(),
                            firstRecordInAggregation,
                            currentState().aggregationTimeWindowSeconds(),
                            Instant.now());
            var tmpNewState = currentState().onTemperatureAddedEvent(temperatureAddedEvent);
            if(tmpNewState.isAggregationFinished()) {
                var temperatureAggregatedEvent =
                        new Model.TemperatureAggregatedEvent(
                                cityId,
                                currentState().aggregationId(),
                                tmpNewState.getAvgTemperature(),
                                tmpNewState.getMaxTemperature(),
                                tmpNewState.getMinTemperature(),
                                tmpNewState.records().size(),
                                tmpNewState.getAggregationStartTime(),
                                tmpNewState.getAggregationEndTime(),
                                Instant.now()
                        );
                return effects().emitEvent(temperatureAggregatedEvent).thenReply(updatedState -> Model.RESPONSE_OK);
            }else{
                //if aggregation is NOT finished, add
                return effects().emitEvent(temperatureAddedEvent).thenReply(newState -> Model.RESPONSE_OK);
            }
        }
    }

    @PostMapping("/aggregation-time-window-timer-trigger")
    public Effect<String> aggregationTimeWindowTimerTrigger(@RequestBody Model.AggregationTimeWindowDoneRequest request){
        logger.info("aggregationTimeWindowTimerTrigger [{}]: {}", cityId,request);
        if(currentState().isEmpty()){
            return effects().reply(Model.RESPONSE_OK);
        }else if (currentState().aggregationId().equals(request.aggregationId()) && currentState().isAggregationFinished()) {
            if (currentState().isAggregationFinished()) {
                var temperatureAggregatedEvent =
                        new Model.TemperatureAggregatedEvent(
                                cityId,
                                currentState().aggregationId(),
                                currentState().getAvgTemperature(),
                                currentState().getMaxTemperature(),
                                currentState().getMinTemperature(),
                                currentState().records().size(),
                                currentState().getAggregationStartTime(),
                                currentState().getAggregationEndTime(),
                                Instant.now()
                        );
                return effects().emitEvent(temperatureAggregatedEvent).thenReply(updatedState -> Model.RESPONSE_OK);
            } else {
                return effects().reply(Model.RESPONSE_OK);
            }
        }else{
            return effects().reply(Model.RESPONSE_OK);
        }
    }

    @GetMapping
    public Effect<Model.GetResponse> get(){
        logger.info("get [{}]", cityId);
        if(currentState().isEmpty())
            return effects().error("Not created", Status.Code.NOT_FOUND);
        return effects().reply(new Model.GetResponse(currentState().records().size(),currentState().aggregationId()));
    }

    @EventHandler
    public Model.City handleCreatedEvent(Model.CreatedEvent event){
        return currentState().onCreatedEvent(event);
    }
    @EventHandler
    public Model.City handleTemperatureAddedEvent(Model.TemperatureAddedEvent event){
        return currentState().onTemperatureAddedEvent(event);
    }
    @EventHandler
    public Model.City handleTemperatureAggregatedEvent(Model.TemperatureAggregatedEvent event){
        return currentState().onTemperatureAggregatedEvent(event);
    }
}

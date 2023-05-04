package com.example.city.input;

import com.example.city.Model;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.spring.KalixClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Subscribe.Topic("input")
//@Profile("prod")
public class InputTopicSubscriberAction extends Action {

    private static Logger logger = LoggerFactory.getLogger(InputTopicSubscriberAction.class);
    private KalixClient kalixClient;

    public InputTopicSubscriberAction(KalixClient kalixClient) {
        this.kalixClient = kalixClient;
    }

    public Effect<String> onTemperature(InputTemperatureMessage message){
        var add = kalixClient.post("/city/%s/add-temperature".formatted(message.cityId()),new Model.AddTemperatureRequest(message.recordId(),message.temperature()),String.class).execute()
                .exceptionally(e->{
                    logger.error("Error on message [{}]: ",message,e);
                    throw (RuntimeException)e;
                });
        return effects().asyncReply(add);
    }
}

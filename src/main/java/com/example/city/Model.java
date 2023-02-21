package com.example.city;

import com.google.common.collect.ImmutableList;

import java.time.Instant;

public interface Model {
    record TemperatureRecord(String recordId,
                             Integer temperature,
                             Instant timestamp) implements Model {};
    record City(ImmutableList<TemperatureRecord> records,
                Integer aggregationLimit,
                Integer aggregationTimeWindowSeconds,
                String aggregationId) implements Model {
        public static City empty(){
            return new City(null,0,0,null);
        }
        public boolean isEmpty(){
            return records==null;
        }
        public boolean isDuplicate(String recordId){
            return records.stream().filter(a -> a.recordId.equals(recordId)).findFirst().isPresent();
        }
        public boolean isAggregationFinished(){
            if(records.isEmpty())
                return false;
            if(records.size() == aggregationLimit)
                return true;
            if(!records.isEmpty() && records.get(0).timestamp.plusSeconds(aggregationTimeWindowSeconds).isBefore(Instant.now()))
                return true;
            return false;
        }

        public Integer getMaxTemperature(){
            return records.stream().mapToInt(TemperatureRecord::temperature).max().orElse(-1);
        }
        public Integer getMinTemperature(){
            return records.stream().mapToInt(TemperatureRecord::temperature).min().orElse(-1);
        }

        public Integer getAvgTemperature(){
            var sum = records.stream().mapToInt(TemperatureRecord::temperature).sum();
            return (int)(sum/records.size());
        }

        public Instant getAggregationStartTime(){
            return records.get(0).timestamp();
        }

        public Instant getAggregationEndTime(){
            return records.get(records.size()-1).timestamp();
        }

        public City onCreatedEvent(CreatedEvent event){
            return new City(ImmutableList.of(),event.aggregationLimit,event.aggregationTimeWindowSeconds,null);
        }
        public City onTemperatureAddedEvent(TemperatureAddedEvent event){
            ImmutableList<TemperatureRecord> newList = ImmutableList.<TemperatureRecord>builder().addAll(records).add(new TemperatureRecord(event.recordId,event.temperature,event.timestamp)).build();
            return new City(newList,aggregationLimit,aggregationTimeWindowSeconds,event.aggregationId);
        }
        public City onTemperatureAggregatedEvent(TemperatureAggregatedEvent event){
            return new City(ImmutableList.of(),aggregationLimit,aggregationTimeWindowSeconds,null);
        }

    }

    //event sourcing durable storage model data definition
    record CreatedEvent(String cityId,
                        Integer aggregationLimit,
                        Integer aggregationTimeWindowSeconds,
                        Instant timestamp) implements Model {}
    record TemperatureAddedEvent(String cityId,
                                 String aggregationId,
                                 String recordId, Integer temperature,
                                 boolean firstRecordInAggregation,
                                 Integer aggregationTimeWindowSeconds,
                                 Instant timestamp) implements Model {}
    record TemperatureAggregatedEvent(String cityId,
                                      String aggregationId,
                                      Integer avgTemperature,
                                      Integer maxTemperature,
                                      Integer minTemperature,
                                      Integer numberOfRecordsAggregated,
                                      Instant aggregationStartTime,
                                      Instant aggregationEndTime,
                                      Instant timestamp) implements Model {}

    //external api data model
    record CreateRequest( String name,
                          Integer aggregationLimit,
                          Integer aggregationTimeWindowSeconds) implements Model {}
    record AddTemperatureRequest(String recordId,
                                 Integer temperature) implements Model {}


    //internal api data model
    record AggregationTimeWindowDoneRequest(String aggregationId) implements Model {}
    record GetResponse(int recordsSize, String aggregationId) implements Model {}

    public static final String RESPONSE_OK = "OK";


}

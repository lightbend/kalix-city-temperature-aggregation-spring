package com.example.city.output;

import java.time.Instant;

public record AggregatedTemperatureMessage(String cityId, String aggregationId, Integer averageTemperature, Integer maxTemperature, Integer minTemperature, Integer numberOfRecordsAggregated, Instant aggregationBeginTime, Instant aggregationEndTime, Instant timestamp) {
}

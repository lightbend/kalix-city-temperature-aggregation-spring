package com.example.city.input;

import java.time.Instant;

public record InputTemperatureMessage(String cityId, String recordId, Integer temperature, Instant timestamp) { }

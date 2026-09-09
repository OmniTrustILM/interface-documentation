package com.otilm.openapi.config.loader.fixtures;

import java.util.List;
import java.util.Map;

public class ExtensionResolverFixtures {

    private static final Map<String, Object> HISTOGRAMS = Map.of("latency", List.of(0.1, 0.2));
    private static final Map<String, String> METRIC_A = Map.of("name", "metric_a", "type", "counter");
    private static final Map<String, String> METRIC_B = Map.of("name", "metric_b", "type", "gauge");
    private static final List<Map<String, String>> REQUIRED = List.of(METRIC_A, METRIC_B);
    public static final Map<String, Object> VALID_MAP = validMap();

    public static final List<Object> VALID_LIST = List.of("one", 2, true, Map.of("k", "v"));

    public static final String INVALID_SCALAR = "not-supported-top-level";

    public static final Map<String, Object> INVALID_NESTED_OBJECT = Map.of("bad", new Object());

    private static final Map<String, Object> PRIVATE_STATIC_MAP = Map.of("x", "y");

    private static Map<String, Object> validMap() {
        return Map.of("version", 1, "histograms", HISTOGRAMS, "required", REQUIRED);
    }
}

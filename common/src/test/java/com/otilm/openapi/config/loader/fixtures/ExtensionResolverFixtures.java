package com.otilm.openapi.config.loader.fixtures;

import java.util.List;
import java.util.Map;

public class ExtensionResolverFixtures {

    public static final Map<String, Object> VALID_MAP = Map
            .of("version", 1, "histograms", Map.of("latency", List.of(0.1, 0.2)), "required", List
                    .of(Map.of("name", "metric_a", "type", "counter"), Map.of("name", "metric_b", "type", "gauge")));

    public static final List<Object> VALID_LIST = List.of("one", 2, true, Map.of("k", "v"));

    public static final String INVALID_SCALAR = "not-supported-top-level";

    public static final Map<String, Object> INVALID_NESTED_OBJECT = Map.of("bad", new Object());

    private static final Map<String, Object> PRIVATE_STATIC_MAP = Map.of("x", "y");
}

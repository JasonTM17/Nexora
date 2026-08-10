package com.nexora.platform.api;

import java.util.Map;

public record ApiProblem(String code, String message, Map<String, String> details, String traceId) {
}

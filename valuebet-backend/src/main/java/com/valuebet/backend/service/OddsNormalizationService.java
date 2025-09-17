package com.valuebet.backend.service;

import com.valuebet.backend.domain.model.Outcome;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OddsNormalizationService {

    public Map<Outcome, Double> removeVig(Map<Outcome, Double> impliedProbs) {
        if (impliedProbs == null || impliedProbs.isEmpty()) {
            return Map.of();
        }
        EnumMap<Outcome, Double> sanitized = new EnumMap<>(Outcome.class);
        double total = impliedProbs.entrySet().stream()
            .mapToDouble(entry -> sanitize(entry.getValue()))
            .sum();
        if (total <= 0.0d) {
            impliedProbs.keySet().forEach(outcome -> sanitized.put(outcome, 0.0d));
            return sanitized;
        }
        impliedProbs.forEach((outcome, value) -> sanitized.put(outcome, sanitize(value) / total));
        return sanitized;
    }

    public Map<Outcome, Double> aggregateAcrossBookmakers(List<Map<Outcome, Double>> noVigProbs) {
        if (noVigProbs == null || noVigProbs.isEmpty()) {
            return Map.of();
        }
        EnumMap<Outcome, List<Double>> accumulator = new EnumMap<>(Outcome.class);
        for (Map<Outcome, Double> map : noVigProbs) {
            if (map == null || map.isEmpty()) {
                continue;
            }
            map.forEach((outcome, value) -> {
                double sanitized = sanitize(value);
                accumulator.computeIfAbsent(outcome, key -> new ArrayList<>()).add(sanitized);
            });
        }
        EnumMap<Outcome, Double> medians = new EnumMap<>(Outcome.class);
        accumulator.forEach((outcome, values) -> medians.put(outcome, median(values)));
        return medians;
    }

    private double sanitize(Double value) {
        return value == null || value.isNaN() || value < 0.0d ? 0.0d : value;
    }

    private double median(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0d;
        }
        List<Double> sorted = values.stream()
            .filter(Objects::nonNull)
            .map(this::sanitize)
            .sorted()
            .collect(Collectors.toList());
        if (sorted.isEmpty()) {
            return 0.0d;
        }
        int size = sorted.size();
        int mid = size / 2;
        if (size % 2 == 0) {
            return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0d;
        }
        return sorted.get(mid);
    }
}

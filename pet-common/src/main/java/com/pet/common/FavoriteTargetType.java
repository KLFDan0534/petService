package com.pet.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FavoriteTargetType {
    private FavoriteTargetType() {}

    public static final String MERCHANT = "merchant";
    public static final String KEEPER = "keeper";
    public static final String SERVICE = "service";

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(MERCHANT, "\u5546\u5bb6");
        LABELS.put(KEEPER, "\u5bc4\u517b\u5458");
        LABELS.put(SERVICE, "\u670d\u52a1");
    }

    public static String normalize(String targetType) {
        return targetType == null ? null : targetType.trim().toLowerCase();
    }

    public static boolean isSupported(String targetType) {
        return LABELS.containsKey(normalize(targetType));
    }

    public static String requireSupported(String targetType) {
        String normalized = normalize(targetType);
        if (!LABELS.containsKey(normalized)) {
            throw new BusinessException(400, "Unsupported favorite target type: " + targetType);
        }
        return normalized;
    }

    public static String labelOf(String targetType) {
        String normalized = normalize(targetType);
        return LABELS.getOrDefault(normalized, "\u672a\u77e5");
    }

    public static List<String> codes() {
        return List.copyOf(LABELS.keySet());
    }

    public static Map<String, String> labels() {
        return Map.copyOf(LABELS);
    }

    public static String detailPath(String targetType, Long targetId) {
        String normalized = normalize(targetType);
        if (Objects.equals(normalized, MERCHANT)) {
            return "/merchants/" + targetId;
        }
        if (Objects.equals(normalized, KEEPER)) {
            return "/keepers/" + targetId;
        }
        if (Objects.equals(normalized, SERVICE)) {
            return "/services/" + targetId;
        }
        return null;
    }
}

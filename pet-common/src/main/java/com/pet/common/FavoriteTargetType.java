package com.pet.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Constants and utility methods for favorite target types.
 * Supports merchant, keeper, and service as valid target types
 * for the user's favorite/bookmark functionality.
 */
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

    /**
     * Normalizes a target type string by trimming and lowercasing.
     *
     * @param targetType the raw target type
     * @return the normalized value, or null if input is null
     */
    public static String normalize(String targetType) {
        return targetType == null ? null : targetType.trim().toLowerCase();
    }

    /**
     * Checks whether the given target type is a supported favorite type.
     *
     * @param targetType the target type to check
     * @return true if the type is supported
     */
    public static boolean isSupported(String targetType) {
        return LABELS.containsKey(normalize(targetType));
    }

    /**
     * Validates and normalizes a target type, throwing BusinessException
     * if the type is not supported.
     *
     * @param targetType the target type to validate
     * @return the normalized target type
     */
    public static String requireSupported(String targetType) {
        String normalized = normalize(targetType);
        if (!LABELS.containsKey(normalized)) {
            throw new BusinessException(400, "Unsupported favorite target type: " + targetType);
        }
        return normalized;
    }

    /**
     * Returns the human-readable label for the given target type.
     *
     * @param targetType the target type
     * @return the Chinese label, or "未知" if unknown
     */
    public static String labelOf(String targetType) {
        String normalized = normalize(targetType);
        return LABELS.getOrDefault(normalized, "\u672a\u77e5");
    }

    /**
     * Returns an immutable list of all supported target type codes.
     *
     * @return list of type codes
     */
    public static List<String> codes() {
        return List.copyOf(LABELS.keySet());
    }

    /**
     * Returns an immutable map of all supported target types to their labels.
     *
     * @return map of type code to display label
     */
    public static Map<String, String> labels() {
        return Map.copyOf(LABELS);
    }

    /**
     * Returns the front-end detail page path for the given target type and ID.
     *
     * @param targetType the target type
     * @param targetId   the target entity ID
     * @return the detail path, or null if the target type is unknown
     */
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

package com.pet.common;

import java.util.Collection;
import java.util.function.Supplier;

public class OwnershipValidator {

    private static final String ACCESS_DENIED = "无权访问此资源";

    public static void checkOwnership(Long resourceUserId, Long currentUserId) {
        if (resourceUserId == null || !resourceUserId.equals(currentUserId)) {
            throw new BusinessException(403, ACCESS_DENIED);
        }
    }

    public static void checkAdminOrOwner(Long resourceUserId, Long currentUserId, boolean isAdmin) {
        if (!isAdmin && (resourceUserId == null || !resourceUserId.equals(currentUserId))) {
            throw new BusinessException(403, ACCESS_DENIED);
        }
    }

    public static void checkAdminOrOwner(Long resourceUserId, Long currentUserId,
                                          Collection<?> adminAuthorities, String adminRole) {
        boolean isAdmin = adminAuthorities.stream()
                .anyMatch(a -> adminRole.equals(a.toString()));
        checkAdminOrOwner(resourceUserId, currentUserId, isAdmin);
    }

    public static void checkAdminOrParticipant(Long currentUserId, boolean isAdmin,
                                                Supplier<Boolean> participantCheck) {
        if (isAdmin) return;
        if (!participantCheck.get()) {
            throw new BusinessException(403, ACCESS_DENIED);
        }
    }

    public static boolean isAdmin(Collection<?> authorities, String adminRole) {
        return authorities.stream().anyMatch(a -> adminRole.equals(a.toString()));
    }
}

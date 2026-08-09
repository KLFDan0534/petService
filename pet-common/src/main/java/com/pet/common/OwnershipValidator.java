package com.pet.common;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * 【资源归属权限校验工具】
 *
 * 业务作用：
 * 提供统一的资源归属校验方法，确保用户只能访问/操作属于自己的资源。
 * 支持普通用户只能操作自己的资源、管理员可以操作所有资源的权限模型。
 *
 * 调用场景：
 * Service 层操作资源前，校验当前登录用户是否有权操作该资源。
 *
 * 调用链：
 * Service.xxx()
 *   ↓
 * OwnershipValidator.checkOwnership()
 *   ↓
 * BusinessException(403) → GlobalExceptionHandler
 *
 * 数据处理：
 * - checkOwnership：严格校验资源必须属于当前用户
 * - checkAdminOrOwner：管理员或资源所有者均可通过
 * - checkAdminOrParticipant：管理员或符合条件的参与者可通过
 *
 * 业务规则：
 * - ADMIN 角色拥有最高权限，可以操作任何资源
 * - 普通用户只能操作 user_id 与当前登录用户一致的资源
 *
 * 注意事项：
 * - isAdmin 方法仅判断角色名称，不校验角色来源
 * - checkAdminOrParticipant 适用于多参与方场景（如订单、工单）
 */
public class OwnershipValidator {

    private static final String ACCESS_DENIED = "无权访问此资源";

    /**
     * 【严格归属校验】
     * 资源必须属于当前用户，否则抛出 403 异常。
     * 调用方：用户个人信息、宠物管理、订单管理等场景。
     */
    public static void checkOwnership(Long resourceUserId, Long currentUserId) {
        if (resourceUserId == null || !resourceUserId.equals(currentUserId)) {
            throw new BusinessException(403, ACCESS_DENIED);
        }
    }

    /**
     * 【管理员或所有者校验】
     * 管理员可以操作任何资源，普通用户只能操作自己的资源。
     * 调用方：后台管理接口、兼有管理员和用户操作场景。
     */
    public static void checkAdminOrOwner(Long resourceUserId, Long currentUserId, boolean isAdmin) {
        if (!isAdmin && (resourceUserId == null || !resourceUserId.equals(currentUserId))) {
            throw new BusinessException(403, ACCESS_DENIED);
        }
    }

    /**
     * 【管理员或所有者校验（带角色解析）】
     * 从 authorities 中解析是否 ADMIN 角色，然后执行管理员或所有者校验。
     * 调用方：Controller 层传入 Authentication.getAuthorities()。
     */
    public static void checkAdminOrOwner(Long resourceUserId, Long currentUserId,
                                          Collection<?> adminAuthorities, String adminRole) {
        boolean isAdmin = adminAuthorities.stream()
                .anyMatch(a -> adminRole.equals(a.toString()));
        checkAdminOrOwner(resourceUserId, currentUserId, isAdmin);
    }

    /**
     * 【管理员或参与者校验】
     * 管理员可以操作任何资源，非管理员需要满足参与方条件。
     * 适用场景：工单系统中客服和客户的权限校验。
     * 参与方条件通过 Supplier 延迟计算，避免无谓查询。
     */
    public static void checkAdminOrParticipant(Long currentUserId, boolean isAdmin,
                                                Supplier<Boolean> participantCheck) {
        if (isAdmin) return;
        if (!participantCheck.get()) {
            throw new BusinessException(403, ACCESS_DENIED);
        }
    }

    /**
     * 【判断是否为管理员】
     * 从用户权限列表中检查是否包含 ADMIN 角色。
     * 调用方：需要判断管理员身份的各种校验场景。
     */
    public static boolean isAdmin(Collection<?> authorities, String adminRole) {
        return authorities.stream().anyMatch(a -> adminRole.equals(a.toString()));
    }
}

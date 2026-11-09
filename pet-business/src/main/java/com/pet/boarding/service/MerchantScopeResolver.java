package com.pet.boarding.service;

import com.pet.boarding.entity.Merchant;
import com.pet.common.BusinessException;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * 产品管理作用域解析：MERCHANT 自动派生自己所属商家；
 * ADMIN 必须显式指定存在的目标商家；其他角色一律拒绝。
 * <p>
 * 产品创建/图片上传等管理入口统一走该解析器，保证归属只能来自服务端。
 */
@Component
public class MerchantScopeResolver {

    private final MerchantService merchantService;

    /**
     * 【构造商家作用域解析器】
     *
     * 业务作用：注入商家业务服务，用于反查商家归属与校验商家存在性。
     *
     * 调用场景：Spring 容器启动创建 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造器 → 赋值 merchantService 字段。
     *
     * 数据处理：无。
     *
     * 业务规则：依赖为 final 字段。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public MerchantScopeResolver(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * @param requestedMerchantId ADMIN 显式指定的目标商家ID（MERCHANT 可省略）
     * @param token               当前认证信息
     * @return 服务端派生的归属商家ID
     * @throws BusinessException 无商家身份 403 / 管理员未指定 400 / 目标商家不存在 404
     */
    public Long resolve(Long requestedMerchantId, JwtAuthenticationToken token) {
        if (isAdmin(token)) {
            if (requestedMerchantId == null) {
                throw new BusinessException(400, "管理员必须显式指定目标商家 merchantId");
            }
            Merchant merchant = merchantService.getById(requestedMerchantId);
            if (merchant == null) {
                throw new BusinessException(404, "目标商家不存在");
            }
            return requestedMerchantId;
        }
        if (token == null) {
            throw new BusinessException(403, "当前用户没有商家身份，无法进行产品管理");
        }
        Merchant merchant = merchantService.findByUserId(token.getUserId());
        if (merchant == null) {
            throw new BusinessException(403, "当前用户没有商家身份，无法进行产品管理");
        }
        return merchant.getId_wsh();
    }

    /**
     * 【判断是否管理员】
     *
     * 业务作用：检查认证信息中是否包含 ROLE_ADMIN 权限。
     *
     * 调用场景：resolve 决定走管理员分支还是商家分支。
     *
     * 调用链：resolve() → isAdmin() → token.getAuthorities() 遍历比对。
     *
     * 数据处理：token 或其权限集合为 null 时返回 false，否则逐个比对权限字符串。
     *
     * 业务规则：只有精确等于 ROLE_ADMIN 才返回 true。
     *
     * 状态影响：无。
     *
     * 异常情况：无，对 null 做了防御。
     *
     * 注意事项：返回 false 后 resolve 会把调用方当作商家身份处理，未登录时最终会抛 403。
     */
    private boolean isAdmin(JwtAuthenticationToken token) {
        if (token == null || token.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : token.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
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
package com.pet.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class LoginResponseVO {
    @Schema(description = "访问令牌")
    private String access_token_wsh;

    @Schema(description = "刷新令牌")
    private String refresh_token_wsh;

    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @Schema(description = "用户名")
    private String username_wsh;

    @Schema(description = "昵称")
    private String nickname_wsh;

    @Schema(description = "头像URL")
    private String avatar_wsh;

    @Schema(description = "角色列表")
    private List<String> roles_wsh;
}

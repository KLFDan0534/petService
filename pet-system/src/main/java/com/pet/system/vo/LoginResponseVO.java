package com.pet.system.vo;

import com.fasterxml.jackson.annotation.JsonGetter;
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

    @JsonGetter("accessToken")
    public String getAccessToken() {
        return access_token_wsh;
    }

    @JsonGetter("refreshToken")
    public String getRefreshToken() {
        return refresh_token_wsh;
    }

    @JsonGetter("userId")
    public Long getUserId() {
        return user_id_wsh;
    }

    @JsonGetter("username")
    public String getUsername() {
        return username_wsh;
    }

    @JsonGetter("nickname")
    public String getNickname() {
        return nickname_wsh;
    }

    @JsonGetter("avatar")
    public String getAvatar() {
        return avatar_wsh;
    }

    @JsonGetter("roles")
    public List<String> getRoles() {
        return roles_wsh;
    }
}

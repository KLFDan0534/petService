package com.pet.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class LoginResponse {
    @JsonProperty("access_token_wsh")
    private String accessToken;

    @JsonProperty("refresh_token_wsh")
    private String refreshToken;

    @JsonProperty("user_id_wsh")
    private Long userId;

    @JsonProperty("username_wsh")
    private String username;

    @JsonProperty("nickname_wsh")
    private String nickname;

    @JsonProperty("roles_wsh")
    private List<String> roles;

    @JsonProperty("accessToken")
    public String getAccessTokenAlias() {
        return accessToken;
    }

    @JsonProperty("refreshToken")
    public String getRefreshTokenAlias() {
        return refreshToken;
    }

    @JsonProperty("userId")
    public Long getUserIdAlias() {
        return userId;
    }

    @JsonProperty("username")
    public String getUsernameAlias() {
        return username;
    }

    @JsonProperty("nickname")
    public String getNicknameAlias() {
        return nickname;
    }

    @JsonProperty("roles")
    public List<String> getRolesAlias() {
        return roles;
    }
}

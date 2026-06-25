package com.pet.system.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UserVO {
    private Long id_wsh;
    private String username_wsh;
    @JsonAlias("nickname")
    private String nickname_wsh;
    @JsonAlias("phone")
    private String phone_wsh;
    private String avatar_wsh;
    @JsonAlias("email")
    private String email_wsh;
    private String address_wsh;
    private BigDecimal latitude_wsh;
    private BigDecimal longitude_wsh;
    private Integer status_wsh;
    private List<String> roles_wsh;
    private LocalDateTime created_at_wsh;

    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

    @JsonGetter("username")
    public String getUsername() {
        return username_wsh;
    }

    @JsonGetter("nickname")
    public String getNickname() {
        return nickname_wsh;
    }

    @JsonGetter("phone")
    public String getPhone() {
        return phone_wsh;
    }

    @JsonGetter("email")
    public String getEmail() {
        return email_wsh;
    }
}

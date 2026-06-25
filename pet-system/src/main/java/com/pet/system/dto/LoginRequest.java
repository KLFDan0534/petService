package com.pet.system.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Username is required")
    @JsonAlias("username")
    @JsonProperty("username_wsh")
    private String username_wsh;

    @NotBlank(message = "Password is required")
    @JsonAlias("password")
    @JsonProperty("password_wsh")
    private String password_wsh;
}

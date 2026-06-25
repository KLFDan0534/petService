package com.pet.system.vo;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class RoleVO {
    private Long id_wsh;
    private String name_wsh;
    private String code_wsh;
    private String description_wsh;
    private LocalDateTime created_at_wsh;
    private Integer user_count_wsh;
}

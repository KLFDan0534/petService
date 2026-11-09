package com.pet.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class RealNameReviewVO {
    @Schema(description = "审核记录ID")
    private Long id_wsh;
    @Schema(description = "用户名")
    private String username_wsh;
    @Schema(description = "昵称")
    private String nickname_wsh;
    @Schema(description = "真实姓名")
    private String real_name_wsh;
    @Schema(description = "身份证号")
    private String id_card_no_wsh;
    @Schema(description = "实名状态：0-未认证 1-待审核 2-已认证 3-已驳回")
    private Integer real_name_status_wsh;
    @Schema(description = "手机号")
    private String phone_wsh;
    @Schema(description = "邮箱")
    private String email_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
    @Schema(description = "驳回原因")
    private String reject_reason_wsh;

    /**
     * 【获取脱敏身份证号】
     *
     * 业务作用：返回身份证号的脱敏展示值，保留前 6 位与后 4 位。
     *
     * 调用场景：前端渲染实名审核详情时由 Jackson 序列化自动调用。
     *
     * 调用链：序列化 RealNameReviewVO → getId_card_no_wsh → 拼接掩码。
     *
     * 数据处理：长度不小于 10 时保留前 6 位与后 4 位，中间以 8 个星号替代。
     *
     * 业务规则：长度不足 10 时原样返回，不做截断。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：覆盖了 Lombok 生成的 getter，序列化结果即为脱敏值，接口不会泄露完整号码。
     */
    public String getId_card_no_wsh() {
        if (id_card_no_wsh != null && id_card_no_wsh.length() >= 10) {
            String raw = id_card_no_wsh;
            return raw.substring(0, 6) + "********" + raw.substring(raw.length() - 4);
        }
        return id_card_no_wsh;
    }
}

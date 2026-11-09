package com.pet.boarding.constant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "商户门店常量")
public final class MerchantStoreConstants {

    /**
     * 【私有构造方法】禁止实例化常量类
     *
     * 业务作用：把类声明为 final 且构造私有，保证只能通过类名访问 MODE_* 与 STATUS_* 常量。
     *
     * 调用场景：无运行时调用，仅供编译期约束。
     *
     * 调用链：无。
     *
     * 数据处理：无。
     *
     * 业务规则：MODE_AUTO=0 表示自动模式，MODE_MANUAL_OPEN=1 手动营业，MODE_MANUAL_CLOSED=2 手动停业；STATUS_CLOSED=0 关店，STATUS_OPEN=1 营业。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：构造方法为空实现，若被反射实例化不会报错；新增状态码需同步更新引用处的判断逻辑。
     */
    private MerchantStoreConstants() {}

    public static final int MODE_AUTO = 0;
    public static final int MODE_MANUAL_OPEN = 1;
    public static final int MODE_MANUAL_CLOSED = 2;

    public static final int STATUS_CLOSED = 0;
    public static final int STATUS_OPEN = 1;
}

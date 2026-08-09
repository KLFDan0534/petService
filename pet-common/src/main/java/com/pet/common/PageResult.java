package com.pet.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 【通用分页结果封装】
 *
 * 业务作用：
 * 统一分页查询的返回结构，所有需要分页的接口使用此类型。
 * 与 MyBatis-Plus 的 IPage 互转，方便 Service 层分页查询。
 *
 * 调用场景：
 * 管理员分页列表、用户订单列表等需要分页的场景。
 *
 * 调用链：
 * Controller
 *   ↓
 * Service → Mapper.selectPage(IPage)
 *   ↓
 * PageResult → Result.success()
 *
 * 字段说明：
 * - list：当前页数据列表
 * - total：总记录数
 * - page：当前页码（从1开始）
 * - size：每页大小
 * - pages：总页数
 */
@Getter
@Setter
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int page;
    private int size;
    private long pages;

    public PageResult() {}

    /**
     * 从 MyBatis-Plus IPage 构造分页结果
     */
    public PageResult(IPage<T> page) {
        this.list = page.getRecords();
        this.total = page.getTotal();
        this.page = (int) page.getCurrent();
        this.size = (int) page.getSize();
        this.pages = page.getPages();
    }

    /**
     * 拷贝分页信息（用于 VO 类型转换后复用分页参数）
     */
    public void copyPageInfo(IPage<?> page) {
        this.total = page.getTotal();
        this.page = (int) page.getCurrent();
        this.size = (int) page.getSize();
        this.pages = page.getPages();
    }
}

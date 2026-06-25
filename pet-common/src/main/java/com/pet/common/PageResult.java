package com.pet.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int page;
    private int size;
    private long pages;

    public PageResult() {}

    public PageResult(IPage<T> page) {
        this.list = page.getRecords();
        this.total = page.getTotal();
        this.page = (int) page.getCurrent();
        this.size = (int) page.getSize();
        this.pages = page.getPages();
    }

    public void copyPageInfo(IPage<?> page) {
        this.total = page.getTotal();
        this.page = (int) page.getCurrent();
        this.size = (int) page.getSize();
        this.pages = page.getPages();
    }
}

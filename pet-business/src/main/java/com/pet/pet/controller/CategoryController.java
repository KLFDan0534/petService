package com.pet.pet.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.pet.entity.Category;
import com.pet.pet.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "分类管理", description = "宠物分类管理")
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 获取所有宠物分类列表
     * @return 分类列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "获取所有分类")
    public Result<List<Category>> listAll() {
        log.info("调用 listAll()");
        return Result.success(categoryService.listAll());
    }

    /**
     * 根据父分类ID获取子分类列表
     * @param parentId 父分类ID
     * @return 子分类列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/parent/{parentId}")
    @Operation(summary = "根据父分类获取子分类")
    public Result<List<Category>> listByParent(@PathVariable Long parentId) {
        log.info("调用 listByParent()");
        return Result.success(categoryService.listByParent(parentId));
    }

    /**
     * 根据ID获取分类详情
     * @param id 分类ID
     * @return 分类详情
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情")
    public Result<Category> getById(@PathVariable Long id) {
        log.info("调用 getById()");
        return Result.success(categoryService.getById(id));
    }

    /**
     * 创建宠物分类
     * @param category 分类信息
     * @return 创建的分类
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建分类")
    public Result<Category> create(@Valid @RequestBody Category category) {
        log.info("调用 create()");
        return Result.success(categoryService.create(category));
    }

    /**
     * 更新宠物分类
     * @param id 分类ID
     * @param category 分类信息
     * @return 更新后的分类
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新分类")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody Category category) {
        log.info("调用 update()");
        return Result.success(categoryService.update(id, category));
    }

    /**
     * 删除宠物分类
     * @param id 分类ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("调用 delete()");
        categoryService.delete(id);
        return Result.success();
    }
}

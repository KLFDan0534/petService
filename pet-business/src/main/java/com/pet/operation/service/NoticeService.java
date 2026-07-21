package com.pet.operation.service;

import com.pet.operation.dto.NoticeCreateRequestDTO;
import com.pet.operation.dto.NoticeUpdateRequestDTO;
import com.pet.operation.entity.Notice;

import java.util.List;

public interface NoticeService {
    /**
     * 获取所有公告列表
     * @return 公告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Notice> listAll();
    /**
     * 根据类型获取公告列表
     * @param type 公告类型
     * @return 公告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Notice> listAll(String type);
    /**
     * 获取指定类型的所有有效公告
     * @param type 公告类型
     * @return 有效公告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Notice> listActive(String type);
    /**
     * 获取用户未读公告列表
     * @param userId 用户ID
     * @return 未读公告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Notice> listUnread(Long userId);
    /**
     * 根据ID获取公告
     * @param id 公告ID
     * @return 公告实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Notice getById(Long id);
    /**
     * 创建公告
     * @param request 创建请求DTO
     * @return 创建后的公告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Notice create(NoticeCreateRequestDTO request);
    /**
     * 更新公告
     * @param id 公告ID
     * @param request 更新请求DTO
     * @return 更新后的公告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Notice update(Long id, NoticeUpdateRequestDTO request);
    /**
     * 获取用户未读弹窗公告
     * @param userId 用户ID
     * @return 未读弹窗列表
     * @author: wsh
     * @date: 2026/7/1 11:00
     **/
    List<Notice> listPopup(Long userId);
    /**
     * 关闭弹窗公告
     * @param id 公告ID
     * @param userId 用户ID
     * @author: wsh
     * @date: 2026/7/1 11:00
     **/
    void dismissPopup(Long id, Long userId);
    /**
     * 标记公告为已读
     * @param id 公告ID
     * @param userId 用户ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void markAsRead(Long id, Long userId);
    /**
     * 删除公告
     * @param id 公告ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void delete(Long id);
}


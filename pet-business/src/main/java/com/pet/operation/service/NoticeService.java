package com.pet.operation.service;

import com.pet.operation.dto.NoticeCreateRequestDTO;
import com.pet.operation.dto.NoticeUpdateRequestDTO;
import com.pet.operation.entity.Notice;

import java.util.List;

/**
 * 系统公告服务接口，提供公告的发布、查询、阅读状态管理和投递控制功能。
 * <p>
 * 公告分为"notice"类型（普通公告）和其他自定义类型。普通公告支持多种投递方式（弹窗/通知/广播），
 * 并会为用户创建对应的通知记录。系统可追踪每位用户的阅读状态。
 */
public interface NoticeService {
    /**
     * 获取所有公告列表，按排序字段升序、创建时间降序排列
     *
     * @return 所有公告列表
     */
    List<Notice> listAll();

    /**
     * 根据类型筛选公告列表，类型不区分大小写
     *
     * @param type 公告类型（如 "notice"），传 null 或空字符串返回所有类型
     * @return 匹配类型的公告列表
     */
    List<Notice> listAll(String type);

    /**
     * 获取指定类型下所有状态为"已启用"的有效公告
     *
     * @param type 公告类型，传 null 表示所有类型
     * @return 有效公告列表
     */
    List<Notice> listActive(String type);

    /**
     * 获取用户尚未阅读的公告列表（仅限 "notice" 类型）
     *
     * @param userId 用户ID
     * @return 用户未读的公告列表
     */
    List<Notice> listUnread(Long userId);

    /**
     * 获取用户尚未关闭的弹窗类公告列表
     * <p>
     * 弹窗公告的投递方式中包含 "popup"，用户关闭后通过 {@link #dismissPopup} 记录已读。
     *
     * @param userId 用户ID
     * @return 未关闭的弹窗公告列表
     */
    List<Notice> listPopup(Long userId);

    /**
     * 关闭（忽略）一条弹窗公告，记录该用户已读
     * <p>
     * 如果已存在阅读记录则直接返回，不会重复插入。
     *
     * @param id     公告ID
     * @param userId 用户ID
     */
    void dismissPopup(Long id, Long userId);

    /**
     * 根据主键ID获取公告详情
     *
     * @param id 公告ID
     * @return 公告实体
     * @throws com.pet.common.BusinessException 公告不存在时抛出
     */
    Notice getById(Long id);

    /**
     * 创建一条新公告，并根据投递方式为用户生成对应的通知记录
     * <p>
     * 仅当公告类型为 "notice"、状态为"已启用"且投递方式包含 "notification" 或 "broadcast" 时，
     * 会为全平台用户逐一创建通知。
     *
     * @param request 创建请求 DTO
     * @return 创建完成后的公告实体（含自增 ID）
     */
    Notice create(NoticeCreateRequestDTO request);

    /**
     * 更新公告内容。更新后会清除该公告的所有用户已读记录，并重新同步通知。
     * <p>
     * 仅当公告类型为 "notice" 且投递方式包含通知相关选项时，才会重新为用户创建通知。
     *
     * @param id      公告ID
     * @param request 更新请求 DTO，仅更新非 null 字段
     * @return 更新后的公告实体
     */
    Notice update(Long id, NoticeUpdateRequestDTO request);

    /**
     * 标记用户对指定公告为"已读"
     * <p>
     * 仅 "notice" 类型的公告允许标记为已读。不重复插入已存在的记录。
     *
     * @param id     公告ID
     * @param userId 用户ID
     * @throws com.pet.common.BusinessException 公告不是 notice 类型时抛出
     */
    void markAsRead(Long id, Long userId);

    /**
     * 删除公告及其关联的已读记录和通知
     * <p>
     * 物理删除公告记录、公告已读记录及通过 {@link NotificationService} 关联的通知。
     *
     * @param id 公告ID
     */
    void delete(Long id);
}


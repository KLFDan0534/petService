package com.pet.operation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.operation.entity.Notification;

import java.util.List;

/**
 * 用户通知服务接口，提供通知的增删查及已读状态管理功能。
 * <p>
 * 通知与公告不同，通知面向单个用户产生（如某个公告推送触发一条通知），
 * 创建后通过 SSE {@link NotificationBroadcaster} 实时推送给用户。
 */
public interface NotificationService {
    /**
     * 查询用户的所有通知，按创建时间倒序排列
     * <p>
     * 自动过滤并清理关联公告已被删除或停用的通知记录。
     *
     * @param userId 用户ID
     * @return 用户通知列表
     */
    List<Notification> listByUser(Long userId);

    /**
     * 统计用户未读通知的数量
     *
     * @param userId 用户ID
     * @return 未读通知数量
     */
    long countUnread(Long userId);

    /**
     * 创建一条通知，同时通过 SSE 实时推送给目标用户
     *
     * @param notification 待创建的通知实体
     * @return 创建完成后的通知实体（含自增 ID）
     */
    Notification create(Notification notification);

    /**
     * 将指定通知标记为已读。仅当通知属于该用户时才执行更新
     *
     * @param id     通知ID
     * @param userId 用户ID
     */
    void markAsRead(Long id, Long userId);

    /**
     * 将用户的所有未读通知一次性标记为已读
     *
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);

    /**
     * 根据关联业务 ID 删除所有相关的通知记录
     * <p>
     * 用于公告更新或删除时同步清理已下发的通知。
     *
     * @param relatedId 关联业务ID（如公告ID）
     */
    void deleteByRelatedId(Long relatedId);

    /**
     * 管理员分页查询全部通知，支持按类型及已读状态精确筛选
     *
     * @param pageParam 分页参数（页码、每页条数）
     * @param type      通知类型，精确匹配，为空时查询全部类型
     * @param isRead    是否已读，精确匹配，为空时不区分已读状态
     * @return 分页通知列表，按创建时间倒序
     */
    IPage<Notification> pageAll(PageRequestDTO pageParam, String type, Integer isRead);
}


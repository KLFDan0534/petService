package com.pet.order.service;

import com.pet.order.entity.Tip;

import java.util.List;

/**
 * 小费服务接口
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
public interface TipService {
    /**
     * 创建小费
     * @param userId 用户ID
     * @param tip 小费信息
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    void create(Long userId, Tip tip);
    /**
     * 根据订单ID获取小费列表
     * @param orderId 订单ID
     * @return 小费列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    List<Tip> listByOrder(Long orderId);
    /**
     * 获取当前用户的小费列表
     * @param userId 用户ID
     * @return 小费列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    List<Tip> listMyTips(Long userId);
}

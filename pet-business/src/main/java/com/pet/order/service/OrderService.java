package com.pet.order.service;

import com.pet.order.dto.CreateOrderRequest;
import com.pet.order.entity.PetOrder;
import java.util.List;

public interface OrderService {
    /**
     * 获取所有订单列表
     * @return 订单列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<PetOrder> listAll();
    /**
     * 根据宠物主人ID获取订单列表
     * @param ownerId 主人ID
     * @return 订单列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<PetOrder> listByOwner(Long ownerId);
    /**
     * 根据商家ID获取订单列表
     * @param merchantId 商家ID
     * @return 订单列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<PetOrder> listByMerchant(Long merchantId);
    /**
     * 根据看护人ID获取订单列表
     * @param keeperId 看护人ID
     * @return 订单列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<PetOrder> listByKeeper(Long keeperId);
    /**
     * 获取看护人待处理订单列表
     * @param keeperId 看护人ID
     * @return 待处理订单列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<PetOrder> listPendingByKeeper(Long keeperId);
    /**
     * 根据订单编号获取订单
     * @param orderNo 订单编号
     * @return 订单实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    PetOrder getByOrderNo(String orderNo);
    /**
     * 根据ID获取订单
     * @param id 订单ID
     * @return 订单实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    PetOrder getById(Long id);
    /**
     * 创建订单
     * @param ownerId 主人ID
     * @param request 创建订单请求
     * @return 创建后的订单
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    PetOrder createOrder(Long ownerId, CreateOrderRequest request);
    /**
     * 根据ID取消订单
     * @param ownerId 主人ID
     * @param orderId 订单ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void cancelOrderById(Long ownerId, Long orderId);
    @Deprecated
    /**
     * 取消订单
     * @param ownerId 主人ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void cancelOrder(Long ownerId, String orderNo);
    /**
     * 完成订单
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void completeOrder(Long userId, String orderNo);
    /**
     * 接单
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void acceptOrder(Long userId, String orderNo);
    /**
     * 拒单
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void rejectOrder(Long userId, String orderNo);
    /**
     * 标记订单已送达
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void markDelivered(Long userId, String orderNo);
    /**
     * 标记订单已接收
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void markReceived(Long userId, String orderNo, String handoverCode);
    /**
     * 开始服务
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void startService(Long userId, String orderNo, String startPhoto);
    /**
     * 更新订单状态
     * @param orderNo 订单编号
     * @param status 新状态
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void updateOrderStatus(String orderNo, String status);
    /**
     * 生成测试订单数据
     * @param userId 用户ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void seedTestOrders(Long userId);
}


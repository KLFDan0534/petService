package com.pet.order.service;

import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderDeliveredRequestDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.order.dto.OrderReceivedRequestDTO;
import com.pet.order.entity.PetOrder;
import java.util.List;

public interface OrderService {
    List<OrderDTO> listAll();
    List<OrderDTO> listByOwner(Long ownerId);
    List<OrderDTO> listByMerchant(Long merchantId);
    List<OrderDTO> listByKeeper(Long keeperId);
    List<OrderDTO> listPendingByKeeper(Long keeperId);
    PetOrder getByOrderNo(String orderNo);
    PetOrder getById(Long id);
    OrderDTO createOrder(Long ownerId, OrderCreateRequestDTO request);
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
    boolean autoAcceptPaidOrderIfTimeout(String orderNo);
    int autoAcceptPaidOrdersIfTimeout();
    boolean cancelPendingOrderIfPaymentTimeout(String orderNo);
    int cancelPaymentTimeoutOrders();
    /**
     * 标记订单已送达
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void markDelivered(Long userId, String orderNo);
    void markDelivered(Long userId, OrderDeliveredRequestDTO request);
    /**
     * 标记订单已接收
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void markReceived(Long userId, String orderNo, String handoverCode);
    void markReceived(Long userId, OrderReceivedRequestDTO request);
    /**
     * 开始服务
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void startService(Long userId, String orderNo, String startPhoto);
    void validateStartServiceAccess(Long userId, String orderNo);
    /**
     * 更新订单状态
     * @param orderNo 订单编号
     * @param status 新状态
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void updateOrderStatus(String orderNo, String status);
    OrderDTO toDTO(PetOrder entity);
    OrderDTO toDTOEnriched(PetOrder entity);
    OrderDTO getDTOById(Long id);
    OrderDTO getDTOByOrderNo(String orderNo);
}


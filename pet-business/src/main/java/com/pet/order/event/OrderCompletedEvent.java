package com.pet.order.event;

/**
 * 订单完成事件
 * @param orderId 订单ID
 * @param petId 宠物ID
 * @param keeperId 看护人ID
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
public record OrderCompletedEvent(Long orderId, Long petId, Long keeperId) {
}

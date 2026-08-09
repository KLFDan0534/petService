package com.pet.order.event;

/**
 * Event published when an order is completed successfully.
 * Carries the order ID, pet ID, and keeper ID so that downstream
 * handlers (e.g., reputation calculation, notifications) can process
 * the completion without performing additional lookups.
 *
 * @param orderId  the completed order ID
 * @param petId    the ID of the pet in the order
 * @param keeperId the ID of the keeper who serviced the order
 */
public record OrderCompletedEvent(Long orderId, Long petId, Long keeperId) {
}

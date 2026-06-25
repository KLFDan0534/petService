package com.pet.order.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建订单请求体
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
public class CreateOrderRequest {
    @JsonAlias({"petId", "pet_id"})
    @NotNull(message = "petId cannot be empty")
    private Long pet_id_wsh;

    @JsonAlias({"keeperId", "keeper_id"})
    @NotNull(message = "keeperId cannot be empty")
    private Long keeper_id_wsh;

    @JsonAlias({"merchantId", "merchant_id"})
    @NotNull(message = "merchantId cannot be empty")
    private Long merchant_id_wsh;

    @JsonAlias({"serviceId", "service_id"})
    private Long service_id_wsh;

    @JsonAlias({"startDate", "start_date"})
    @NotNull(message = "startDate cannot be empty")
    private LocalDate start_date_wsh;

    @JsonAlias({"endDate", "end_date"})
    @NotNull(message = "endDate cannot be empty")
    private LocalDate end_date_wsh;

    @JsonAlias({"deliveryAddress", "delivery_address", "dropoffAddress", "dropoff_address"})
    @Size(max = 500, message = "deliveryAddress cannot exceed 500 characters")
    private String delivery_address_wsh;

    @JsonAlias({"deliveryTime", "delivery_time", "dropoffTime", "dropoff_time"})
    private LocalDateTime delivery_time_wsh;

    @JsonAlias({"receiverAvailableStart", "receiver_available_start", "keeperAvailableStart", "keeper_available_start"})
    private LocalDateTime receiver_available_start_wsh;

    @JsonAlias({"receiverAvailableEnd", "receiver_available_end", "keeperAvailableEnd", "keeper_available_end"})
    private LocalDateTime receiver_available_end_wsh;

    @JsonAlias({"pickupTime", "pickup_time", "returnTime", "return_time"})
    private LocalDateTime pickup_time_wsh;

    @JsonAlias({"pickupAddress", "pickup_address", "returnAddress", "return_address"})
    @Size(max = 500, message = "pickupAddress cannot exceed 500 characters")
    private String pickup_address_wsh;

    @JsonAlias({"remark", "note"})
    @Size(max = 200, message = "remark cannot exceed 200 characters")
    private String remark_wsh;
}

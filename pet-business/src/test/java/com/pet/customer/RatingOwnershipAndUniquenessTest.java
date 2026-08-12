package com.pet.customer;

import com.pet.common.BookingErrorCode;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.customer.dto.RatingCreateRequestDTO;
import com.pet.customer.dto.RatingDTO;
import com.pet.customer.mapper.RatingMapper;
import com.pet.customer.service.impl.RatingServiceImpl;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * U4 RED: 评价归属与唯一约束 —— 评价目标必须与订单的真实归属（商户/看护者/服务）一致，
 * 且同一订单同一维度（order_id + user_id + target_type）只允许一条评价。
 * 防止利用他人订单刷评、错评与并发重复评价。
 */
@ExtendWith(MockitoExtension.class)
class RatingOwnershipAndUniquenessTest {

    @Mock private RatingMapper ratingMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private KeeperMapper keeperMapper;
    @Mock private ServiceItemMapper serviceItemMapper;

    private RatingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RatingServiceImpl(ratingMapper, orderMapper, merchantMapper, keeperMapper, serviceItemMapper);
    }

    @Test
    void rejectsRatingForOrderNotBelongingToUser() {
        PetOrder order = order(1L, 2L, 11L, 21L, 31L);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createRating(3L, request(1L, 11L, "merchant", 5)));

        assertEquals(BookingErrorCode.RATING_TARGET_MISMATCH, ex.getErrorCode());
    }

    @Test
    void merchantTargetMustMatchOrderMerchant() {
        PetOrder order = order(1L, 2L, 11L, 21L, 31L);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createRating(2L, request(1L, 12L, "merchant", 4)));

        assertEquals(BookingErrorCode.RATING_TARGET_MISMATCH, ex.getErrorCode());
    }

    @Test
    void keeperTargetMustMatchOrderKeeper() {
        PetOrder order = order(1L, 2L, 11L, 21L, 31L);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createRating(2L, request(1L, 22L, "keeper", 5)));

        assertEquals(BookingErrorCode.RATING_TARGET_MISMATCH, ex.getErrorCode());
    }

    @Test
    void serviceTargetMustMatchOrderService() {
        PetOrder order = order(1L, 2L, 11L, 21L, 31L);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createRating(2L, request(1L, 32L, "service", 5)));

        assertEquals(BookingErrorCode.RATING_TARGET_MISMATCH, ex.getErrorCode());
    }

    @Test
    void duplicateRatingOnSameOrderAndTypeRejected() {
        PetOrder order = order(1L, 2L, 11L, 21L, 31L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(ratingMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createRating(2L, request(1L, 11L, "merchant", 5)));

        assertEquals(BookingErrorCode.RATING_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void sameOrderAllowsSeparateRatingPerDimension() {
        PetOrder order = order(1L, 2L, 11L, 21L, 31L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(ratingMapper.selectCount(any())).thenReturn(0L);

        service.createRating(2L, request(1L, 11L, "merchant", 4));
        service.createRating(2L, request(1L, 21L, "keeper", 5));

        verify(ratingMapper, times(2)).insert(any(com.pet.customer.entity.Rating.class));
    }

    @Test
    void serviceRatingWithoutOrderResolvesBelongingCompletedOrder() {
        when(ratingMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectOne(any())).thenReturn(order(9L, 2L, 11L, 21L, 31L));

        RatingDTO dto = service.createRating(2L, request(null, 31L, "service", 5));

        assertNotNull(dto);
        assertEquals(31L, dto.getTarget_id_wsh());
        assertEquals("service", dto.getTarget_type_wsh());
        ArgumentCaptor<com.pet.customer.entity.Rating> captor =
                ArgumentCaptor.forClass(com.pet.customer.entity.Rating.class);
        verify(ratingMapper).insert(captor.capture());
        assertEquals(9L, captor.getValue().getOrder_id_wsh());
        assertEquals(2L, captor.getValue().getUser_id_wsh());
    }

    @Test
    void uniqueKeyConflictIsMappedToStableRatingAlreadyExistsError() {
        PetOrder order = order(1L, 2L, 11L, 21L, 31L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(ratingMapper.selectCount(any())).thenReturn(0L);
        doThrow(new DuplicateKeyException("duplicate"))
                .when(ratingMapper).insert(any(com.pet.customer.entity.Rating.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createRating(2L, request(1L, 11L, "merchant", 5)));

        assertEquals(BookingErrorCode.RATING_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void merchantCannotReplyToRatingOfAnotherMerchantsService() {
        when(ratingMapper.selectById(7L)).thenReturn(ratingRecord(7L, 31L, "service"));
        when(merchantMapper.selectOne(any())).thenReturn(merchant(11L));
        when(serviceItemMapper.selectById(31L)).thenReturn(serviceItem(31L, 99L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.replyRating(7L, "谢谢", 5L));

        assertEquals(403, ex.getCode());
    }

    @Test
    void ownerMerchantCanReplyToOwnServiceRating() {
        when(merchantMapper.selectOne(any())).thenReturn(merchant(11L));
        when(serviceItemMapper.selectById(31L)).thenReturn(serviceItem(31L, 11L));
        when(ratingMapper.selectById(7L)).thenReturn(ratingRecord(7L, 31L, "service"));

        RatingDTO dto = service.replyRating(7L, "谢谢", 5L);

        assertNotNull(dto);
        assertEquals("谢谢", dto.getReply_wsh());
        verify(ratingMapper).updateById(any(com.pet.customer.entity.Rating.class));
    }

    @Test
    void getMyRatingsByOrderReturnsOnlyOwnedRatings() {
        when(ratingMapper.selectList(any())).thenReturn(java.util.List.of(
                ratingRecord(1L, 11L, "merchant"),
                ratingRecord(2L, 21L, "keeper")));

        var list = service.getMyRatingsByOrder(2L, 1L);

        assertEquals(2, list.size());
        assertEquals("merchant", list.get(0).getTarget_type_wsh());
    }

    @Test
    void getMyRatingsByOrderWithNullOrderReturnsEmpty() {
        var list = service.getMyRatingsByOrder(2L, null);
        assertEquals(0, list.size());
        verify(ratingMapper, times(0)).selectList(any());
    }

    private static Merchant merchant(long id) {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(id);
        return merchant;
    }

    private static ServiceItem serviceItem(long id, long merchantId) {
        ServiceItem item = new ServiceItem();
        item.setId_wsh(id);
        item.setMerchant_id_wsh(merchantId);
        return item;
    }

    private static com.pet.customer.entity.Rating ratingRecord(long id, long targetId, String targetType) {
        com.pet.customer.entity.Rating rating = new com.pet.customer.entity.Rating();
        rating.setId_wsh(id);
        rating.setTarget_id_wsh(targetId);
        rating.setTarget_type_wsh(targetType);
        return rating;
    }

    private static PetOrder order(long id, long owner, long merchant, long keeper, long service) {
        PetOrder order = new PetOrder();
        order.setId_wsh(id);
        order.setOwner_id_wsh(owner);
        order.setMerchant_id_wsh(merchant);
        order.setKeeper_id_wsh(keeper);
        order.setService_id_wsh(service);
        order.setStatus_wsh(OrderStatus.COMPLETED);
        order.setCreated_at_wsh(LocalDateTime.of(2026, 8, 1, 10, 0));
        return order;
    }

    private static RatingCreateRequestDTO request(Long orderId, Long targetId, String targetType, int score) {
        RatingCreateRequestDTO dto = new RatingCreateRequestDTO();
        dto.setOrder_id_wsh(orderId);
        dto.setTarget_id_wsh(targetId);
        dto.setTarget_type_wsh(targetType);
        dto.setScore_wsh(score);
        return dto;
    }

    @SuppressWarnings("unused")
    private void unusedRefs() {
        Merchant.class.getName();
        Keeper.class.getName();
    }
}
package com.pet.admin;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.common.BusinessException;
import com.pet.customer.dto.ChatMessageDTO;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.service.ChatEventBroadcaster;
import com.pet.customer.service.ChatService;
import com.pet.fulfillment.dto.SendOrderMessageRequestDTO;
import com.pet.fulfillment.service.impl.OrderFulfillmentServiceImpl;
import com.pet.operation.service.FileRecordService;
import com.pet.operation.service.NotificationService;
import com.pet.operation.service.impl.MinIoService;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.mapper.CareRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderConversationServiceTest {

    private static final long ORDER_ID = 1L;
    private static final long OWNER_USER_ID = 10L;
    private static final long KEEPER_ID = 20L;
    private static final long KEEPER_USER_ID = 21L;
    private static final long MERCHANT_ID = 30L;
    private static final long MERCHANT_USER_ID = 31L;

    @Mock private OrderMapper orderMapper;
    @Mock private KeeperMapper keeperMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private CareRecordMapper careRecordMapper;
    @Mock private ChatService chatService;
    @Mock private ChatEventBroadcaster chatEventBroadcaster;
    @Mock private MinIoService minIoService;
    @Mock private FileRecordService fileRecordService;
    @Mock private NotificationService notificationService;
    @Mock private KeeperAttendanceService keeperAttendanceService;

    private OrderFulfillmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderFulfillmentServiceImpl(
                orderMapper,
                keeperMapper,
                merchantMapper,
                careRecordMapper,
                chatService,
                chatEventBroadcaster,
                minIoService,
                fileRecordService,
                notificationService,
                keeperAttendanceService);
    }

    @Test
    void ownerMessageWithoutReceiver_goesToKeeperForOrderConversation() {
        stubOrderAccess();
        when(chatService.sendMessage(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ChatMessageDTO dto = new ChatMessageDTO();
            dto.setId_wsh(100L);
            dto.setOrder_id_wsh(message.getOrder_id_wsh());
            dto.setFrom_user_id_wsh(message.getFrom_user_id_wsh());
            dto.setTo_user_id_wsh(message.getTo_user_id_wsh());
            dto.setContent_wsh(message.getContent_wsh());
            dto.setType_wsh(message.getType_wsh());
            dto.setRead_wsh(0);
            return dto;
        });

        SendOrderMessageRequestDTO request = new SendOrderMessageRequestDTO();
        request.setContent_wsh("Please send a lunch photo");
        request.setType_wsh("text");

        ChatMessageDTO result = service.sendMessage(OWNER_USER_ID, false, ORDER_ID, request);

        assertEquals(OWNER_USER_ID, result.getFrom_user_id_wsh());
        assertEquals(KEEPER_USER_ID, result.getTo_user_id_wsh());
        assertEquals(ORDER_ID, result.getOrder_id_wsh());
        verify(chatEventBroadcaster).broadcastMessage(result);
    }

    @Test
    void explicitReceiverMustBeOrderParticipant() {
        stubOrderAccess();
        SendOrderMessageRequestDTO request = new SendOrderMessageRequestDTO();
        request.setTo_user_id_wsh(999L);
        request.setContent_wsh("hello");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sendMessage(OWNER_USER_ID, false, ORDER_ID, request));

        assertEquals(403, ex.getCode());
    }

    @Test
    void outsiderCannotReadOrderConversation() {
        stubOrderAccess();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.listConversation(999L, false, ORDER_ID, KEEPER_USER_ID, null, null));

        assertEquals(403, ex.getCode());
    }

    @Test
    void markReadUsesCurrentUserAsReceiverAndOtherParticipantAsSender() {
        stubOrderAccess();

        service.markConversationAsRead(OWNER_USER_ID, false, ORDER_ID, KEEPER_USER_ID);

        verify(chatService).markConversationAsRead(OWNER_USER_ID, KEEPER_USER_ID, ORDER_ID);
    }

    @Test
    void ownerCanSelectMerchantWhenKeeperAlsoExists() {
        stubOrderAccess();

        service.listConversation(OWNER_USER_ID, false, ORDER_ID, MERCHANT_USER_ID, 50L, 20);

        verify(chatService).getConversation(OWNER_USER_ID, MERCHANT_USER_ID, ORDER_ID, 50L, 20);
    }

    private void stubOrderAccess() {
        PetOrder order = new PetOrder();
        order.setId_wsh(ORDER_ID);
        order.setOwner_id_wsh(OWNER_USER_ID);
        order.setKeeper_id_wsh(KEEPER_ID);
        order.setMerchant_id_wsh(MERCHANT_ID);

        Keeper keeper = new Keeper();
        keeper.setId_wsh(KEEPER_ID);
        keeper.setUser_id_wsh(KEEPER_USER_ID);

        Merchant merchant = new Merchant();
        merchant.setId_wsh(MERCHANT_ID);
        merchant.setUser_id_wsh(MERCHANT_USER_ID);

        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(keeper);
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant);
    }
}

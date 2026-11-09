package com.pet.order.service;

import com.pet.order.dto.OrderBatchCreateRequestDTO;
import com.pet.order.dto.OrderCheckoutDTO;
import com.pet.order.dto.OrderCompleteRequestDTO;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.order.dto.OrderPreviewRequestDTO;
import com.pet.order.dto.OrderQuoteDTO;
import com.pet.order.dto.OrderReceivedRequestDTO;
import com.pet.order.dto.OrderBatchCreateRequestDTO;
import com.pet.order.entity.PetOrder;
import java.util.List;

public interface OrderService {

    /**
     * 【查询全部订单】
     *
     * 业务作用：
     * 获取系统中所有用户的全部订单列表，用于管理后台全局订单查看。
     *
     * 调用场景：
     * 管理员后台查看所有订单记录。
     *
     * 调用链：
     * 管理后台
     * ↓
     * listAll()
     * ↓
     * orderMapper.selectList() + toDTOEnrichedList()
     *
     * 数据处理：
     * 按创建时间降序排列，批量加载关联实体（服务、用户、宠物、看护者、商家）和订单快照。
     *
     * 业务规则：
     * 无状态过滤，返回全量订单。
     *
     * 状态影响：
     * 只读操作，不修改业务状态。
     *
     * 异常情况：
     * 无特殊业务异常。
     *
     * 注意事项：
     * 数据量大时建议增加分页功能。
     *
     * @return 包含关联实体信息的增强订单DTO列表
     */
    List<OrderDTO> listAll();
    /**
     * 【查询宠物主人的订单列表】
     *
     * 业务作用：
     * 根据宠物主人ID查询其名下所有订单，用于主人端订单管理。
     *
     * 调用场景：
     * 宠物主人查看"我的订单"列表。
     *
     * 调用链：
     * 主人端/Controller
     * ↓
     * listByOwner(ownerId)
     * ↓
     * orderMapper.selectList() + toDTOEnrichedList()
     *
     * 数据处理：
     * 按主人ID过滤，按创建时间降序排列，批量加载关联实体信息。
     *
     * 业务规则：
     * 仅返回指定主人的订单，不涉及权限校验。
     *
     * 状态影响：
     * 只读操作。
     *
     * 异常情况：
     * 无特殊业务异常。
     *
     * @param ownerId 宠物主人的用户ID
     * @return 该主人的订单DTO列表
     */
    List<OrderDTO> listByOwner(Long ownerId);

    /**
     * 【查询商家的订单列表】
     *
     * 业务作用：
     * 根据商家ID查询该商家相关的所有订单，用于商家端订单管理。
     *
     * 调用场景：
     * 商家查看"店铺订单"列表。
     *
     * 调用链：
     * 商家端/Controller
     * ↓
     * listByMerchant(merchantId)
     * ↓
     * orderMapper.selectList() + toDTOEnrichedList()
     *
     * 数据处理：
     * 按商家ID过滤，按创建时间降序排列，批量加载关联实体信息。
     *
     * 业务规则：
     * 仅返回指定商家的订单。
     *
     * 状态影响：
     * 只读操作。
     *
     * @param merchantId 商家ID
     * @return 该商家的订单DTO列表
     */
    List<OrderDTO> listByMerchant(Long merchantId);
    /**
     * 【查询看护者的活跃订单列表】
     *
     * 业务作用：
     * 根据看护者ID查询其正在履约中的订单（排除待付款和已支付状态），用于看护者端查看当前任务。
     *
     * 调用场景：
     * 看护者查看"我的任务"中的活跃订单。
     *
     * 调用链：
     * 看护者端/Controller
     * ↓
     * listByKeeper(keeperId)
     * ↓
     * orderMapper.selectList() + toDTOEnrichedList()
     *
     * 数据处理：
     * 按看护者ID过滤，排除PENDING和PAID状态，按创建时间降序排列。
     *
     * 业务规则：
     * 仅返回已进入履约流程（CONFIRMED及之后状态）的订单。
     *
     * 状态影响：
     * 只读操作。
     *
     * @param keeperId 看护者ID
     * @return 该看护者的活跃订单DTO列表
     */
    List<OrderDTO> listByKeeper(Long keeperId);
    /**
     * 【根据订单号查询订单实体】
     *
     * 业务作用：
     * 根据唯一订单编号获取原始订单实体，用于内部服务间调用。
     *
     * 调用场景：
     * 其他服务层方法内部调用，用于获取订单原始数据。
     *
     * 调用链：
     * 内部服务方法
     * ↓
     * getByOrderNo(orderNo)
     * ↓
     * orderMapper.selectOne()
     *
     * 数据处理：
     * 通过订单号精确匹配查询。
     *
     * 业务规则：
     * 订单号不能为空，未找到则抛出异常。
     *
     * 状态影响：
     * 只读操作。
     *
     * 异常情况：
     * 订单号为空抛出400异常；订单不存在抛出404异常。
     *
     * @param orderNo 订单编号
     * @return 订单实体
     */
    PetOrder getByOrderNo(String orderNo);

    /**
     * 【根据主键ID查询订单实体】
     *
     * 业务作用：
     * 根据订单主键ID获取原始订单实体，用于内部服务间调用。
     *
     * 调用场景：
     * 其他服务层方法内部调用。
     *
     * 调用链：
     * 内部服务方法
     * ↓
     * getById(id)
     * ↓
     * orderMapper.selectById()
     *
     * 数据处理：
     * 通过主键ID直接查询。
     *
     * 业务规则：
     * 未找到则抛出异常。
     *
     * 状态影响：
     * 只读操作。
     *
     * 异常情况：
     * 订单不存在抛出BusinessException。
     *
     * @param id 订单ID
     * @return 订单实体
     */
    PetOrder getById(Long id);

    /**
     * 【创建宠物寄养订单】
     *
     * 业务作用：
     * 创建新的宠物寄养订单，包含完整的业务校验（宠物归属、看护者/商家/服务存在性、日期可用性、
     * 看护者容量、资质检查、日期冲突检测），并应用长住优惠、优惠券和会员折扣。
     *
     * 调用场景：
     * 宠物主人提交寄养订单时调用。
     *
     * 调用链：
     * 主人端/OrderController.createOrder()
     * ↓
     * createOrder(ownerId, request)
     * ↓
     * 校验(宠物/看护者/商家/服务/日期/冲突) → 计算金额(长住折扣+优惠券+会员) → insert订单
     * → 锁定优惠券/会员权益 → 创建快照 → 刷新看护者容量 → 发送支付超时检查消息
     *
     * 数据处理：
     * 输入请求参数经完整校验后组装订单实体，计算最终金额（原价-折扣+优惠券+会员优惠），
     * 生成订单号和交接码，写入数据库。
     *
     * 业务规则：
     * 1. 宠物必须属于当前主人
     * 2. 看护者必须属于所选商家且资质审核通过
     * 3. 商家必须审核通过且营业中
     * 4. 开始日期不能早于今天，结束日期必须在开始日期之后
     * 5. 同JVM内串行化（synchronized）防止并发冲突
     * 6. 宠物在该时间段内不能有其他冲突订单
     * 7. 看护者当日接单数不能超过最大容量
     * 8. 看护者不能处于请假状态
     *
     * 状态影响：
     * 创建后订单状态为PENDING（待付款）。
     *
     * 异常情况：
     * 校验失败抛出BusinessException，包含具体失败原因。
     *
     * 注意事项：
     * 教学注释说明synchronized是在没有排班表或数据库约束前的临时并发控制方案。
     *
     * @param ownerId 宠物主人的用户ID
     * @param request 订单创建请求参数
     * @return 创建成功的增强订单DTO
     */
    OrderDTO createOrder(Long ownerId, OrderCreateRequestDTO request);

    /**
     * 【批量创建订单（多宠物连续下单）】
     *
     * 业务作用：
     * 在一个事务内为多个宠物分别创建独立订单（每单一只宠物，各自日期区间），
     * 任一订单失败整批回滚。共享校验（服务/商家/看护人/单位/价格/版本）只执行一次；
     * 容量校验沿用单笔路径，利用同事务读己之写保证批次内部互相挤占不被漏检。
     *
     * 业务规则：
     * 1. 仅支持 day 单位服务；items 数量 2..10
     * 2. 每只宠物必须属于当前用户，同一宠物重叠区间被拒（不重叠允许）
     * 3. 优惠券只应用于批次中 baseAmount 最大的订单（并列取 items 顺序靠前者）
     * 4. 创建后每单状态为 PENDING，各自独立支付
     *
     * @param ownerId 宠物主人的用户ID
     * @param request 批量订单创建请求
     * @return 按 items 顺序的创建成功订单DTO列表
     */
    List<OrderDTO> createOrders(Long ownerId, OrderBatchCreateRequestDTO request);
    /**
     * 【根据订单ID取消订单】
     *
     * 业务作用：
     * 宠物主人在订单未支付前取消订单，释放已锁定的优惠券和会员权益，刷新看护者容量。
     *
     * 调用场景：
     * 宠物主人在待付款状态下点击"取消订单"。
     *
     * 调用链：
     * 主人端/OrderController
     * ↓
     * cancelOrderById(ownerId, orderId)
     * ↓
     * 校验归属/状态 → 乐观锁更新状态 → 释放优惠券/会员权益 → 刷新看护者容量 → 广播状态变更
     *
     * 数据处理：
     * 校验通过后使用乐观锁（WHERE status=PENDING）更新状态，防止并发。
     *
     * 业务规则：
     * 1. 仅订单主人可取消
     * 2. 仅PENDING状态的订单可取消
     * 3. 取消后释放已锁定的优惠券和会员权益
     *
     * 状态影响：
     * PENDING → CANCELLED
     *
     * 异常情况：
     * 非订单主人抛403异常；状态非PENDING抛400异常；乐观锁更新失败抛400异常。
     *
     * @param ownerId 宠物主人用户ID
     * @param orderId 订单ID
     */
    void cancelOrderById(Long ownerId, Long orderId);

    /**
     * 【根据订单号取消订单（已废弃）】
     *
     * 业务作用：
     * 通过订单号取消订单，委托给cancelOrderById处理。
     *
     * 调用场景：
     * 已废弃，保留兼容旧版调用。
     *
     * 调用链：
     * Controller（旧版本）
     * ↓
     * cancelOrder(ownerId, orderNo)
     * ↓
     * getByOrderNo() → cancelOrderById()
     *
     * 业务规则：
     * 先根据订单号查询订单实体，再委托给cancelOrderById处理。
     *
     * 状态影响：
     * PENDING → CANCELLED
     *
     * 注意事项：
     * 已废弃，请使用cancelOrderById替代。
     *
     * @deprecated 请使用 {@link #cancelOrderById(Long, Long)} 替代
     * @param ownerId 宠物主人用户ID
     * @param orderNo 订单编号
     */
    @Deprecated
    void cancelOrder(Long ownerId, String orderNo);
    /**
     * 【确认接走（完成订单）】
     *
     * 业务作用：
     * keeper 输入独立接走码后固定实际接走时刻，按冻结快照计算晚接费：
     * 0 费直接完成并释放容量；有费则创建唯一补费单，等主人余额支付成功后完成。
     * 重复提交同一接走码返回原冻结结果（不重新计时、不生成第二笔补费单）。
     *
     * 业务规则：
     * 1. 仅看护者或商家可操作，且必须在岗（已确认的重复请求不要求在岗）
     * 2. 仅 IN_PROGRESS 状态可确认接走
     * 3. 接走码必须为 4 位数字且与本单接走码一致；送达码不能用于完成订单
     * 4. 实际接走时刻与金额全部由服务端计算，不接受前端提供
     *
     * 状态影响：
     * IN_PROGRESS → COMPLETED（0 费），或保持 IN_PROGRESS 并生成待补费单
     *
     * @param userId  用户ID（必须是订单的看护者或商家）
     * @param request 订单号 + 独立接走码
     * @return 接走结果（冻结时间、晚接小时、补费金额、补费单号、是否已完成）
     */
    OrderCheckoutDTO completeOrder(Long userId, OrderCompleteRequestDTO request);

    /**
     * 【晚接补费支付成功回调（受控内部方法）】
     *
     * 业务作用：
     * 补费支付成功后把补费金额一次性累计进订单金额并完成订单。
     * 仅供支付模块在补费支付成功后调用，不暴露为 HTTP 接口。
     *
     * @param orderId   订单ID
     * @param paymentId 补费支付单ID
     */
    void applyOvertimePayment(Long orderId, Long paymentId);

    /**
     * 【待分配订单池（看护者视角）】
     *
     * 业务作用：列出当前看护者所属商家名下、已支付但尚未分配负责人的订单，供其抢单。
     *
     * @param userId 当前登录用户ID
     * @return 可抢订单列表；未绑定商家时返回空列表
     */
    List<OrderDTO> listAssignableForKeeper(Long userId);

    /**
     * 【看护者抢单】
     *
     * 业务作用：看护者主动领取本商家名下待分配订单，成功后订单进入 CONFIRMED。
     *
     * 业务规则：仅该商家名下看护者可抢；仅 PAID 且 keeper_id 为空的订单可抢；
     * 抢单前重新校验资质、休假与容量；CAS 更新保证同一订单只被一人抢到。
     *
     * @param userId  看护者对应的用户账号ID
     * @param orderNo 订单编号
     */
    void claimOrder(Long userId, String orderNo);

    /**
     * 【商家指派负责人】
     *
     * 业务作用：商家管理员把待分配订单指派给名下看护者，成功后订单进入 CONFIRMED。
     *
     * 业务规则：仅订单所属商家的管理员可指派；指派前重新校验资质、休假与容量。
     *
     * @param userId  商家管理员用户ID
     * @param orderNo 订单编号
     * @param keeperId 目标看护者ID
     */
    void assignOrder(Long userId, String orderNo, Long keeperId);

    /**
     * 【拒绝订单（拒单）】
     *
     * 业务作用：
     * 看护者或商家拒绝已支付的订单，系统自动全额退款并回冲平台补贴，释放优惠券和会员权益。
     *
     * 调用场景：
     * 看护者/商家在"待处理订单"中点击"拒单"。
     *
     * 调用链：
     * 看护者端/商家端/Controller
     * ↓
     * rejectOrder(userId, orderNo)
     * ↓
     * 校验权限/状态 → 乐观锁更新CANCELLED → 退款(财务转账) → 回冲补贴 → 释放券/会员 → 广播
     *
     * 数据处理：
     * 全额退款(final_amount)从系统账户转给主人，回冲平台补贴(subsidy)。
     *
     * 业务规则：
     * 1. 看护者或商家均可拒单
     * 2. 仅PAID状态可拒单
     * 3. 拒单自动触发全额退款
     * 4. 释放已锁定的优惠券和会员权益
     *
     * 状态影响：
     * PAID → CANCELLED（含退款流程）
     *
     * 异常情况：
     * 权限不足抛403；状态非PAID抛400。
     *
     * @param userId  用户ID（必须是订单的看护者或商家）
     * @param orderNo 订单编号
     */
    void rejectOrder(Long userId, String orderNo);

    /**
     * 【自动接单（超时触发）】
     *
     * 业务作用：
     * 当看护者接单超时（超时窗口过期），系统自动将订单从PAID转为CONFIRMED。在新事务中执行，
     * 避免影响调用方的已有事务。
     *
     * 调用场景：
     * MQ延迟消息消费者（OrderAcceptTimeoutListener）在接单超时窗口到期后调用。
     *
     * 调用链：
     * OrderAcceptTimeoutListener
     * ↓
     * autoAcceptPaidOrderIfTimeout(orderNo)
     * ↓
     * [REQUIRES_NEW事务] → 查询订单 → confirmPaidOrder() → 更新CONFIRMED → 刷新容量 → 广播
     *
     * 数据处理：
     * 使用REQUIRES_NEW独立事务提交，避免与MQ消费事务相互干扰。
     *
     * 业务规则：
     * 1. 订单已不存在或非PAID状态时静默返回false
     * 2. 自动确认时同样检查资质、容量和请假状态
     *
     * 状态影响：
     * PAID → CONFIRMED（超时自动）
     *
     * 异常情况：
     * 校验失败仅记录日志，不抛异常。
     *
     * @param orderNo 待自动接单的订单编号
     * @return 自动接单成功返回true，否则false
     */
    boolean autoAcceptPaidOrderIfTimeout(String orderNo);

    /**
     * 【批量自动接单（定时任务兜底）】
     *
     * 业务作用：
     * 作为定时任务兜底方案，扫描所有已超时未接单的PAID订单并自动确认。
     * 弥补MQ延迟消息可能丢失或系统重启的场景。
     *
     * 调用场景：
     * OrderAcceptTimeoutScheduler定时任务（每分钟执行一次）。
     *
     * 调用链：
     * OrderAcceptTimeoutScheduler
     * ↓
     * autoAcceptPaidOrdersIfTimeout()
     * ↓
     * 查询超时支付记录 → 遍历订单 → confirmPaidOrder()逐个确认
     *
     * 数据处理：
     * 查询已成功支付且超过接单超时窗口的付款记录，关联订单后逐个处理。
     * 单次扫描上限为100条。
     *
     * 业务规则：
     * 单个订单处理失败不影响其他订单（捕获异常继续处理）。
     *
     * 状态影响：
     * PAID → CONFIRMED（批量自动）
     *
     * @return 自动接单成功的订单数量
     */
    int autoAcceptPaidOrdersIfTimeout();

    /**
     * 【取消超时未支付订单（单条触发）】
     *
     * 业务作用：
     * 当订单支付超时后自动取消订单（PENDING→CANCELLED），释放已锁定的优惠券和会员权益。
     * 在新事务中执行避免干扰调用方。
     *
     * 调用场景：
     * MQ延迟消息消费者（OrderPaymentTimeoutListener）在支付超时后调用。
     *
     * 调用链：
     * OrderPaymentTimeoutListener
     * ↓
     * cancelPendingOrderIfPaymentTimeout(orderNo)
     * ↓
     * [REQUIRES_NEW事务] → 查询订单 → cancelPaymentTimeoutOrder() → 释放券/会员 → 广播
     *
     * 数据处理：
     * 使用REQUIRES_NEW独立事务提交。
     *
     * 业务规则：
     * 订单为空、已支付或在超时窗口内创建时静默返回false。
     *
     * 状态影响：
     * PENDING → CANCELLED（支付超时）
     *
     * @param orderNo 待取消的订单编号
     * @return 取消成功返回true，否则false
     */
    boolean cancelPendingOrderIfPaymentTimeout(String orderNo);

    /**
     * 【批量取消超时未支付订单（定时任务兜底）】
     *
     * 业务作用：
     * 作为定时任务兜底方案，扫描所有超时未支付的PENDING订单并自动取消。
     * 弥补MQ延迟消息可能丢失或系统重启的场景。
     *
     * 调用场景：
     * OrderPaymentTimeoutScheduler定时任务（每分钟执行一次）。
     *
     * 调用链：
     * OrderPaymentTimeoutScheduler
     * ↓
     * cancelPaymentTimeoutOrders()
     * ↓
     * 查询超时PENDING订单 → 遍历 → cancelPaymentTimeoutOrder()逐个取消
     *
     * 数据处理：
     * 查询超过支付超时窗口的PENDING订单，按创建时间升序排列，单次上限100条。
     *
     * 业务规则：
     * 单个订单取消失败不影响其他订单。
     *
     * 状态影响：
     * PENDING → CANCELLED（批量超时）
     *
     * @return 取消的订单数量
     */
    int cancelPaymentTimeoutOrders();
    /**
     * 【标记订单已接收（看护者接宠）】
     *
     * 业务作用：
     * 看护者接收宠物时校验交接码与在岗状态，把订单推进到 RECEIVED。
     * 不校验 GPS 距离：寄养场景下宠物由主人送到看护者处，下单地址与
     * 实际交接地点可能不同（例如帮他人下单），距离无法作为有效凭证。
     *
     * 调用链：
     * 看护者端/Controller
     * ↓
     * markReceived(userId, request)
     * ↓
     * 校验权限/在岗/交接码 → 乐观锁更新 RECEIVED → 广播
     *
     * 业务规则：
     * 1. 仅看护者或商家可操作，且必须在岗
     * 2. 仅 CONFIRMED 状态可接收
     * 3. 交接码必须与订单的一致
     *
     * 状态影响：
     * CONFIRMED → RECEIVED
     *
     * @param userId  用户ID（必须是看护者或商家）
     * @param request 接收请求（交接码、接收地址文本）
     */
    void markReceived(Long userId, OrderReceivedRequestDTO request);
    /**
     * 【开始服务（开始照护）】
     *
     * 业务作用：
     * 看护者或商家在接收宠物后开始实际照护服务，拍摄宠物接收状态照片作为凭证。
     *
     * 调用场景：
     * 看护者/商家在接收宠物后点击"开始服务"。
     *
     * 调用链：
     * 看护者端/商家端/Controller
     * ↓
     * startService(userId, orderNo, startPhoto)
     * ↓
     * 校验权限/状态/在岗/照片 → 乐观锁更新IN_PROGRESS + 开始时间 + 照片 → 刷新容量 → 广播
     *
     * 数据处理：
     * 记录开始时间和开始照片URL到订单。
     *
     * 业务规则：
     * 1. 仅看护者或商家可操作，且必须在岗
     * 2. 仅RECEIVED状态可开始服务
     * 3. 开始照片不能为空
     *
     * 状态影响：
     * RECEIVED → IN_PROGRESS
     *
     * 异常情况：
     * 权限不足抛403；不在岗抛400；状态不对抛400；照片为空抛400。
     *
     * @param userId     用户ID（必须是看护者或商家）
     * @param orderNo    订单编号
     * @param startPhoto 服务开始时的宠物状态照片URL
     */
    void startService(Long userId, String orderNo, String startPhoto);

    /**
     * 【预校验开始服务权限】
     *
     * 业务作用：
     * 在用户点击"开始服务"前预校验操作权限，不实际变更订单状态。用于前端按钮权限控制。
     *
     * 调用场景：
     * 前端页面渲染时调用，判断是否显示"开始服务"按钮。
     *
     * 调用链：
     * 前端/Controller
     * ↓
     * validateStartServiceAccess(userId, orderNo)
     * ↓
     * 同startService校验逻辑（只读，不更新状态）
     *
     * 业务规则：
     * 校验逻辑与startService完全相同，但不修改数据库。
     *
     * 状态影响：
     * 只读操作，不修改业务状态。
     *
     * @param userId  要校验的用户ID
     * @param orderNo 订单编号
     */
    void validateStartServiceAccess(Long userId, String orderNo);

    /**
     * 【手动更新订单状态（通用状态修正接口）】
     *
     * 业务作用：
     * 提供通用的订单状态修正接口，带有安全防护：禁止直接操作涉及资金的状态（PAID/COMPLETED/CANCELLED等），
     * 仅允许在履约流程内修正（CONFIRMED→RECEIVED→IN_PROGRESS→COMPLETED）。
     *
     * 调用场景：
     * 管理后台或运营人员手动修正异常状态的订单。
     *
     * 调用链：
     * 管理后台/Controller
     * ↓
     * updateOrderStatus(orderNo, status)
     * ↓
     * 校验状态合法性 → 同状态直接返回 → 查 MANUAL_TRANSITIONS 转移表 → 冲突/容量检查
     * → 乐观锁更新 → 刷新看护者容量 → 广播
     *
     * 数据处理：
     * 根据目标状态自动记录对应的时间戳（接收/开始时间）。
     *
     * 业务规则：
     * 1. 禁止手动操作会计敏感状态（PAID/COMPLETED/CANCELLED/REFUNDING/REFUNDED）
     * 2. 仅允许在履约状态中流转
     * 3. 当进入预订状态时需检查宠物日期冲突和看护者容量
     * 4. 切换到COMPLETED时触发结算和完成事件
     * 5. 切换到CANCELLED时释放优惠券和会员权益
     *
     * 状态影响：
     * 根据传入的目标状态变更，影响范围受限。
     *
     * 异常情况：
     * 状态无效抛400；目标状态受保护抛400；乐观锁失败抛400。
     *
     * @param orderNo 订单编号
     * @param status  目标状态
     */
    void updateOrderStatus(String orderNo, String status);

    /**
     * 【订单实体转基础DTO】
     *
     * 业务作用：
     * 将订单实体转换为基础DTO（浅拷贝属性），不含关联实体信息。
     *
     * 调用场景：
     * 内部转换使用，当只需要订单基本字段时。
     *
     * 数据处理：
     * 使用BeanUtils.copyProperties进行属性拷贝。
     *
     * @param entity 订单实体（可为null）
     * @return 基础订单DTO，入参为null时返回null
     */
    OrderDTO toDTO(PetOrder entity);

    /**
     * 【订单实体转增强DTO】
     *
     * 业务作用：
     * 将订单实体转换为增强DTO，包含关联实体名称（服务、主人、宠物、看护者、商家）和订单快照。
     *
     * 调用场景：
     * 需要展示完整订单关联信息时使用。
     *
     * 调用链：
     * 内部方法
     * ↓
     * toDTOEnriched(entity)
     * ↓
     * toDTOEnrichedList(List.of(entity))
     *
     * 数据处理：
     * 委托给批量增强方法（避免N+1问题），加载关联实体名称。
     *
     * @param entity 订单实体（可为null）
     * @return 增强订单DTO，入参为null时返回null
     */
    OrderDTO toDTOEnriched(PetOrder entity);

    /**
     * 【删除订单（逻辑删除）】
     *
     * 业务作用：
     * 管理员后台删除订单记录。复用 MyBatis-Plus 的 @TableLogic 逻辑删除机制，
     * 仅将 deleted_wsh 置为 1，不物理删除记录，避免破坏支付/退款/履约/评价/财务等关联数据。
     *
     * 调用场景：
     * 管理端订单列表点击删除按钮时调用（DELETE /api/orders/{id}）。
     *
     * 数据处理：
     * 校验订单存在后调用 orderMapper.deleteById(id)。
     *
     * 业务规则：
     * 仅进行逻辑删除，不改变订单状态字段，不触发退款等资金操作。
     *
     * 异常情况：
     * 订单不存在时抛 BusinessException。
     *
     * @param id 订单ID
     */
    void delete(Long id);
    /**
     * 【价格预览】
     *
     * 业务作用：
     * 在不创建订单的前提下，返回与真实下单完全一致的金额明细与服务时间语义。
     *
     * 调用场景：
     * 用户在下单弹窗中选择宠物、时间、优惠券时实时询价（POST /api/orders/preview）。
     *
     * 业务规则：
     * 与 createOrder 共用同一套校验与计价代码，因此参数非法/时间冲突/价格变更会抛同样的业务异常。
     *
     * 数据处理：
     * 只读，不产生任何写副作用（不建单、不锁券、不排超时、不发广播）。
     *
     * @param ownerId 当前登录用户ID
     * @param request 预览请求
     * @return 金额明细与规范化后的服务时间
     */
    OrderQuoteDTO previewOrder(Long ownerId, OrderPreviewRequestDTO request);

}


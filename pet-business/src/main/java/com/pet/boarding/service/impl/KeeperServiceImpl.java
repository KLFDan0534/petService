package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.KeeperCreateRequestDTO;
import com.pet.boarding.dto.KeeperUpdateRequestDTO;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.vo.KeeperVO;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.StatusCode;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.qualification.service.QualificationService;
import com.pet.system.entity.Role;
import com.pet.system.entity.UserRole;
import com.pet.system.mapper.RoleMapper;
import com.pet.system.mapper.UserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KeeperServiceImpl implements KeeperService {

    private static final Set<String> BLOCKING_ORDER_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.PAID,
            OrderStatus.CONFIRMED,
            OrderStatus.DELIVERED,
            OrderStatus.RECEIVED,
            OrderStatus.IN_PROGRESS,
            OrderStatus.REFUNDING);

    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final QualificationService qualificationService;
    private final OrderMapper orderMapper;

    public KeeperServiceImpl(KeeperMapper keeperMapper, MerchantMapper merchantMapper,
                             RoleMapper roleMapper, UserRoleMapper userRoleMapper,
                             QualificationService qualificationService,
                             OrderMapper orderMapper) {
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.qualificationService = qualificationService;
        this.orderMapper = orderMapper;
    }

    @Override
    public List<Keeper> listAll() {
        log.info("listAll() called");
        return keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>().orderByDesc(Keeper::getCreated_at_wsh));
    }

    @Override
    public List<Keeper> listPending() {
        log.info("listPending() called");
        return keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>().eq(Keeper::getStatus_wsh, StatusCode.KEEPER_PENDING.getValue()));
    }

    @Override
    public Keeper getById(Long id) {
        log.info("getById() called");
        Keeper keeper = keeperMapper.selectById(id);
        if (keeper == null) {
            throw new BusinessException("瀵勫吇鍛樹笉瀛樺湪");
        }
        return keeper;
    }

    @Override
    public List<Keeper> listByIds(Collection<Long> ids) {
        log.info("listByIds() called");
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return keeperMapper.selectBatchIds(ids);
    }

    @Override
    public List<Keeper> findByMerchantId(Long merchantId) {
        log.info("findByMerchantId() called");
        return keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getMerchant_id_wsh, merchantId)
                        .eq(Keeper::getDeleted_wsh, 0));
    }

    @Override
    public List<KeeperVO> searchNearby(double lat, double lng, double radius) {
        log.info("searchNearby() called");
        List<Merchant> nearbyMerchants = merchantMapper.searchNearby(lat, lng, radius);
        if (nearbyMerchants.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Merchant> merchantMap = nearbyMerchants.stream()
                .collect(Collectors.toMap(Merchant::getId_wsh, m -> m));
        List<Long> merchantIds = nearbyMerchants.stream().map(Merchant::getId_wsh).collect(Collectors.toList());

        List<Keeper> keepers = keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>()
                        .in(Keeper::getMerchant_id_wsh, merchantIds)
                        .eq(Keeper::getStatus_wsh, StatusCode.KEEPER_ACTIVE.getValue()));

        return keepers.stream().map(k -> {
            KeeperVO vo = toKeeperVO(k);
            Merchant m = merchantMap.get(k.getMerchant_id_wsh());
            if (m != null) {
                vo.setMerchant_name_wsh(m.getName_wsh());
                vo.setMerchant_latitude_wsh(m.getLatitude_wsh());
                vo.setMerchant_longitude_wsh(m.getLongitude_wsh());
                vo.setDistance_wsh(calculateDistance(lat, lng,
                        m.getLatitude_wsh().doubleValue(), m.getLongitude_wsh().doubleValue()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Keeper create(KeeperCreateRequestDTO dto, Long userId) {
        log.info("create() called");
        if (dto.getMerchant_id_wsh() == null) {
            throw new BusinessException("蹇呴』閫夋嫨鎵€灞炲晢瀹舵墠鑳芥垚涓虹湅鎶や汉");
        }
        Merchant merchant = merchantMapper.selectById(dto.getMerchant_id_wsh());
        if (merchant == null) {
            throw new BusinessException("鎵€閫夊晢瀹朵笉瀛樺湪");
        }
        Keeper existing = keeperMapper.selectOne(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getUser_id_wsh, userId)
                        .orderByDesc(Keeper::getId_wsh)
                        .last("LIMIT 1"));
        if (existing != null) {
            if (existing.getStatus_wsh() != StatusCode.KEEPER_REJECTED.getValue()
                    && existing.getStatus_wsh() != StatusCode.KEEPER_RESIGNED.getValue()
                    && existing.getStatus_wsh() != StatusCode.KEEPER_TERMINATED.getValue()) {
                throw new BusinessException("鎮ㄥ凡鏈夊緟瀹℃牳鎴栧凡閫氳繃鐨勫瘎鍏诲憳鐢宠锛屼笉鑳介噸澶嶇敵璇?");
            }
            existing.setMerchant_id_wsh(dto.getMerchant_id_wsh());
            existing.setName_wsh(dto.getName_wsh());
            existing.setPhone_wsh(dto.getPhone_wsh());
            existing.setAvatar_wsh(dto.getAvatar_wsh());
            existing.setExperience_years_wsh(dto.getExperience_years_wsh());
            existing.setPrice_per_day_wsh(dto.getPrice_per_day_wsh());
            existing.setMax_pets_wsh(dto.getMax_pets_wsh());
            existing.setBio_wsh(dto.getBio_wsh());
            existing.setStatus_wsh(StatusCode.KEEPER_PENDING.getValue());
            existing.setRating_wsh(java.math.BigDecimal.valueOf(5.0));
            keeperMapper.updateById(existing);
            qualificationService.createOrUpdatePending(
                    QualificationService.OWNER_TYPE_KEEPER,
                    existing.getId_wsh(),
                    userId,
                    QualificationService.QUAL_TYPE_KEEPER_CERTIFICATE,
                    "鐪嬫姢璧勮川",
                    dto.getQualification_image_wsh(),
                    "瀵勫吇鍛樼敵璇疯祫璐?");
            return existing;
        }
        Keeper keeper = new Keeper();
        keeper.setUser_id_wsh(userId);
        keeper.setMerchant_id_wsh(dto.getMerchant_id_wsh());
        keeper.setName_wsh(dto.getName_wsh());
        keeper.setPhone_wsh(dto.getPhone_wsh());
        keeper.setAvatar_wsh(dto.getAvatar_wsh());
        keeper.setExperience_years_wsh(dto.getExperience_years_wsh());
        keeper.setPrice_per_day_wsh(dto.getPrice_per_day_wsh());
        keeper.setMax_pets_wsh(dto.getMax_pets_wsh());
        keeper.setBio_wsh(dto.getBio_wsh());
        keeper.setStatus_wsh(StatusCode.KEEPER_PENDING.getValue());
        keeper.setRating_wsh(java.math.BigDecimal.valueOf(5.0));
        keeperMapper.insert(keeper);
        qualificationService.createOrUpdatePending(
                QualificationService.OWNER_TYPE_KEEPER,
                keeper.getId_wsh(),
                userId,
                QualificationService.QUAL_TYPE_KEEPER_CERTIFICATE,
                "鐪嬫姢璧勮川",
                dto.getQualification_image_wsh(),
                "瀵勫吇鍛樼敵璇疯祫璐?");
        return keeper;
    }

    @Override
    @Transactional
    public Keeper update(Long id, KeeperUpdateRequestDTO dto) {
        log.info("update() called");
        Keeper existing = getById(id);
        if (dto.getName_wsh() != null) existing.setName_wsh(dto.getName_wsh());
        if (dto.getPhone_wsh() != null) existing.setPhone_wsh(dto.getPhone_wsh());
        if (dto.getAvatar_wsh() != null) existing.setAvatar_wsh(dto.getAvatar_wsh());
        if (dto.getExperience_years_wsh() != null) existing.setExperience_years_wsh(dto.getExperience_years_wsh());
        if (dto.getPrice_per_day_wsh() != null) existing.setPrice_per_day_wsh(dto.getPrice_per_day_wsh());
        if (dto.getMax_pets_wsh() != null) existing.setMax_pets_wsh(dto.getMax_pets_wsh());
        if (dto.getBio_wsh() != null) existing.setBio_wsh(dto.getBio_wsh());
        keeperMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("delete() called");
        Keeper keeper = getById(id);
        keeperMapper.deleteById(id);
        revokeKeeperRole(keeper.getUser_id_wsh());
    }

    @Override
    @Transactional
    public void resign(Long id, Long userId) {
        log.info("resign() called");
        Keeper keeper = getById(id);
        if (keeper.getUser_id_wsh() == null || !keeper.getUser_id_wsh().equals(userId)) {
            throw new BusinessException(403, "Only the keeper can resign this profile");
        }
        assertEmploymentActionAllowed(keeper);
        keeper.setStatus_wsh(StatusCode.KEEPER_RESIGNED.getValue());
        keeperMapper.updateById(keeper);
        revokeKeeperRole(keeper.getUser_id_wsh());
    }

    @Override
    @Transactional
    public void terminateByMerchant(Long id, Long merchantUserId) {
        log.info("terminateByMerchant() called");
        Keeper keeper = getById(id);
        Merchant merchant = requireMerchantByUserId(merchantUserId);
        if (!merchant.getId_wsh().equals(keeper.getMerchant_id_wsh())) {
            throw new BusinessException(403, "Only the owning merchant can terminate this keeper");
        }
        assertEmploymentActionAllowed(keeper);
        keeper.setStatus_wsh(StatusCode.KEEPER_TERMINATED.getValue());
        keeperMapper.updateById(keeper);
        revokeKeeperRole(keeper.getUser_id_wsh());
    }

    @Override
    @Transactional
    public void approve(Long id) {
        log.info("approve() called");
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_PENDING.getValue()) {
            throw new BusinessException("璇ョ湅鎶や汉涓嶅湪寰呭鏍哥姸鎬?");
        }
        keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        keeperMapper.updateById(keeper);
        grantKeeperRole(keeper.getUser_id_wsh());
    }

    @Override
    @Transactional
    public void reject(Long id) {
        log.info("reject() called");
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_PENDING.getValue()) {
            throw new BusinessException("璇ョ湅鎶や汉涓嶅湪寰呭鏍哥姸鎬?");
        }
        keeper.setStatus_wsh(StatusCode.KEEPER_REJECTED.getValue());
        keeperMapper.updateById(keeper);
    }

    @Override
    @Transactional
    public void setOnlineStatus(Long id, int status) {
        log.info("setOnlineStatus() called");
        if (status != StatusCode.KEEPER_ACTIVE.getValue() && status != StatusCode.KEEPER_OFFLINE.getValue()
                && status != StatusCode.KEEPER_BUSY.getValue()) {
            throw new BusinessException("鏃犳晥鐨勫湪绾跨姸鎬?");
        }
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_ACTIVE.getValue() && keeper.getStatus_wsh() != StatusCode.KEEPER_OFFLINE.getValue()
                && keeper.getStatus_wsh() != StatusCode.KEEPER_BUSY.getValue()) {
            throw new BusinessException("褰撳墠鐘舵€佷笉鍙垏鎹㈠湪绾跨姸鎬?");
        }
        if (keeper.getStatus_wsh() == StatusCode.KEEPER_BUSY.getValue()) {
            throw new BusinessException("蹇欑鐘舵€佷笉鍙墜鍔ㄥ垏鎹紝璇风瓑寰呭綋鍓嶈鍗曞畬鎴愬悗鑷姩鎭㈠");
        }
        keeper.setStatus_wsh(status);
        keeperMapper.updateById(keeper);
    }

    @Override
    @Transactional
    public void syncMerchantStoreStatus(Long merchantId, boolean storeOpen) {
        log.info("syncMerchantStoreStatus() called, merchantId={}, storeOpen={}", merchantId, storeOpen);
        List<Keeper> keepers = keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getMerchant_id_wsh, merchantId)
                        .eq(Keeper::getDeleted_wsh, 0));
        for (Keeper keeper : keepers) {
            Integer currentStatus = keeper.getStatus_wsh();
            Integer targetStatus = currentStatus;
            if (storeOpen) {
                if (currentStatus == null || currentStatus == StatusCode.KEEPER_OFFLINE.getValue()) {
                    targetStatus = StatusCode.KEEPER_ACTIVE.getValue();
                }
            } else {
                if (currentStatus == null || currentStatus == StatusCode.KEEPER_ACTIVE.getValue()) {
                    targetStatus = StatusCode.KEEPER_OFFLINE.getValue();
                }
            }
            if (targetStatus != null && !targetStatus.equals(currentStatus)) {
                keeper.setStatus_wsh(targetStatus);
                keeperMapper.updateById(keeper);
            }
        }
    }

    @Override
    public KeeperVO toDTO(Keeper entity) {
        if (entity == null) return null;
        return toKeeperVO(entity);
    }

    @Override
    public Keeper findByUserId(Long userId) {
        log.info("findByUserId() called");
        return keeperMapper.selectOne(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getUser_id_wsh, userId)
                        .orderByDesc(Keeper::getId_wsh)
                        .last("LIMIT 1"));
    }

    @Override
    public List<Keeper> listPendingByMerchant(Long merchantUserId) {
        log.info("listPendingByMerchant() called");
        Merchant merchant = requireMerchantByUserId(merchantUserId);
        if (merchant == null) {
            throw new BusinessException("鎮ㄦ病鏈夊晢瀹朵俊鎭?");
        }
        return keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getMerchant_id_wsh, merchant.getId_wsh())
                        .eq(Keeper::getStatus_wsh, StatusCode.KEEPER_PENDING.getValue()));
    }

    @Override
    @Transactional
    public void approveByMerchant(Long id, Long merchantUserId) {
        log.info("approveByMerchant() called");
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_PENDING.getValue()) {
            throw new BusinessException("璇ョ湅鎶や汉涓嶅湪寰呭鏍哥姸鎬?");
        }
        Merchant merchant = requireMerchantByUserId(merchantUserId);
        if (!merchant.getId_wsh().equals(keeper.getMerchant_id_wsh())) {
            throw new BusinessException("鏃犳潈瀹℃牳姝ゅ瘎鍏诲憳鐢宠");
        }
        keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        keeperMapper.updateById(keeper);
        grantKeeperRole(keeper.getUser_id_wsh());
    }

    @Override
    @Transactional
    public void rejectByMerchant(Long id, Long merchantUserId) {
        log.info("rejectByMerchant() called");
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_PENDING.getValue()) {
            throw new BusinessException("璇ョ湅鎶や汉涓嶅湪寰呭鏍哥姸鎬?");
        }
        Merchant merchant = requireMerchantByUserId(merchantUserId);
        if (!merchant.getId_wsh().equals(keeper.getMerchant_id_wsh())) {
            throw new BusinessException("鏃犳潈瀹℃牳姝ゅ瘎鍏诲憳鐢宠");
        }
        keeper.setStatus_wsh(StatusCode.KEEPER_REJECTED.getValue());
        keeperMapper.updateById(keeper);
    }

    private void assertEmploymentActionAllowed(Keeper keeper) {
        Integer status = keeper.getStatus_wsh();
        if (status == null
                || (status != StatusCode.KEEPER_ACTIVE.getValue()
                && status != StatusCode.KEEPER_OFFLINE.getValue()
                && status != StatusCode.KEEPER_BUSY.getValue())) {
            throw new BusinessException(400, "Keeper status does not allow resignation or termination");
        }
        Long blockingOrders = orderMapper.selectCount(new LambdaQueryWrapper<PetOrder>()
                .eq(PetOrder::getKeeper_id_wsh, keeper.getId_wsh())
                .in(PetOrder::getStatus_wsh, BLOCKING_ORDER_STATUSES));
        if (blockingOrders != null && blockingOrders > 0) {
            throw new BusinessException(400, "Keeper has unfinished orders");
        }
    }

    private Merchant requireMerchantByUserId(Long merchantUserId) {
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getUser_id_wsh, merchantUserId)
                        .last("LIMIT 1"));
        if (merchant == null) {
            throw new BusinessException(404, "Merchant not found");
        }
        return merchant;
    }

    private void grantKeeperRole(Long userId) {
        Role keeperRole = findKeeperRole();
        if (keeperRole != null && userId != null) {
            LambdaQueryWrapper<UserRole> qw = new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUser_id_wsh, userId)
                    .eq(UserRole::getRole_id_wsh, keeperRole.getId_wsh());
            if (userRoleMapper.selectCount(qw) == 0) {
                if (userRoleMapper.restoreUserRole(userId, keeperRole.getId_wsh()) > 0) {
                    return;
                }
                UserRole ur = new UserRole();
                ur.setUser_id_wsh(userId);
                ur.setRole_id_wsh(keeperRole.getId_wsh());
                userRoleMapper.insert(ur);
            }
        }
    }

    private void revokeKeeperRole(Long userId) {
        Role keeperRole = findKeeperRole();
        if (keeperRole != null && userId != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUser_id_wsh, userId)
                    .eq(UserRole::getRole_id_wsh, keeperRole.getId_wsh()));
        }
    }

    private Role findKeeperRole() {
        return roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode_wsh, "KEEPER"));
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lng1) - Math.toRadians(lng2);
        double s = 2 * Math.asin(Math.sqrt(
                Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        return s * 6371;
    }

    private KeeperVO toKeeperVO(Keeper k) {
        KeeperVO vo = new KeeperVO();
        vo.setId_wsh(k.getId_wsh());
        vo.setMerchant_id_wsh(k.getMerchant_id_wsh());
        vo.setUser_id_wsh(k.getUser_id_wsh());
        vo.setName_wsh(k.getName_wsh());
        vo.setPhone_wsh(k.getPhone_wsh());
        vo.setAvatar_wsh(k.getAvatar_wsh());
        vo.setExperience_years_wsh(k.getExperience_years_wsh());
        vo.setRating_wsh(k.getRating_wsh());
        vo.setCompletion_rate_wsh(k.getCompletion_rate_wsh());
        vo.setComplaint_rate_wsh(k.getComplaint_rate_wsh());
        vo.setPrice_per_day_wsh(k.getPrice_per_day_wsh());
        vo.setMax_pets_wsh(k.getMax_pets_wsh());
        vo.setCurrent_pets_wsh(k.getCurrent_pets_wsh());
        vo.setBio_wsh(k.getBio_wsh());
        vo.setStatus_wsh(k.getStatus_wsh());
        vo.setQualifications_wsh(qualificationService.listByOwner(
                QualificationService.OWNER_TYPE_KEEPER, k.getId_wsh(), true));
        return vo;
    }
}

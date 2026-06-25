package com.pet.boarding.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.vo.KeeperVO;
import com.pet.system.entity.Role;
import com.pet.system.entity.UserRole;
import com.pet.system.mapper.RoleMapper;
import com.pet.system.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 看护者服务实现
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class KeeperServiceImpl implements KeeperService {

    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public KeeperServiceImpl(KeeperMapper keeperMapper, MerchantMapper merchantMapper,
                             RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 获取所有看护者列表
     * @return 看护者列表
     */
    public List<Keeper> listAll() {
        log.info("listAll() called");
        return keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>().orderByDesc(Keeper::getCreated_at_wsh));
    }

    /**
     * 获取待审核的看护者列表
     * @return 待审核看护者列表
     */
    public List<Keeper> listPending() {
        log.info("listPending() called");
        return keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>().eq(Keeper::getStatus_wsh, StatusCode.KEEPER_PENDING.getValue()));
    }

    /**
     * 根据ID获取看护者
     * @param id 看护者ID
     * @return 看护者实体
     */
    public Keeper getById(Long id) {
        log.info("getById() called");
        Keeper keeper = keeperMapper.selectById(id);
        if (keeper == null) {
            throw new BusinessException("寄养员不存在");
        }
        return keeper;
    }

    /**
     * 根据商家ID获取看护者列表
     * @param merchantId 商家ID
     * @return 看护者列表
     */
    public List<Keeper> findByMerchantId(Long merchantId) {
        log.info("findByMerchantId() called");
        return keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getMerchant_id_wsh, merchantId)
                        .eq(Keeper::getDeleted_wsh, 0));
    }

    /**
     * 搜索附近商家下的看护者
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（公里）
     * @return 附近看护者视图列表
     */
    public List<KeeperVO> searchNearby(double lat, double lng, double radius) {
        log.info("searchNearby() called");
        List<Merchant> nearbyMerchants = merchantMapper.searchNearby(lat, lng, radius);
        if (nearbyMerchants.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Merchant> merchantMap = nearbyMerchants.stream()
                .collect(Collectors.toMap(Merchant::getId_wsh, m -> m));
        List<Long> merchantIds = nearbyMerchants.stream()
                .map(Merchant::getId_wsh).collect(Collectors.toList());

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

    /**
     * 创建看护者，初始状态为待审核
     * @param keeper 看护者实体
     * @return 创建后的看护者
     */
    @Transactional
    public Keeper create(Keeper keeper) {
        log.info("create() called");
        if (keeper.getMerchant_id_wsh() == null) {
            throw new BusinessException("必须选择所属商家才能成为看护人");
        }
        Merchant merchant = merchantMapper.selectById(keeper.getMerchant_id_wsh());
        if (merchant == null) {
            throw new BusinessException("所选商家不存在");
        }
        keeper.setStatus_wsh(StatusCode.KEEPER_PENDING.getValue());
        keeper.setRating_wsh(java.math.BigDecimal.valueOf(5.0));
        keeperMapper.insert(keeper);
        return keeper;
    }

    /**
     * 更新看护者信息（非空字段覆盖）
     * @param keeper 看护者实体
     * @return 更新后的看护者
     */
    @Transactional
    public Keeper update(Keeper keeper) {
        log.info("update() called");
        Keeper existing = getById(keeper.getId_wsh());
        if (keeper.getName_wsh() != null) existing.setName_wsh(keeper.getName_wsh());
        if (keeper.getPhone_wsh() != null) existing.setPhone_wsh(keeper.getPhone_wsh());
        if (keeper.getAvatar_wsh() != null) existing.setAvatar_wsh(keeper.getAvatar_wsh());
        if (keeper.getExperience_years_wsh() != null) existing.setExperience_years_wsh(keeper.getExperience_years_wsh());
        if (keeper.getPrice_per_day_wsh() != null) existing.setPrice_per_day_wsh(keeper.getPrice_per_day_wsh());
        if (keeper.getMax_pets_wsh() != null) existing.setMax_pets_wsh(keeper.getMax_pets_wsh());
        if (keeper.getBio_wsh() != null) existing.setBio_wsh(keeper.getBio_wsh());
        keeperMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除看护者
     * @param id 看护者ID
     */
    @Transactional
    public void delete(Long id) {
        log.info("delete() called");
        getById(id);
        keeperMapper.deleteById(id);
    }

    /**
     * 审核通过看护者申请，自动分配看护者角色
     * @param id 看护者ID
     */
    @Transactional
    public void approve(Long id) {
        log.info("approve() called");
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_PENDING.getValue()) {
            throw new BusinessException("该看护人不在待审核状态");
        }
        keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        keeperMapper.updateById(keeper);
        Role keeperRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode_wsh, "KEEPER"));
        if (keeperRole != null && keeper.getUser_id_wsh() != null) {
            LambdaQueryWrapper<UserRole> qw = new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUser_id_wsh, keeper.getUser_id_wsh())
                    .eq(UserRole::getRole_id_wsh, keeperRole.getId_wsh());
            if (userRoleMapper.selectCount(qw) == 0) {
                UserRole ur = new UserRole();
                ur.setUser_id_wsh(keeper.getUser_id_wsh());
                ur.setRole_id_wsh(keeperRole.getId_wsh());
                userRoleMapper.insert(ur);
            }
        }
    }

    /**
     * 驳回看护者申请
     * @param id 看护者ID
     */
    @Transactional
    public void reject(Long id) {
        log.info("reject() called");
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_PENDING.getValue()) {
            throw new BusinessException("该看护人不在待审核状态");
        }
        keeper.setStatus_wsh(StatusCode.KEEPER_REJECTED.getValue());
        keeperMapper.updateById(keeper);
    }

    /**
     * 设置看护者在线状态
     * @param id 看护者ID
     * @param status 状态值（1-在线，3-离线，4-忙碌）
     */
    @Transactional
    public void setOnlineStatus(Long id, int status) {
        log.info("setOnlineStatus() called");
        if (status != StatusCode.KEEPER_ACTIVE.getValue() && status != StatusCode.KEEPER_OFFLINE.getValue()
                && status != StatusCode.KEEPER_BUSY.getValue()) {
            throw new BusinessException("无效的在线状态");
        }
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_ACTIVE.getValue() && keeper.getStatus_wsh() != StatusCode.KEEPER_OFFLINE.getValue()
                && keeper.getStatus_wsh() != StatusCode.KEEPER_BUSY.getValue()) {
            throw new BusinessException("当前状态不可切换在线状态");
        }
        keeper.setStatus_wsh(status);
        keeperMapper.updateById(keeper);
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
        vo.setStatus_wsh(k.getStatus_wsh());
        return vo;
    }
}

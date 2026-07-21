package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.KeeperLeaveCreateRequestDTO;
import com.pet.boarding.dto.KeeperLeaveDTO;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.KeeperLeave;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperLeaveMapper;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperLeaveService;
import com.pet.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class KeeperLeaveServiceImpl implements KeeperLeaveService {

    private final KeeperLeaveMapper leaveMapper;
    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;

    public KeeperLeaveServiceImpl(KeeperLeaveMapper leaveMapper,
                                  KeeperMapper keeperMapper,
                                  MerchantMapper merchantMapper) {
        this.leaveMapper = leaveMapper;
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
    }

    @Override
    public List<KeeperLeaveDTO> listByMerchant(Long merchantUserId) {
        Merchant merchant = requireMerchantByUser(merchantUserId);
        List<KeeperLeave> leaves = leaveMapper.selectList(
                new LambdaQueryWrapper<KeeperLeave>()
                        .eq(KeeperLeave::getMerchant_id_wsh, merchant.getId_wsh())
                        .orderByAsc(KeeperLeave::getStart_date_wsh)
                        .orderByAsc(KeeperLeave::getId_wsh));
        Map<Long, Keeper> keeperMap = loadKeeperMap(leaves);
        return leaves.stream()
                .map(leave -> toDTO(leave, keeperMap.get(leave.getKeeper_id_wsh()), merchant))
                .toList();
    }

    @Transactional
    @Override
    public KeeperLeaveDTO createByMerchant(Long merchantUserId, KeeperLeaveCreateRequestDTO request) {
        Merchant merchant = requireMerchantByUser(merchantUserId);
        validateRequest(request);
        Keeper keeper = requireMerchantKeeper(merchant.getId_wsh(), request.getKeeper_id_wsh());
        if (hasLeaveOverlap(keeper.getId_wsh(), request.getStart_date_wsh(), request.getEnd_date_wsh().plusDays(1))) {
            throw new BusinessException(400, "所选日期已存在休假安排");
        }
        KeeperLeave leave = new KeeperLeave();
        leave.setKeeper_id_wsh(keeper.getId_wsh());
        leave.setMerchant_id_wsh(merchant.getId_wsh());
        leave.setStart_date_wsh(request.getStart_date_wsh());
        leave.setEnd_date_wsh(request.getEnd_date_wsh());
        leave.setReason_wsh(trimToNull(request.getReason_wsh()));
        leave.setCreated_by_wsh(merchantUserId);
        leaveMapper.insert(leave);
        return toDTO(leave, keeper, merchant);
    }

    @Transactional
    @Override
    public void deleteByMerchant(Long merchantUserId, Long id) {
        Merchant merchant = requireMerchantByUser(merchantUserId);
        KeeperLeave leave = leaveMapper.selectById(id);
        if (leave == null) {
            throw new BusinessException(404, "休假记录不存在");
        }
        if (!merchant.getId_wsh().equals(leave.getMerchant_id_wsh())) {
            throw new BusinessException(403, "无权删除此休假记录");
        }
        leaveMapper.deleteById(id);
    }

    @Override
    public boolean isKeeperOnLeave(Long keeperId, LocalDate date) {
        if (keeperId == null || date == null) {
            return false;
        }
        KeeperLeave leave = leaveMapper.selectOne(
                new LambdaQueryWrapper<KeeperLeave>()
                        .eq(KeeperLeave::getKeeper_id_wsh, keeperId)
                        .le(KeeperLeave::getStart_date_wsh, date)
                        .ge(KeeperLeave::getEnd_date_wsh, date)
                        .last("LIMIT 1"));
        return leave != null;
    }

    @Override
    public boolean hasLeaveOverlap(Long keeperId, LocalDate startDate, LocalDate endExclusive) {
        if (keeperId == null || startDate == null || endExclusive == null) {
            return false;
        }
        LocalDate endInclusive = endExclusive.minusDays(1);
        if (endInclusive.isBefore(startDate)) {
            endInclusive = startDate;
        }
        KeeperLeave leave = leaveMapper.selectOne(
                new LambdaQueryWrapper<KeeperLeave>()
                        .eq(KeeperLeave::getKeeper_id_wsh, keeperId)
                        .le(KeeperLeave::getStart_date_wsh, endInclusive)
                        .ge(KeeperLeave::getEnd_date_wsh, startDate)
                        .last("LIMIT 1"));
        return leave != null;
    }

    @Override
    public void requireKeeperAvailable(Long keeperId, LocalDate startDate, LocalDate endExclusive) {
        if (hasLeaveOverlap(keeperId, startDate, endExclusive)) {
            throw new BusinessException(400, "看护者在所选服务日期内有休假安排");
        }
    }

    private Merchant requireMerchantByUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getUser_id_wsh, userId)
                        .last("LIMIT 1"));
        if (merchant == null) {
            throw new BusinessException(404, "商家不存在");
        }
        return merchant;
    }

    private Keeper requireMerchantKeeper(Long merchantId, Long keeperId) {
        Keeper keeper = keeperMapper.selectById(keeperId);
        if (keeper == null) {
            throw new BusinessException(404, "看护者不存在");
        }
        if (!merchantId.equals(keeper.getMerchant_id_wsh())) {
            throw new BusinessException(403, "只能管理本商家的看护者休假");
        }
        return keeper;
    }

    private void validateRequest(KeeperLeaveCreateRequestDTO request) {
        if (request == null) {
            throw new BusinessException(400, "休假信息不能为空");
        }
        if (request.getKeeper_id_wsh() == null) {
            throw new BusinessException(400, "看护者不能为空");
        }
        if (request.getStart_date_wsh() == null || request.getEnd_date_wsh() == null) {
            throw new BusinessException(400, "休假日期不能为空");
        }
        if (request.getEnd_date_wsh().isBefore(request.getStart_date_wsh())) {
            throw new BusinessException(400, "结束日期不能早于开始日期");
        }
    }

    private Map<Long, Keeper> loadKeeperMap(List<KeeperLeave> leaves) {
        Set<Long> keeperIds = leaves.stream()
                .map(KeeperLeave::getKeeper_id_wsh)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (keeperIds.isEmpty()) {
            return Map.of();
        }
        return keeperMapper.selectList(new LambdaQueryWrapper<Keeper>().in(Keeper::getId_wsh, keeperIds))
                .stream()
                .collect(Collectors.toMap(Keeper::getId_wsh, Function.identity()));
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private KeeperLeaveDTO toDTO(KeeperLeave entity, Keeper keeper, Merchant merchant) {
        KeeperLeaveDTO dto = new KeeperLeaveDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setKeeper_id_wsh(entity.getKeeper_id_wsh());
        dto.setMerchant_id_wsh(entity.getMerchant_id_wsh());
        dto.setKeeper_name_wsh(keeper == null ? null : keeper.getName_wsh());
        dto.setMerchant_name_wsh(merchant == null ? null : merchant.getName_wsh());
        dto.setStart_date_wsh(entity.getStart_date_wsh());
        dto.setEnd_date_wsh(entity.getEnd_date_wsh());
        dto.setReason_wsh(entity.getReason_wsh());
        dto.setCreated_by_wsh(entity.getCreated_by_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        dto.setUpdated_at_wsh(entity.getUpdated_at_wsh());
        return dto;
    }
}

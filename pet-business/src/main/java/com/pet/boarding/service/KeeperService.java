package com.pet.boarding.service;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.vo.KeeperVO;

import java.util.List;

/**
 * 看护者服务接口
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
public interface KeeperService {
    /**
     * 获取所有看护人列表
     * @return 看护人列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Keeper> listAll();
    /**
     * 获取待审核看护人列表
     * @return 待审核看护人列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Keeper> listPending();
    /**
     * 根据ID获取看护人
     * @param id 看护人ID
     * @return 看护人实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Keeper getById(Long id);
    /**
     * 根据商家ID获取看护人列表
     * @param merchantId 商家ID
     * @return 看护人列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Keeper> findByMerchantId(Long merchantId);
    /**
     * 搜索附近的看护人
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径
     * @return 附近的看护人视图列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<KeeperVO> searchNearby(double lat, double lng, double radius);
    /**
     * 创建看护人
     * @param keeper 看护人实体
     * @return 创建后的看护人
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Keeper create(Keeper keeper);
    /**
     * 更新看护人信息
     * @param keeper 看护人实体
     * @return 更新后的看护人
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Keeper update(Keeper keeper);
    /**
     * 删除看护人
     * @param id 看护人ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void delete(Long id);
    /**
     * 审核通过看护人
     * @param id 看护人ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void approve(Long id);
    /**
     * 驳回看护人审核
     * @param id 看护人ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void reject(Long id);
    /**
     * 设置看护人在线状态
     * @param id 看护人ID
     * @param status 在线状态（0-离线，1-在线）
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void setOnlineStatus(Long id, int status);
}


package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.ServiceMedia;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 服务产品图片（ServiceMedia）数据访问层。
 * <p>
 * 映射表：pet_service_media_wsh
 */
@Mapper
public interface ServiceMediaMapper extends BaseMapper<ServiceMedia> {

    /**
     * 物理删除某个服务的全部媒体行（替换语义下旧图册整体删除后重新插入）。
     */
    @Delete("DELETE FROM pet_service_media_wsh WHERE service_id_wsh = #{serviceId}")
    int deleteByServiceId(Long serviceId);

    /**
     * 查询某个服务的全部媒体行（接口内部按 sort_order_wsh 排序）。
     */
    @Select("SELECT * FROM pet_service_media_wsh WHERE service_id_wsh = #{serviceId} ORDER BY sort_order_wsh ASC")
    List<ServiceMedia> selectByServiceIdOrdered(Long serviceId);

    /**
     * 批量查询多个服务的全部媒体行（先按 service_id_wsh 再按 sort_order_wsh 排序）。
     */
    @Select("<script>"
            + "SELECT * FROM pet_service_media_wsh WHERE service_id_wsh IN "
            + "<foreach collection='serviceIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + " ORDER BY service_id_wsh ASC, sort_order_wsh ASC"
            + "</script>")
    List<ServiceMedia> selectByServiceIdsOrdered(Collection<Long> serviceIds);
}
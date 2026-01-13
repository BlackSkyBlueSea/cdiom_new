package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.InboundRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 入库记录Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface InboundRecordMapper extends BaseMapper<InboundRecord> {

    /**
     * 查询订单某个药品的已入库数量（合格入库）
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM inbound_record WHERE order_id = #{orderId} AND drug_id = #{drugId} AND status = 'QUALIFIED'")
    Integer getInboundQuantityByOrderAndDrug(Long orderId, Long drugId);

    /**
     * 查询今日入库数量
     */
    @Select("SELECT COUNT(*) FROM inbound_record WHERE create_time >= #{todayStart} AND create_time < #{todayEnd} AND status = 'QUALIFIED'")
    Long countTodayInbound(LocalDateTime todayStart, LocalDateTime todayEnd);
}


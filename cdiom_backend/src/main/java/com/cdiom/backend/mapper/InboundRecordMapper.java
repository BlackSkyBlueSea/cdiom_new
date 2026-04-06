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
    /**
     * 已确认入账的合格数量（计入可用库存、用于判断订单是否全部到货）
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM inbound_record WHERE order_id = #{orderId} AND drug_id = #{drugId} AND status = 'QUALIFIED' AND second_confirm_status = 'CONFIRMED'")
    Integer getInboundQuantityByOrderAndDrug(Long orderId, Long drugId);

    /**
     * 已从订单占用的到货数量：合格（已入账或待第二人确认）+ 不合格登记，防止超量；不含第二人驳回/撤回/超时关闭的合格单
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM inbound_record WHERE order_id = #{orderId} AND drug_id = #{drugId} AND (status = 'UNQUALIFIED' OR (status = 'QUALIFIED' AND second_confirm_status IN ('CONFIRMED', 'PENDING_SECOND')))")
    Integer getInboundCommittedQuantityByOrderAndDrug(Long orderId, Long drugId);

    /**
     * 查询今日入库数量（已第二人确认入账的合格入库）
     */
    @Select("SELECT COUNT(*) FROM inbound_record WHERE create_time >= #{todayStart} AND create_time < #{todayEnd} AND status = 'QUALIFIED' AND second_confirm_status = 'CONFIRMED'")
    Long countTodayInbound(LocalDateTime todayStart, LocalDateTime todayEnd);
}









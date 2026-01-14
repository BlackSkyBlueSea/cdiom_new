package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.OutboundApply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 出库申请Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface OutboundApplyMapper extends BaseMapper<OutboundApply> {

    /**
     * 查询待审批的出库申请数量
     */
    @Select("SELECT COUNT(*) FROM outbound_apply WHERE status = 'PENDING'")
    Long countPendingOutbound();

    /**
     * 查询今日出库数量
     */
    @Select("SELECT COUNT(*) FROM outbound_apply WHERE outbound_time >= #{todayStart} AND outbound_time < #{todayEnd} AND status = 'OUTBOUND'")
    Long countTodayOutbound(LocalDateTime todayStart, LocalDateTime todayEnd);
}




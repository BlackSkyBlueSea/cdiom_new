package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.OutboundApplyItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 出库申请明细Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface OutboundApplyItemMapper extends BaseMapper<OutboundApplyItem> {

    /**
     * 根据申请ID查询申请明细
     */
    @Select("SELECT * FROM outbound_apply_item WHERE apply_id = #{applyId}")
    List<OutboundApplyItem> selectByApplyId(Long applyId);
}





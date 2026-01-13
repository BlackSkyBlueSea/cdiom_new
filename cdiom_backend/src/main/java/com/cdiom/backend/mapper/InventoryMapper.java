package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 库存Mapper接口
 * 
 * @author cdiom
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 查询近效期预警（黄色预警：90-180天）
     */
    @Select("SELECT COUNT(*) FROM inventory WHERE expiry_date >= #{today} AND expiry_date <= #{yellowWarningDate} AND quantity > 0")
    Long countYellowWarning(LocalDate today, LocalDate yellowWarningDate);

    /**
     * 查询近效期预警（红色预警：≤90天）
     */
    @Select("SELECT COUNT(*) FROM inventory WHERE expiry_date >= #{today} AND expiry_date <= #{redWarningDate} AND quantity > 0")
    Long countRedWarning(LocalDate today, LocalDate redWarningDate);

    /**
     * 查询库存总量（所有批次的数量总和）
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM inventory WHERE quantity > 0")
    Long getTotalInventory();
}


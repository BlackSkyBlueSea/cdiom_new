package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.Inventory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

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
     * 黄色预警批次数量（与红色互斥：效期在严重预警天数之后、预警天数之内）
     */
    @Select("SELECT COUNT(*) FROM inventory WHERE quantity > 0 AND expiry_date > #{redBoundary} AND expiry_date <= #{yellowBoundary}")
    Long countYellowExclusive(@Param("redBoundary") LocalDate redBoundary, @Param("yellowBoundary") LocalDate yellowBoundary);

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

    /**
     * 查询某药品的可用库存总量（未过期且数量>0的批次合计）
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM inventory WHERE drug_id = #{drugId} AND quantity > 0 AND expiry_date >= #{today}")
    Integer getTotalAvailableQuantityByDrugId(@Param("drugId") Long drugId, @Param("today") LocalDate today);

    /**
     * 使用悲观锁查询库存（SELECT ... FOR UPDATE）
     * 用于并发安全的库存操作
     */
    @Select("SELECT * FROM inventory WHERE drug_id = #{drugId} AND batch_number = #{batchNumber} FOR UPDATE")
    Inventory selectForUpdate(@Param("drugId") Long drugId, @Param("batchNumber") String batchNumber);

    /**
     * 原子减少库存（使用数据库原子操作，防止并发问题）
     * 返回受影响的行数，如果为0表示库存不足或记录不存在
     */
    @Update("UPDATE inventory SET quantity = quantity - #{quantity} WHERE drug_id = #{drugId} AND batch_number = #{batchNumber} AND quantity >= #{quantity}")
    int decreaseQuantityAtomically(@Param("drugId") Long drugId, @Param("batchNumber") String batchNumber, @Param("quantity") Integer quantity);

    /**
     * 原子增加库存（使用数据库原子操作，防止并发问题）
     */
    @Update("UPDATE inventory SET quantity = quantity + #{quantity} WHERE drug_id = #{drugId} AND batch_number = #{batchNumber}")
    int increaseQuantityAtomically(@Param("drugId") Long drugId, @Param("batchNumber") String batchNumber, @Param("quantity") Integer quantity);

    /**
     * 插入或更新库存（使用 INSERT ... ON DUPLICATE KEY UPDATE）
     * 如果记录不存在则插入，存在则更新数量
     * 用于并发安全的入库操作
     */
    @Insert("INSERT INTO inventory (drug_id, batch_number, quantity, expiry_date, storage_location, production_date, manufacturer) " +
            "VALUES (#{drugId}, #{batchNumber}, #{quantity}, #{expiryDate}, #{storageLocation}, #{productionDate}, #{manufacturer}) " +
            "ON DUPLICATE KEY UPDATE quantity = quantity + #{quantity}")
    int insertOrUpdateInventory(@Param("drugId") Long drugId, 
                                @Param("batchNumber") String batchNumber, 
                                @Param("quantity") Integer quantity,
                                @Param("expiryDate") LocalDate expiryDate,
                                @Param("storageLocation") String storageLocation,
                                @Param("productionDate") LocalDate productionDate,
                                @Param("manufacturer") String manufacturer);

    /**
     * 使用JOIN查询优化N+1问题：根据关键字和特殊药品标识查询库存列表
     * 一次性JOIN查询，避免先查DrugInfo再查Inventory的多次查询
     * SQL 定义在 resources/mapper/InventoryMapper.xml 中，避免注解内 XML 解析问题
     */
    List<Inventory> selectInventoryListWithJoin(@Param("keyword") String keyword,
                                                 @Param("drugId") Long drugId,
                                                 @Param("batchNumber") String batchNumber,
                                                 @Param("storageLocation") String storageLocation,
                                                 @Param("expiryDateStart") LocalDate expiryDateStart,
                                                 @Param("expiryDateEnd") LocalDate expiryDateEnd,
                                                 @Param("isSpecial") Integer isSpecial);

    /**
     * 近效期明细（与仪表盘预警区间一致，level=red 或 yellow）
     */
    List<Inventory> selectNearExpiryDetailsWithJoin(@Param("today") LocalDate today,
                                                    @Param("redBoundary") LocalDate redBoundary,
                                                    @Param("yellowBoundary") LocalDate yellowBoundary,
                                                    @Param("level") String level);
}


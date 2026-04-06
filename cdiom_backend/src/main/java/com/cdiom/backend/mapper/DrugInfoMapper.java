package com.cdiom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdiom.backend.model.DrugInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 药品信息Mapper
 * 
 * @author cdiom
 */
@Mapper
public interface DrugInfoMapper extends BaseMapper<DrugInfo> {

    /**
     * 将逻辑删除的药品恢复为未删除（绕过 BaseMapper.updateById 对 @TableLogic 的 WHERE deleted=0 限制）
     */
    @Update("UPDATE drug_info SET deleted = 0, update_time = #{updateTime} WHERE id = #{id} AND deleted = 1")
    int restoreLogicallyDeletedById(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 分页查询已逻辑删除的药品（绕过 @TableLogic）
     */
    @Select("<script>" +
            "SELECT * FROM drug_info WHERE deleted = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (drug_name LIKE CONCAT('%', #{keyword}, '%') OR national_code LIKE CONCAT('%', #{keyword}, '%') " +
            "OR approval_number LIKE CONCAT('%', #{keyword}, '%') OR manufacturer LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY update_time DESC " +
            "LIMIT #{offset}, #{limit} " +
            "</script>")
    List<DrugInfo> selectDeletedDrugList(@Param("keyword") String keyword, @Param("offset") Long offset, @Param("limit") Long limit);

    /**
     * 统计已逻辑删除的药品总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM drug_info WHERE deleted = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (drug_name LIKE CONCAT('%', #{keyword}, '%') OR national_code LIKE CONCAT('%', #{keyword}, '%') " +
            "OR approval_number LIKE CONCAT('%', #{keyword}, '%') OR manufacturer LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "</script>")
    Long countDeletedDrugs(@Param("keyword") String keyword);
}













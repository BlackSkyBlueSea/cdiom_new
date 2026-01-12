package com.cdiom.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdiom.backend.model.DrugInfo;

/**
 * 药品信息服务接口
 * 
 * @author cdiom
 */
public interface DrugInfoService {

    /**
     * 分页查询药品信息列表
     */
    Page<DrugInfo> getDrugInfoList(Integer page, Integer size, String keyword, Integer isSpecial);

    /**
     * 根据ID查询药品信息
     */
    DrugInfo getDrugInfoById(Long id);

    /**
     * 创建药品信息
     */
    DrugInfo createDrugInfo(DrugInfo drugInfo);

    /**
     * 更新药品信息
     */
    DrugInfo updateDrugInfo(DrugInfo drugInfo);

    /**
     * 删除药品信息
     */
    void deleteDrugInfo(Long id);
}


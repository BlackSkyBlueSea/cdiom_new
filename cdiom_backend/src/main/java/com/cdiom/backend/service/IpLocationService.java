package com.cdiom.backend.service;

/**
 * IP地理位置查询服务接口
 * 
 * @author cdiom
 */
public interface IpLocationService {
    
    /**
     * 根据IP地址获取地理位置信息
     * 
     * @param ip IP地址
     * @return 地理位置信息，格式：国家 省份 城市，如果查询失败返回IP地址
     */
    String getLocationByIp(String ip);
}


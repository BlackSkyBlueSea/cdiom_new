package com.cdiom.backend.service.impl;

import com.cdiom.backend.service.IpLocationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

/**
 * IP地理位置查询服务实现类
 * 使用免费的IP地理位置API：ip-api.com
 * 
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpLocationServiceImpl implements IpLocationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 使用ip-api.com免费API（无需API key，有请求限制：每分钟45次）
    private static final String IP_API_URL = "http://ip-api.com/json/{ip}?lang=zh-CN&fields=status,message,country,regionName,city,query";
    
    // 本地IP地址
    private static final String LOCAL_IP = "127.0.0.1";
    private static final String LOCALHOST = "localhost";
    private static final String LOCAL_IP_V6 = "0:0:0:0:0:0:0:1";

    @Override
    public String getLocationByIp(String ip) {
        // 处理空值或本地IP
        if (ip == null || ip.trim().isEmpty()) {
            return "未知";
        }
        
        ip = ip.trim();
        
        // 本地IP地址
        if (LOCAL_IP.equals(ip) || LOCALHOST.equalsIgnoreCase(ip) || LOCAL_IP_V6.equals(ip) || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "内网IP";
        }

        try {
            // 调用IP地理位置API
            ResponseEntity<String> response = restTemplate.getForEntity(IP_API_URL, String.class, ip);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                
                // 检查API返回状态
                String status = jsonNode.path("status").asText();
                if ("success".equals(status)) {
                    String country = jsonNode.path("country").asText("");
                    String region = jsonNode.path("regionName").asText("");
                    String city = jsonNode.path("city").asText("");
                    
                    // 构建详细地址信息，格式：国家、省份、城市
                    StringBuilder location = new StringBuilder();
                    
                    // 国家
                    if (!country.isEmpty()) {
                        location.append(country);
                    }
                    
                    // 省份
                    if (!region.isEmpty() && !region.equals(city)) {
                        if (location.length() > 0) {
                            location.append("、");
                        }
                        location.append(region);
                    }
                    
                    // 城市
                    if (!city.isEmpty()) {
                        if (location.length() > 0) {
                            location.append("、");
                        }
                        location.append(city);
                    }
                    
                    // 如果获取到了地址信息，返回；否则返回IP地址
                    if (location.length() > 0) {
                        return location.toString();
                    }
                } else {
                    String message = jsonNode.path("message").asText("");
                    log.warn("IP地理位置查询失败，IP: {}, 错误: {}", ip, message);
                }
            }
        } catch (ResourceAccessException e) {
            // 网络连接失败，可能是API服务不可用
            log.warn("IP地理位置API连接失败，IP: {}, 错误: {}", ip, e.getMessage());
        } catch (Exception e) {
            // 其他异常，记录日志但不影响登录流程
            log.warn("IP地理位置查询异常，IP: {}, 错误: {}", ip, e.getMessage());
        }
        
        // 查询失败时返回IP地址
        return ip;
    }
}


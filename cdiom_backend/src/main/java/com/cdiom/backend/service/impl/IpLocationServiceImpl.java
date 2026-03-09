package com.cdiom.backend.service.impl;

import com.cdiom.backend.service.IpLocationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * IP地理位置查询服务实现类
 * 使用高德地图IP定位API
 *
 * @author cdiom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpLocationServiceImpl implements IpLocationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 高德IP定位API基础URL和Key（在配置文件中配置）
     * base-url 属于非敏感配置，放在 application.yml
     * key 属于敏感配置，放在 application-local.yml
     */
    @Value("${amap.ip.base-url:https://restapi.amap.com/v3/ip}")
    private String amapBaseUrl;

    @Value("${amap.ip.key:}")
    private String amapKey;

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

        // 本地IP地址或内网IP，直接返回“内网IP”
        if (LOCAL_IP.equals(ip) || LOCALHOST.equalsIgnoreCase(ip) || LOCAL_IP_V6.equals(ip)
                || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "内网IP";
        }

        // 如果未配置高德Key，则直接返回IP，避免无效请求
        if (amapKey == null || amapKey.trim().isEmpty()) {
            log.warn("高德IP定位未配置API Key，返回IP地址: {}", ip);
            return ip;
        }

        try {
            // 构建高德IP定位请求URL
            String url = UriComponentsBuilder
                    .fromHttpUrl(amapBaseUrl != null ? amapBaseUrl : "https://restapi.amap.com/v3/ip")
                    .queryParam("key", amapKey.trim())
                    .queryParam("ip", ip)
                    .build()
                    .toUriString();

            // 调用高德IP地理位置API
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                // 检查API返回状态：1 表示成功
                String status = jsonNode.path("status").asText();
                if ("1".equals(status)) {
                    String province = jsonNode.path("province").asText("");
                    String city = jsonNode.path("city").asText("");

                    // 构建地址信息，格式：省份、城市
                    StringBuilder location = new StringBuilder();

                    if (!province.isEmpty()) {
                        location.append(province);
                    }

                    if (!city.isEmpty() && !city.equals(province)) {
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
                    String info = jsonNode.path("info").asText("");
                    log.warn("高德IP地理位置查询失败，IP: {}, info: {}", ip, info);
                }
            }
        } catch (ResourceAccessException e) {
            // 网络连接失败，可能是API服务不可用
            log.warn("高德IP地理位置API连接失败，IP: {}, 错误: {}", ip, e.getMessage());
        } catch (Exception e) {
            // 其他异常，记录日志但不影响登录流程
            log.warn("高德IP地理位置查询异常，IP: {}, 错误: {}", ip, e.getMessage());
        }

        // 查询失败时返回IP地址
        return ip;
    }
}


package com.cdiom.backend.service.impl;

import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.service.YuanyanyaoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 万维易源药品信息服务实现类
 * 通过万维易源API查询药品详细信息
 * 
 * @author cdiom
 */
@Slf4j
@Service
public class YuanyanyaoServiceImpl implements YuanyanyaoService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${yuanyanyao.api.base-url:https://route.showapi.com/1468-3}")
    private String baseUrl;

    @Value("${yuanyanyao.api.app-key:54A4f73B557144aEB53EFCB7a4bC7d01}")
    private String appKey;

    @Value("${yuanyanyao.api.default-classify-id:599ad2a0600b2149d689b75a}")
    private String defaultClassifyId;

    public YuanyanyaoServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public DrugInfo searchByApprovalNumber(String approvalNumber) {
        if (approvalNumber == null || approvalNumber.trim().isEmpty()) {
            return null;
        }

        try {
            // searchType=3 表示按药准字号查询
            return searchDrug(defaultClassifyId, "3", approvalNumber.trim());
        } catch (Exception e) {
            log.warn("通过批准文号查询药品信息失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public DrugInfo searchByDrugName(String drugName) {
        if (drugName == null || drugName.trim().isEmpty()) {
            return null;
        }

        try {
            // searchType=1 表示按药品名称查询
            return searchDrug(defaultClassifyId, "1", drugName.trim());
        } catch (Exception e) {
            log.warn("通过药品名称查询药品信息失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public DrugInfo searchByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String trimmedCode = code.trim();
        
        // 先尝试作为批准文号查询（searchType=3）
        DrugInfo drugInfo = searchDrug(defaultClassifyId, "3", trimmedCode);
        if (drugInfo != null) {
            return drugInfo;
        }

        // 如果没找到，尝试作为药品名称查询（searchType=1）
        return searchDrug(defaultClassifyId, "1", trimmedCode);
    }

    /**
     * 调用万维易源API查询药品信息
     * 
     * @param classifyId 药品分类Id（1468-4接口不需要此参数，但保留以兼容旧代码）
     * @param searchType 搜索类型（1468-4接口不需要此参数，但保留以兼容旧代码）
     * @param searchKey 查询关键字
     * @return 药品信息，如果未找到返回null
     */
    private DrugInfo searchDrug(String classifyId, String searchType, String searchKey) {
        try {
            // 1468-4接口：根据base-url判断使用哪个接口
            boolean useNewApi = baseUrl != null && baseUrl.contains("1468-4");
            
            if (useNewApi) {
                // 使用1468-4接口（简化版，只需要searchKey参数）
                return searchDrugV4(searchKey);
            } else {
                // 使用1468-3接口（需要classifyId和searchType）
                return searchDrugV3(classifyId, searchType, searchKey);
            }
        } catch (Exception e) {
            log.warn("调用万维易源API失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 调用万维易源API 1468-4接口查询药品信息（简化版）
     * 
     * @param searchKey 查询关键字
     * @return 药品信息，如果未找到返回null
     */
    private DrugInfo searchDrugV4(String searchKey) {
        try {
            // 构建请求URL
            String url = UriComponentsBuilder.fromHttpUrl(Objects.requireNonNull(baseUrl, "baseUrl不能为null"))
                    .queryParam("appKey", Objects.requireNonNull(appKey, "appKey不能为null"))
                    .build()
                    .toUriString();

            // 构建POST请求参数（1468-4接口只需要searchKey、page、maxResult）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("searchKey", searchKey);
            params.add("page", "1");
            params.add("maxResult", "10");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            // 发送POST请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            String responseBody = response.getBody();

            if (responseBody == null) {
                log.debug("API响应为空");
                return null;
            }

            // 解析响应
            log.debug("API响应内容: {}", responseBody);
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // 检查showapi_res_code（外层状态码）
            JsonNode showapiResCode = rootNode.get("showapi_res_code");
            if (showapiResCode != null && !"0".equals(showapiResCode.asText())) {
                String errorMsg = rootNode.has("showapi_res_error") ? rootNode.get("showapi_res_error").asText() : "API调用失败";
                log.warn("API调用失败: showapi_res_code={}, error={}", showapiResCode.asText(), errorMsg);
                return null;
            }
            
            JsonNode showapiResBody = rootNode.get("showapi_res_body");
            
            if (showapiResBody == null) {
                log.warn("API响应格式错误，缺少showapi_res_body。响应内容: {}", responseBody);
                return null;
            }

            JsonNode retCodeNode = showapiResBody.get("ret_code");
            if (retCodeNode == null || !"0".equals(retCodeNode.asText())) {
                String msg = showapiResBody.has("msg") ? showapiResBody.get("msg").asText() : "查询失败";
                log.warn("API返回错误: ret_code={}, msg={}", retCodeNode != null ? retCodeNode.asText() : "null", msg);
                return null;
            }

            // 获取药品列表
            JsonNode drugList = showapiResBody.get("drugList");
            if (drugList == null || !drugList.isArray() || drugList.size() == 0) {
                log.warn("未找到药品信息，drugList为空或不存在。响应内容: {}", responseBody);
                return null;
            }

            // 取第一个药品信息
            JsonNode drugNode = drugList.get(0);
            log.debug("解析药品节点: {}", drugNode.toString());
            DrugInfo drugInfo = parseDrugInfo(drugNode);
            if (drugInfo == null) {
                log.warn("解析药品信息失败，drugNode: {}", drugNode.toString());
            } else {
                log.debug("成功解析药品信息: {}", drugInfo.getDrugName());
            }
            return drugInfo;

        } catch (Exception e) {
            log.error("调用万维易源API 1468-4失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 调用万维易源API 1468-3接口查询药品信息（原版本）
     * 
     * @param classifyId 药品分类Id
     * @param searchType 搜索类型：1-药品名称 2-药企名称 3-药准字号 4-药品Id
     * @param searchKey 查询关键字
     * @return 药品信息，如果未找到返回null
     */
    private DrugInfo searchDrugV3(String classifyId, String searchType, String searchKey) {
        try {
            // 构建请求URL
            String url = UriComponentsBuilder.fromHttpUrl(Objects.requireNonNull(baseUrl, "baseUrl不能为null"))
                    .queryParam("appKey", Objects.requireNonNull(appKey, "appKey不能为null"))
                    .build()
                    .toUriString();

            // 构建POST请求参数
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("classifyId", classifyId);
            params.add("searchType", searchType);
            params.add("searchKey", searchKey);
            params.add("page", "1");
            params.add("maxResult", "10");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            // 发送POST请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            String responseBody = response.getBody();

            if (responseBody == null) {
                return null;
            }

            // 解析响应
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode showapiResBody = rootNode.get("showapi_res_body");
            
            if (showapiResBody == null) {
                log.debug("API响应格式错误，缺少showapi_res_body");
                return null;
            }

            JsonNode retCodeNode = showapiResBody.get("ret_code");
            if (retCodeNode == null || !"0".equals(retCodeNode.asText())) {
                String msg = showapiResBody.has("msg") ? showapiResBody.get("msg").asText() : "查询失败";
                log.debug("API返回错误: {}", msg);
                return null;
            }

            // 获取药品列表
            JsonNode drugList = showapiResBody.get("drugList");
            if (drugList == null || !drugList.isArray() || drugList.size() == 0) {
                log.debug("未找到药品信息");
                return null;
            }

            // 取第一个药品信息
            JsonNode drugNode = drugList.get(0);
            return parseDrugInfo(drugNode);

        } catch (Exception e) {
            log.warn("调用万维易源API 1468-3失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析药品信息
     */
    private DrugInfo parseDrugInfo(JsonNode drugNode) {
        try {
            if (drugNode == null) {
                log.warn("drugNode为null，无法解析");
                return null;
            }
            
            DrugInfo drugInfo = new DrugInfo();

            // 通用名称 (tymc) - 优先使用通用名称
            String drugName = getTextValue(drugNode, "tymc");
            if (drugName == null || drugName.isEmpty()) {
                // 如果没有通用名称，使用药品名称 (drugName)
                drugName = getTextValue(drugNode, "drugName");
            }
            if (drugName == null || drugName.isEmpty()) {
                log.warn("药品名称为空，无法创建药品信息。drugNode: {}", drugNode.toString());
                return null;
            }
            drugInfo.setDrugName(drugName);

            // 批准文号 (pzwh)
            drugInfo.setApprovalNumber(getTextValue(drugNode, "pzwh"));

            // 生产企业 (manu)
            String manufacturer = getTextValue(drugNode, "manu");
            if (manufacturer != null) {
                // 移除"(国产)"等后缀
                manufacturer = manufacturer.replaceAll("\\(国产\\)", "").trim();
            }
            drugInfo.setManufacturer(manufacturer);

            // 剂型 (jx)
            drugInfo.setDosageForm(getTextValue(drugNode, "jx"));

            // 规格 (gg)
            drugInfo.setSpecification(getTextValue(drugNode, "gg"));

            // 有效期 (yxq) - 需要解析，格式可能是"48个月"、"24个月"、"一年半"等
            String expiryDateStr = getTextValue(drugNode, "yxq");
            if (expiryDateStr != null && !expiryDateStr.isEmpty()) {
                LocalDate expiryDate = parseExpiryDate(expiryDateStr);
                if (expiryDate != null) {
                    drugInfo.setExpiryDate(expiryDate);
                }
            }

            // 贮藏 (zc)
            drugInfo.setStorageRequirement(getTextValue(drugNode, "zc"));

            log.debug("成功解析药品信息: {}", drugName);
            return drugInfo;
        } catch (Exception e) {
            log.error("解析药品信息失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析有效期字符串
     * 支持格式：48个月、24个月、36个月、一年半等
     */
    private LocalDate parseExpiryDate(String expiryDateStr) {
        try {
            if (expiryDateStr == null || expiryDateStr.trim().isEmpty()) {
                return null;
            }
            
            String trimmed = expiryDateStr.trim();
            
            // 处理"一年半"格式
            if (trimmed.contains("一年半")) {
                LocalDate now = LocalDate.now();
                return now.plusMonths(18);
            }
            
            // 处理"X个月"格式
            Pattern pattern = Pattern.compile("(\\d+)\\s*个月");
            Matcher matcher = pattern.matcher(trimmed);
            if (matcher.find()) {
                int months = Integer.parseInt(matcher.group(1));
                // 从当前日期开始计算有效期
                LocalDate now = LocalDate.now();
                return now.plusMonths(months);
            }
            
            // 处理"X年"格式
            Pattern yearPattern = Pattern.compile("(\\d+)\\s*年");
            Matcher yearMatcher = yearPattern.matcher(trimmed);
            if (yearMatcher.find()) {
                int years = Integer.parseInt(yearMatcher.group(1));
                LocalDate now = LocalDate.now();
                return now.plusYears(years);
            }
            
            log.debug("无法解析有效期格式: {}", expiryDateStr);
        } catch (Exception e) {
            log.debug("解析有效期失败: {}, 错误: {}", expiryDateStr, e.getMessage());
        }
        return null;
    }

    /**
     * 安全获取文本值
     */
    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field != null && !field.isNull()) {
            return field.asText();
        }
        return null;
    }
}

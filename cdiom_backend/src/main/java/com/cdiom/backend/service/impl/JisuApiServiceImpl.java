package com.cdiom.backend.service.impl;

import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.service.JisuApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 极速数据药品信息服务实现类
 * 通过极速数据API查询药品详细信息
 * API文档：https://api.jisuapi.com/medicine/detail
 * 
 * @author cdiom
 */
@Slf4j
@Service
public class JisuApiServiceImpl implements JisuApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${jisuapi.api.base-url:https://api.jisuapi.com/medicine/detail}")
    private String baseUrl;

    @Value("${jisuapi.api.app-key:}")
    private String appKey;

    public JisuApiServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public DrugInfo searchByProductCode(String productCode) {
        if (productCode == null || productCode.trim().isEmpty()) {
            return null;
        }

        try {
            // 构建请求URL
            String url = UriComponentsBuilder.fromHttpUrl(Objects.requireNonNull(baseUrl, "baseUrl不能为null"))
                    .queryParam("appkey", Objects.requireNonNull(appKey, "appKey不能为null"))
                    .queryParam("code", productCode.trim())
                    .build()
                    .toUriString();

            // 发送GET请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();

            if (responseBody == null) {
                return null;
            }

            // 解析响应
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // 检查状态码
            String status = rootNode.has("status") ? rootNode.get("status").asText() : "";
            if (!"0".equals(status)) {
                String msg = rootNode.has("msg") ? rootNode.get("msg").asText() : "查询失败";
                log.debug("极速数据API返回错误: status={}, msg={}", status, msg);
                return null;
            }

            // 获取药品信息
            JsonNode result = rootNode.get("result");
            if (result == null || result.isNull()) {
                log.debug("未找到药品信息");
                return null;
            }

            return parseDrugInfo(result);

        } catch (Exception e) {
            log.warn("调用极速数据API失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public DrugInfo searchByApprovalNumber(String approvalNumber) {
        if (approvalNumber == null || approvalNumber.trim().isEmpty()) {
            return null;
        }

        try {
            // 构建请求URL
            String url = UriComponentsBuilder.fromHttpUrl(Objects.requireNonNull(baseUrl, "baseUrl不能为null"))
                    .queryParam("appkey", Objects.requireNonNull(appKey, "appKey不能为null"))
                    .queryParam("approval_num", approvalNumber.trim())
                    .build()
                    .toUriString();

            log.debug("调用极速数据API（批准文号）: {}", url);

            // 发送GET请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();

            if (responseBody == null) {
                log.debug("极速数据API返回空响应");
                return null;
            }

            log.debug("极速数据API响应: {}", responseBody);

            // 解析响应
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // 检查状态码
            String status = rootNode.has("status") ? rootNode.get("status").asText() : "";
            if (!"0".equals(status)) {
                String msg = rootNode.has("msg") ? rootNode.get("msg").asText() : "查询失败";
                log.debug("极速数据API返回错误: status={}, msg={}", status, msg);
                return null;
            }

            // 获取药品信息
            JsonNode result = rootNode.get("result");
            if (result == null || result.isNull()) {
                log.debug("未找到药品信息");
                return null;
            }

            return parseDrugInfo(result);

        } catch (Exception e) {
            log.warn("调用极速数据API失败（批准文号）: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析药品信息
     * 根据极速数据API的实际返回格式进行解析
     * API返回字段：name, spec, type, unit, approval_num, reference_code, manufacturer, barcode, disease, desc, prescription
     */
    private DrugInfo parseDrugInfo(JsonNode resultNode) {
        try {
            DrugInfo drugInfo = new DrugInfo();

            // 药品名称
            drugInfo.setDrugName(getTextValue(resultNode, "name"));

            // 批准文号 (API返回字段为approval_num)
            String approvalNum = getTextValue(resultNode, "approval_num");
            if (approvalNum == null || approvalNum.isEmpty()) {
                approvalNum = getTextValue(resultNode, "approvalNumber");
            }
            drugInfo.setApprovalNumber(approvalNum);

            // 生产厂家
            drugInfo.setManufacturer(getTextValue(resultNode, "manufacturer"));

            // 剂型 (API返回字段为type)
            String dosageForm = getTextValue(resultNode, "type");
            if (dosageForm == null || dosageForm.isEmpty()) {
                dosageForm = getTextValue(resultNode, "dosageForm");
            }
            drugInfo.setDosageForm(dosageForm);

            // 规格 (API返回字段为spec，优先使用spec替换规格)
            String spec = getTextValue(resultNode, "spec");
            if (spec == null || spec.isEmpty()) {
                spec = getTextValue(resultNode, "specification");
            }
            drugInfo.setSpecification(spec);

            // 国家本位码 (API返回字段为reference_code)
            drugInfo.setNationalCode(getTextValue(resultNode, "reference_code"));

            // 条形码 (API返回字段为barcode，DrugInfo模型中没有此字段，暂时不存储)
            // String barcode = getTextValue(resultNode, "barcode");

            // 描述/说明书 (API返回字段为desc)
            drugInfo.setDescription(getTextValue(resultNode, "desc"));

            // 有效期
            String expiryDateStr = getTextValue(resultNode, "expiryDate");
            if (expiryDateStr != null && !expiryDateStr.isEmpty()) {
                LocalDate expiryDate = parseExpiryDate(expiryDateStr);
                if (expiryDate != null) {
                    drugInfo.setExpiryDate(expiryDate);
                }
            }

            // 存储要求
            drugInfo.setStorageRequirement(getTextValue(resultNode, "storage"));

            // 商品码 (API可能返回barcode作为商品码)
            String productCode = getTextValue(resultNode, "code");
            if (productCode == null || productCode.isEmpty()) {
                productCode = getTextValue(resultNode, "barcode");
            }
            drugInfo.setProductCode(productCode);

            return drugInfo;
        } catch (Exception e) {
            log.warn("解析药品信息失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析有效期字符串
     */
    private LocalDate parseExpiryDate(String expiryDateStr) {
        try {
            // 尝试直接解析日期格式
            if (expiryDateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(expiryDateStr);
            }
            // 提取月份数字（如"48个月"）
            Pattern pattern = Pattern.compile("(\\d+)\\s*个月");
            Matcher matcher = pattern.matcher(expiryDateStr);
            if (matcher.find()) {
                int months = Integer.parseInt(matcher.group(1));
                return LocalDate.now().plusMonths(months);
            }
        } catch (Exception e) {
            log.debug("解析有效期失败: {}", expiryDateStr);
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


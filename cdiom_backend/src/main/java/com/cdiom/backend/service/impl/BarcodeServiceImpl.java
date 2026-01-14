package com.cdiom.backend.service.impl;

import com.cdiom.backend.service.BarcodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 条形码生成服务实现类
 * 使用Code128码制，符合GSP规范要求
 * 
 * @author cdiom
 */
@Slf4j
@Service
public class BarcodeServiceImpl implements BarcodeService {

    @Override
    public String generateBarcodeBase64(String content, int width, int height) {
        try {
            byte[] imageBytes = generateBarcodeBytes(content, width, height);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            log.error("生成条形码Base64失败：content={}", content, e);
            throw new RuntimeException("生成条形码失败：" + e.getMessage());
        }
    }

    @Override
    public void generateBarcodeToStream(String content, int width, int height, OutputStream outputStream) {
        try {
            byte[] imageBytes = generateBarcodeBytes(content, width, height);
            outputStream.write(imageBytes);
            outputStream.flush();
        } catch (Exception e) {
            log.error("生成条形码到输出流失败：content={}", content, e);
            throw new RuntimeException("生成条形码失败：" + e.getMessage());
        }
    }

    @Override
    public byte[] generateBarcodeBytes(String content, int width, int height) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("条形码内容不能为空");
        }

        // 使用Code128码制（符合GSP规范，支持字母和数字）
        Code128Writer writer = new Code128Writer();
        
        // 设置编码提示
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2); // 设置边距

        try {
            // 生成条形码矩阵
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, width, height, hints);
            
            // 转换为PNG图片字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("生成条形码失败：content={}", content, e);
            throw new RuntimeException("生成条形码失败：" + e.getMessage());
        }
    }
}


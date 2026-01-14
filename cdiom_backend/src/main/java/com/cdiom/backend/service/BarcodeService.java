package com.cdiom.backend.service;

import java.io.OutputStream;

/**
 * 条形码生成服务接口
 * 
 * @author cdiom
 */
public interface BarcodeService {

    /**
     * 生成Code128条形码图片（Base64编码）
     * 
     * @param content 条形码内容（订单编号）
     * @param width 图片宽度（像素）
     * @param height 图片高度（像素）
     * @return Base64编码的图片字符串
     */
    String generateBarcodeBase64(String content, int width, int height);

    /**
     * 生成Code128条形码图片并写入输出流
     * 
     * @param content 条形码内容（订单编号）
     * @param width 图片宽度（像素）
     * @param height 图片高度（像素）
     * @param outputStream 输出流
     */
    void generateBarcodeToStream(String content, int width, int height, OutputStream outputStream);

    /**
     * 生成Code128条形码图片字节数组
     * 
     * @param content 条形码内容（订单编号）
     * @param width 图片宽度（像素）
     * @param height 图片高度（像素）
     * @return 图片字节数组
     */
    byte[] generateBarcodeBytes(String content, int width, int height);
}




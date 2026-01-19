package com.cdiom.backend.service;

import com.cdiom.backend.model.DrugInfo;
import com.cdiom.backend.model.Inventory;
import com.cdiom.backend.model.PurchaseOrder;
import com.cdiom.backend.model.SysUser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel导出服务
 * 
 * @author cdiom
 */
@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出药品列表到Excel
     */
    public byte[] exportDrugInfoList(List<DrugInfo> drugList, SysUser exporter) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("药品列表");

        // 创建样式
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // 创建标题行
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("药品信息列表");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

        // 创建表头
        Row headerRow = sheet.createRow(1);
        String[] headers = {"ID", "国家本位码", "商品码", "药品名称", "剂型", "规格", "批准文号", 
                           "生产厂家", "是否特殊药品", "存储要求", "单位", "创建时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 2;
        for (DrugInfo drug : drugList) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            
            Cell idCell = row.createCell(colNum++);
            if (drug.getId() != null) {
                idCell.setCellValue(drug.getId());
            } else {
                idCell.setCellValue("");
            }
            row.createCell(colNum++).setCellValue(drug.getNationalCode() != null ? drug.getNationalCode() : "");
            row.createCell(colNum++).setCellValue(drug.getProductCode() != null ? drug.getProductCode() : "");
            row.createCell(colNum++).setCellValue(drug.getDrugName() != null ? drug.getDrugName() : "");
            row.createCell(colNum++).setCellValue(drug.getDosageForm() != null ? drug.getDosageForm() : "");
            row.createCell(colNum++).setCellValue(drug.getSpecification() != null ? drug.getSpecification() : "");
            row.createCell(colNum++).setCellValue(drug.getApprovalNumber() != null ? drug.getApprovalNumber() : "");
            row.createCell(colNum++).setCellValue(drug.getManufacturer() != null ? drug.getManufacturer() : "");
            row.createCell(colNum++).setCellValue(drug.getIsSpecial() != null && drug.getIsSpecial() == 1 ? "是" : "否");
            row.createCell(colNum++).setCellValue(drug.getStorageRequirement() != null ? drug.getStorageRequirement() : "");
            row.createCell(colNum++).setCellValue(drug.getUnit() != null ? drug.getUnit() : "");
            row.createCell(colNum++).setCellValue(drug.getCreateTime() != null ? 
                drug.getCreateTime().format(DATETIME_FORMATTER) : "");
            
            // 设置数据样式
            for (int i = 0; i < colNum; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }

        // 添加水印
        addWatermark(sheet, exporter, workbook);

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000); // 增加一些额外宽度
        }

        // 写入到字节数组
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    /**
     * 导出库存列表到Excel
     */
    public byte[] exportInventoryList(List<Inventory> inventoryList, SysUser exporter) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("库存列表");

        // 创建样式
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // 创建标题行
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("库存信息列表");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

        // 创建表头
        Row headerRow = sheet.createRow(1);
        String[] headers = {"ID", "药品ID", "批次号", "库存数量", "有效期至", "存储位置", 
                           "生产日期", "生产厂家", "备注", "创建时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 2;
        for (Inventory inventory : inventoryList) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            
            Cell idCell = row.createCell(colNum++);
            if (inventory.getId() != null) {
                idCell.setCellValue(inventory.getId());
            } else {
                idCell.setCellValue("");
            }
            Cell drugIdCell = row.createCell(colNum++);
            if (inventory.getDrugId() != null) {
                drugIdCell.setCellValue(inventory.getDrugId());
            } else {
                drugIdCell.setCellValue("");
            }
            row.createCell(colNum++).setCellValue(inventory.getBatchNumber() != null ? inventory.getBatchNumber() : "");
            row.createCell(colNum++).setCellValue(inventory.getQuantity() != null ? inventory.getQuantity() : 0);
            row.createCell(colNum++).setCellValue(inventory.getExpiryDate() != null ? 
                inventory.getExpiryDate().format(DATE_FORMATTER) : "");
            row.createCell(colNum++).setCellValue(inventory.getStorageLocation() != null ? inventory.getStorageLocation() : "");
            row.createCell(colNum++).setCellValue(inventory.getProductionDate() != null ? 
                inventory.getProductionDate().format(DATE_FORMATTER) : "");
            row.createCell(colNum++).setCellValue(inventory.getManufacturer() != null ? inventory.getManufacturer() : "");
            row.createCell(colNum++).setCellValue(inventory.getRemark() != null ? inventory.getRemark() : "");
            row.createCell(colNum++).setCellValue(inventory.getCreateTime() != null ? 
                inventory.getCreateTime().format(DATETIME_FORMATTER) : "");
            
            // 设置数据样式
            for (int i = 0; i < colNum; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }

        // 添加水印
        addWatermark(sheet, exporter, workbook);

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }

        // 写入到字节数组
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    /**
     * 导出采购订单列表到Excel
     */
    public byte[] exportPurchaseOrderList(List<PurchaseOrder> orderList, SysUser exporter) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("采购订单列表");

        // 创建样式
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // 创建标题行
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("采购订单列表");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

        // 创建表头
        Row headerRow = sheet.createRow(1);
        String[] headers = {"ID", "订单编号", "供应商ID", "采购员ID", "订单状态", "预计交货日期", 
                           "物流单号", "发货日期", "订单总金额", "创建时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 2;
        for (PurchaseOrder order : orderList) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            
            Cell idCell = row.createCell(colNum++);
            if (order.getId() != null) {
                idCell.setCellValue(order.getId());
            } else {
                idCell.setCellValue("");
            }
            row.createCell(colNum++).setCellValue(order.getOrderNumber() != null ? order.getOrderNumber() : "");
            Cell supplierIdCell = row.createCell(colNum++);
            if (order.getSupplierId() != null) {
                supplierIdCell.setCellValue(order.getSupplierId());
            } else {
                supplierIdCell.setCellValue("");
            }
            Cell purchaserIdCell = row.createCell(colNum++);
            if (order.getPurchaserId() != null) {
                purchaserIdCell.setCellValue(order.getPurchaserId());
            } else {
                purchaserIdCell.setCellValue("");
            }
            row.createCell(colNum++).setCellValue(order.getStatus() != null ? order.getStatus() : "");
            row.createCell(colNum++).setCellValue(order.getExpectedDeliveryDate() != null ? 
                order.getExpectedDeliveryDate().format(DATE_FORMATTER) : "");
            row.createCell(colNum++).setCellValue(order.getLogisticsNumber() != null ? order.getLogisticsNumber() : "");
            row.createCell(colNum++).setCellValue(order.getShipDate() != null ? 
                order.getShipDate().format(DATETIME_FORMATTER) : "");
            row.createCell(colNum++).setCellValue(order.getTotalAmount() != null ? 
                order.getTotalAmount().doubleValue() : 0.0);
            row.createCell(colNum++).setCellValue(order.getCreateTime() != null ? 
                order.getCreateTime().format(DATETIME_FORMATTER) : "");
            
            // 设置数据样式
            for (int i = 0; i < colNum; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }

        // 添加水印
        addWatermark(sheet, exporter, workbook);

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }

        // 写入到字节数组
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建数据样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建水印样式
     */
    private CellStyle createWatermarkStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.GREY_40_PERCENT.getIndex());
        font.setItalic(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.BOTTOM);
        return style;
    }

    /**
     * 添加水印（导出人+导出时间）
     */
    private void addWatermark(XSSFSheet sheet, SysUser exporter, XSSFWorkbook workbook) {
        if (exporter == null) {
            return;
        }
        
        String watermarkText = String.format("导出人：%s | 导出时间：%s", 
            exporter.getUsername() != null ? exporter.getUsername() : "未知",
            LocalDateTime.now().format(DATETIME_FORMATTER));
        
        // 在最后一行添加水印信息
        int lastRowNum = sheet.getLastRowNum();
        Row watermarkRow = sheet.createRow(lastRowNum + 2);
        Cell watermarkCell = watermarkRow.createCell(0);
        watermarkCell.setCellValue(watermarkText);
        watermarkCell.setCellStyle(createWatermarkStyle(workbook));
        
        // 合并单元格以显示完整水印
        int maxColumn = sheet.getRow(1).getLastCellNum() - 1;
        if (maxColumn > 0) {
            sheet.addMergedRegion(new CellRangeAddress(lastRowNum + 2, lastRowNum + 2, 0, maxColumn));
        }
    }
}


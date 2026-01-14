package com.cdiom.backend.controller;

import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传控制器
 * 
 * @author cdiom
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@RequiresPermission({"drug:view", "drug:manage"})
public class FileUploadController {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:/api/v1/files}")
    private String urlPrefix;

    /**
     * 上传文件
     */
    @PostMapping
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            // 验证文件类型（仅允许图片）
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error("文件名不能为空");
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
            boolean isAllowed = false;
            for (String ext : allowedExtensions) {
                if (extension.equals(ext)) {
                    isAllowed = true;
                    break;
                }
            }

            if (!isAllowed) {
                return Result.error("仅支持图片格式：jpg, jpeg, png, gif, bmp, webp");
            }

            // 验证文件大小（最大10MB）
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (file.getSize() > maxSize) {
                return Result.error("文件大小不能超过10MB");
            }

            // 创建上传目录（按日期分类）
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fullUploadPath = uploadPath + "/" + dateDir;
            File uploadDir = new File(fullUploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + extension;
            Path filePath = Paths.get(fullUploadPath, fileName);

            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 返回文件访问URL
            String fileUrl = urlPrefix + "/" + dateDir + "/" + fileName;
            log.info("文件上传成功: {}", fileUrl);
            
            return Result.success("文件上传成功", fileUrl);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping
    public Result<Void> deleteFile(@RequestParam("url") String url) {
        try {
            // 从URL中提取文件路径
            if (url.startsWith(urlPrefix)) {
                String relativePath = url.substring(urlPrefix.length());
                if (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
                Path filePath = Paths.get(uploadPath, relativePath);
                Files.deleteIfExists(filePath);
                log.info("文件删除成功: {}", url);
                return Result.success();
            } else {
                return Result.error("无效的文件URL");
            }
        } catch (IOException e) {
            log.error("文件删除失败", e);
            return Result.error("文件删除失败: " + e.getMessage());
        }
    }
}


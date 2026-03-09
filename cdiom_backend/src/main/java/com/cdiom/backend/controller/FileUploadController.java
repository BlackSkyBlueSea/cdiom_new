package com.cdiom.backend.controller;

import com.cdiom.backend.annotation.RequiresPermission;
import com.cdiom.backend.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private DataSize maxFileSize;

    // 允许的MIME类型映射
    private static final Map<String, String[]> ALLOWED_MIME_TYPES = new HashMap<>();
    static {
        ALLOWED_MIME_TYPES.put(".jpg", new String[]{"image/jpeg"});
        ALLOWED_MIME_TYPES.put(".jpeg", new String[]{"image/jpeg"});
        ALLOWED_MIME_TYPES.put(".png", new String[]{"image/png"});
        ALLOWED_MIME_TYPES.put(".gif", new String[]{"image/gif"});
        ALLOWED_MIME_TYPES.put(".bmp", new String[]{"image/bmp", "image/x-ms-bmp"});
        ALLOWED_MIME_TYPES.put(".webp", new String[]{"image/webp"});
        ALLOWED_MIME_TYPES.put(".pdf", new String[]{"application/pdf"});
        ALLOWED_MIME_TYPES.put(".doc", new String[]{"application/msword"});
        ALLOWED_MIME_TYPES.put(".docx", new String[]{"application/vnd.openxmlformats-officedocument.wordprocessingml.document"});
    }

    // 文件魔数（文件头）映射
    private static final Map<String, byte[][]> FILE_MAGIC_NUMBERS = new HashMap<>();
    static {
        FILE_MAGIC_NUMBERS.put(".jpg", new byte[][]{{(byte)0xFF, (byte)0xD8, (byte)0xFF}});
        FILE_MAGIC_NUMBERS.put(".jpeg", new byte[][]{{(byte)0xFF, (byte)0xD8, (byte)0xFF}});
        FILE_MAGIC_NUMBERS.put(".png", new byte[][]{{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}});
        FILE_MAGIC_NUMBERS.put(".gif", new byte[][]{{0x47, 0x49, 0x46, 0x38, 0x37, 0x61}, {0x47, 0x49, 0x46, 0x38, 0x39, 0x61}});
        FILE_MAGIC_NUMBERS.put(".bmp", new byte[][]{{0x42, 0x4D}});
        FILE_MAGIC_NUMBERS.put(".webp", new byte[][]{{0x52, 0x49, 0x46, 0x46}}); // RIFF header
        FILE_MAGIC_NUMBERS.put(".pdf", new byte[][]{{0x25, 0x50, 0x44, 0x46}}); // %PDF
        FILE_MAGIC_NUMBERS.put(".doc", new byte[][]{{(byte)0xD0, (byte)0xCF, 0x11, (byte)0xE0, (byte)0xA1, (byte)0xB1, 0x1A, (byte)0xE1}}); // OLE2 header
        FILE_MAGIC_NUMBERS.put(".docx", new byte[][]{{0x50, 0x4B, 0x03, 0x04}}); // ZIP header (docx is a zip file)
    }

    /**
     * 验证文件魔数（文件头）
     */
    private boolean validateFileMagicNumber(InputStream inputStream, String extension) throws IOException {
        byte[][] expectedMagicNumbers = FILE_MAGIC_NUMBERS.get(extension);
        if (expectedMagicNumbers == null) {
            return false;
        }

        // 读取文件前几个字节
        byte[] fileHeader = new byte[8];
        int bytesRead = inputStream.read(fileHeader);
        if (bytesRead < 2) {
            return false;
        }

        // 重置流以便后续使用
        inputStream.reset();

        // 检查是否匹配任一预期的魔数
        for (byte[] magicNumber : expectedMagicNumbers) {
            if (bytesRead >= magicNumber.length) {
                boolean matches = true;
                for (int i = 0; i < magicNumber.length; i++) {
                    if (fileHeader[i] != magicNumber[i]) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return true;
                }
            }
        }

        return false;
    }

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
            // 支持图片和文档格式（用于协议文件上传）
            String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".pdf", ".doc", ".docx"};
            boolean isAllowed = false;
            for (String ext : allowedExtensions) {
                if (extension.equals(ext)) {
                    isAllowed = true;
                    break;
                }
            }

            if (!isAllowed) {
                return Result.error("仅支持以下格式：jpg, jpeg, png, gif, bmp, webp, pdf, doc, docx");
            }

            // 验证MIME类型
            String contentType = file.getContentType();
            if (contentType == null) {
                return Result.error("无法识别文件类型");
            }
            String[] allowedMimeTypes = ALLOWED_MIME_TYPES.get(extension);
            if (allowedMimeTypes == null) {
                return Result.error("不支持的文件类型");
            }
            boolean mimeTypeValid = Arrays.asList(allowedMimeTypes).contains(contentType);
            if (!mimeTypeValid) {
                log.warn("文件MIME类型不匹配：扩展名={}, 声明MIME类型={}, 允许的MIME类型={}", 
                    extension, contentType, Arrays.toString(allowedMimeTypes));
                return Result.error("文件类型验证失败：MIME类型与文件扩展名不匹配");
            }

            // 验证文件魔数（文件头）
            InputStream inputStream = file.getInputStream();
            inputStream.mark(16); // 标记以便重置
            boolean magicNumberValid = validateFileMagicNumber(inputStream, extension);
            inputStream.reset(); // 重置流以便后续使用
            if (!magicNumberValid) {
                log.warn("文件魔数验证失败：扩展名={}, 文件名={}", extension, originalFilename);
                return Result.error("文件类型验证失败：文件内容与扩展名不匹配");
            }

            // 验证文件大小（从配置读取）
            long maxSizeBytes = maxFileSize.toBytes();
            if (file.getSize() > maxSizeBytes) {
                String maxSizeMB = String.format("%.1f", maxSizeBytes / (1024.0 * 1024.0));
                return Result.error("文件大小不能超过" + maxSizeMB + "MB");
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


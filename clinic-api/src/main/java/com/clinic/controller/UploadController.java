package com.clinic.controller;

import com.clinic.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Api(tags = "文件上传")
@RestController
@RequestMapping("/upload")
public class UploadController {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Value("${upload.url-prefix:/uploads}")
    private String urlPrefix;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @ApiOperation("上传图片")
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        return uploadFile(file, request, true);
    }

    @ApiOperation("上传文件")
    @PostMapping("/file")
    public Result<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        return uploadFile(file, request, false);
    }

    private Result<Map<String, String>> uploadFile(MultipartFile file, HttpServletRequest request, boolean isImage) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (isImage) {
            if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                return Result.error("只允许上传图片文件(jpg, jpeg, png, gif, webp)");
            }
            // 检查文件大小
            if (file.getSize() > MAX_IMAGE_SIZE) {
                return Result.error("图片大小不能超过2MB");
            }
        } else {
            if (!ALLOWED_FILE_TYPES.contains(contentType)) {
                return Result.error("不支持的文件类型");
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                return Result.error("文件大小不能超过10MB");
            }
        }

        try {
            // 创建上传目录
            String subDir = isImage ? "images" : "files";
            Path uploadDir = Paths.get(uploadPath, subDir).toAbsolutePath();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件
            Path filePath = uploadDir.resolve(newFilename);
            File destFile = filePath.toFile();
            
            log.info("准备保存文件到: {}", destFile.getAbsolutePath());
            
            // 确保父目录存在
            if (!destFile.getParentFile().exists()) {
                destFile.getParentFile().mkdirs();
            }
            
            file.transferTo(destFile);

            // 验证文件是否保存成功
            boolean fileExists = destFile.exists() && destFile.isFile();
            log.info("文件保存路径: {}", destFile.getAbsolutePath());
            log.info("文件大小: {} bytes", destFile.length());
            log.info("文件是否存在: {}", fileExists);
            
            if (!fileExists || destFile.length() == 0) {
                return Result.error("文件保存失败，请检查服务器权限或磁盘空间");
            }

            // 生成访问URL
            String serverUrl = getServerUrl(request);
            String fileUrl = serverUrl + urlPrefix + "/" + subDir + "/" + newFilename;

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", newFilename);
            result.put("path", filePath.toString()); // 调试用

            log.info("文件上传成功: {}", fileUrl);
            return Result.success(result);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    private String getServerUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }
        // 不包含 context-path，让 uploads 路径直接映射到根路径
        return url.toString();
    }
}

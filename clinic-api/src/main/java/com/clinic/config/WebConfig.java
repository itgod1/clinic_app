package com.clinic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Value("${upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取绝对路径
        Path path = Paths.get(uploadPath).toAbsolutePath();
        String absolutePath = path.toString().replace("\\", "/");

        // 确保路径以 / 结尾
        if (!absolutePath.endsWith("/")) {
            absolutePath += "/";
        }

        // 映射上传文件夹为静态资源
        // 例如: /uploads/** -> file:D:/project/uploads/
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + absolutePath);

        System.out.println("========================================");
        System.out.println("静态资源映射: " + urlPrefix + "/** -> file:" + absolutePath);
        System.out.println("========================================");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许前端访问上传的图片
        registry.addMapping("/uploads/**")
                .allowedOrigins("*")
                .allowedMethods("GET");
    }
}

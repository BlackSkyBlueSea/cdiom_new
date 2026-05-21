package com.cdiom.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CDIOM系统启动类
 * 
 * @author cdiom
 */
@SpringBootApplication
@MapperScan("com.cdiom.backend.mapper")
@EnableScheduling
public class CdiomApplication {
    public static void main(String[] args) {
        SpringApplication.run(CdiomApplication.class, args);
        // 控制台仅输出 ASCII：避免 Windows 默认 GBK 终端将 UTF-8 制表符/中文/Emoji 误解析为乱码
        System.out.println("=============================================================");
        System.out.println("  CDIOM backend started successfully.");
        System.out.println("  System is ready to serve HTTP requests.");
        System.out.println("=============================================================");
    }
}


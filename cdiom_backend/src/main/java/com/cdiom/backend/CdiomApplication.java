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
        System.out.println("(♥◠‿◠)ﾉﾞ  CDIOM系统启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}


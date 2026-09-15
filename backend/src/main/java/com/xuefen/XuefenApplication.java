package com.xuefen;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xuefen.mapper")
public class XuefenApplication {
    public static void main(String[] args) {
        SpringApplication.run(XuefenApplication.class, args);
        System.out.println("========================================");
        System.out.println("  校园课程学业规划智能助手系统 启动成功!");
        System.out.println("  后端地址: http://localhost:8080/api");
        System.out.println("  H2控制台: http://localhost:8080/api/h2-console");
        System.out.println("========================================");
    }
}

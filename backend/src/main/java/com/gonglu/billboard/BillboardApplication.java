package com.gonglu.billboard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gonglu.billboard.mapper")
public class BillboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillboardApplication.class, args);
    }
}

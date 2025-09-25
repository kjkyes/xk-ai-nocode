package com.xk.xkainocode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.xk.xkainocode.mapper")
public class XkAiNocodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(XkAiNocodeApplication.class, args);
    }

}

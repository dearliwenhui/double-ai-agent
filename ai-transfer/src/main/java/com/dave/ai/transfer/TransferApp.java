package com.dave.ai.transfer;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@MapperScan({"com.dave.ai.transfer.mapper"})
public class TransferApp {
    public static void main(String[] args) {
        SpringApplication.run(TransferApp.class, args);
        log.info("TransferApp started successfully.");
    }
}

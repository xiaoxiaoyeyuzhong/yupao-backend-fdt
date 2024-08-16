package com.fdt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.fdt.mapper")
@EnableScheduling
public class YupaoBackendFdtApplication {

	public static void main(String[] args) {
		SpringApplication.run(YupaoBackendFdtApplication.class, args);
	}

}

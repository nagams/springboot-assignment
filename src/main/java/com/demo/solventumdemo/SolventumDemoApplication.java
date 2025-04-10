package com.demo.solventumdemo;

import com.demo.solventumdemo.config.DemoConfigProperties;
import com.demo.solventumdemo.config.RsaKeyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({DemoConfigProperties.class, RsaKeyProperties.class})
@SpringBootApplication
public class SolventumDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolventumDemoApplication.class, args);
    }

}

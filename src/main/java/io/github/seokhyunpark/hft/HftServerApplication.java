package io.github.seokhyunpark.hft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import io.github.seokhyunpark.hft.trading.config.TradingProperties;

@SpringBootApplication
@EnableConfigurationProperties(TradingProperties.class)
public class HftServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(HftServerApplication.class, args);
    }
}

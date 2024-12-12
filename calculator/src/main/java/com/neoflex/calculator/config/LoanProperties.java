package com.neoflex.calculator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Data
@Configuration
@ConfigurationProperties("loan")
public class LoanProperties {

    private BigDecimal baseRate;
    private BigDecimal insuranceDiscount;
    private BigDecimal salaryClientDiscount;
    private BigDecimal insuranceCost;
}

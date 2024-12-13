package com.neoflex.calculator.dto;

import com.neoflex.calculator.enums.EmploymentStatus;
import com.neoflex.calculator.enums.Position;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmploymentDto {

    private EmploymentStatus employmentStatus;
    private String employerINN;
    private BigDecimal salary;
    private Position position;
    private Integer workExperienceTotal;
    private Integer workExperienceCurrent;
}

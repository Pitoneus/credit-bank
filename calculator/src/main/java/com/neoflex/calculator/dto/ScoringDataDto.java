package com.neoflex.calculator.dto;

import com.neoflex.calculator.enums.Gender;
import com.neoflex.calculator.enums.MaritalStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ScoringDataDto {

    private BigDecimal amount;
    private Integer term;
    private String firstName;
    private String lastName;
    private String middleName;
    private Gender gender;
    private LocalDate birthDate;
    private String passportSeries;
    private String passportNumber;
    private LocalDate passportIssueDate;
    private String passportIssueBranch;
    private MaritalStatus maritalStatus;
    private Integer dependentAmount;
    private EmploymentDto employment;
    private String accountNumber;
    private Boolean isInsuranceEnabled;
    private Boolean isSalaryClient;
}

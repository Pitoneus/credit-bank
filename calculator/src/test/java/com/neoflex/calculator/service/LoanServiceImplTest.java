package com.neoflex.calculator.service;

import com.neoflex.calculator.config.LoanProperties;
import com.neoflex.calculator.dto.*;
import com.neoflex.calculator.enums.EmploymentStatus;
import com.neoflex.calculator.enums.Gender;
import com.neoflex.calculator.enums.MaritalStatus;
import com.neoflex.calculator.enums.Position;
import com.neoflex.calculator.exception.LoanServiceException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public class LoanServiceImplTest {

    @Mock
    private LoanProperties loanProperties;

    @InjectMocks
    private LoanServiceImpl loanService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Mockito.when(loanProperties.getBaseRate()).thenReturn(BigDecimal.valueOf(15.0));
        Mockito.when(loanProperties.getInsuranceDiscount()).thenReturn(BigDecimal.valueOf(3.0));
        Mockito.when(loanProperties.getSalaryClientDiscount()).thenReturn(BigDecimal.valueOf(1.0));
        Mockito.when(loanProperties.getInsuranceCost()).thenReturn(BigDecimal.valueOf(1000.0));
    }

    @Test
    public void testGenerateLoanOffers() {
        LoanStatementRequestDto request = new LoanStatementRequestDto();
        request.setAmount(BigDecimal.valueOf(100000));
        request.setTerm(12);
        request.setEmail("test@example.com");
        request.setPassportSeries("1234");
        request.setPassportNumber("123456");
        request.setBirthDate(LocalDate.of(1990, 1, 1));

        List<LoanOfferDto> offers = loanService.generateLoanOffers(request);

        Assertions.assertEquals(4, offers.size());
        LoanOfferDto firstOffer = offers.get(0);
        LoanOfferDto lastOffer = offers.get(offers.size() - 1);
        Assertions.assertTrue(firstOffer.getRate().compareTo(lastOffer.getRate()) <= 0);
    }

    @Test
    public void testInvalidEmail() {
        LoanStatementRequestDto request = new LoanStatementRequestDto();
        request.setAmount(BigDecimal.valueOf(100000));
        request.setTerm(12);
        request.setEmail("invalid-email");
        request.setPassportSeries("1234");
        request.setPassportNumber("123456");
        request.setBirthDate(LocalDate.of(1990, 1, 1));

        try {
            loanService.generateLoanOffers(request);
            Assertions.fail("Expected LoanServiceException due to invalid email format");
        } catch (LoanServiceException e) {
            Assertions.assertEquals("Invalid email format.", e.getMessage());
        }
    }

    @Test
    public void testCalculateCredit() {
        ScoringDataDto scoringData = new ScoringDataDto();
        scoringData.setAmount(BigDecimal.valueOf(100000));
        scoringData.setTerm(12);
        scoringData.setIsInsuranceEnabled(true);
        scoringData.setIsSalaryClient(true);
        scoringData.setGender(Gender.MALE);
        scoringData.setBirthDate(LocalDate.of(1990, 1, 1));
        scoringData.setEmployment(new EmploymentDto());
        scoringData.getEmployment().setEmploymentStatus(EmploymentStatus.EMPLOYED);
        scoringData.getEmployment().setSalary(BigDecimal.valueOf(50000));
        scoringData.getEmployment().setPosition(Position.WORKER);
        scoringData.getEmployment().setWorkExperienceTotal(20);
        scoringData.getEmployment().setWorkExperienceCurrent(5);
        scoringData.setMaritalStatus(MaritalStatus.SINGLE);

        CreditDto creditDto = loanService.calculateCredit(scoringData);

        Assertions.assertNotNull(creditDto);
        Assertions.assertEquals(BigDecimal.valueOf(12).setScale(2, RoundingMode.HALF_UP), creditDto.getRate());
        Assertions.assertEquals(12, creditDto.getTerm());
        Assertions.assertEquals(BigDecimal.valueOf(100000), creditDto.getAmount());
    }

    @Test
    public void testInvalidAge() {
        ScoringDataDto scoringData = new ScoringDataDto();
        scoringData.setAmount(BigDecimal.valueOf(100000));
        scoringData.setTerm(12);
        scoringData.setIsInsuranceEnabled(true);
        scoringData.setIsSalaryClient(true);
        scoringData.setGender(Gender.MALE);
        scoringData.setBirthDate(LocalDate.of(1990, 1, 1));
        scoringData.setEmployment(new EmploymentDto());
        scoringData.getEmployment().setSalary(BigDecimal.valueOf(50000));
        scoringData.getEmployment().setEmploymentStatus(EmploymentStatus.EMPLOYED);
        scoringData.getEmployment().setWorkExperienceTotal(5);
        scoringData.setMaritalStatus(MaritalStatus.SINGLE);

        try {
            scoringData.setBirthDate(LocalDate.of(2005, 1, 1));
            loanService.calculateCredit(scoringData);
            Assertions.fail("Expected LoanServiceException due to invalid age");
        } catch (LoanServiceException e) {
            Assertions.assertEquals("Borrower age must be between 20 and 65 years.", e.getMessage());
        }
    }

    @Test
    public void testInvalidPassport() {
        LoanStatementRequestDto request = new LoanStatementRequestDto();
        request.setAmount(BigDecimal.valueOf(100000));
        request.setTerm(12);
        request.setEmail("test@example.com");
        request.setPassportSeries("1234");
        request.setPassportNumber("12345"); // Invalid passport number
        request.setBirthDate(LocalDate.of(1990, 1, 1));

        try {
            loanService.generateLoanOffers(request);
            Assertions.fail("Expected LoanServiceException due to invalid passport details");
        } catch (LoanServiceException e) {
            Assertions.assertEquals("Invalid passport details.", e.getMessage());
        }
    }
}

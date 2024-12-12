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
        Mockito.when(loanProperties.getBaseRate()).thenReturn(BigDecimal.valueOf(15.00));
        Mockito.when(loanProperties.getInsuranceDiscount()).thenReturn(BigDecimal.valueOf(3.00));
        Mockito.when(loanProperties.getSalaryClientDiscount()).thenReturn(BigDecimal.valueOf(1.00));
        Mockito.when(loanProperties.getInsuranceCost()).thenReturn(BigDecimal.valueOf(1000.00));
    }

    @Test
    public void testGenerateLoanOffers() {
        LoanStatementRequestDto request = new LoanStatementRequestDto();
        request.setAmount(BigDecimal.valueOf(100000));
        request.setTerm(12);
        request.setEmail("test@example.com");
        request.setPassportSeries("1234");
        request.setPassportNumber("123456");
        request.setBirthDate(LocalDate.of(2000, 1, 1));

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
        request.setEmail("invalid email");
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
        scoringData.setBirthDate(LocalDate.of(2000, 1, 1));
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
        request.setEmail("test@mail.com");
        request.setPassportSeries("1234");
        request.setPassportNumber("12345");
        request.setBirthDate(LocalDate.of(1990, 1, 1));

        try {
            loanService.generateLoanOffers(request);
            Assertions.fail("Expected LoanServiceException due to invalid passport details");
        } catch (LoanServiceException e) {
            Assertions.assertEquals("Invalid passport details.", e.getMessage());
        }
    }

    @Test
    public void testLoanAmountBelowMinimum() {
        LoanStatementRequestDto request = new LoanStatementRequestDto();
        request.setAmount(BigDecimal.valueOf(15000));
        request.setTerm(12);
        request.setEmail("test@mail.com");
        request.setPassportSeries("1234");
        request.setPassportNumber("123456");
        request.setBirthDate(LocalDate.of(1990, 1, 1));

        try {
            loanService.generateLoanOffers(request);
            Assertions.fail("Expected LoanServiceException due to loan amount below minimum");
        } catch (LoanServiceException e) {
            Assertions.assertEquals("Loan amount must be at least 20000.", e.getMessage());
        }
    }

    @Test
    public void testLoanTermBelowMinimum() {
        LoanStatementRequestDto request = new LoanStatementRequestDto();
        request.setAmount(BigDecimal.valueOf(50000));
        request.setTerm(5);
        request.setEmail("test@mail.com");
        request.setPassportSeries("1234");
        request.setPassportNumber("123456");
        request.setBirthDate(LocalDate.of(1990, 1, 1));

        try {
            loanService.generateLoanOffers(request);
            Assertions.fail("Expected LoanServiceException due to loan term below minimum");
        } catch (LoanServiceException e) {
            Assertions.assertEquals("Loan term must be at least 6 months.", e.getMessage());
        }
    }

    @Test
    public void testBorrowerBelowMinimumAge() {
        LoanStatementRequestDto request = new LoanStatementRequestDto();
        request.setAmount(BigDecimal.valueOf(50000));
        request.setTerm(12);
        request.setEmail("test@mail.com");
        request.setPassportSeries("1234");
        request.setPassportNumber("123456");
        request.setBirthDate(LocalDate.now().minusYears(17));

        try {
            loanService.generateLoanOffers(request);
            Assertions.fail("Expected LoanServiceException due to borrower below minimum age");
        } catch (LoanServiceException e) {
            Assertions.assertEquals("Borrower must be at least 18 years old.", e.getMessage());
        }
    }

    @Test
    public void testInsufficientWorkExperience() {
        ScoringDataDto scoringData = new ScoringDataDto();
        scoringData.setAmount(BigDecimal.valueOf(100000));
        scoringData.setTerm(12);
        scoringData.setIsInsuranceEnabled(true);
        scoringData.setIsSalaryClient(true);
        scoringData.setGender(Gender.MALE);
        scoringData.setBirthDate(LocalDate.of(1990, 1, 1));
        EmploymentDto employment = new EmploymentDto();
        employment.setEmploymentStatus(EmploymentStatus.EMPLOYED);
        employment.setSalary(BigDecimal.valueOf(50000));
        employment.setWorkExperienceTotal(10);
        employment.setWorkExperienceCurrent(1);
        scoringData.setEmployment(employment);

        try {
            loanService.calculateCredit(scoringData);
            Assertions.fail("Expected LoanServiceException due to insufficient work experience");
        } catch (LoanServiceException e) {
            Assertions.assertEquals("Insufficient work experience.", e.getMessage());
        }
    }

    @Test
    public void testLoanAmountExceedsIncomeLimit() {
        ScoringDataDto scoringData = new ScoringDataDto();
        scoringData.setAmount(BigDecimal.valueOf(1500000));
        scoringData.setTerm(12);
        scoringData.setIsInsuranceEnabled(true);
        scoringData.setIsSalaryClient(true);
        scoringData.setGender(Gender.MALE);
        scoringData.setBirthDate(LocalDate.of(1990, 1, 1));
        EmploymentDto employment = new EmploymentDto();
        employment.setEmploymentStatus(EmploymentStatus.EMPLOYED);
        employment.setSalary(BigDecimal.valueOf(50000));
        employment.setWorkExperienceTotal(20);
        employment.setWorkExperienceCurrent(10);
        scoringData.setEmployment(employment);

        try {
            loanService.calculateCredit(scoringData);
            Assertions.fail("Expected LoanServiceException due to loan amount exceeding income limit");
        } catch (LoanServiceException e) {
            Assertions.assertEquals("Loan amount exceeds 24 times monthly income.", e.getMessage());
        }
    }

    @Test
    public void testLoanForUnemployedBorrower() {
        ScoringDataDto scoringData = new ScoringDataDto();
        scoringData.setAmount(BigDecimal.valueOf(50000));
        scoringData.setTerm(12);
        scoringData.setIsInsuranceEnabled(true);
        scoringData.setIsSalaryClient(true);
        scoringData.setGender(Gender.MALE);
        scoringData.setBirthDate(LocalDate.of(1990, 1, 1));
        EmploymentDto employment = new EmploymentDto();
        employment.setEmploymentStatus(EmploymentStatus.UNEMPLOYED);
        scoringData.setEmployment(employment);
        employment.setSalary(BigDecimal.valueOf(50000));
        employment.setWorkExperienceTotal(20);
        employment.setWorkExperienceCurrent(10);

        try {
            loanService.calculateCredit(scoringData);
            Assertions.fail("Expected LoanServiceException due to unemployed borrower");
        } catch (LoanServiceException e) {
            Assertions.assertEquals("Loan cannot be issued to unemployed borrowers.", e.getMessage());
        }
    }

    @Test
    public void testEmploymentStatusRateChanges() {
        ScoringDataDto scoringData = new ScoringDataDto();
        scoringData.setAmount(BigDecimal.valueOf(50000));
        scoringData.setTerm(12);
        scoringData.setGender(Gender.MALE);
        scoringData.setBirthDate(LocalDate.of(2000, 1, 1));
        scoringData.setMaritalStatus(MaritalStatus.SINGLE);
        EmploymentDto employment = new EmploymentDto();
        employment.setSalary(BigDecimal.valueOf(50000));
        employment.setPosition(Position.WORKER);
        employment.setWorkExperienceTotal(20);
        employment.setWorkExperienceCurrent(10);

        employment.setEmploymentStatus(EmploymentStatus.SELF_EMPLOYED);
        scoringData.setEmployment(employment);
        Assertions.assertEquals(
                loanProperties.getBaseRate().add(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP),
                loanService.calculateCredit(scoringData).getRate()
        );

        employment.setEmploymentStatus(EmploymentStatus.BUSINESS_OWNER);
        scoringData.setEmployment(employment);
        Assertions.assertEquals(
                loanProperties.getBaseRate().add(BigDecimal.valueOf(1)).setScale(2, RoundingMode.HALF_UP),
                loanService.calculateCredit(scoringData).getRate()
        );
    }

    @Test
    public void testGenderRateChanges() {
        ScoringDataDto scoringData = new ScoringDataDto();
        scoringData.setAmount(BigDecimal.valueOf(50000));
        scoringData.setTerm(12);
        scoringData.setGender(Gender.NON_BINARY);
        scoringData.setBirthDate(LocalDate.of(1990, 1, 1));
        EmploymentDto employment = new EmploymentDto();
        employment.setEmploymentStatus(EmploymentStatus.EMPLOYED);
        employment.setSalary(BigDecimal.valueOf(50000));
        employment.setPosition(Position.WORKER);
        employment.setWorkExperienceTotal(20);
        employment.setWorkExperienceCurrent(10);
        scoringData.setEmployment(employment);
        scoringData.setMaritalStatus(MaritalStatus.SINGLE);

        Assertions.assertEquals(
                loanProperties.getBaseRate().add(BigDecimal.valueOf(7)).setScale(2, RoundingMode.HALF_UP),
                loanService.calculateCredit(scoringData).getRate()
        );
    }

    @Test
    public void testPositionRateChanges() {
        ScoringDataDto scoringData = new ScoringDataDto();
        scoringData.setAmount(BigDecimal.valueOf(50000));
        scoringData.setTerm(12);
        scoringData.setGender(Gender.MALE);
        scoringData.setBirthDate(LocalDate.of(2000, 1, 1));
        EmploymentDto employment = new EmploymentDto();
        employment.setEmploymentStatus(EmploymentStatus.EMPLOYED);
        employment.setSalary(BigDecimal.valueOf(50000));
        employment.setWorkExperienceTotal(20);
        employment.setWorkExperienceCurrent(10);

        employment.setPosition(Position.MID_MANAGER);
        scoringData.setEmployment(employment);
        scoringData.setMaritalStatus(MaritalStatus.SINGLE);
        Assertions.assertEquals(
                loanProperties.getBaseRate().subtract(BigDecimal.valueOf(2).setScale(2, RoundingMode.HALF_UP)),
                loanService.calculateCredit(scoringData).getRate()
        );

        employment.setPosition(Position.TOP_MANAGER);
        scoringData.setEmployment(employment);
        Assertions.assertEquals(
                loanProperties.getBaseRate().subtract(BigDecimal.valueOf(3).setScale(2, RoundingMode.HALF_UP)),
                loanService.calculateCredit(scoringData).getRate()
        );
    }

    @Test
    public void testMaritalStatusRateChanges() {
        ScoringDataDto scoringData = new ScoringDataDto();
        scoringData.setAmount(BigDecimal.valueOf(50000));
        scoringData.setTerm(12);
        scoringData.setGender(Gender.FEMALE);
        scoringData.setBirthDate(LocalDate.of(1980, 1, 1));
        EmploymentDto employment = new EmploymentDto();
        employment.setEmploymentStatus(EmploymentStatus.EMPLOYED);
        employment.setSalary(BigDecimal.valueOf(50000));
        employment.setPosition(Position.WORKER);
        employment.setWorkExperienceTotal(20);
        employment.setWorkExperienceCurrent(10);
        scoringData.setEmployment(employment);

        scoringData.setMaritalStatus(MaritalStatus.MARRIED);
        Assertions.assertEquals(
                loanProperties.getBaseRate().subtract(BigDecimal.valueOf(6).setScale(2, RoundingMode.HALF_UP)),
                loanService.calculateCredit(scoringData).getRate()
        );

        scoringData.setMaritalStatus(MaritalStatus.DIVORCED);
        Assertions.assertEquals(
                loanProperties.getBaseRate().subtract(BigDecimal.valueOf(2).setScale(2, RoundingMode.HALF_UP)),
                loanService.calculateCredit(scoringData).getRate()
        );
    }
}

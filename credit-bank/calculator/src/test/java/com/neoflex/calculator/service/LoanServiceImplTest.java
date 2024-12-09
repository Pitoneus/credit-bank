package com.neoflex.calculator.service;

import com.neoflex.calculator.config.LoanProperties;
import com.neoflex.calculator.dto.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
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

        List<LoanOfferDto> offers = loanService.generateLoanOffers(request);

        Assertions.assertEquals(4, offers.size());
    }

    @Test
    public void testCalculateCredit() {
        ScoringDataDto scoringData = new ScoringDataDto();
        scoringData.setAmount(BigDecimal.valueOf(100000));
        scoringData.setTerm(12);
        scoringData.setIsInsuranceEnabled(true);
        scoringData.setIsSalaryClient(true);

        CreditDto creditDto = loanService.calculateCredit(scoringData);

        Assertions.assertEquals(BigDecimal.valueOf(11.0), creditDto.getRate());
        Assertions.assertEquals(12, creditDto.getTerm());
        Assertions.assertEquals(BigDecimal.valueOf(100000), creditDto.getAmount());
    }
}

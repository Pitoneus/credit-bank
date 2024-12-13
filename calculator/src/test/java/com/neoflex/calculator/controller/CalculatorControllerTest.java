package com.neoflex.calculator.controller;

import com.neoflex.calculator.dto.CreditDto;
import com.neoflex.calculator.dto.LoanOfferDto;
import com.neoflex.calculator.dto.LoanStatementRequestDto;
import com.neoflex.calculator.dto.ScoringDataDto;
import com.neoflex.calculator.service.LoanServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class CalculatorControllerTest {

    @InjectMocks
    private CalculatorController calculatorController;

    @Mock
    private LoanServiceImpl loanService;

    @Test
    void testCalculateLoanOffers() {

        LoanStatementRequestDto request = new LoanStatementRequestDto();
        List<LoanOfferDto> expectedOffers = List.of(LoanOfferDto.builder().build());
        Mockito.when(loanService.generateLoanOffers(request)).thenReturn(expectedOffers);

        ResponseEntity<List<LoanOfferDto>> response = calculatorController.calculateLoanOffers(request);

        Mockito.verify(loanService).generateLoanOffers(request);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatusCodeValue());
        Assertions.assertEquals(expectedOffers, response.getBody());
    }

    @Test
    void testCalculateCredit() {

        ScoringDataDto scoringData = new ScoringDataDto();
        CreditDto expectedCredit = CreditDto.builder().build();
        Mockito.when(loanService.calculateCredit(scoringData)).thenReturn(expectedCredit);

        ResponseEntity<CreditDto> response = calculatorController.calculateCredit(scoringData);

        Mockito.verify(loanService).calculateCredit(scoringData);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatusCodeValue());
        Assertions.assertEquals(expectedCredit, response.getBody());
    }

}

package com.neoflex.calculator.controller;

import com.neoflex.calculator.dto.CreditDto;
import com.neoflex.calculator.dto.LoanOfferDto;
import com.neoflex.calculator.dto.LoanStatementRequestDto;
import com.neoflex.calculator.dto.ScoringDataDto;
import com.neoflex.calculator.service.LoanServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/calculator")
public class CalculatorController {

    private final LoanServiceImpl loanService;
    private static final Logger logger = LoggerFactory.getLogger(CalculatorController.class);

    @PostMapping("/offers")
    public List<LoanOfferDto> calculateLoanOffers(@RequestBody LoanStatementRequestDto request){

        logger.info("Received request for loan offers: {}", request);

        List<LoanOfferDto> loanOffers = loanService.generateLoanOffers(request);
        logger.info("Generated loan offers: {}", loanOffers);

        return loanOffers;
    }

    @PostMapping("/calc")
    public CreditDto calculateCredit(@RequestBody ScoringDataDto scoringData){

        logger.info("Received scoring data for credit calculation: {}", scoringData);

        CreditDto credit = loanService.calculateCredit(scoringData);
        logger.info("Calculated credit: {}", credit);

        return credit;
    }
}

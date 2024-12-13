package com.neoflex.calculator.controller;

import com.neoflex.calculator.dto.CreditDto;
import com.neoflex.calculator.dto.LoanOfferDto;
import com.neoflex.calculator.dto.LoanStatementRequestDto;
import com.neoflex.calculator.dto.ScoringDataDto;
import com.neoflex.calculator.service.LoanServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/calculator")
public class CalculatorController {

    private final LoanServiceImpl loanService;

    @PostMapping("/offers")
    public ResponseEntity<List<LoanOfferDto>> calculateLoanOffers(@RequestBody LoanStatementRequestDto request) {
        log.info("Received request for loan offers: {}", request);
        List<LoanOfferDto> loanOffers = loanService.generateLoanOffers(request);
        log.info("Generated loan offers: {}", loanOffers);

        return ResponseEntity.ok(loanOffers);
    }

    @PostMapping("/calc")
    public ResponseEntity<CreditDto> calculateCredit(@RequestBody ScoringDataDto scoringData) {
        log.info("Received scoring data for credit calculation: {}", scoringData);
        CreditDto credit = loanService.calculateCredit(scoringData);
        log.info("Calculated credit: {}", credit);

        return ResponseEntity.ok(credit);
    }

}

package com.neoflex.calculator.service.api;

import com.neoflex.calculator.dto.CreditDto;
import com.neoflex.calculator.dto.LoanOfferDto;
import com.neoflex.calculator.dto.LoanStatementRequestDto;
import com.neoflex.calculator.dto.ScoringDataDto;

import java.util.List;

public interface LoanService {

    List<LoanOfferDto> generateLoanOffers(LoanStatementRequestDto request);
    CreditDto calculateCredit(ScoringDataDto scoringData);
}

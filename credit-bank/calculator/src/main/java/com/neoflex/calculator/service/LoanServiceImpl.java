package com.neoflex.calculator.service;

import com.neoflex.calculator.config.LoanProperties;
import com.neoflex.calculator.dto.*;
import com.neoflex.calculator.service.api.LoanService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanProperties loanProperties;
    private static final Logger logger = LoggerFactory.getLogger(LoanServiceImpl.class);

    @Override
    public List<LoanOfferDto> generateLoanOffers(LoanStatementRequestDto request) {

        logger.debug("Starting to generate loan offers for request: {}", request);

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Invalid amount: {}. Amount of request must be greater than zero.", request.getAmount());
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (request.getTerm() <= 0) {
            logger.error("Invalid term: {}. Term of request must be greater than zero.", request.getTerm());
            throw new IllegalArgumentException("Term must be greater than zero.");
        }

        List<LoanOfferDto> loanOffers = new ArrayList<>();

        boolean[] insuranceOptions = {false, true};
        boolean[] salaryClientOptions = {false, true};

        for (boolean insurance : insuranceOptions) {
            for (boolean salaryClient : salaryClientOptions) {
                LoanOfferDto offer = createLoanOffer(request, insurance, salaryClient);
                logger.debug("Generated loan offer: {}", offer);

                loanOffers.add(offer);
            }
        }

        loanOffers.sort((o1, o2) -> o2.getRate().compareTo(o1.getRate()));
        logger.debug("Sorted loan offers by rate: {}", loanOffers);

        return loanOffers;
    }

    @Override
    public CreditDto calculateCredit(ScoringDataDto scoringData) {

        logger.debug("Calculating credit for scoring data: {}", scoringData);

        if (scoringData.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Invalid amount: {}. Amount of scoringData must be greater than zero.", scoringData.getAmount());
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (scoringData.getTerm() <= 0) {
            logger.error("Invalid term: {}. Term of scoringData must be greater than zero.", scoringData.getTerm());
            throw new IllegalArgumentException("Term must be greater than zero.");
        }

        BigDecimal rate = determineRate(scoringData);

        BigDecimal psk = calculatePSK(scoringData.getAmount(), rate, scoringData.getTerm());

        BigDecimal monthlyPayment = calculateMonthlyPayment(scoringData.getAmount(), rate, scoringData.getTerm());

        List<PaymentScheduleElementDto> paymentSchedule = generatePaymentSchedule(scoringData.getAmount(), rate, scoringData.getTerm());
        logger.debug("Generated payment schedule: {}", paymentSchedule);

        CreditDto credit = CreditDto.builder()
                .amount(scoringData.getAmount())
                .term(scoringData.getTerm())
                .monthlyPayment(monthlyPayment)
                .rate(rate)
                .psk(psk)
                .isInsuranceEnabled(scoringData.getIsInsuranceEnabled())
                .isSalaryClient(scoringData.getIsSalaryClient())
                .paymentSchedule(paymentSchedule)
                .build();
        logger.info("Calculated credit: {}", credit);

        return credit;
    }

    private LoanOfferDto createLoanOffer(LoanStatementRequestDto request, boolean isInsuranceEnabled, boolean isSalaryClient){

        BigDecimal rate = loanProperties.getBaseRate();

        if (isInsuranceEnabled) rate = rate.subtract(loanProperties.getInsuranceDiscount());
        if (isSalaryClient) rate = rate.subtract(loanProperties.getSalaryClientDiscount());

        BigDecimal totalAmount = request.getAmount();

        if (isInsuranceEnabled) totalAmount = totalAmount.add(loanProperties.getInsuranceCost());

        BigDecimal monthlyPayment = calculateMonthlyPayment(totalAmount, rate, request.getTerm());

        LoanOfferDto loanOffer = LoanOfferDto.builder()
                .statementId(UUID.randomUUID())
                .requestedAmount(request.getAmount())
                .totalAmount(totalAmount)
                .term(request.getTerm())
                .monthlyPayment(monthlyPayment)
                .rate(rate)
                .isInsuranceEnabled(isInsuranceEnabled)
                .isSalaryClient(isSalaryClient)
                .build();
        logger.debug("Created loan offer: {}", loanOffer);

        return loanOffer;
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal rate, Integer term) {

        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal numerator = monthlyRate.multiply(amount);
        BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.add(monthlyRate).pow(-term, MathContext.DECIMAL128));
        BigDecimal monthlyPayment = numerator.divide(denominator, 2, RoundingMode.HALF_UP);
        logger.debug("Calculated monthly payment: {}", monthlyPayment);

        return monthlyPayment;
    }

    private BigDecimal determineRate(ScoringDataDto scoringData) {

        BigDecimal rate = loanProperties.getBaseRate();
        if (scoringData.getIsInsuranceEnabled()) rate = rate.subtract(loanProperties.getInsuranceDiscount());
        if (scoringData.getIsSalaryClient()) rate = rate.subtract(loanProperties.getSalaryClientDiscount()).setScale(2, RoundingMode.HALF_UP);
        logger.debug("Determined rate: {}", rate);

        return rate;
    }

    private BigDecimal calculatePSK(BigDecimal amount, BigDecimal rate, Integer term) {

        BigDecimal totalPayment = calculateMonthlyPayment(amount, rate, term).multiply(BigDecimal.valueOf(term));
        BigDecimal psk = totalPayment.subtract(amount).divide(amount, 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        logger.debug("Calculated PSK: {}", psk);

        return psk;
    }

    private List<PaymentScheduleElementDto> generatePaymentSchedule(BigDecimal amount, BigDecimal rate, Integer term){

        List<PaymentScheduleElementDto> paymentSchedule = new ArrayList<>();
        BigDecimal remainingDebt = amount;
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        for (int i = 1; i <= term; i++) {
            BigDecimal interestPayment = remainingDebt.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalPayment = calculateMonthlyPayment(amount, rate, term);

            if (i == term) {
                totalPayment = remainingDebt.add(interestPayment).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal debtPayment = totalPayment.subtract(interestPayment).setScale(2, RoundingMode.HALF_UP);
            remainingDebt = remainingDebt.subtract(debtPayment).setScale(2, RoundingMode.HALF_UP);

            PaymentScheduleElementDto paymentScheduleElement = PaymentScheduleElementDto.builder()
                    .number(i)
                    .date(LocalDate.now().plusMonths(i))
                    .totalPayment(totalPayment)
                    .interestPayment(interestPayment)
                    .debtPayment(debtPayment)
                    .remainingDebt(remainingDebt)
                    .build();
            logger.debug("Generated payment schedule element: {}", paymentScheduleElement);

            paymentSchedule.add(paymentScheduleElement);
        }

        logger.debug("Completed payment schedule: {}", paymentSchedule);
        return paymentSchedule;
    }
}

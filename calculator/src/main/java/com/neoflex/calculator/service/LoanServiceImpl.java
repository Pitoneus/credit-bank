package com.neoflex.calculator.service;

import com.neoflex.calculator.config.LoanProperties;
import com.neoflex.calculator.dto.*;
import com.neoflex.calculator.enums.EmploymentStatus;
import com.neoflex.calculator.enums.Gender;
import com.neoflex.calculator.exception.LoanServiceException;
import com.neoflex.calculator.service.api.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanProperties loanProperties;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-z0-9A-Z_!#$%&'*+/=?`{|}~^.-]+@[a-z0-9A-Z.-]+$");

    @Override
    public List<LoanOfferDto> generateLoanOffers(LoanStatementRequestDto request) {
        log.debug("Starting to generate loan offers for request: {}", request);
        validatePreScoring(request);

        List<LoanOfferDto> loanOffers = new ArrayList<>();
        boolean[] insuranceOptions = {false, true};
        boolean[] salaryClientOptions = {false, true};
        for (boolean insurance : insuranceOptions) {
            for (boolean salaryClient : salaryClientOptions) {
                LoanOfferDto offer = createLoanOffer(request, insurance, salaryClient);
                log.debug("Generated loan offer: {}", offer);

                loanOffers.add(offer);
            }
        }

        loanOffers.sort((firstOffer, secondOffer) -> firstOffer.getRate().compareTo(secondOffer.getRate()));
        log.debug("Sorted loan offers by rate: {}", loanOffers);

        return loanOffers;
    }

    private LoanOfferDto createLoanOffer(LoanStatementRequestDto request, boolean isInsuranceEnabled, boolean isSalaryClient) {
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
        log.debug("Created loan offer: {}", loanOffer);

        return loanOffer;
    }

    @Override
    public CreditDto calculateCredit(ScoringDataDto scoringData) {
        log.debug("Calculating credit for scoring data: {}", scoringData);
        validateScoring(scoringData);

        BigDecimal rate = determineRate(scoringData);
        BigDecimal psk = calculatePSK(scoringData.getAmount(), rate, scoringData.getTerm());
        BigDecimal monthlyPayment = calculateMonthlyPayment(scoringData.getAmount(), rate, scoringData.getTerm());
        List<PaymentScheduleElementDto> paymentSchedule = generatePaymentSchedule(scoringData.getAmount(), rate, scoringData.getTerm());
        log.debug("Generated payment schedule: {}", paymentSchedule);

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
        log.info("Calculated credit: {}", credit);

        return credit;
    }

    private void validatePreScoring(LoanStatementRequestDto request) {
        if (request.getAmount().compareTo(BigDecimal.valueOf(20000)) < 0) {
            throw new LoanServiceException("Loan amount must be at least 20000.");
        }
        if (request.getTerm() < 6) {
            throw new LoanServiceException("Loan term must be at least 6 months.");
        }
        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new LoanServiceException("Invalid email format.");
        }
        if (request.getPassportSeries().length() != 4 || request.getPassportNumber().length() != 6) {
            throw new LoanServiceException("Invalid passport details.");
        }
        LocalDate birthDate = request.getBirthDate();
        if (Period.between(birthDate, LocalDate.now()).getYears() < 18) {
            throw new LoanServiceException("Borrower must be at least 18 years old.");
        }
    }

    private void validateScoring(ScoringDataDto scoringData) {
        LocalDate birthDate = scoringData.getBirthDate();
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 20 || age > 65) {
            throw new LoanServiceException("Borrower age must be between 20 and 65 years.");
        }
        if (scoringData.getEmployment().getWorkExperienceTotal() < 18 || scoringData.getEmployment().getWorkExperienceCurrent() < 3) {
            throw new LoanServiceException("Insufficient work experience.");
        }
        BigDecimal maxLoanAmount = scoringData.getEmployment().getSalary().multiply(BigDecimal.valueOf(24));
        if (scoringData.getAmount().compareTo(maxLoanAmount) > 0) {
            throw new LoanServiceException("Loan amount exceeds 24 times monthly income.");
        }
        if (scoringData.getEmployment().getEmploymentStatus() == EmploymentStatus.UNEMPLOYED) {
            throw new LoanServiceException("Loan cannot be issued to unemployed borrowers.");
        }
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal rate, Integer term) {
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal numerator = monthlyRate.multiply(amount);
        BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.add(monthlyRate).pow(-term, MathContext.DECIMAL128));
        BigDecimal monthlyPayment = numerator.divide(denominator, 2, RoundingMode.HALF_UP);
        log.debug("Calculated monthly payment: {}", monthlyPayment);

        return monthlyPayment;
    }

    private BigDecimal determineRate(ScoringDataDto scoringData) {
        BigDecimal rate = loanProperties.getBaseRate();

        switch (scoringData.getEmployment().getEmploymentStatus()) {
            case SELF_EMPLOYED -> rate = rate.add(BigDecimal.valueOf(2));
            case BUSINESS_OWNER -> rate = rate.add(BigDecimal.valueOf(1));
        }

        switch (scoringData.getEmployment().getPosition()) {
            case MID_MANAGER -> rate = rate.subtract(BigDecimal.valueOf(2));
            case TOP_MANAGER -> rate = rate.subtract(BigDecimal.valueOf(3));
        }

        switch (scoringData.getMaritalStatus()) {
            case MARRIED -> rate = rate.subtract(BigDecimal.valueOf(3));
            case DIVORCED -> rate = rate.add(BigDecimal.valueOf(1));
        }

        int age = Period.between(scoringData.getBirthDate(), LocalDate.now()).getYears();
        if (scoringData.getGender() == Gender.FEMALE && age >= 32 && age <= 60 ||
                scoringData.getGender() == Gender.MALE && age >= 30 && age <= 55) {
            rate = rate.subtract(BigDecimal.valueOf(3));
        } else if (scoringData.getGender() == Gender.NON_BINARY) {
            rate = rate.add(BigDecimal.valueOf(7));
        }

        return rate.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePSK(BigDecimal amount, BigDecimal rate, Integer term) {
        BigDecimal totalPayment = calculateMonthlyPayment(amount, rate, term).multiply(BigDecimal.valueOf(term));
        BigDecimal psk = totalPayment.subtract(amount).divide(amount, 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        log.debug("Calculated PSK: {}", psk);

        return psk;
    }

    private List<PaymentScheduleElementDto> generatePaymentSchedule(BigDecimal amount, BigDecimal rate, Integer term) {
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
            log.debug("Generated payment schedule element: {}", paymentScheduleElement);
            paymentSchedule.add(paymentScheduleElement);
        }

        log.debug("Completed payment schedule: {}", paymentSchedule);
        return paymentSchedule;
    }
}

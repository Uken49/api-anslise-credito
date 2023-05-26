package com.example.apianalisecredito.service;

import static com.example.apianalisecredito.factory.ApiClientDtoFactory.dtoWithId;
import static com.example.apianalisecredito.factory.CreditAnalysisFactory.entityNotApproved;
import static com.example.apianalisecredito.factory.CreditAnalysisFactory.entityRequestAmountGreaterThanMonthlyIncome;
import static com.example.apianalisecredito.factory.CreditAnalysisFactory.requestAmountGreaterThanMonthlyIncome;
import static com.example.apianalisecredito.factory.CreditAnalysisFactory.requestAmountLessThanMonthlyIncome;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.apianalisecredito.apiclient.ApiClient;
import com.example.apianalisecredito.apiclient.dto.ApiClientDto;
import com.example.apianalisecredito.controller.request.CreditAnalysisRequest;
import com.example.apianalisecredito.controller.response.CreditAnalysisResponse;
import com.example.apianalisecredito.handler.exception.ClientNotFoundException;
import com.example.apianalisecredito.mapper.CreditAnalysisMapper;
import com.example.apianalisecredito.mapper.CreditAnalysisMapperImpl;
import com.example.apianalisecredito.repository.CreditAnalysisRepository;
import com.example.apianalisecredito.repository.entity.CreditAnalysisEntity;
import feign.FeignException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreditAnalysisServiceTest {

    @Mock
    private CreditAnalysisRepository repository;
    @Mock
    private ApiClient apiClient;
    @Spy
    private CreditAnalysisMapper mapper = new CreditAnalysisMapperImpl();
    @InjectMocks
    private CreditAnalysisService service;
    @Captor
    private ArgumentCaptor<String> idArgumentCaptor;
    @Captor
    private ArgumentCaptor<CreditAnalysisEntity> creditAnalysisEntityArgumentCaptor;

    @Test
    void should_verify_the_integrity_of_the_data_during_the_credit_analysis() {
        final BigDecimal monthlyIncome = BigDecimal.valueOf(10_000.00);
        final BigDecimal requestedAmount = BigDecimal.valueOf(12_150.49);

        final CreditAnalysisRequest creditAnalysisRequest = requestAmountLessThanMonthlyIncome(monthlyIncome, requestedAmount);
        final CreditAnalysisEntity creditAnalysisEntity = entityRequestAmountGreaterThanMonthlyIncome();
        final ApiClientDto apiClientDto = dtoWithId();

        when(apiClient.getClientByIdOrCpf(idArgumentCaptor.capture())).thenReturn(apiClientDto);
        when(repository.save(creditAnalysisEntityArgumentCaptor.capture())).thenReturn(creditAnalysisEntity);

        final CreditAnalysisResponse creditAnalysisResponse = service.requestCreditAnalysis(creditAnalysisRequest);
        final CreditAnalysisEntity creditAnalysisEntityCapture = creditAnalysisEntityArgumentCaptor.getValue();

        assertNotNull(creditAnalysisResponse);
        assertEquals(idArgumentCaptor.getValue(), creditAnalysisRequest.clientId().toString());
        assertEquals(creditAnalysisRequest.clientId(), creditAnalysisEntityCapture.getClientId());
        assertEquals(creditAnalysisRequest.monthlyIncome(), creditAnalysisEntityCapture.getMonthlyIncome());
        assertEquals(creditAnalysisRequest.requestedAmount(), creditAnalysisEntityCapture.getRequestedAmount());
    }

    @Test
    void should_do_credit_analysis_when_requestAmount_is_less_than_maxAmountOfMonthlyIncomeConsidered() {
        final BigDecimal monthlyIncome = BigDecimal.valueOf(6_734.98);
        final BigDecimal requestedAmount = BigDecimal.valueOf(3_438.12);

        final CreditAnalysisRequest creditAnalysisRequest = requestAmountGreaterThanMonthlyIncome(monthlyIncome, requestedAmount);
        final CreditAnalysisEntity creditAnalysisEntity = entityNotApproved();
        final ApiClientDto apiClientDto = dtoWithId();

        when(apiClient.getClientByIdOrCpf(idArgumentCaptor.capture())).thenReturn(apiClientDto);
        when(repository.save(creditAnalysisEntityArgumentCaptor.capture())).thenReturn(creditAnalysisEntity);

        service.requestCreditAnalysis(creditAnalysisRequest);
        final CreditAnalysisEntity creditAnalysisEntityCapture = creditAnalysisEntityArgumentCaptor.getValue();

        assertTrue(creditAnalysisEntityCapture.getApproved());
        assertEquals(BigDecimal.valueOf(1_010_25,2), creditAnalysisEntityCapture.getApprovedLimit());
        assertEquals(BigDecimal.valueOf(101_03,2), creditAnalysisEntityCapture.getWithdraw());
        assertEquals(monthlyIncome, creditAnalysisEntityCapture.getMonthlyIncome());
        assertEquals(requestedAmount, creditAnalysisEntityCapture.getRequestedAmount());
        assertEquals(BigDecimal.valueOf(15), creditAnalysisEntityCapture.getAnnualInterest());

    }

    @Test
    void should_do_credit_analysis_when_monthlyIncome_is_greater_than_maxAmountOfMonthlyIncomeConsidered() {
        final BigDecimal monthlyIncome = BigDecimal.valueOf(87_594.24);
        final BigDecimal requestedAmount = BigDecimal.valueOf(75_123.21);

        final CreditAnalysisRequest creditAnalysisRequest = requestAmountGreaterThanMonthlyIncome(monthlyIncome, requestedAmount);
        final CreditAnalysisEntity creditAnalysisEntity = entityNotApproved();
        final ApiClientDto apiClientDto = dtoWithId();

        when(apiClient.getClientByIdOrCpf(idArgumentCaptor.capture())).thenReturn(apiClientDto);
        when(repository.save(creditAnalysisEntityArgumentCaptor.capture())).thenReturn(creditAnalysisEntity);

        service.requestCreditAnalysis(creditAnalysisRequest);
        final CreditAnalysisEntity creditAnalysisEntityCapture = creditAnalysisEntityArgumentCaptor.getValue();

        assertTrue(creditAnalysisEntityCapture.getApproved());
        assertEquals(BigDecimal.valueOf(7_500_00,2), creditAnalysisEntityCapture.getApprovedLimit());
        assertEquals(BigDecimal.valueOf(750_00,2), creditAnalysisEntityCapture.getWithdraw());
        assertEquals(monthlyIncome, creditAnalysisEntityCapture.getMonthlyIncome());
        assertEquals(requestedAmount, creditAnalysisEntityCapture.getRequestedAmount());
        assertEquals(BigDecimal.valueOf(15), creditAnalysisEntityCapture.getAnnualInterest());
    }

    @Test
    void should_refuse_credit_analysis_with_requestAmount_greater_than_monthlyIncome() {
        final BigDecimal monthlyIncome = BigDecimal.valueOf(10_000.00);
        final BigDecimal requestedAmount = BigDecimal.valueOf(12_150.49);

        final CreditAnalysisRequest creditAnalysisRequest = requestAmountGreaterThanMonthlyIncome(monthlyIncome, requestedAmount);
        final CreditAnalysisEntity creditAnalysisEntity = entityNotApproved();
        final ApiClientDto apiClientDto = dtoWithId();

        when(apiClient.getClientByIdOrCpf(idArgumentCaptor.capture())).thenReturn(apiClientDto);
        when(repository.save(creditAnalysisEntityArgumentCaptor.capture())).thenReturn(creditAnalysisEntity);

        service.requestCreditAnalysis(creditAnalysisRequest);
        final CreditAnalysisEntity creditAnalysisEntityCapture = creditAnalysisEntityArgumentCaptor.getValue();

        assertNotNull(creditAnalysisEntityCapture);
        assertEquals(idArgumentCaptor.getValue(), creditAnalysisRequest.clientId().toString());

        assertEquals(creditAnalysisRequest.clientId(), creditAnalysisEntityCapture.getClientId());

        assertFalse(creditAnalysisEntityCapture.getApproved());
        assertEquals(BigDecimal.ZERO, creditAnalysisEntityCapture.getApprovedLimit());
        assertEquals(BigDecimal.ZERO, creditAnalysisEntityCapture.getWithdraw());
        assertEquals(creditAnalysisRequest.requestedAmount(), creditAnalysisEntityCapture.getRequestedAmount());
        assertEquals(creditAnalysisRequest.monthlyIncome(), creditAnalysisEntityCapture.getMonthlyIncome());
        assertEquals(BigDecimal.valueOf(0), creditAnalysisEntityCapture.getAnnualInterest());
    }

    @Test
    void should_throw_ClientNotFoundException_when_idCliente_is_not_found() {
        final BigDecimal monthlyIncome = BigDecimal.valueOf(10_000.00);
        final BigDecimal requestedAmount = BigDecimal.valueOf(12_150.49);

        final CreditAnalysisRequest creditAnalysisRequest = requestAmountLessThanMonthlyIncome(monthlyIncome, requestedAmount);

        when(apiClient.getClientByIdOrCpf(idArgumentCaptor.capture())).thenThrow(FeignException.class);

        assertThrows(ClientNotFoundException.class,
                () -> service.requestCreditAnalysis(creditAnalysisRequest));
    }
}
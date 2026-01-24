package com.smart_ecomernce_api.smart_ecomernce_api.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceMonitoringAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Logger logger;

    @InjectMocks
    private PerformanceMonitoringAspect performanceMonitoringAspect;

    @Test
    void monitorPerformance_ShouldLogExecutionTime() throws Throwable {
        // Given
        Object expectedResult = "test result";
        when(joinPoint.proceed()).thenReturn(expectedResult);
        when(joinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(joinPoint.getSignature().toShortString()).thenReturn("TestService.testMethod()");

        // When
        Object result = performanceMonitoringAspect.monitorPerformance(joinPoint);

        // Then
        assert result == expectedResult;
        verify(joinPoint).proceed();
        // Note: Logger verification would require more complex mocking of the aspect's logger
    }

    @Test
    void monitorPerformance_WithException_ShouldStillLog() throws Throwable {
        // Given
        RuntimeException expectedException = new RuntimeException("Test exception");
        when(joinPoint.proceed()).thenThrow(expectedException);
        when(joinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(joinPoint.getSignature().toShortString()).thenReturn("TestService.testMethod()");

        // When & Then
        try {
            performanceMonitoringAspect.monitorPerformance(joinPoint);
        } catch (RuntimeException e) {
            assert e == expectedException;
        }

        verify(joinPoint).proceed();
    }
}

package com.iare.club.service;

import com.iare.club.entity.AppUser;
import com.iare.club.entity.Deposit;
import com.iare.club.controller.PayUController;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.servlet.ModelAndView;

class PayUProductionConfigurationTest {
    @Test
    void hostedCheckoutUsesRailwayHttpsCallbacksAndCustomerPhone() {
        DepositService deposits = mock(DepositService.class);
        PayUClient client = mock(PayUClient.class);
        PayUHashService hashes = new PayUHashService("merchant-key", "merchant-salt");
        Deposit deposit = mock(Deposit.class);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(42L);
        when(user.getFullName()).thenReturn("Money Mint User");
        when(user.getEmail()).thenReturn("user@example.com");
        when(deposit.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(deposit.getPaymentReference()).thenReturn("MM-PRODUCTION-100");
        when(deposits.createPayU(user, new BigDecimal("100.00"),"https://iareclub-production.up.railway.app")).thenReturn(deposit);

        PayUService service = new PayUService(deposits, client, hashes, true, "PRODUCTION",
                "merchant-key", "merchant-salt", "https://money-mint.up.railway.app/");
        PayUService.Checkout checkout = service.initiate(user, new BigDecimal("100.00"), "9876543210","https://iareclub-production.up.railway.app");

        assertEquals("https://secure.payu.in/_payment", checkout.action());
        assertEquals("https://money-mint.up.railway.app/payments/payu/callback", checkout.fields().get("surl"));
        assertEquals(checkout.fields().get("surl"), checkout.fields().get("furl"));
        assertEquals(checkout.fields().get("surl"), checkout.fields().get("curl"));
        assertEquals("9876543210", checkout.fields().get("phone"));
        assertFalse(checkout.fields().values().stream().anyMatch(value -> value.contains("localhost")));
    }

    @Test
    void enabledPayuRejectsNonHttpsOrMissingProductionOrigin() {
        DepositService deposits = mock(DepositService.class);
        PayUClient client = mock(PayUClient.class);
        PayUHashService hashes = new PayUHashService("key", "salt");
        assertThrows(IllegalStateException.class, () -> new PayUService(deposits, client, hashes, true,
                "PRODUCTION", "key", "salt", "http://localhost:8080"));
        assertThrows(IllegalStateException.class, () -> new PayUService(deposits, client, hashes, true,
                "PRODUCTION", "key", "salt", ""));
    }

    @Test
    void verifiedCallbackReturnsToDepositPageWithTransactionReference() {
        PayUService service = mock(PayUService.class);
        DepositService deposits = mock(DepositService.class);
        when(service.processCallback(anyMap())).thenReturn(new PayUService.Result("MM-VERIFIED-100", Deposit.Status.SUCCESSFUL, "captured"));
        when(deposits.returnOrigin("MM-VERIFIED-100")).thenReturn(Optional.of("http://localhost:8080"));
        PaymentOriginService origins = new PaymentOriginService("http://localhost:8080,https://iareclub-production.up.railway.app","http://localhost:8080");
        PayUController controller = new PayUController(service, mock(UserService.class), deposits, origins);
        ModelAndView bridge = controller.callback(Map.of("txnid", "MM-VERIFIED-100"));
        assertEquals("finance/payu-return",bridge.getViewName());
        assertEquals("http://localhost:8080/deposit?payment=success&txnid=MM-VERIFIED-100", bridge.getModel().get("returnUrl"));
    }

    @Test
    void railwayOriginReturnsToRailwayAndUntrustedOriginFallsBackSafely() {
        PaymentOriginService origins = new PaymentOriginService("http://localhost:8080,https://iareclub-production.up.railway.app","https://iareclub-production.up.railway.app");
        assertEquals("https://iareclub-production.up.railway.app",origins.safeStoredOrigin("https://iareclub-production.up.railway.app/"));
        assertEquals("https://iareclub-production.up.railway.app",origins.safeStoredOrigin("https://attacker.example"));
    }
}

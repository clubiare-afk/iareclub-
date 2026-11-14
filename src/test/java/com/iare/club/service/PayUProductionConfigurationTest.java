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
import org.springframework.mock.web.MockHttpServletRequest;

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
        // Hosted Checkout resolves the customer-facing merchant identity from
        // the PayU account associated with the key. Supplying an invented
        // merchant-name field here would not update PayU/UPI and risks an
        // unsupported request diverging from the signed integration contract.
        assertFalse(checkout.fields().containsKey("merchantName"));
        assertFalse(checkout.fields().containsKey("merchant_name"));
        assertFalse(checkout.fields().containsKey("display_name"));
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

    @Test
    void paymentOriginComesFromValidatedBrowserOriginOnLocalhostAndRailway() {
        PaymentOriginService origins = new PaymentOriginService("http://localhost:8080,https://iareclub-production.up.railway.app,https://fundrise.shop,https://www.fundrise.shop","https://iareclub-production.up.railway.app");
        MockHttpServletRequest local = new MockHttpServletRequest("POST","/deposit");
        local.addHeader("Origin","http://localhost:8080");
        assertEquals("http://localhost:8080",origins.trustedOrigin(local));
        MockHttpServletRequest railway = new MockHttpServletRequest("POST","/deposit");
        railway.addHeader("Origin","https://iareclub-production.up.railway.app");
        railway.addHeader("X-Forwarded-Host","internal.railway.app");
        railway.addHeader("X-Forwarded-Proto","https");
        assertEquals("https://iareclub-production.up.railway.app",origins.trustedOrigin(railway));
        MockHttpServletRequest customDomain = new MockHttpServletRequest("POST","/deposit");
        customDomain.addHeader("Origin","https://www.fundrise.shop");
        assertEquals("https://www.fundrise.shop",origins.trustedOrigin(customDomain));
    }

    @Test
    void verifiedFundrisePaymentReturnsToFundriseDepositPage() {
        PayUService service = mock(PayUService.class);
        DepositService deposits = mock(DepositService.class);
        when(service.processCallback(anyMap())).thenReturn(new PayUService.Result("MM-FUND-100", Deposit.Status.SUCCESSFUL, "captured"));
        when(deposits.returnOrigin("MM-FUND-100")).thenReturn(Optional.of("https://www.fundrise.shop"));
        PaymentOriginService origins = new PaymentOriginService("http://localhost:8080,https://iareclub-production.up.railway.app,https://fundrise.shop,https://www.fundrise.shop","https://iareclub-production.up.railway.app");
        PayUController controller = new PayUController(service, mock(UserService.class), deposits, origins);
        ModelAndView bridge = controller.callback(Map.of("txnid", "MM-FUND-100"));
        assertEquals("https://www.fundrise.shop/deposit?payment=success&txnid=MM-FUND-100",bridge.getModel().get("returnUrl"));
    }
}

package com.iare.club;

import com.iare.club.dto.RegistrationRequest;
import com.iare.club.entity.*;
import com.iare.club.repository.*;
import com.iare.club.service.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminWalletFeatureTests {
    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired WalletService wallets;
    @Autowired AdminWalletService adminWallets;
    @Autowired DepositRepository deposits;
    @Autowired WalletTransactionRepository transactions;
    @Autowired AdminCreditAuditRepository audits;

    @Test
    void adminCreditIsAtomicAuditedDistinctFromPayUAndIdempotent() throws Exception {
        AppUser user = register("Wallet Credit User", "wallet-credit@example.com");
        String requestId = UUID.randomUUID().toString();

        var first = adminWallets.credit(user.getId(), new BigDecimal("1250.50"), requestId, "admin@gmail.com");
        var duplicate = adminWallets.credit(user.getId(), new BigDecimal("1250.50"), requestId, "admin@gmail.com");

        assertTrue(first.credited());
        assertFalse(duplicate.credited());
        assertEquals(0, new BigDecimal("1250.50").compareTo(wallets.getOrCreate(user).getDepositBalance()));

        Deposit deposit = deposits.findByPaymentReference(first.transactionId()).orElseThrow();
        assertEquals("ADMIN_CREDIT", deposit.getPaymentMethod());
        assertEquals(Deposit.Status.SUCCESSFUL, deposit.getStatus());
        assertNull(deposit.getPayuPaymentId());
        assertNull(deposit.getGatewayStatus());
        assertEquals("WALLET", deposit.getDisplayMethod());
        assertEquals("Money Mint", deposit.getDisplayProvider());
        assertTrue(deposit.getDisplayPaymentId().matches("\\d{10}"));

        WalletTransaction transaction = transactions.findByReference(first.transactionId()).orElseThrow();
        assertEquals(WalletTransaction.Type.ADMIN_CREDIT, transaction.getType());
        assertEquals("ADMIN_CREDIT", transaction.getPaymentMethod());
        assertEquals("admin@gmail.com", transaction.getAdminIdentity());
        assertEquals(0, BigDecimal.ZERO.compareTo(transaction.getBalanceBefore()));
        assertEquals(0, new BigDecimal("1250.50").compareTo(transaction.getBalanceAfter()));
        assertTrue(audits.existsByTransactionReference(first.transactionId()));
        assertEquals(1, deposits.findTop50ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(item -> item.getPaymentReference().equals(first.transactionId())).count());
        mvc.perform(get("/deposit").with(user(user.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Money Mint: " + deposit.getDisplayPaymentId())))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Administrator credit"))));
    }

    @Test
    void walletManagementAndCreditApiAreAdminOnlyAndValidateAmounts() throws Exception {
        AppUser user = register("Wallet API User", "wallet-api@example.com");
        mvc.perform(get("/admin/wallets")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/wallets").with(user("member@example.com").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/wallets?q=wallet-api&balance=ZERO")
                        .with(user("admin@gmail.com").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("wallet-api@example.com")));

        String endpoint = "/admin/api/wallets/" + user.getId() + "/credits";
        mvc.perform(post(endpoint).with(user("member@example.com").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":100,\"requestId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(endpoint).with(user("admin@gmail.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":0,\"requestId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnprocessableEntity());

        String requestId = UUID.randomUUID().toString();
        mvc.perform(post(endpoint).with(user("admin@gmail.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":500.25,\"requestId\":\"" + requestId + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.credited").value(true))
                .andExpect(jsonPath("$.newBalance").value(500.25))
                .andExpect(jsonPath("$.transactionId").value(org.hamcrest.Matchers.startsWith("ADM")));
    }

    private AppUser register(String name, String email) {
        RegistrationRequest request = new RegistrationRequest(); request.setFullName(name);
        request.setEmail(email); request.setPassword("Secure123"); return users.register(request);
    }
}

package com.iare.club.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.iare.club.entity.AppUser;
import com.iare.club.entity.Deposit;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ZapUpiDirectQrTest {
    @Test void acceptsOfficialDirectUpiPayloadWithExactAmountAndReference(){
        DepositService deposits=mock(DepositService.class);ZapUpiClient client=mock(ZapUpiClient.class);AppUser user=new AppUser();Deposit deposit=mock(Deposit.class);
        when(client.isConfigured()).thenReturn(true);when(deposit.getPaymentReference()).thenReturn("ZAPORDER1");when(deposit.getAmount()).thenReturn(new BigDecimal("1.00"));when(deposits.createZapUpi(user,new BigDecimal("1.00"),"https://fundrise.shop")).thenReturn(deposit);
        when(client.createOrder(anyString(),any(),anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(new ZapUpiClient.CreatedOrder("ZAPORDER1","ZTX1","cashier","https://pay.zapupi.com/instent-pay-x","PAY1","upi%3A%2F%2Fpay%3Fpa%3Dmerchant%40upi%26pn%3DFundRise+Wallet%26am%3D1.00%26tr%3DZAPORDER1%26cu%3DINR"));
        ZapUpiService service=new ZapUpiService(deposits,client,true,true,"https://fundrise.shop");
        assertEquals("ZAPORDER1",service.initiate(user,new BigDecimal("1.00"),"https://fundrise.shop").orderId());
        verify(deposits).recordZapUpiOrder(eq("ZAPORDER1"),eq("ZTX1"),eq("cashier"),eq("https://pay.zapupi.com/instent-pay-x"),eq("PAY1"),contains("upi://pay"));
    }

    @Test void hostedCashierWorksWithoutCustomQrPayload(){
        DepositService deposits=mock(DepositService.class);ZapUpiClient client=mock(ZapUpiClient.class);AppUser user=new AppUser();Deposit deposit=mock(Deposit.class);
        when(client.isConfigured()).thenReturn(true);when(deposit.getPaymentReference()).thenReturn("ZAPORDER2");when(deposit.getAmount()).thenReturn(new BigDecimal("1.00"));when(deposits.createZapUpi(user,new BigDecimal("1.00"),"https://fundrise.shop")).thenReturn(deposit);
        ZapUpiService service=new ZapUpiService(deposits,client,true,true,"https://fundrise.shop");
        when(client.createOrder(anyString(),any(),anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(new ZapUpiClient.CreatedOrder("ZAPORDER2","ZTX2","cashier","https://pay.zapupi.com/instent-pay-x",null,null));
        ZapUpiService.Checkout checkout=service.initiate(user,new BigDecimal("1.00"),"https://fundrise.shop");
        assertEquals("https://pay.zapupi.com/instent-pay-x",checkout.paymentUrl());
        verify(deposits).recordZapUpiOrder("ZAPORDER2","ZTX2","cashier","https://pay.zapupi.com/instent-pay-x",null,null);
    }

    @Test void browserReturnUsesInitiatingLocalhostWhileWebhookUsesPublicServer(){
        DepositService deposits=mock(DepositService.class);ZapUpiClient client=mock(ZapUpiClient.class);AppUser user=new AppUser();Deposit deposit=mock(Deposit.class);
        when(client.isConfigured()).thenReturn(true);when(deposit.getPaymentReference()).thenReturn("ZAPLOCAL");when(deposit.getAmount()).thenReturn(new BigDecimal("5.00"));when(deposits.createZapUpi(user,new BigDecimal("5.00"),"http://localhost:8080")).thenReturn(deposit);
        when(client.createOrder(anyString(),any(),anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(new ZapUpiClient.CreatedOrder("ZAPLOCAL","ZTXLOCAL","cashier","https://pay.zapupi.com/instent-pay-local",null,null));
        ZapUpiService service=new ZapUpiService(deposits,client,true,false,"https://public.ngrok-free.app");service.initiate(user,new BigDecimal("5.00"),"http://localhost:8080");
        verify(client).createOrder(eq("ZAPLOCAL"),eq(new BigDecimal("5.00")),anyString(),eq("http://localhost:8080/payments/zapupi/return"),eq("http://localhost:8080/payments/zapupi/return"),eq("http://localhost:8080/payments/zapupi/return"),eq("https://public.ngrok-free.app/payments/zapupi/webhook"));
    }
}

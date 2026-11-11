package com.iare.club;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import com.iare.club.repository.ProductRepository;
import org.springframework.mock.web.MockHttpSession;
import com.iare.club.dto.*;
import com.iare.club.entity.*;
import com.iare.club.service.*;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ClubApplicationTests.ClockConfiguration.class)
class ClubApplicationTests {
	@Autowired MockMvc mvc;
	@Autowired ProductRepository products;
	@Autowired UserService userService;
	@Autowired DepositService depositService;
	@Autowired InvestmentService investmentService;
	@Autowired WithdrawalService withdrawalService;
	@Autowired ScratchService scratchService;
	@Autowired ScratchRewardGenerator scratchRewardGenerator;
	@Autowired ProductService productService;
	@Autowired WalletService walletService;
	@Autowired ReferralService referralService;
	@Autowired TestClock testClock;

	@TestConfiguration static class ClockConfiguration {
		@Bean @Primary TestClock testClock(){return new TestClock(Instant.parse("2026-01-01T00:00:00Z"));}
	}
	static class TestClock extends Clock {
		private Instant instant; TestClock(Instant instant){this.instant=instant;}
		void advance(Duration duration){instant=instant.plus(duration);}
		void set(Instant value){instant=value;}
		@Override public ZoneId getZone(){return ZoneOffset.UTC;}
		@Override public Clock withZone(ZoneId zone){return this;}
		@Override public Instant instant(){return instant;}
	}

	@Test
	void contextLoads() {
	}

	@Test void registrationLoginAndProtectedDashboardWork() throws Exception {
		mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://localhost/login"));
		mvc.perform(post("/signup").with(csrf()).param("fullName","Aarav Sharma").param("email","aarav@example.com").param("password","Secure123"))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
		mvc.perform(post("/login").with(csrf()).param("email","aarav@example.com").param("password","Secure123"))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"));
	}

	@Test void adminLoginRoleProtectionAndProductCrudWork() throws Exception {
		mvc.perform(get("/admin/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://localhost/money"));
		mvc.perform(get("/admin/dashboard").with(user("user@example.com").roles("USER"))).andExpect(status().isForbidden());
		mvc.perform(formLogin("/money").userParameter("email").user("admin@gmail.com").password("admin@123"))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/dashboard"));
		mvc.perform(post("/admin/products").with(user("admin@gmail.com").roles("ADMIN")).with(csrf())
			.param("name","Green Growth Plan").param("imageUrl","https://example.com/growth.jpg")
			.param("originalPrice","12000.00").param("discountPrice","10000.00")
			.param("dailyEarnings","450.00").param("totalDays","30").param("active","true"))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/dashboard#products"));
		mvc.perform(post("/signup").with(csrf()).param("fullName","Investor Test").param("email","investor@example.com").param("password","Secure123"));
		mvc.perform(get("/products").with(user("investor@example.com").roles("USER")))
			.andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Green Growth Plan")));
		long productId=products.findAllByOrderByCreatedAtDesc().get(0).getId();
		mvc.perform(post("/admin/products/"+productId+"/toggle").with(user("admin@gmail.com").roles("ADMIN")).with(csrf()))
			.andExpect(status().is3xxRedirection());
		mvc.perform(get("/products").with(user("investor@example.com").roles("USER")))
			.andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Green Growth Plan"))));
		mvc.perform(get("/admin/users").with(user("admin@gmail.com").roles("ADMIN")))
			.andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("investor@example.com")));
	}

	@Test void adminAndUserCanStayLoggedInInTheSameBrowserSession() throws Exception {
		MockHttpSession session=(MockHttpSession)mvc.perform(post("/signup").with(csrf())
			.param("fullName","Dual Session User").param("email","dual@example.com").param("password","Secure123"))
			.andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
		session=(MockHttpSession)mvc.perform(post("/login").session(session).with(csrf()).param("email","dual@example.com").param("password","Secure123"))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard")).andReturn().getRequest().getSession(false);
		session=(MockHttpSession)mvc.perform(post("/money").session(session).with(csrf()).param("email","admin@gmail.com").param("password","admin@123"))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/dashboard")).andReturn().getRequest().getSession(false);
		mvc.perform(get("/dashboard").session(session)).andExpect(status().isOk());
		mvc.perform(get("/admin/dashboard").session(session)).andExpect(status().isOk());
		mvc.perform(post("/admin/logout").session(session).with(csrf())).andExpect(status().is3xxRedirection());
		mvc.perform(get("/dashboard").session(session)).andExpect(status().isOk());
		mvc.perform(get("/admin/dashboard").session(session)).andExpect(status().is3xxRedirection());
	}

	@Test void walletInvestmentRewardWithdrawalAndRefundInvariantsHold() {
		RegistrationRequest registration=new RegistrationRequest();registration.setFullName("Finance Test User");registration.setEmail("finance@example.com");registration.setPassword("Secure123");AppUser user=userService.register(registration);
		InvestmentProduct product=new InvestmentProduct();product.setName("Secure Growth Test");product.setImageUrl("https://example.com/test.jpg");product.setOriginalPrice(new BigDecimal("600.00"));product.setDiscountPrice(new BigDecimal("500.00"));product.setDailyEarnings(new BigDecimal("200.00"));product.setTotalDays(2);product.setActive(true);product=productService.save(product);
		Deposit deposit=depositService.create(user,new BigDecimal("1000.00"));depositService.confirmDemo(user,deposit.getPaymentReference());depositService.confirmDemo(user,deposit.getPaymentReference());
		assertEquals(0,new BigDecimal("1000.00").compareTo(walletService.getOrCreate(user).getDepositBalance()));
		UserInvestment investment=investmentService.purchase(user,product.getId(),"wallet-flow-request");investmentService.purchase(user,product.getId(),"wallet-flow-request");
		assertEquals(0,new BigDecimal("500.00").compareTo(walletService.getOrCreate(user).getDepositBalance()));
		investmentService.collectReward(user,investment.getId());
		assertThrows(InvestmentService.RewardNotReadyException.class,()->investmentService.collectReward(user,investment.getId()));
		testClock.advance(Duration.ofHours(24).minusMillis(1));
		assertThrows(InvestmentService.RewardNotReadyException.class,()->investmentService.collectReward(user,investment.getId()));
		testClock.advance(Duration.ofMillis(1));investmentService.collectReward(user,investment.getId());
		assertEquals(0,new BigDecimal("400.00").compareTo(walletService.getOrCreate(user).getRewardBalance()));
		BankAccountRequest bank=new BankAccountRequest();bank.setAccountHolderName("Finance Test User");bank.setBankName("Demo Bank");bank.setAccountNumber("123456789012");bank.setConfirmAccountNumber("123456789012");bank.setIfscCode("ABCD0123456");withdrawalService.saveBank(user,bank);
		WithdrawalRequest request=withdrawalService.request(user,new BigDecimal("100.00"));assertEquals(0,new BigDecimal("100.00").compareTo(walletService.getOrCreate(user).getPendingWithdrawals()));
		withdrawalService.transition(request.getId(),WithdrawalRequest.Status.REFUNDED,"Demo refund",null,"admin@gmail.com");
		assertEquals(0,new BigDecimal("400.00").compareTo(walletService.getOrCreate(user).getRewardBalance()));assertEquals(0,BigDecimal.ZERO.compareTo(walletService.getOrCreate(user).getPendingWithdrawals()));
		assertThrows(WithdrawalService.WithdrawalException.class,()->withdrawalService.transition(request.getId(),WithdrawalRequest.Status.REFUNDED,"Again",null,"admin@gmail.com"));
		testClock.advance(Duration.ofHours(24));
		WithdrawalRequest approved=withdrawalService.request(user,new BigDecimal("100.00"));
		assertThrows(WithdrawalService.WithdrawalException.class,()->withdrawalService.transition(approved.getId(),WithdrawalRequest.Status.SUCCESSFUL,"Paid",null,"admin@gmail.com"));
		withdrawalService.transition(approved.getId(),WithdrawalRequest.Status.SUCCESSFUL,"Bank payout verified","BANK-UTR-123","admin@gmail.com");
		Wallet completedWallet=walletService.getOrCreate(user);
		assertEquals(0,new BigDecimal("300.00").compareTo(completedWallet.getRewardBalance()));
		assertEquals(0,BigDecimal.ZERO.compareTo(completedWallet.getPendingWithdrawals()));
		assertEquals(0,new BigDecimal("100.00").compareTo(completedWallet.getTotalWithdrawn()));
		assertThrows(WithdrawalService.WithdrawalException.class,()->withdrawalService.transition(approved.getId(),WithdrawalRequest.Status.SUCCESSFUL,"Again","BANK-UTR-456","admin@gmail.com"));
	}

	@Test void concurrentPurchasesCannotOverspendDepositBalance() throws Exception {
		RegistrationRequest registration=new RegistrationRequest();registration.setFullName("Concurrency User");registration.setEmail("concurrent@example.com");registration.setPassword("Secure123");AppUser user=userService.register(registration);
		InvestmentProduct product=new InvestmentProduct();product.setName("Concurrency Product");product.setImageUrl("https://example.com/concurrent.jpg");product.setOriginalPrice(new BigDecimal("500.00"));product.setDiscountPrice(new BigDecimal("500.00"));product.setDailyEarnings(new BigDecimal("20.00"));product.setTotalDays(5);product.setActive(true);product=productService.save(product);
		Deposit deposit=depositService.create(user,new BigDecimal("500.00"));depositService.confirmDemo(user,deposit.getPaymentReference());long productId=product.getId();
		ExecutorService executor=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
		Callable<Boolean> first=()->{start.await();try{investmentService.purchase(user,productId,"concurrent-a");return true;}catch(InvestmentService.InsufficientDepositException|InvestmentService.DuplicatePurchaseException e){return false;}};
		Callable<Boolean> second=()->{start.await();try{investmentService.purchase(user,productId,"concurrent-b");return true;}catch(InvestmentService.InsufficientDepositException|InvestmentService.DuplicatePurchaseException e){return false;}};
		Future<Boolean> a=executor.submit(first),b=executor.submit(second);start.countDown();int successes=(a.get()?1:0)+(b.get()?1:0);executor.shutdown();assertEquals(1,successes);assertEquals(0,BigDecimal.ZERO.compareTo(walletService.getOrCreate(user).getDepositBalance()));
	}

	@Test void financialUserAndAdminPagesRender() throws Exception {
		RegistrationRequest registration=new RegistrationRequest();registration.setFullName("Page Render User");registration.setEmail("pages@example.com");registration.setPassword("Secure123");userService.register(registration);
		for(String path:new String[]{"/wallet","/deposit","/products","/my-products","/scratch","/investments","/withdrawal","/activity"})mvc.perform(get(path).with(user("pages@example.com").roles("USER"))).andExpect(status().isOk());
		for(String path:new String[]{"/admin/deposits","/admin/withdrawals"})mvc.perform(get(path).with(user("admin@gmail.com").roles("ADMIN"))).andExpect(status().isOk());
	}

	@Test void depositFormCannotDirectlyCreditTheWallet() throws Exception {
		RegistrationRequest registration=new RegistrationRequest();registration.setFullName("Direct Deposit User");registration.setEmail("direct-deposit@example.com");registration.setPassword("Secure123");AppUser account=userService.register(registration);
		mvc.perform(post("/deposit").with(user("direct-deposit@example.com").roles("USER")).with(csrf()).param("amount","750.00").param("phone","9876543210"))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/deposit"));
		assertEquals(0,BigDecimal.ZERO.compareTo(walletService.getOrCreate(account).getDepositBalance()));
	}

	@Test void payuVerificationCreditsExactlyOnceAndRejectsUnverifiedStates() {
		RegistrationRequest registration=new RegistrationRequest();registration.setFullName("PayU Test User");registration.setEmail("payu-flow@example.com");registration.setPassword("Secure123");AppUser account=userService.register(registration);
		Deposit success=depositService.createPayU(account,new BigDecimal("750.00"));
		PayUClient.Verification captured=new PayUClient.Verification(success.getPaymentReference(),"success","captured",new BigDecimal("750.00"),"PAYU-1","UPI","BANK-1","No Error");
		depositService.applyPayUVerification(captured);depositService.applyPayUVerification(captured);
		assertEquals(0,new BigDecimal("750.00").compareTo(walletService.getOrCreate(account).getDepositBalance()));
		Deposit pending=depositService.createPayU(account,new BigDecimal("200.00"));
		assertEquals(Deposit.Status.PENDING,depositService.applyPayUVerification(new PayUClient.Verification(pending.getPaymentReference(),"pending","pending",new BigDecimal("200.00"),"PAYU-2","NB",null,"Pending")).getStatus());
		Deposit failed=depositService.createPayU(account,new BigDecimal("300.00"));
		assertEquals(Deposit.Status.CANCELLED,depositService.applyPayUVerification(new PayUClient.Verification(failed.getPaymentReference(),"failure","userCancelled",new BigDecimal("300.00"),"PAYU-3","UPI",null,"Cancelled")).getStatus());
		Deposit tampered=depositService.createPayU(account,new BigDecimal("400.00"));
		assertThrows(DepositService.FinancialException.class,()->depositService.applyPayUVerification(new PayUClient.Verification(tampered.getPaymentReference(),"success","captured",new BigDecimal("401.00"),"PAYU-4","CC",null,"No Error")));
		assertEquals(0,new BigDecimal("750.00").compareTo(walletService.getOrCreate(account).getDepositBalance()));
	}

	@Test void withdrawalCooldownRejectsDuplicatesAndAllowsTheExactBoundary() {
		RegistrationRequest registration=new RegistrationRequest();registration.setFullName("Cooldown User");registration.setEmail("cooldown@example.com");registration.setPassword("Secure123");AppUser account=userService.register(registration);
		InvestmentProduct product=new InvestmentProduct();product.setName("Cooldown Product");product.setImageUrl("https://example.com/cooldown.jpg");product.setOriginalPrice(new BigDecimal("100.00"));product.setDiscountPrice(new BigDecimal("100.00"));product.setDailyEarnings(new BigDecimal("300.00"));product.setTotalDays(2);product.setActive(true);product=productService.save(product);
		depositService.depositInstantly(account,new BigDecimal("100.00"));UserInvestment investment=investmentService.purchase(account,product.getId(),"cooldown-investment");investmentService.collectReward(account,investment.getId());
		BankAccountRequest bank=new BankAccountRequest();bank.setAccountHolderName("Cooldown User");bank.setBankName("Test Bank");bank.setAccountNumber("123456789012");bank.setConfirmAccountNumber("123456789012");bank.setIfscCode("ABCD0123456");withdrawalService.saveBank(account,bank);
		withdrawalService.request(account,new BigDecimal("100.00"));
		assertThrows(WithdrawalService.WithdrawalCooldownException.class,()->withdrawalService.request(account,new BigDecimal("100.00")));
		testClock.advance(Duration.ofHours(24).minusMillis(1));
		assertThrows(WithdrawalService.WithdrawalCooldownException.class,()->withdrawalService.request(account,new BigDecimal("100.00")));
		testClock.advance(Duration.ofMillis(1));
		assertDoesNotThrow(()->withdrawalService.request(account,new BigDecimal("100.00")));
	}

	@Test void concurrentWithdrawalRequestsCannotBypassCooldown() throws Exception {
		RegistrationRequest registration=new RegistrationRequest();registration.setFullName("Concurrent Withdrawal User");registration.setEmail("concurrent-withdrawal@example.com");registration.setPassword("Secure123");AppUser account=userService.register(registration);
		InvestmentProduct product=new InvestmentProduct();product.setName("Concurrent Withdrawal Product");product.setImageUrl("https://example.com/withdrawal.jpg");product.setOriginalPrice(new BigDecimal("100.00"));product.setDiscountPrice(new BigDecimal("100.00"));product.setDailyEarnings(new BigDecimal("300.00"));product.setTotalDays(1);product.setActive(true);product=productService.save(product);
		depositService.depositInstantly(account,new BigDecimal("100.00"));UserInvestment investment=investmentService.purchase(account,product.getId(),"concurrent-withdrawal-investment");investmentService.collectReward(account,investment.getId());
		BankAccountRequest bank=new BankAccountRequest();bank.setAccountHolderName("Concurrent Withdrawal User");bank.setBankName("Test Bank");bank.setAccountNumber("123456789012");bank.setConfirmAccountNumber("123456789012");bank.setIfscCode("ABCD0123456");withdrawalService.saveBank(account,bank);
		ExecutorService executor=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
		Callable<Boolean> request=()->{start.await();try{withdrawalService.request(account,new BigDecimal("100.00"));return true;}catch(WithdrawalService.WithdrawalCooldownException exception){return false;}};
		Future<Boolean> first=executor.submit(request),second=executor.submit(request);start.countDown();int successes=(first.get()?1:0)+(second.get()?1:0);executor.shutdown();
		assertEquals(1,successes);assertEquals(0,new BigDecimal("100.00").compareTo(walletService.getOrCreate(account).getPendingWithdrawals()));
	}

	@Test void scratchProbabilityBoundariesMatchPublishedRules() {
		assertEquals(new BigDecimal("50.00"),scratchRewardGenerator.rewardForTicket(0,0,0,0));
		assertEquals(new BigDecimal("50.00"),scratchRewardGenerator.rewardForTicket(499,14,4,28));
		assertEquals(new BigDecimal("21.00"),scratchRewardGenerator.rewardForTicket(500,0,0,0));
		assertEquals(new BigDecimal("49.00"),scratchRewardGenerator.rewardForTicket(4_999,0,0,28));
		assertEquals(new BigDecimal("16.00"),scratchRewardGenerator.rewardForTicket(5_000,0,0,0));
		assertEquals(new BigDecimal("20.00"),scratchRewardGenerator.rewardForTicket(49_999,0,4,0));
		assertEquals(new BigDecimal("1.00"),scratchRewardGenerator.rewardForTicket(50_000,0,0,0));
		assertEquals(new BigDecimal("15.00"),scratchRewardGenerator.rewardForTicket(999_999,14,0,0));
	}

	@Test void scratchBecomesAvailableExactlyTwentyFourHoursLater() {
		testClock.set(Instant.parse("2026-06-01T20:29:00Z"));
		RegistrationRequest registration=new RegistrationRequest();registration.setFullName("Scratch Test User");registration.setEmail("scratch@example.com");registration.setPassword("Secure123");AppUser account=userService.register(registration);walletService.getOrCreate(account);
		ScratchReward first=scratchService.scratch(account);assertTrue(first.getAmount().compareTo(BigDecimal.ONE)>=0&&first.getAmount().compareTo(new BigDecimal("50.00"))<=0);
		assertThrows(ScratchService.ScratchCooldownException.class,()->scratchService.scratch(account));
		assertEquals(first.getAmount(),walletService.getOrCreate(account).getRewardBalance());
		testClock.advance(Duration.ofHours(24).minusSeconds(1));
		assertThrows(ScratchService.ScratchCooldownException.class,()->scratchService.scratch(account));
		testClock.advance(Duration.ofSeconds(1));
		ScratchReward second=assertDoesNotThrow(()->scratchService.scratch(account));
		assertNotEquals(first.getTransactionReference(),second.getTransactionReference());
		assertEquals(first.getAmount().add(second.getAmount()),walletService.getOrCreate(account).getRewardBalance());
	}

	@Test void referralBonusIsPaidOnceAfterFirstConfirmedPurchaseAndDashboardsRender() throws Exception {
		RegistrationRequest ownerRequest=new RegistrationRequest();ownerRequest.setFullName("Referral Owner");ownerRequest.setEmail("ref-owner@example.com");ownerRequest.setPassword("Secure123");AppUser owner=userService.register(ownerRequest);
		RegistrationRequest friendRequest=new RegistrationRequest();friendRequest.setFullName("Referral Friend");friendRequest.setEmail("ref-friend@example.com");friendRequest.setPassword("Secure123");friendRequest.setReferralCode(owner.getReferralCode());AppUser friend=userService.register(friendRequest);
		assertEquals(1,referralService.dashboard(owner).pending());assertEquals(BigDecimal.ZERO,referralService.dashboard(owner).earnings());
		InvestmentProduct product=new InvestmentProduct();product.setName("Referral Test Product");product.setImageUrl("https://example.com/referral.jpg");product.setOriginalPrice(new BigDecimal("100.00"));product.setDiscountPrice(new BigDecimal("100.00"));product.setDailyEarnings(new BigDecimal("5.00"));product.setTotalDays(2);product.setActive(true);product=productService.save(product);
		long productId=product.getId();depositService.depositInstantly(friend,new BigDecimal("200.00"));investmentService.purchase(friend,productId,"referral-first-purchase");
		assertEquals(0,new BigDecimal("200.00").compareTo(walletService.getOrCreate(owner).getRewardBalance()));assertEquals(1,referralService.dashboard(owner).successful());
		assertThrows(InvestmentService.DuplicatePurchaseException.class,()->investmentService.purchase(friend,productId,"referral-second-purchase"));assertEquals(0,new BigDecimal("200.00").compareTo(walletService.getOrCreate(owner).getRewardBalance()));
		mvc.perform(get("/refer-and-earn").with(user(owner.getEmail()).roles("USER"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Referral Friend")));
		mvc.perform(get("/admin/referrals").with(user("admin@gmail.com").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Referral Test Product")));
	}

	@Test void purchasedProductsAreMarkedAndCannotBePurchasedTwiceButRemainAvailableToOthers() throws Exception {
		RegistrationRequest firstRequest=new RegistrationRequest();firstRequest.setFullName("Purchase State User");firstRequest.setEmail("purchase-state@example.com");firstRequest.setPassword("Secure123");AppUser first=userService.register(firstRequest);
		RegistrationRequest secondRequest=new RegistrationRequest();secondRequest.setFullName("Other Purchase User");secondRequest.setEmail("other-purchase@example.com");secondRequest.setPassword("Secure123");AppUser second=userService.register(secondRequest);
		InvestmentProduct product=new InvestmentProduct();product.setName("One Per User Product");product.setImageUrl("https://example.com/full-product.jpg");product.setOriginalPrice(new BigDecimal("300.00"));product.setDiscountPrice(new BigDecimal("200.00"));product.setDailyEarnings(new BigDecimal("10.00"));product.setTotalDays(10);product.setActive(true);product=productService.save(product);
		long productId=product.getId();
		depositService.depositInstantly(first,new BigDecimal("500.00"));depositService.depositInstantly(second,new BigDecimal("200.00"));
		investmentService.purchase(first,productId,"purchase-state-first");
		assertThrows(InvestmentService.DuplicatePurchaseException.class,()->investmentService.purchase(first,productId,"purchase-state-duplicate"));
		assertEquals(1,investmentService.findFor(first).stream().filter(item->item.getProduct().getId().equals(productId)).count());
		assertEquals(0,new BigDecimal("300.00").compareTo(walletService.getOrCreate(first).getDepositBalance()));
		assertDoesNotThrow(()->investmentService.purchase(second,productId,"purchase-state-other-user"));
		mvc.perform(get("/products").with(user(first.getEmail()).roles("USER")))
			.andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Already Purchased")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("product-purchased")));
		mvc.perform(get("/my-products").with(user(first.getEmail()).roles("USER")))
			.andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("portfolio-visual")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("loading=\"lazy\"")));
		mvc.perform(post("/api/investments/"+productId).with(user(first.getEmail()).roles("USER")).with(csrf())
			.contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"requestReference\":\"purchase-state-api-duplicate\"}"))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("You have already purchased this product"));
	}

	@Test void adminProductActiveSwitchRendersAndPersistsBothStates() throws Exception {
		InvestmentProduct product=new InvestmentProduct();product.setName("Toggle State Product");product.setImageUrl("https://example.com/toggle.jpg");product.setOriginalPrice(new BigDecimal("200.00"));product.setDiscountPrice(new BigDecimal("100.00"));product.setDailyEarnings(new BigDecimal("10.00"));product.setTotalDays(10);product.setActive(true);product=productService.save(product);
		mvc.perform(get("/admin/products/"+product.getId()+"/edit").with(user("admin@gmail.com").roles("ADMIN")))
			.andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"active\""))).andExpect(content().string(org.hamcrest.Matchers.containsString("checked=\"checked\"")));
		mvc.perform(post("/admin/products/"+product.getId()).with(user("admin@gmail.com").roles("ADMIN")).with(csrf())
			.param("name",product.getName()).param("imageUrl",product.getImageUrl()).param("originalPrice","200.00").param("discountPrice","100.00").param("dailyEarnings","10.00").param("totalDays","10").param("_active","on"))
			.andExpect(status().is3xxRedirection());
		assertFalse(productService.find(product.getId()).isActive());
	}

}

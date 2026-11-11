package com.iare.club.service;
import com.iare.club.entity.*;import com.iare.club.repository.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.List;import org.springframework.data.domain.*;
@Service public class WalletService{
 private final WalletRepository wallets;private final WalletTransactionRepository transactions;
 public WalletService(WalletRepository wallets,WalletTransactionRepository transactions){this.wallets=wallets;this.transactions=transactions;}
 @Transactional public Wallet getOrCreate(AppUser user){return wallets.findByUserId(user.getId()).orElseGet(()->wallets.saveAndFlush(new Wallet(user)));}
 @Transactional(readOnly=true) public Wallet get(AppUser user){return wallets.findByUserId(user.getId()).orElseGet(()->new Wallet(user));}
 @Transactional(readOnly=true) public List<WalletTransaction> history(AppUser user){return transactions.findTop100ByUserIdOrderByCreatedAtDesc(user.getId());}
 @Transactional(readOnly=true) public Page<WalletTransaction> activity(AppUser user,WalletTransaction.Type type,WalletTransaction.Status status,int page){return transactions.search(user.getId(),type,status,PageRequest.of(Math.max(0,page),50));}
 Wallet lock(AppUser user){Wallet existing=wallets.findByUserIdForUpdate(user.getId()).orElse(null);if(existing!=null)return existing;wallets.saveAndFlush(new Wallet(user));return wallets.findByUserIdForUpdate(user.getId()).orElseThrow();}
}

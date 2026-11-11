package com.iare.club.repository;
import com.iare.club.entity.DailyReward;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DailyRewardRepository extends JpaRepository<DailyReward,Long>{boolean existsByInvestmentIdAndRewardNumber(Long investmentId,int rewardNumber);}

package com.pet.boarding.job;

import com.pet.boarding.service.MerchantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MerchantStoreSyncScheduler {

    private final MerchantService merchantService;

    public MerchantStoreSyncScheduler(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @Scheduled(fixedDelay = 60000)
    public void syncStoreStates() {
        try {
            merchantService.refreshAllStoreStates();
        } catch (Exception e) {
            log.warn("syncStoreStates failed", e);
        }
    }
}

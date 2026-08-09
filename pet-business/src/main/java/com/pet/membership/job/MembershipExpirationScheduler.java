package com.pet.membership.job;

import com.pet.membership.service.MembershipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MembershipExpirationScheduler {
    private final MembershipService membershipService;

    public MembershipExpirationScheduler(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    /**
     * Scheduled task that runs every 60 seconds to expire memberships
     * whose validity period has ended. Logs the count of expired memberships.
     */
    @Scheduled(fixedDelay = 60000)
    public void expireMemberships() {
        try {
            int expired = membershipService.expireMemberships();
            if (expired > 0) {
                log.info("Membership expiration scheduler expired {} memberships", expired);
            }
        } catch (Exception e) {
            log.warn("Membership expiration scheduler failed", e);
        }
    }
}

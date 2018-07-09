package com.gemalto.chaos.attack;

import com.gemalto.chaos.attack.enums.AttackState;
import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.PlatformManager;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

@Component
public class AttackManager {
    private static final Logger log = LoggerFactory.getLogger(AttackManager.class);
    private final Set<Attack> activeAttacks = new HashSet<>();
    private Queue<Attack> newAttackQueue = new LinkedBlockingDeque<>();
    @Autowired
    private NotificationManager notificationManager;
    @Autowired
    private PlatformManager platformManager;
    @Autowired
    private HolidayManager holidayManager;

    private Attack addAttack (Attack attack) {
        newAttackQueue.offer(attack);
        return attack;
    }

    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 15 * 1000)
    public synchronized void updateAttackStatus () {
        synchronized (activeAttacks) {
            startNewAttacks();
            log.debug("Checking on existing attacks");
            if (activeAttacks.isEmpty()) {
                log.debug("No attacks are currently active.");
            } else {
                updateAttackStatusImpl();
            }
        }
    }

    private void startNewAttacks () {
        if (holidayManager.isHoliday() || !holidayManager.isWorkingHours()) {
            log.debug("Cannot start new attacks right now.");
        }
        Attack attack = newAttackQueue.poll();
        while (attack != null) {
            if (attack.startAttack(notificationManager)) {
                activeAttacks.add(attack);
            }
            attack = newAttackQueue.poll();
        }
    }

    private void updateAttackStatusImpl () {
        log.info("Updating status on active attacks");
        Set<Attack> finishedAttacks = new HashSet<>();
        for (Attack attack : activeAttacks) {
            AttackState attackState = attack.getAttackState();
            if (attackState == AttackState.FINISHED) {
                log.info("Removing attack from active attack roster");
                finishedAttacks.add(attack);
            }
        }
        activeAttacks.removeAll(finishedAttacks);
    }

    @Scheduled(cron = "${schedule:0 0 * * * *}")
    void startAttacks () {
        if (activeAttacks.isEmpty()) {
            PlatformLevel attackLevel = platformManager.getAttackableLevel();
            platformManager.getPlatforms()
                           .stream()
                           .filter(platform -> platform.getPlatformLevel() == attackLevel)
                           .filter(platformManager::otherPlatformsHealthy)
                           .filter(AttackableObject::canAttack)
                           .map(Platform::getRoster)
                           .flatMap(Collection::stream)
                           .filter(container -> !this.attackAlreadyExists(container))
                           .filter(Container::canDestroy)
                           .map(Container::createAttack)
                           .map(this::addAttack)
                           .forEach(attack -> log.info("{}", attack));
        }
    }

    Set<Attack> getActiveAttacks () {
        return activeAttacks;
    }

    private boolean attackAlreadyExists (Container newContainer) {
        return activeAttacks.stream().map(Attack::getContainer).anyMatch(container -> container.equals(newContainer));
    }

    Set<Attack> attackContainerId (Long containerIdentity) {
        return platformManager.getPlatforms()
                              .stream()
                              .map(Platform::getRoster)
                              .flatMap(Collection::stream)
                              .filter(container -> container.getIdentity() == containerIdentity)
                              .map(Container::createAttack)
                              .map(this::addAttack)
                              .collect(Collectors.toSet());
    }

    Queue<Attack> getNewAttackQueue () {
        return newAttackQueue;
    }
}

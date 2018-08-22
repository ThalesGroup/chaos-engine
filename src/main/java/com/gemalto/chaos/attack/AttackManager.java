package com.gemalto.chaos.attack;

import com.gemalto.chaos.attack.enums.AttackState;
import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.PlatformManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

@Component
public class AttackManager {
    private static final Logger log = LoggerFactory.getLogger(AttackManager.class);
    private final Set<Attack> activeAttacks = new HashSet<>();
    private final Queue<Attack> newAttackQueue = new LinkedBlockingDeque<>();
    private NotificationManager notificationManager;
    private PlatformManager platformManager;
    private HolidayManager holidayManager;

    @Autowired
    public AttackManager (NotificationManager notificationManager, PlatformManager platformManager, HolidayManager holidayManager) {
        this.notificationManager = notificationManager;
        this.platformManager = platformManager;
        this.holidayManager = holidayManager;
    }

    private Attack addAttack (Attack attack) {
        newAttackQueue.offer(attack);
        return attack;
    }

    @Scheduled(fixedDelay = 15 * 1000)
    private synchronized void updateAttackStatus () {
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
        if (holidayManager.isHoliday() || holidayManager.isOutsideWorkingHours()) {
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
        log.info("Active attacks: {}", activeAttacks.size());
        Set<Attack> finishedAttacks = new HashSet<>();
        activeAttacks.parallelStream().forEach(attack -> {
            AttackState attackState = attack.getAttackState();
            if (attackState == AttackState.FINISHED) {
                log.info("Removing attack {} from active attack roster", attack.getId());
                finishedAttacks.add(attack);
            }
        });
        activeAttacks.removeAll(finishedAttacks);
    }

    @Scheduled(cron = "${schedule:0 0 * * * *}")
    void startAttacks () {
        startAttacks(false);
    }

    void startAttacks (final boolean force) {
        if (activeAttacks.isEmpty()) {
            Optional<Platform> optionalPlatform = platformManager.getPlatforms().parallelStream()
                                                                 .peek(platform -> platform.usingHolidayManager(holidayManager))
                                                                 .filter(platform1 -> force || platform1.canAttack())
                                                                 // "force" needs to be before the .canAttack(), so if it's true canAttack is not evaluated.
                                                                 .findFirst();
            if (optionalPlatform.isPresent()) {
                Platform platform = optionalPlatform.get();
                platform.startAttack().getRoster().parallelStream().filter(Container::canAttack)
                        .map(Container::createAttack)
                        .map(this::addAttack)
                        .forEach(attack -> log.info("Starting attack {}, {}", attack.getId(), attack));
            }
        }
    }

    Set<Attack> getActiveAttacks () {
        return activeAttacks;
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

    Attack getAttackByUUID (String uuid) {
        return activeAttacks.stream().filter(attack -> attack.getId().equals(uuid)).findFirst().orElse(null);
    }
}

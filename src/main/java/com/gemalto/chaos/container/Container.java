package com.gemalto.chaos.container;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.fateengine.FateManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class Container {

    @Autowired
    private FateManager fateManager;

    protected static List<AttackType> supportedAttackTypes = new ArrayList<>();
    protected ContainerHealth containerHealth;

    public boolean supportsAttackType(AttackType attackType) {
        return supportedAttackTypes != null && supportedAttackTypes.contains(attackType);
    }

    public abstract void updateContainerHealth();

    public ContainerHealth getContainerHealth() {
        updateContainerHealth();
        return containerHealth;
    }

    public Attack createAttack() {
        return createAttack(
                supportedAttackTypes
                        .get(new Random()
                                .nextInt(supportedAttackTypes.size())
                        )
        );
    }

    public abstract Attack createAttack(AttackType attackType);

    public boolean canDestroy() {
        return fateManager.getFateEngineForContainer(this).canDestroy();
    }
}

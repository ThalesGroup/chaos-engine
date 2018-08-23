package com.gemalto.chaos.container;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.AttackableObject;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.CRC32;

import static com.gemalto.chaos.util.MethodUtils.getMethodsWithAnnotation;

public abstract class Container implements AttackableObject {
    protected final transient Logger log = LoggerFactory.getLogger(getClass());
    private final List<AttackType> supportedAttackTypes = new ArrayList<>();
    private ContainerHealth containerHealth;
    private Method lastAttackMethod;
    private Attack currentAttack;

    protected Container () {
        for (AttackType attackType : AttackType.values()) {
            if (!getMethodsWithAnnotation(this.getClass(), attackType.getAnnotation()).isEmpty()) {
                supportedAttackTypes.add(attackType);
            }
        }
    }

    @Override
    public boolean canAttack () {
        return new Random().nextDouble() < getPlatform().getDestructionProbability();
    }

    protected abstract Platform getPlatform ();

    public List<AttackType> getSupportedAttackTypes () {
        return supportedAttackTypes;
    }

    @Override
    public boolean equals (Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        Container other = (Container) o;
        return this.getIdentity() == other.getIdentity();
    }

    /**
     * Uses all the fields in the container implementation (but not the Container parent class)
     * to create a checksum of the container. This checksum should be immutable and can be used
     * to recognize when building a roster if the container already exists, and can reference
     * the same object.
     *
     * @return A checksum (format long) of the class based on the implementation specific fields
     */
    public long getIdentity () {
        StringBuilder identity = new StringBuilder();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers())) continue;
            if (field.isSynthetic()) continue;
            field.setAccessible(true);
            try {
                if (field.get(this) != null) {
                    if (identity.length() > 1) {
                        identity.append("$$$$$");
                    }
                    identity.append(field.get(this).toString());
                }
            } catch (IllegalAccessException e) {
                log.error("Caught IllegalAccessException ", e);
            }
        }
        byte[] primitiveByteArray = identity.toString().getBytes();
        CRC32 checksum = new CRC32();
        checksum.update(primitiveByteArray);
        return checksum.getValue();
    }

    @Override
    public String toString () {
        StringBuilder output = new StringBuilder();
        output.append("Container type: ");
        output.append(this.getClass().getSimpleName());
        for (Field field : this.getClass().getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers())) continue;
            if (field.isSynthetic()) continue;
            field.setAccessible(true);
            try {
                output.append("\n\t");
                output.append(field.getName());
                output.append(":\t");
                output.append(field.get(this));
            } catch (IllegalAccessException e) {
                log.error("Could not read from field {}", field.getName(), e);
            }
        }
        return output.toString();
    }

    public boolean supportsAttackType (AttackType attackType) {
        return supportedAttackTypes.contains(attackType);
    }

    public ContainerHealth getContainerHealth (AttackType attackType) {
        updateContainerHealth(attackType);
        return containerHealth;
    }

    private void updateContainerHealth (AttackType attackType) {
        containerHealth = updateContainerHealthImpl(attackType);
    }

    protected abstract ContainerHealth updateContainerHealthImpl (AttackType attackType);

    public Attack createAttack () {
        currentAttack = createAttack(supportedAttackTypes.get(new Random().nextInt(supportedAttackTypes.size())));
        return currentAttack;
    }

    public abstract Attack createAttack (AttackType attackType);

    public void attackContainer (Attack attack) {
        containerHealth = ContainerHealth.UNDER_ATTACK;
        log.info("Starting a attack {} against container {}", attack.getId(), this);
        attackWithAnnotation(attack);
    }

    @SuppressWarnings("unchecked")
    private void attackWithAnnotation (Attack attack) {
        List<Method> attackMethods = getMethodsWithAnnotation(this.getClass(), attack.getAttackType().getAnnotation());
        if (attackMethods.isEmpty()) {
            throw new ChaosException("Could not find an attack vector");
        }
        int index = ThreadLocalRandom.current().nextInt(attackMethods.size());
        try {
            lastAttackMethod = attackMethods.get(index);
            lastAttackMethod.invoke(this, attack);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to run attack {} on container {}: {}", attack.getId(), this, e);
            throw new ChaosException(e);
        }
    }

    public void repeatAttack (Attack attack) {
        if (lastAttackMethod == null) {
            throw new ChaosException("Trying to repeat an attack without having a prior one");
        }
        containerHealth = ContainerHealth.UNDER_ATTACK;
        try {
            lastAttackMethod.invoke(this, attack);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new ChaosException(e);
        }
    }

    public abstract String getSimpleName ();
}

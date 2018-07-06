package com.gemalto.chaos.container;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.fateengine.FateEngine;
import com.gemalto.chaos.fateengine.FateManager;
import com.gemalto.chaos.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static com.gemalto.chaos.util.MethodUtils.getMethodsWithAnnotation;

@Component
public abstract class Container {
    @Override
    public boolean equals (Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        Container other = (Container) o;
        return this.getIdentity() == other.getIdentity();
    }

    private final List<AttackType> supportedAttackTypes = new ArrayList<>();
    protected final transient Logger log = LoggerFactory.getLogger(getClass());
    protected transient FateManager fateManager;
    private ContainerHealth containerHealth;
    private Method lastAttackMethod;

    @Autowired
    protected Container () {
        for (AttackType attackType : AttackType.values()) {
            if (!getMethodsWithAnnotation(this.getClass(), attackType.getAnnotation()).isEmpty()) {
                supportedAttackTypes.add(attackType);
            }
        }
    }

    protected abstract Platform getPlatform ();

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
        return createAttack(supportedAttackTypes.get(new Random().nextInt(supportedAttackTypes.size())));
    }

    public abstract Attack createAttack (AttackType attackType);

    public boolean canDestroy () {
        FateEngine fateEngine = fateManager.getFateEngineForContainer(this);
        return this.containerHealth != ContainerHealth.UNDER_ATTACK && fateEngine.canDestroy();
    }

    public Callable<Void> attackContainer (AttackType attackType) {
        containerHealth = ContainerHealth.UNDER_ATTACK;
        log.info("Starting a {} attack against container {}", attackType, this);
        return attackWithAnnotation(attackType.getAnnotation());
    }

    @SuppressWarnings("unchecked")
    private Callable<Void> attackWithAnnotation (Class<? extends Annotation> annotation) {
        List<Method> attackMethods = getMethodsWithAnnotation(this.getClass(), annotation);
        if (attackMethods.isEmpty()) {
            throw new ChaosException("Could not find an attack vector");
        }
        Integer index = ThreadLocalRandom.current().nextInt(attackMethods.size());
        try {
            lastAttackMethod = attackMethods.get(index);
            return (Callable<Void>) lastAttackMethod.invoke(this);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to run attack on container {}", this, e);
            throw new ChaosException(e);
        }
    }

    public void repeatAttack () {
        if (lastAttackMethod == null) {
            throw new ChaosException("Trying to repeat an attack without having a prior one");
        }
        containerHealth = ContainerHealth.UNDER_ATTACK;
        try {
            lastAttackMethod.invoke(this);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new ChaosException(e);
        }
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
        Checksum checksum = new CRC32();
        ((CRC32) checksum).update(primitiveByteArray);
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

    public abstract String getSimpleName ();
}

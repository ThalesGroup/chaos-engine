package com.gemalto.chaos.notification;

import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.notification.enums.NotificationLevel;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;

@SuppressWarnings("unused")
public class ChaosEvent {
    private Container targetContainer;
    private Date chaosTime;
    private String message;
    private AttackType attackType;
    private NotificationLevel notificationLevel;

    public AttackType getAttackType () {
        return attackType;
    }

    public NotificationLevel getNotificationLevel () {
        return notificationLevel;
    }

    public static ChaosEventBuilder builder () {
        return ChaosEventBuilder.builder();
    }

    public Container getTargetContainer () {
        return targetContainer;
    }

    public Date getChaosTime () {
        return chaosTime;
    }

    public String getMessage () {
        return message;
    }

    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder("Chaos Event: ");
        for (Field field : ChaosEvent.class.getDeclaredFields()) {
            if (field.isSynthetic()) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;
            field.setAccessible(true);
            try {
                if (field.get(this) == null) continue;
            } catch (IllegalAccessException e) {
                continue;
            }
            sb.append("[");
            sb.append(field.getName());
            sb.append("=");
            try {
                sb.append(field.get(this));
            } catch (IllegalAccessException e) {
                sb.append("IllegalAccessException");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    public static final class ChaosEventBuilder {
        private Container targetContainer;
        private Date chaosTime;
        private String message;
        private AttackType attackType;
        private NotificationLevel notificationLevel;

        private ChaosEventBuilder () {
        }

        private static ChaosEventBuilder builder () {
            return new ChaosEventBuilder();
        }

        public ChaosEventBuilder withTargetContainer (Container targetContainer) {
            this.targetContainer = targetContainer;
            return this;
        }

        public ChaosEventBuilder withChaosTime (Date chaosTime) {
            this.chaosTime = chaosTime;
            return this;
        }

        public ChaosEventBuilder withMessage (String message) {
            this.message = message;
            return this;
        }

        public ChaosEventBuilder withAttackType (AttackType attackType) {
            this.attackType = attackType;
            return this;
        }

        public ChaosEventBuilder withNotificationLevel (NotificationLevel notificationLevel) {
            this.notificationLevel = notificationLevel;
            return this;
        }

        public ChaosEvent build () {
            ChaosEvent chaosEvent = new ChaosEvent();
            chaosEvent.targetContainer = this.targetContainer;
            chaosEvent.message = this.message;
            chaosEvent.chaosTime = this.chaosTime;
            chaosEvent.attackType = this.attackType;
            chaosEvent.notificationLevel = this.notificationLevel;
            return chaosEvent;
        }
    }
}

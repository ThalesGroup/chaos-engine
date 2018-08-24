package com.gemalto.chaos.notification;

import com.gemalto.chaos.attack.Attack;
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
    private String attackId;
    private String message;
    private AttackType attackType;
    private String attackLayer;
    private String attackMethod;
    private NotificationLevel notificationLevel;

    public static ChaosEventBuilder builder () {
        return ChaosEventBuilder.builder();
    }

    public AttackType getAttackType () {
        return attackType;
    }

    public NotificationLevel getNotificationLevel () {
        return notificationLevel;
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
            boolean usedField = true;
            field.setAccessible(true);
            try {
                if (field.isSynthetic() || Modifier.isTransient(field.getModifiers()) || field.get(this) == null) {
                    usedField = false;
                }
            } catch (IllegalAccessException e) {
                usedField = false;
            }
            if (!usedField) continue;
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
        private String attackId;
        private String message;
        private AttackType attackType;
        private String attackLayer;
        private String attackMethod;
        private NotificationLevel notificationLevel;

        private ChaosEventBuilder () {
        }

        private static ChaosEventBuilder builder () {
            return new ChaosEventBuilder();
        }

        public ChaosEventBuilder fromAttack (Attack attack) {
            this.chaosTime = Date.from(attack.getStartTime());
            this.targetContainer = attack.getContainer();
            this.attackType = attack.getAttackType();
            this.attackId = attack.getId();
            this.attackLayer = attack.getAttackLayer().getPlatformType();
            this.attackMethod = attack.getAttackMethod().getName();
            return this;
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

        public void withAttackId (String attackId) {
            this.attackId = attackId;
        }

        public ChaosEvent build () {
            ChaosEvent chaosEvent = new ChaosEvent();
            chaosEvent.targetContainer = this.targetContainer;
            chaosEvent.message = this.message;
            chaosEvent.chaosTime = this.chaosTime;
            chaosEvent.attackId = this.attackId;
            chaosEvent.attackType = this.attackType;
            chaosEvent.attackLayer = this.attackLayer;
            chaosEvent.attackMethod = this.attackMethod;
            chaosEvent.notificationLevel = this.notificationLevel;
            return chaosEvent;
        }
    }
}

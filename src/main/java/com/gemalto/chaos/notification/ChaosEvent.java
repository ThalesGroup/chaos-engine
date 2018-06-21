package com.gemalto.chaos.notification;

import com.gemalto.chaos.container.Container;

import java.lang.reflect.Field;
import java.util.Date;

public class ChaosEvent {
    @SuppressWarnings("unused")
    private Container targetContainer;
    @SuppressWarnings("unused")
    private Date chaosTime;
    @SuppressWarnings("unused")
    private String message;

    public static ChaosEventBuilder builder () {
        return ChaosEventBuilder.builder();
    }

    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder("Chaos Event: ");
        for (Field field : ChaosEvent.class.getFields()) {
            field.setAccessible(true);
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

        public ChaosEvent build () {
            ChaosEvent chaosEvent = new ChaosEvent();
            chaosEvent.targetContainer = this.targetContainer;
            chaosEvent.message = this.message;
            chaosEvent.chaosTime = this.chaosTime;
            return chaosEvent;
        }
    }
}

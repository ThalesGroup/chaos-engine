package com.gemalto.chaos.notification;

import com.gemalto.chaos.container.Container;

import java.util.Date;

public class ChaosEvent {
    private Container targetContainer;
    private Date chaosTime;
    private String message;

    public static ChaosEventBuilder builder () {
        return ChaosEventBuilder.builder();
    }

    @Override
    public String toString () {
        return "ChaosEvent{" + "targetContainer=" + targetContainer + ", chaosTime=" + chaosTime + ", message='" + message + '\'' + '}';
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

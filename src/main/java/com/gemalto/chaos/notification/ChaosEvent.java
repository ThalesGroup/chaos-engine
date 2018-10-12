package com.gemalto.chaos.notification;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.enums.NotificationLevel;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;

@SuppressWarnings("unused")
public class ChaosEvent {
    private Container targetContainer;
    private Date chaosTime;
    private String experimentId;
    private String message;
    private ExperimentType experimentType;
    private String experimentMethod;
    private NotificationLevel notificationLevel;

    public static ChaosEventBuilder builder () {
        return ChaosEventBuilder.builder();
    }

    public String getExperimentId () {
        return experimentId;
    }

    public String getExperimentMethod () {
        return experimentMethod;
    }

    public ExperimentType getExperimentType () {
        return experimentType;
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
        private String experimentId;
        private String message;
        private ExperimentType experimentType;
        private String experimentMethod;
        private NotificationLevel notificationLevel;

        private ChaosEventBuilder () {
        }

        private static ChaosEventBuilder builder () {
            return new ChaosEventBuilder();
        }

        public ChaosEventBuilder fromExperiment (Experiment experiment) {
            this.chaosTime = Date.from(experiment.getStartTime());
            this.targetContainer = experiment.getContainer();
            this.experimentType = experiment.getExperimentType();
            this.experimentId = experiment.getId();
            this.experimentMethod = experiment.getExperimentMethod().getName();
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

        public ChaosEventBuilder withExperimentType (ExperimentType experimentType) {
            this.experimentType = experimentType;
            return this;
        }

        public ChaosEventBuilder withNotificationLevel (NotificationLevel notificationLevel) {
            this.notificationLevel = notificationLevel;
            return this;
        }

        public ChaosEventBuilder withExperimentId (String experimentId) {
            this.experimentId = experimentId;
            return this;
        }

        public ChaosEvent build () {
            ChaosEvent chaosEvent = new ChaosEvent();
            chaosEvent.targetContainer = this.targetContainer;
            chaosEvent.message = this.message;
            chaosEvent.chaosTime = this.chaosTime;
            chaosEvent.experimentId = this.experimentId;
            chaosEvent.experimentType = this.experimentType;
            chaosEvent.experimentMethod = this.experimentMethod;
            chaosEvent.notificationLevel = this.notificationLevel;
            return chaosEvent;
        }
    }
}

package com.thales.chaos.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thales.chaos.container.Container;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.enums.NotificationLevel;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Map;

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

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Map<Object, Object> asMap () {
        return (Map<Object, Object>) new ObjectMapper().convertValue(this, Map.class);
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

    @Override
    public int hashCode () {
        int result = targetContainer != null ? targetContainer.hashCode() : 0;
        result = 31 * result + (chaosTime != null ? chaosTime.hashCode() : 0);
        result = 31 * result + (experimentId != null ? experimentId.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (experimentType != null ? experimentType.hashCode() : 0);
        result = 31 * result + (experimentMethod != null ? experimentMethod.hashCode() : 0);
        result = 31 * result + (notificationLevel != null ? notificationLevel.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChaosEvent that = (ChaosEvent) o;
        if (targetContainer != null ? !targetContainer.equals(that.targetContainer) : that.targetContainer != null)
            return false;
        if (chaosTime != null ? !chaosTime.equals(that.chaosTime) : that.chaosTime != null) return false;
        if (experimentId != null ? !experimentId.equals(that.experimentId) : that.experimentId != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (experimentType != that.experimentType) return false;
        if (experimentMethod != null ? !experimentMethod.equals(that.experimentMethod) : that.experimentMethod != null)
            return false;
        return notificationLevel == that.notificationLevel;
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
            this.experimentMethod = experiment.getExperimentMethod() != null ? experiment.getExperimentMethod()
                                                                                         .getExperimentName() : "";
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

        public ChaosEventBuilder withExperimentMethod(String experimentMethod){
            this.experimentMethod = experimentMethod;
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

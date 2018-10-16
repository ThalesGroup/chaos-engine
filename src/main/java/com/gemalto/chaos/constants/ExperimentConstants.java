package com.gemalto.chaos.constants;

public class ExperimentConstants {
    public static final int DEFAULT_EXPERIMENT_DURATION_MINUTES = 5;
    public static final int DEFAULT_TIME_BEFORE_FINALIZATION_SECONDS = 30;
    public static final int DEFAULT_SELF_HEALING_INTERVAL_MINUTES = 2;
    public static final String CANNOT_RUN_SELF_HEALING_AGAIN_YET = "Cannot run self healing again yet";
    public static final String SYSTEM_IS_PAUSED_AND_UNABLE_TO_RUN_SELF_HEALING = "System is paused and unable to run self healing";
    public static final String AN_EXCEPTION_OCCURRED_WHILE_RUNNING_SELF_HEALING = "An exception occurred while running self-healing.";

    private ExperimentConstants () {
    }
}

package com.gemalto.chaos.scripts;

import java.util.Collection;

public interface Script {
    String getHealthCheckCommand ();

    String getSelfHealingCommand ();

    boolean isRequiresCattle ();

    String getScriptName ();

    boolean doesNotUseMissingDependencies (Collection<String> knownMissingDependencies);

    String getFinalizeCommand ();
}

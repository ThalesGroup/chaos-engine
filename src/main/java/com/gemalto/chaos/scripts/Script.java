package com.gemalto.chaos.scripts;

import java.util.Collection;

public interface Script {
    String getScriptName ();

    boolean doesNotUseMissingDependencies (Collection<String> knownMissingDependencies);
}

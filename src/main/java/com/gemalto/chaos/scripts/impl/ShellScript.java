package com.gemalto.chaos.scripts.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.scripts.Script;
import com.gemalto.chaos.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.v;

public class ShellScript implements Script {
    private static final Logger log = LoggerFactory.getLogger(ShellScript.class);
    private String scriptName;
    private Resource scriptResource;
    private Collection<String> dependencies;
    private Collection<String> commentBlock;
    private String scriptContents;
    private String description;
    private String shebang;
    private String healthCheckCommand;
    private String selfHealingCommand;
    private boolean requiresCattle;

    public static ShellScript fromResource (Resource resource) {
        ShellScript script = new ShellScript();
        script.scriptName = resource.getFilename();
        script.scriptResource = resource;
        try {
            script.scriptContents = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new ChaosException(e);
        }
        script.buildFields();
        log.debug("Created script {}", v("ShellScript", script));
        return script;
    }

    private void buildFields () {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("scriptName", scriptName)) {
            buildCommentBlock();
            buildShebang();
            buildDependencies();
            buildDescription();
            buildRequiresCattle();
            buildHealthCheckCommand();
            buildSelfHealingCommand();
        }
    }

    private void buildCommentBlock () {
        commentBlock = new ArrayList<>();
        for (String s : scriptContents.split("\n")) {
            if (!s.startsWith("#")) break;
            commentBlock.add(s);
        }
        log.debug("Comment block evaluated to be {}", v("scriptCommentBlock", commentBlock));
    }

    private void buildShebang () {
        String firstLine = scriptContents.split("\n")[0];
        shebang = firstLine.startsWith("#!") ? firstLine.substring(2) : null;
    }

    private void buildDependencies () {
        Optional<String> dependencyComment = commentBlock.stream()
                                                         .filter(s -> s.startsWith("# Dependencies"))
                                                         .findFirst();
        dependencies = dependencyComment.map(s -> Arrays.stream(s.substring("# Dependencies".length()).split(","))
                                                        .map(ShellScript::stripNonAlphasFromEnd)
                                                        .filter(s1 -> !s1.isEmpty())
                                                        .collect(Collectors.toSet()))
                                        .orElseGet(() -> Arrays.stream(scriptContents.split("\n"))
                                                               .filter(s -> !s.startsWith("#"))
                                                               .filter(s -> !s.isEmpty())
                                                               .map(ShellScript::stripNonAlphasFromEnd)
                                                               .map(s -> s.split(" ")[0])
                                                               .map(ShellScript::stripNonAlphasFromEnd)
                                                               .collect(Collectors.toSet()));
        if (shebang != null) {
            dependencies.add(shebang);
        }
        log.debug("Dependencies evaluated to be {}", v("scriptDependencies", dependencies));
    }

    private void buildDescription () {
        description = getOptionalFieldFromCommentBlock("Description").orElse("No description provided");
        log.debug("Description evaluated to be {}", description);
    }

    private void buildRequiresCattle () {
        requiresCattle = Boolean.valueOf(getOptionalFieldFromCommentBlock("Cattle").orElse(null));
    }

    private void buildHealthCheckCommand () {
        if (requiresCattle) return;
        healthCheckCommand = getOptionalFieldFromCommentBlock("Health check").orElse(null);
    }

    private void buildSelfHealingCommand () {
        if (requiresCattle) return;
        selfHealingCommand = getOptionalFieldFromCommentBlock("Self healing").orElse(null);
    }

    private static String stripNonAlphasFromEnd (String s) {
        return s.replaceAll("^[^A-Za-z0-9]+", "").replaceAll("[^A-Za-z0-9]+$", "");
    }

    private Optional<String> getOptionalFieldFromCommentBlock (String demarcation) {
        return commentBlock.stream()
                           .filter(s -> s.startsWith("# " + demarcation + ":"))
                           .map(s -> s.replaceAll("^# " + demarcation + ": *", ""))
                           .map(StringUtils::trimSpaces)
                           .findFirst();
    }

    public String getHealthCheckCommand () {
        return healthCheckCommand;
    }

    public String getSelfHealingCommand () {
        return selfHealingCommand;
    }

    public boolean isRequiresCattle () {
        return requiresCattle;
    }

    public String getDescription () {
        return description;
    }

    public Collection<String> getDependencies () {
        return dependencies;
    }

    public Collection<String> getCommentBlock () {
        return commentBlock;
    }

    public String getScriptContents () {
        return scriptContents;
    }

    @JsonIgnore
    public Resource getScriptResource () {
        return scriptResource;
    }

    @Override
    public String getScriptName () {
        return scriptName;
    }

    @Override
    public boolean doesNotUseMissingDependencies (Collection<String> knownMissingDependencies) {
        return dependencies.stream().noneMatch(knownMissingDependencies::contains);
    }
}

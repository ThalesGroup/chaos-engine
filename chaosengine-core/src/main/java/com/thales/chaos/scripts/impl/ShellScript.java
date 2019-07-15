package com.thales.chaos.scripts.impl;

import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.scripts.Script;
import com.thales.chaos.util.ShellUtils;
import com.thales.chaos.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.thales.chaos.exception.enums.ChaosErrorCode.SHELL_SCRIPT_FORMATTING_ERROR;
import static com.thales.chaos.exception.enums.ChaosErrorCode.SHELL_SCRIPT_READ_FAILURE;
import static java.util.function.Predicate.not;
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
    private String finalizeCommand;
    private ExperimentType experimentType;

    public static ShellScript fromResource (Resource resource) {
        ShellScript script = new ShellScript();
        script.scriptName = resource.getFilename();
        script.scriptResource = resource;
        try {
            script.scriptContents = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new ChaosException(SHELL_SCRIPT_READ_FAILURE, e);
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
            buildHealthCheckCommand();
            buildSelfHealingCommand();
            buildRequiresCattle();
            buildFinalizeCommand();
            buildExperimentType();
        }
    }

    private void buildCommentBlock () {
        commentBlock = scriptContents.lines().takeWhile(ShellUtils::isCommentedLine).collect(Collectors.toList());
        log.debug("Comment block evaluated to be {}", v("scriptCommentBlock", commentBlock));
    }

    private void buildShebang () {
        String firstLine = scriptContents.split("\n")[0];
        shebang = firstLine.startsWith("#!") ? firstLine.substring(2) : null;
    }

    private void buildRequiresCattle () {
        switch ((healthCheckCommand != null ? 1 : 0) + (selfHealingCommand != null ? 1 : 0)) {
            case 0:
                requiresCattle = true;
                break;
            case 2:
                requiresCattle = false;
                break;
            case 1:
            default:
                throw new ChaosException(SHELL_SCRIPT_FORMATTING_ERROR);
        }
    }

    public Collection<String> getCommentBlock () {
        return commentBlock;
    }

    private void buildExperimentType () {
        String experimentType = getOptionalFieldFromCommentBlock("Experiment type").orElse("State");
        try {
            this.experimentType = ExperimentType.valueOf(experimentType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Illegal experiment type in script header. Defaulting to state", e);
            this.experimentType = ExperimentType.STATE;
        }
    }

    private void buildDependencies () {
        Optional<String> dependencyComment = commentBlock.stream()
                                                         .filter(s -> s.startsWith("# Dependencies"))
                                                         .findFirst();
        dependencies = dependencyComment.map(s -> Arrays.stream(s.substring("# Dependencies".length()).split(","))
                                                        .map(ShellScript::stripNonAlphasFromEnd)
                                                        .filter(not(String::isEmpty))
                                                        .collect(Collectors.toSet()))
                                        .orElseGet(() -> Arrays.stream(scriptContents.split("\n"))
                                                               .filter(not(ShellUtils::isCommentedLine))
                                                               .filter(not(String::isEmpty))
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

    public String getShebang () {
        return shebang;
    }

    private void buildHealthCheckCommand () {
        healthCheckCommand = getOptionalFieldFromCommentBlock("Health check").orElse(null);
    }

    private void buildSelfHealingCommand () {
        selfHealingCommand = getOptionalFieldFromCommentBlock("Self healing").orElse(null);
    }

    private void buildFinalizeCommand () {
        finalizeCommand = getOptionalFieldFromCommentBlock("Finalize command").orElse(null);
        log.debug("Finalize Command evaluated to be {}", finalizeCommand);
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

    @Override
    public String getHealthCheckCommand () {
        return healthCheckCommand;
    }

    @Override
    public String getSelfHealingCommand () {
        return selfHealingCommand;
    }

    @Override
    public boolean isRequiresCattle () {
        return requiresCattle;
    }

    @Override
    public String getScriptName () {
        return scriptName;
    }

    @Override
    public boolean doesNotUseMissingDependencies (Collection<String> knownMissingDependencies) {
        return dependencies.stream().noneMatch(knownMissingDependencies::contains);
    }

    @Override
    public String getFinalizeCommand () {
        return finalizeCommand;
    }

    @Override
    public Resource getResource () {
        return scriptResource;
    }

    @Override
    public ExperimentType getExperimentType () {
        return Optional.ofNullable(experimentType).orElse(ExperimentType.STATE);
    }

    @Override
    public Collection<String> getDependencies () {
        return dependencies;
    }

    public String getDescription () {
        return description;
    }

    public String getScriptContents () {
        return scriptContents;
    }
}

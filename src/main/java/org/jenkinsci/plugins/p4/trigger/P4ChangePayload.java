package org.jenkinsci.plugins.p4.trigger;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

/**
 * Inject the payload attributes received as {@code P4_COMMIT_*} variables.
 */
public class P4ChangePayload extends InvisibleAction implements EnvironmentContributingAction {

    @NonNull
    private final String port;

    @NonNull
    private final Integer change;

    @NonNull
    private final String pushedBy;

    public P4ChangePayload(@NonNull String port, @NonNull  Integer change, @NonNull String pushedBy) {
        this.port = port;
        this.change = change;
        this.pushedBy = pushedBy;
    }

    @NonNull
    public String getPort() {
        return port;
    }

    @NonNull
    public Integer getChange() {
        return change;
    }

    @NonNull
    public String getPushedBy() {
        return pushedBy;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars envVars) {
        envVars.put("P4_COMMIT_PORT", port);
        envVars.put("P4_COMMIT_NUMBER", Integer.toString(change));
        envVars.put("P4_COMMIT_USER", pushedBy);
    }
}
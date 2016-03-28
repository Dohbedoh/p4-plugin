package org.jenkinsci.plugins.p4.trigger;

import hudson.triggers.SCMTrigger;

import java.io.File;
import java.io.IOException;

/**
 * UI object that says a build is started by P4 Trigger.
 */
public class P4ChangeCause extends SCMTrigger.SCMTriggerCause {

    private final String pushedBy;

    public P4ChangeCause(P4ChangePayload pPayload) {
        this("", pPayload);
    }

    public P4ChangeCause(String pollingLog, P4ChangePayload pPayload) {
        super(pollingLog);
        pushedBy = pPayload.getPushedBy();
    }

    public P4ChangeCause(File pollingLog, P4ChangePayload pPayload) throws IOException {
        super(pollingLog);
        pushedBy = pPayload.getPushedBy();
    }

    @Override
    public String getShortDescription() {
        String pusher = pushedBy != null ? pushedBy : "";
        return "Started by P4 Trigger commit by " + pusher;
    }
}
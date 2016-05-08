package org.jenkinsci.plugins.p4.trigger;

import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.scm.SCM;
import org.jenkinsci.plugins.p4.PerforceScm;
import org.jenkinsci.plugins.p4.client.ConnectionHelper;
import org.jenkinsci.plugins.p4.credentials.P4BaseCredentials;

import java.util.logging.Logger;

/**
 * Probe to find all matching jobs of a P4 Trigger payload.
 */
public class P4JobProbe {

    private static final Logger LOGGER = Logger.getLogger(P4JobProbe.class.getName());

    /**
     * Return if the job passed in matches the trigger's payload.
     */
    public boolean matchingJob(Job<?,?> job, P4ChangePayload pPayload) {
        if (job instanceof AbstractProject) {
            AbstractProject<?, ?> project = (AbstractProject<?, ?>) job;
            SCM scm = project.getScm();

            if (scm instanceof PerforceScm) {
                PerforceScm p4scm = (PerforceScm) scm;
                String id = p4scm.getCredential();
                P4BaseCredentials credential = ConnectionHelper.findCredential(id);

                return credential != null && credential.getP4port().equalsIgnoreCase(pPayload.getPort());
            }
        }
        return false;
    }
}

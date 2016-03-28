package org.jenkinsci.plugins.p4.trigger;

import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.triggers.Trigger;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.jenkinsci.plugins.p4.PerforceScm;
import org.jenkinsci.plugins.p4.client.ConnectionHelper;
import org.jenkinsci.plugins.p4.credentials.P4BaseCredentials;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Probe to find all matching jobs of a P4 Trigger payload.
 */
public class P4JobProbe {

    private static final Logger LOGGER = Logger.getLogger(P4JobProbe.class.getName());

    /**
     * Find all matching jobs and trigger if it matches the {@link P4ChangePayload} passed in.
     */
    public final void triggerMatchingJobs(P4ChangePayload pPayload) {

        /* Run in high privilege to see all the projects anonymous users don't see. This is safe because when we
         actually schedule a build, it's a build that can happen at some random time anyway. */
        SecurityContext old = ACL.impersonate(ACL.SYSTEM);
        try {
            for (Job<?, ?> job : Jenkins.getInstance().getAllItems(Job.class)) {
                P4Trigger trigger = null;
                LOGGER.log(Level.FINE, "P4: Inspecting: {0}", job.getName());

                if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
                    ParameterizedJobMixIn.ParameterizedJob pJob = (ParameterizedJobMixIn.ParameterizedJob) job;
                    for (Trigger<?> t : pJob.getTriggers().values()) {
                        if (t instanceof P4Trigger) {
                            trigger = (P4Trigger) t;
                            break;
                        }
                    }
                }

                if (trigger != null) {

                    LOGGER.log(Level.FINE, "Considering to poke {0}", job.getFullDisplayName());

                    if (!matchingJob(job, pPayload)) {
                        return;
                    }

                    trigger.onPost(pPayload);
                } else {
                    LOGGER.log(Level.FINE, "P4: trigger not set: ", job.getFullDisplayName());
                }
            }
        } finally {
            SecurityContextHolder.setContext(old);
        }
    }

    /**
     * Return if the job passed in matches the trigger's payload.
     */
    private boolean matchingJob(Job<?,?> job, P4ChangePayload pPayload) {
        if (job instanceof AbstractProject) {
            AbstractProject<?, ?> project = (AbstractProject<?, ?>) job;
            SCM scm = project.getScm();

            if (scm instanceof PerforceScm) {
                PerforceScm p4scm = (PerforceScm) scm;
                String id = p4scm.getCredential();
                P4BaseCredentials credential = ConnectionHelper.findCredential(id);

                return credential != null && credential.getP4port().equals(pPayload.getPort());
            }
        }
        return false;
    }
}

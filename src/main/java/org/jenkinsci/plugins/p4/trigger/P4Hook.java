package org.jenkinsci.plugins.p4.trigger;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.UnprotectedRootAction;
import hudson.triggers.Trigger;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Logger;

@Extension
public class P4Hook implements UnprotectedRootAction {

    private static final Logger LOGGER = Logger.getLogger(P4Hook.class.getName());

    public static final String P4_HOOK_URL = "p4";

    private final P4JobProbe probe = new P4JobProbe();

    // https://github.com/jenkinsci/bitbucket-plugin

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return P4_HOOK_URL;
    }

    public void doChange(StaplerRequest req) throws IOException {
        String body = IOUtils.toString(req.getInputStream());
        String contentType = req.getContentType();
        if (contentType != null && contentType.startsWith("application/json")) {
            body = URLDecoder.decode(body, "UTF-8");
        }
        if (body.startsWith("payload=")) {
            body = body.substring(8);
            JSONObject payload = JSONObject.fromObject(body);

            LOGGER.info("Received trigger event: " + body);

            final P4ChangePayload pPayload = new P4ChangePayload(
                    payload.getString("p4port"),
                    payload.getInt("change"),
                    payload.getString("user")
            );

            probe.triggerMatchingJobs(pPayload);
        }
    }

    @Deprecated
    private void probeJobs(String port, String change) throws IOException {
        for (Job<?, ?> job : Jenkins.getInstance().getAllItems(Job.class)) {
            P4Trigger trigger = null;
            LOGGER.fine("P4: trying: " + job.getName());

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
                LOGGER.info("P4: probing: " + job.getName());
                trigger.poke(job, port);
            } else {
                LOGGER.fine("P4: trigger not set: " + job.getName());
            }
        }
    }
}

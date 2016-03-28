package org.jenkinsci.plugins.p4.trigger;

import hudson.Extension;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Job;
import hudson.scm.PollingResult;
import hudson.scm.SCM;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;
import org.apache.commons.jelly.XMLOutput;
import org.jenkinsci.plugins.p4.PerforceScm;
import org.jenkinsci.plugins.p4.client.ConnectionHelper;
import org.jenkinsci.plugins.p4.credentials.P4BaseCredentials;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class P4Trigger extends Trigger<Job<?, ?>> {

    final static Logger LOGGER = Logger.getLogger(P4Trigger.class.getName());

    @DataBoundConstructor
    public P4Trigger() {
    }

	@Deprecated
    public void poke(Job<?, ?> job, String port) throws IOException {
        // exit early if job does not match trigger
        if (!matchServer(job, port)) {
            return;
        }

        LOGGER.info("P4: poking: " + job.getName());

        StreamTaskListener listener = new StreamTaskListener(getLogFile());
        try {
            PrintStream log = listener.getLogger();

            SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems
                    .asSCMTriggerItem(job);

            PollingResult pollResult = item.poll(listener);

            if (pollResult.hasChanges()) {
                log.println("Changes found");
            } else {
                log.println("No changes");
            }
        } catch (Exception e) {
            String msg = "P4: Failed to record P4 trigger: ";
            e.printStackTrace(listener.error(msg));
            LOGGER.severe(msg + e);
        } finally {
            listener.close();
        }
    }

    @Deprecated
    private boolean matchServer(Job<?, ?> job, String port) {
        if (job instanceof AbstractProject) {
            AbstractProject<?, ?> project = (AbstractProject<?, ?>) job;
            SCM scm = project.getScm();

            if (scm instanceof PerforceScm) {
                PerforceScm p4scm = (PerforceScm) scm;
                String id = p4scm.getCredential();
                P4BaseCredentials credential = ConnectionHelper
                        .findCredential(id);
                if (credential != null && credential.getP4port().equals(port)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onPost(final P4ChangePayload pPayload) {
        getDescriptor().queue.execute(new Runnable() {

            private boolean runPolling() {

                LOGGER.log(Level.INFO, "P4: poking: {0}", job.getName());

                try {
                    StreamTaskListener listener = new StreamTaskListener(getLogFile());
                    try {
                        PrintStream logger = listener.getLogger();
                        long start = System.currentTimeMillis();
                        logger.println("Started on "+ DateFormat.getDateTimeInstance().format(new Date()));
                        boolean result = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(job).poll(listener).hasChanges();
                        logger.println("Done. Took "+ Util.getTimeSpanString(System.currentTimeMillis()-start));
                        if(result)
                            logger.println("Changes found");
                        else
                            logger.println("No changes");
                        return result;
                    } catch (Error e) {
                        String msg = "P4: Failed to record P4 trigger: ";
                        e.printStackTrace(listener.error(msg));
                        LOGGER.log(Level.SEVERE, msg + e);
                        throw e;
                    } catch (RuntimeException e) {
                        String msg = "P4: Failed to record P4 trigger: ";
                        e.printStackTrace(listener.error(msg));
                        LOGGER.log(Level.SEVERE, msg + e);
                        throw e;
                    } finally {
                        listener.close();
                    }
                } catch (IOException e) {
                    String msg = "P4: Failed to record P4 trigger: ";
                    LOGGER.log(Level.SEVERE, msg + e);
                }
                return false;
            }

            @Override
            public void run() {

                if (runPolling()) {
                    String name = " #" + job.getNextBuildNumber();
                    P4ChangeCause cause;
                    try {
                        cause = new P4ChangeCause(getLogFile(), pPayload);
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Failed to parse the polling log", e);
                        cause = new P4ChangeCause(pPayload);
                    }

                    ParameterizedJobMixIn pJob = new ParameterizedJobMixIn() {
                        @Override
                        protected Job asJob() {
                            return job;
                        }
                    };

                    if (pJob.scheduleBuild(cause)) {
                        LOGGER.log(Level.INFO, "SCM changes detected in {0}. Triggering {1}",
                                new Object[]{job.getName() + name});
                    } else {
                        LOGGER.log(Level.INFO, "SCM changes detected in {0}. Job is already in the queue",
                                job.getName());
                    }
                }
            }
        });
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.singleton(new P4TriggerAction());
    }

    public File getLogFile() {
        return new File(job.getRootDir(), "p4trigger.log");
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        private transient final SequentialExecutionQueue queue
                = new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof Job;
        }

        @Override
        public String getDisplayName() {
            return "Build when change is commit to P4";
        }
    }

    /**
     * Perforce Trigger Log Action
     *
     * @author pallen
     */

    public final class P4TriggerAction implements Action {
        public Job<?, ?> getOwner() {
            return job;
        }

        public String getIconFileName() {
            return "clipboard.png";
        }

        public String getDisplayName() {
            return "P4 Trigger Log";
        }

        public String getUrlName() {
            return "P4TriggerLog";
        }

        public String getLog() throws IOException {
            return Util.loadFile(getLogFile());
        }

        /**
         * Writes the annotated log to the given output.
         */
        public void writeLogTo(XMLOutput out) throws IOException {
            new AnnotatedLargeText<P4TriggerAction>(getLogFile(),
                    Charset.defaultCharset(), true, this).writeHtmlTo(0,
                    out.asWriter());
        }
    }
}

package org.jenkinsci.plugins.p4.trigger;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.p4.PerforceScm;
import org.jenkinsci.plugins.p4.credentials.P4PasswordImpl;
import org.jenkinsci.plugins.p4.populate.AutoCleanImpl;
import org.jenkinsci.plugins.p4.populate.Populate;
import org.jenkinsci.plugins.p4.workspace.StaticWorkspaceImpl;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by allan on 28/03/2016.
 */
public class P4JobProbeTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private P4JobProbe probe = new P4JobProbe();

    @Test
    public void testJobMatchesPayload() throws Exception {

        FreeStyleProject project = jenkins.createFreeStyleProject("Static-Change");
        StaticWorkspaceImpl workspace = new StaticWorkspaceImpl("none", false, "test.ws");
        Populate populate = new AutoCleanImpl(true, true, false, false, null);

        //Create/Add credentials
        P4PasswordImpl auth = new P4PasswordImpl(CredentialsScope.SYSTEM,
                "id", "desc", "localhost:1666", null, "jenkins", "0", "0", "jenkins");
        SystemCredentialsProvider.getInstance().getCredentials().add(auth);
        SystemCredentialsProvider.getInstance().save();

        PerforceScm scm = new PerforceScm(auth.getId(), workspace, populate);
        project.setScm(scm);
        project.save();

        //Simple cases
        assertTrue("Should match if hostname:port equals", probe.matchingJob(project, new P4ChangePayload("localhost:1666", 2,"user")));
        //Test casing
        assertTrue("Should ignore casing", probe.matchingJob(project, new P4ChangePayload("LOCALHOST:1666", 2,"user")));

        //Check with Environment Variables
        /**
         * TODO
         */
    }

    @Test
    public void testJobDoesNotMatchPayload() throws Exception {

        FreeStyleProject project = jenkins.createFreeStyleProject("Static-Change");
        StaticWorkspaceImpl workspace = new StaticWorkspaceImpl("none", false, "test.ws");
        Populate populate = new AutoCleanImpl(true, true, false, false, null);

        //Create/Add credentials
        P4PasswordImpl auth = new P4PasswordImpl(CredentialsScope.SYSTEM,
                "id", "desc", "localhost:1666", null, "jenkins", "0", "0", "jenkins");
        SystemCredentialsProvider.getInstance().getCredentials().add(auth);
        SystemCredentialsProvider.getInstance().save();

        PerforceScm scm = new PerforceScm(auth.getId(), workspace, populate);
        project.setScm(scm);
        project.save();

        //Simple cases
        assertFalse("Should not match if port is different", probe.matchingJob(project, new P4ChangePayload("localhost:1667", 2,"user")));
        assertFalse("Should not match if host is different", probe.matchingJob(project, new P4ChangePayload("localhos:1666", 2,"user")));
    }
}

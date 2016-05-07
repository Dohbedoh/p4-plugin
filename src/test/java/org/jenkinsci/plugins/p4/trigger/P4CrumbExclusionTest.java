package org.jenkinsci.plugins.p4.trigger;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebRequestSettings;
import com.gargoylesoftware.htmlunit.WebResponse;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class P4CrumbExclusionTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void shouldNotRequireACrumbForTheP4HookUrl() throws IOException, SAXException {
        JenkinsRule.WebClient webClient = jenkins.createWebClient();
        WebRequestSettings wrs = new WebRequestSettings(new URL(webClient.getContextPath() + P4Hook.P4_HOOK_URL + "/change"),
                HttpMethod.POST);
        WebResponse resp = webClient.getPage(wrs).getWebResponse();

        assertEquals(resp.getStatusCode(), 200);
    }
}

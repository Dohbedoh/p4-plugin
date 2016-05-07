package org.jenkinsci.plugins.p4.trigger;

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Extension
public class P4CrumbExclusion extends CrumbExclusion {

    private static final String EXCLUSION_PATH = "/" + P4Hook.P4_HOOK_URL;

    @Override
    public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && (pathInfo.startsWith(EXCLUSION_PATH) || pathInfo.startsWith(EXCLUSION_PATH + "/"))) {
            chain.doFilter(req, resp);
            return true;
        }
        return false;
    }
}
package org.vaadin.anna;

import com.vaadin.server.CleanupServletService;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;

/**
 * Extended VaadinServlet that provides a cleanup thread to clear out session
 * when all UIs have been closed, without needing to wait for the session
 * timeout.
 * <p>
 * By default the check is performed every two seconds. You can change this
 * value by overriding {@link #getCleanupPollingInterval()}
 *
 * @author anna@vaadin.com
 */
public class CleanupServlet extends VaadinServlet {
    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        VaadinServletService service = new CleanupServletService(this,
                deploymentConfiguration, getCleanupPollingInterval(),
                alwaysCheckUITimeOuts());
        service.init();
        return service;
    }

    /**
     * Returns the time interval in milliseconds which used within the
     * background thread to check session timeout. Default is 2000 milliseconds,
     * override this method to change the value.
     * <p>
     * NOTE: updating this value after ServletService has been created won't
     * have any effect on the functionality.
     *
     * @return cleanup polling interval
     */
    protected int getCleanupPollingInterval() {
        return 2000;
    }

    /**
     * Should the UI timeouts be checked even if session itself doesn't timeout
     * yet. Default is {@code false}, override this method to change the value.
     * <p>
     * NOTE: updating this value after ServletService has been created won't
     * have any effect on the functionality.
     *
     * @return {@code true} if the UI timeout check is to be performed on every
     *         cleanup polling, {@code false} otherwise.
     */
    protected boolean alwaysCheckUITimeOuts() {
        return false;
    }
}
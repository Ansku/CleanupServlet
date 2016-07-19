package com.vaadin.server;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.server.VaadinSession.State;
import com.vaadin.ui.UI;

/**
 * Customized VaadinServletService that provides a cleanup thread to clear out
 * session when all UIs have been closed, without needing to wait for the
 * session timeout.
 * <p>
 * Note: this file must be within package com.vaadin.server for to gain have
 * access to cleanupSession method.
 *
 * @author anna@vaadin.com
 */
public class CleanupServletService extends VaadinServletService {

    private static final Logger getLogger() {
        return Logger.getLogger(CleanupServletService.class.getName());
    }

    // check frequency in milliseconds
    int cleanupPollingInterval;
    // check UI timeouts even if Session doesn't timeout
    boolean alwaysCheckUITimeouts;

    public CleanupServletService(VaadinServlet servlet,
            DeploymentConfiguration deploymentConfiguration,
            int cleanupPollingInterval, boolean alwaysCheckUITimeouts)
            throws ServiceException {
        super(servlet, deploymentConfiguration);
        this.cleanupPollingInterval = cleanupPollingInterval;
        this.alwaysCheckUITimeouts = alwaysCheckUITimeouts;
    }

    @Override
    public void init() throws ServiceException {
        super.init();
        addSessionInitListener(new SessionInitListener() {
            @Override
            public void sessionInit(SessionInitEvent event)
                    throws ServiceException {
                // one background thread for each session
                createCleanupThread(event.getSession()).start();
            }
        });
    }

    private Thread createCleanupThread(final VaadinSession session) {
        return new Thread() {

            @Override
            public void run() {
                try {
                    // initial check delayed
                    Thread.sleep(cleanupPollingInterval);

                    // poll cleanup need
                    while (isSessionOpen(session)) {
                        if (!hasTimeLeftBeforeTimeout(session)) {
                            session.access(new Runnable() {

                                @Override
                                public void run() {
                                    /*
                                     * Usually called at the end of each
                                     * request, but it's possible to close a
                                     * browser window in such way that no more
                                     * requests appear. In such cases the
                                     * session cleanup is never called again
                                     * without this check.
                                     */
                                    cleanupSession(session);
                                }
                            });
                        } else if (alwaysCheckUITimeouts) {
                            closeInactiveUIs(session);
                            removeClosedUIs(session);
                            if (session.getUIs().isEmpty()) {
                                session.access(new Runnable() {

                                    @Override
                                    public void run() {
                                        session.close();
                                    }
                                });
                            }
                        }
                        // wait for the next check
                        Thread.sleep(cleanupPollingInterval);
                    }
                } catch (InterruptedException e) {
                    //
                }
            }
        };
    }

    /**
     * Checks whether the session is open.
     *
     * @see VaadinService#isSessionActive(VaadinSession)
     *
     * @param session
     * @return
     */
    private boolean isSessionOpen(VaadinSession session) {
        return !(session.getState() != State.OPEN || session.getSession() == null);
    }

    /**
     * Checks whether session has time left before the timeout should happen.
     *
     * @see VaadinService#isSessionActive(VaadinSession)
     *
     * @param session
     * @return {@code true} if there is time left, {@code false} if time is out
     */
    public boolean hasTimeLeftBeforeTimeout(VaadinSession session) {
        long now = System.currentTimeMillis();
        int timeout = 1000 * getUidlRequestTimeout(session);
        return timeout < 0 || now - session.getLastRequestTimestamp() < timeout;
    }

    /**
     * @see VaadinService#getUidlRequestTimeout(VaadinSession)
     *
     * @param session
     * @return The UIDL request timeout in seconds, or a negative number if
     *         timeout never occurs.
     */
    private int getUidlRequestTimeout(VaadinSession session) {
        return getDeploymentConfiguration().isCloseIdleSessions() ? session
                .getSession().getMaxInactiveInterval() : -1;
    }

    /**
     * Closes those UIs in the given session for which {@link #isUIActive}
     * yields false.
     *
     * @since 7.0.0
     */
    private void closeInactiveUIs(VaadinSession session) {
        final String sessionId = session.getSession().getId();
        for (final UI ui : session.getUIs()) {
            if (!isUIActive(ui) && !ui.isClosing()) {
                ui.access(new Runnable() {
                    @Override
                    public void run() {
                        getLogger().log(Level.FINE,
                                "Closing inactive UI #{0} in session {1}",
                                new Object[] { ui.getUIId(), sessionId });
                        ui.close();
                    }
                });
            }
        }
    }

    /**
     * @see VaadinService#isUIActive(UI)
     *
     * @param ui
     * @return {@code true} if the UI is active, {@code false} if it could be
     *         removed.
     */
    private boolean isUIActive(UI ui) {
        if (ui.isClosing()) {
            return false;
        } else {
            long now = System.currentTimeMillis();
            int timeout = 1000 * getHeartbeatTimeout();
            return timeout < 0
                    || now - ui.getLastHeartbeatTimestamp() < timeout;
        }
    }

    /**
     * @see VaadinService#getHeartbeatTimeout()
     *
     * @return The heartbeat timeout in seconds or a negative number if timeout
     *         never occurs.
     */
    private int getHeartbeatTimeout() {
        // Permit three missed heartbeats before closing the UI
        return (int) (getDeploymentConfiguration().getHeartbeatInterval() * (3.1));
    }

    /**
     * @see VaadinService#removeClosedUIs(VaadinSession)
     *
     * @param session
     */
    private void removeClosedUIs(final VaadinSession session) {
        ArrayList<UI> uis = new ArrayList<UI>(session.getUIs());
        for (final UI ui : uis) {
            if (ui.isClosing()) {
                ui.access(new Runnable() {
                    @Override
                    public void run() {
                        getLogger().log(Level.FINER, "Removing closed UI {0}",
                                ui.getUIId());
                        session.removeUI(ui);
                    }
                });
            }
        }
    }

}
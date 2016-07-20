# CleanupServlet Add-on for Vaadin 7

CleanupServlet is an add-on for Vaadin 7. It's an extended VaadinServlet that provides a cleanup thread to clear out session when all UIs have been closed, without needing to wait for the session timeout.

Usage:

```
    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = CleanupServletDemoUI.class, heartbeatInterval = 1)
    public static class Servlet extends CleanupServlet {
        @Override
        protected int getCleanupPollingInterval() {
            // how long to wait between session timeout checks
            return 3000;
        }
        @Override
        protected boolean alwaysCheckUITimeOuts() {
            // if you want to ensure UI cleanup on every check
            // regardless of session timeout, default false
            return true;
        }
    }
```

For a more comprehensive example, see src/main/java/org/vaadin/anna/cleanupservlet/demo/CleanupServletDemoUI.java

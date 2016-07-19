package org.vaadin.anna.cleanupservlet.demo;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import org.vaadin.anna.CleanupServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Theme("valo")
@Title("CleanupServlet Demo")
@SuppressWarnings("serial")
public class CleanupServletDemoUI extends UI {

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = CleanupServletDemoUI.class, heartbeatInterval = 1)
    public static class Servlet extends CleanupServlet {
        @Override
        protected int getCleanupPollingInterval() {
            // 3 seconds
            return 3000;
        }

        @Override
        protected boolean alwaysCheckUITimeOuts() {
            return true;
        }
    }

    Logger log = Logger.getLogger(CleanupServletDemoUI.class.getName());

    final DetachListener detachListener = new DetachListener() {
        @Override
        public void detach(final DetachEvent detachEvent) {
            log.info("--------------- DETACHED ---------------");
        }
    };

    @Override
    protected void init(VaadinRequest request) {
        addDetachListener(detachListener);

        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        setContent(layout);

        Button button = new Button("Click Me To Delay Cleanup");
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                layout.addComponent(new Label("Thank you for clicking"));
            }
        });
        layout.addComponent(button);
    }

}

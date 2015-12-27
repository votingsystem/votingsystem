package org.votingsystem.client.webextension.pane;

import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.votingsystem.client.webextension.util.Utils;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class HTMLPane extends VBox{

    private static Logger log = Logger.getLogger(HTMLPane.class.getSimpleName());

    private WebView webView;

    public HTMLPane(String paneContent) {
        webView = new WebView();
        webView.getEngine().loadContent(paneContent);
        Utils.browserVSLinkListener(webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        getChildren().addAll(webView);
    }

}
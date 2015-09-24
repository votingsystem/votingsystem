package org.votingsystem.client.dialog;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Window;
import netscape.javascript.JSException;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;

import java.io.File;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageDialog extends DialogVS {

    private static Logger log = Logger.getLogger(MessageDialog.class.getSimpleName());

    private static String webViewContent = "<html style='font-size:1.1em; font-weight: bold; background: #f9f9f9;" +
            "font-family:arial, helvetica, sans-serif;border-radius: 5px;padding: 5px;'>%s</html>";

    private VBox mainPane;
    private HBox footerButtonsBox;
    private Label messageLabel;
    private WebView messageWebView;

    public MessageDialog(Window parentWindow) {
        super(new VBox(10), parentWindow);
        mainPane = (VBox) getContentPane();
        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageWebView = new WebView();
        messageWebView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        messageWebView.setPrefHeight(150);
        HBox.setHgrow(messageWebView, Priority.ALWAYS);
        VBox.setVgrow(messageWebView, Priority.ALWAYS);
        messageWebView.setStyle("-fx-word-wrap:break-word;");
        footerButtonsBox = new HBox(10);
        mainPane.getChildren().addAll(messageLabel, messageWebView, footerButtonsBox);
        mainPane.setStyle("-fx-max-width: 600px;-fx-padding: 3 20 20 20;-fx-spacing: 10;-fx-alignment: center;" +
                "-fx-font-size: 16;-fx-font-weight: bold;-fx-pref-width: 500px;");
    }

    private void isHTMLView(boolean isHTMLView) {
        if(isHTMLView) {
            if(!mainPane.getChildren().contains(messageWebView)) {
                mainPane.getChildren().add(messageWebView);
            } if(mainPane.getChildren().contains(messageLabel)) {
                mainPane.getChildren().remove(messageLabel);
            }
            adjustWebViewHeight();
        } else {
            if(mainPane.getChildren().contains(messageWebView)) {
                mainPane.getChildren().remove(messageWebView);
            }if(!mainPane.getChildren().contains(messageLabel)) {
                mainPane.getChildren().add(messageLabel);
            }
        }
    }

    public void showHtmlMessage(String htmlMessage, String caption) {
        messageWebView.getEngine().loadContent(String.format(webViewContent, htmlMessage));
        isHTMLView(true);
        if(caption != null) setCaption(caption);
        show();
    }

    public void showHtmlMessage(String htmlMessage, Parent parent) {
        if(parent != null) {
            footerButtonsBox.getChildren().add(0, parent);
        }
        messageWebView.getEngine().loadContent(String.format(webViewContent, htmlMessage));
        isHTMLView(true);
        show();
    }


    public void showMessage(Integer statusCode, String message) {
        messageLabel.setText(message);
        setCaption(ContextVS.getMessage("errorLbl"));
        isHTMLView(false);
        if(statusCode != null) {
            if(ResponseVS.SC_OK == statusCode) {
                setCaption(null);
                messageLabel.setGraphic(Utils.getIcon(FontAwesomeIcons.CHECK, Utils.COLOR_RESULT_OK ,32));
            } else {
                setCaption(ContextVS.getMessage("errorLbl"));
                messageLabel.setGraphic(Utils.getIcon(FontAwesomeIcons.TIMES, Utils.COLOR_RED_DARK, 32));
            }
            messageLabel.setGraphicTextGap(15);
        }
        show();
    }

    //http://java-no-makanaikata.blogspot.com.es/2012/10/javafx-webview-size-trick.html
    private void adjustWebViewHeight() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Object result = messageWebView.getEngine().executeScript(
                            "document.getElementById('msgDiv').offsetHeight");
                    if (result instanceof Integer) {
                        double height = new Double((Integer) result) + 40;
                        messageWebView.setPrefHeight(height);
                    }
                    mainPane.getScene().getWindow().sizeToScene();
                } catch (JSException e) {
                    // not important
                }
            }
        });
    }
}
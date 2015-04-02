package org.votingsystem.client.dialog;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.votingsystem.client.pane.DecoratedPane;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;

import java.io.File;
import java.util.logging.Logger;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageDialog extends VBox {

    private static Logger log = Logger.getLogger(MessageDialog.class.getSimpleName());

    private static String webViewContent = "<html style='font-size:1.1em; font-weight: bold; background: #f9f9f9;" +
            "font-family:arial, helvetica, sans-serif;border-radius: 5px;padding: 5px;'>%s</html>";

    private Stage stage;
    private HBox footerButtonsBox;
    private Label messageLabel;
    private DecoratedPane decoratedPane;
    private WebView messageWebView;

    public MessageDialog() {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        //stage.initOwner(window);
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageWebView = new WebView();
        messageWebView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        messageWebView.setPrefHeight(150);
        setPrefWidth(450);
        HBox.setHgrow(messageWebView, Priority.ALWAYS);
        VBox.setVgrow(messageWebView, Priority.ALWAYS);
        messageWebView.setStyle("-fx-word-wrap:break-word;");
        footerButtonsBox = new HBox(10);
        getChildren().addAll(messageLabel, messageWebView, footerButtonsBox);
        setStyle("-fx-max-width: 600px;-fx-padding: 3 20 20 20;-fx-spacing: 10;-fx-alignment: center;-fx-font-size: 16;" +
                "-fx-font-weight: bold;");
        stage.setScene(new Scene(this, Color.TRANSPARENT));
        decoratedPane = new DecoratedPane(null, null, this, stage);
        stage.setScene(new Scene(decoratedPane));
        Utils.addMouseDragSupport(stage);
        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
    }

    private void isHTMLView(boolean isHTMLView) {
        if(isHTMLView) {
            if(!getChildren().contains(messageWebView)) {
                getChildren().add(messageWebView);
            } if(getChildren().contains(messageLabel)) {
                getChildren().remove(messageLabel);
            }
        } else {
            if(getChildren().contains(messageWebView)) {
                getChildren().remove(messageWebView);
            }if(!getChildren().contains(messageLabel)) {
                getChildren().add(messageLabel);
            }
        }
    }

    public void showHtmlMessage(String htmlMessage, String caption) {
        messageWebView.getEngine().loadContent(String.format(webViewContent, htmlMessage));
        isHTMLView(true);
        if(caption != null) decoratedPane.setCaption(caption);
        show();
    }

    public void showHtmlMessage(String htmlMessage, Button optionButton) {
        if(optionButton != null) {
            footerButtonsBox.getChildren().add(0, optionButton);
            optionButton.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> stage.hide());
        }
        messageWebView.getEngine().loadContent(String.format(webViewContent, htmlMessage));
        isHTMLView(true);
        show();
    }


    public void showMessage(Integer statusCode, String message) {
        messageLabel.setText(message);
        decoratedPane.setCaption(ContextVS.getMessage("errorLbl"));
        isHTMLView(false);
        if(statusCode != null) {
            if(ResponseVS.SC_OK == statusCode) {
                decoratedPane.setCaption(null);
                messageLabel.setGraphic(Utils.getIcon(FontAwesomeIconName.CHECK, 32));
            } else {
                decoratedPane.setCaption(ContextVS.getMessage("errorLbl"));
                messageLabel.setGraphic(Utils.getIcon(FontAwesomeIconName.TIMES, Utils.COLOR_RED_DARK, 32));
            }
            messageLabel.setGraphicTextGap(15);
        }
        show();
    }
    
    private void show() {
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
    }

}
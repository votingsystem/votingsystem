package org.votingsystem.client.dialog;

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
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.pane.DecoratedPane;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

import java.io.File;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageDialog extends VBox {

    private static Logger log = Logger.getLogger(MessageDialog.class);

    private static String webViewContent = "<html style='font-size:1.1em; font-weight: bold; background: #f9f9f9;" +
            "font-family:arial, helvetica, sans-serif;border-radius: 5px;padding: 5px;'>%s</html>";

    private Stage stage;
    private HBox footerButtonsBox;
    private Label messageLabel;
    private Label captionLabel;
    private WebView messageWebView;
    private Button acceptButton;

    public MessageDialog() {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        //stage.initOwner(window);
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
        messageLabel = new Label();
        captionLabel = new Label();
        messageLabel.setWrapText(true);
        messageWebView = new WebView();
        messageWebView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        messageWebView.setPrefHeight(200);
        setPrefWidth(500);
        HBox.setHgrow(messageWebView, Priority.ALWAYS);
        VBox.setVgrow(messageWebView, Priority.ALWAYS);
        messageWebView.setStyle("-fx-word-wrap:break-word;");
        acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(actionEvent -> stage.hide());
        acceptButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));

        footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), acceptButton);

        getChildren().addAll(messageLabel, messageWebView, footerButtonsBox);
        setStyle("-fx-max-width: 600px;-fx-padding: 3 20 20 20;-fx-spacing: 10;-fx-alignment: center;-fx-font-size: 16;" +
                "-fx-font-weight: bold;");
        stage.setScene(new Scene(this, Color.TRANSPARENT));
        stage.setScene(new Scene(new DecoratedPane(null, null, this, stage)));
        Utils.addMouseDragSupport(stage);
        stage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
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
        if(caption != null) {
            if(!getChildren().contains(captionLabel)) getChildren().add(0, captionLabel);
            captionLabel.setText(caption);
        } else if(getChildren().contains(captionLabel)) getChildren().remove(captionLabel);
        show();
    }

    public void showHtmlMessage(String htmlMessage, Button optionButton) {
        if(getChildren().contains(captionLabel)) getChildren().remove(captionLabel);
        if(optionButton != null) {
            acceptButton.setVisible(false);
            footerButtonsBox.getChildren().add(0, optionButton);
            optionButton.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> stage.hide());
        }
        messageWebView.getEngine().loadContent(String.format(webViewContent, htmlMessage));
        isHTMLView(true);
        show();
    }


    public void showMessage(Integer statusCode, String message) {
        if(getChildren().contains(captionLabel)) getChildren().remove(captionLabel);
        messageLabel.setText(message);
        isHTMLView(false);
        if(statusCode != null) {
            if(ResponseVS.SC_OK == statusCode) {
                messageLabel.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK, 32));
            } else messageLabel.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK, 32));
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
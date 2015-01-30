package org.votingsystem.client.dialog;

import javafx.event.EventHandler;
import javafx.scene.Node;
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

    private Stage stage;
    private HBox footerButtonsBox;
    private Label messageLabel;
    private Label captionLabel;
    private WebView messageWebView;

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
        messageWebView.setPrefHeight(270);
        //messageWebView.setPrefWidth(600);
        HBox.setHgrow(messageWebView, Priority.ALWAYS);
        VBox.setVgrow(messageWebView, Priority.ALWAYS);
        messageWebView.setStyle("-fx-word-wrap:break-word;");
        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(actionEvent -> stage.hide());
        acceptButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));

        footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), acceptButton);

        getChildren().addAll(messageLabel, messageWebView, footerButtonsBox);
        getStyleClass().add("modal-dialog");
        stage.setScene(new Scene(this, Color.TRANSPARENT));
        stage.getScene().getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        // allow the dialog to be dragged around.
        final Node root = stage.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = stage.getX() - mouseEvent.getScreenX();
                dragDelta.y = stage.getY() - mouseEvent.getScreenY();
            }
        });
        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                stage.setX(mouseEvent.getScreenX() + dragDelta.x);
                stage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
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
        String finalHTML = "<html style='font-size:1.1em; font-weight: bold; background: #f9f9f9;" +
                "font-family:arial, helvetica, sans-serif;'>" + htmlMessage + "</html>";
        messageWebView.getEngine().loadContent(finalHTML);
        isHTMLView(true);
        if(caption != null) {
            if(!getChildren().contains(captionLabel)) getChildren().add(0, captionLabel);
            captionLabel.setText(caption);
        } else if(getChildren().contains(captionLabel)) getChildren().remove(captionLabel);
        show();
    }

    public void showHtmlMessage(String htmlMessage, Button optionButton) {
        if(getChildren().contains(captionLabel)) getChildren().remove(captionLabel);
        String finalHTML = "<html style='font-size:1.1em; font-weight: bold; background: #f9f9f9;" +
                "font-family:arial, helvetica, sans-serif;'>" + htmlMessage + "</html>";
        if(optionButton != null) {
            footerButtonsBox.getChildren().add(0, optionButton);
            optionButton.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> stage.hide());
        }
        messageWebView.getEngine().loadContent(finalHTML);
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

    class Delta { double x, y; }

}
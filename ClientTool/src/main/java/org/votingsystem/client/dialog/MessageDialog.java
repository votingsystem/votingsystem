package org.votingsystem.client.dialog;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
public class MessageDialog {

    private static Logger log = Logger.getLogger(MessageDialog.class);

    private final Stage stage;
    private HBox messageBox;
    private Label messageLabel;
    private WebView messageWebView;

    public MessageDialog() {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        //stage.initOwner(window);
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent window) {      }
        });

        VBox mainBox = new VBox(10);
        messageBox = new HBox(10);
        messageLabel = new Label();
        messageLabel.setWrapText(true);

        messageWebView = new WebView();
        messageWebView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        messageWebView.setPrefHeight(270);
        //messageWebView.setPrefWidth(600);
        HBox.setHgrow(messageWebView, Priority.ALWAYS);
        VBox.setVgrow(messageWebView, Priority.ALWAYS);
        messageWebView.setStyle("-fx-word-wrap:break-word;");
        messageBox.getChildren().add(messageLabel);
        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                stage.hide();
            }});
        acceptButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));

        HBox footerButtonsBox = new HBox(10);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footerButtonsBox.getChildren().addAll(spacer, acceptButton);

        mainBox.getChildren().addAll(messageLabel, messageWebView, footerButtonsBox);
        mainBox.getStyleClass().add("modal-dialog");
        stage.setScene(new Scene(mainBox, Color.TRANSPARENT));
        stage.getScene().getStylesheets().add(getClass().getResource("/css/modal-dialog.css").toExternalForm());
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
    }

    public void showMessage(String message) {
        messageWebView.setVisible(false);
        messageLabel.setText(message);
        messageLabel.setGraphic(null);
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
        messageLabel.setVisible(true);
    }

    public void showHtmlMessage(String htmlMessage) {
        messageLabel.setVisible(false);
        messageWebView.getEngine().loadContent(htmlMessage);
        messageWebView.setVisible(true);
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
    }

    public void showMessage(int statusCode, String message) {
        messageLabel.setText(message);
        if(ResponseVS.SC_OK == statusCode) {
            messageLabel.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK, 32));
        } else messageLabel.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK, 32));
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
    }

    class Delta { double x, y; }

}
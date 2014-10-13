package org.votingsystem.client.dialog;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
import jdk.nashorn.internal.parser.JSONParser;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.vicket.model.VicketTransactionBatch;

import java.io.File;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class JSONFormDialog {

    private static Logger log = Logger.getLogger(JSONFormDialog.class);

    public interface Listener {
        public void processJSONForm(JSONObject jsonForm);
    }

    private final Stage stage;
    private TextArea textArea;
    private Listener listener;
    private Label messageLabel;

    public JSONFormDialog() {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        //stage.initOwner(window);

        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent window) {      }
        });

        VBox mainBox = new VBox(10);
        messageLabel = new Label();
        messageLabel.setWrapText(true);
        textArea = new TextArea();
        textArea.setPrefHeight(300);
        textArea.setPrefWidth(600);
        HBox.setHgrow(textArea, Priority.ALWAYS);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        textArea.setStyle("-fx-word-wrap:break-word;");


        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                if(listener != null) listener.processJSONForm((JSONObject) JSONSerializer.toJSON(textArea.getText()));
                else log.debug("No listeners to send JSON form");
                stage.hide();
            }});
        acceptButton.setGraphic(new ImageView(Utils.getImage(this, "accept")));

        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                stage.hide();
            }});
        cancelButton.setGraphic(new ImageView(Utils.getImage(this, "cancel_16")));

        HBox footerButtonsBox = new HBox(10);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footerButtonsBox.getChildren().addAll(spacer,cancelButton, acceptButton);

        mainBox.getChildren().addAll(textArea, footerButtonsBox);
        mainBox.getStyleClass().add("modal-dialog");
        stage.setScene(new Scene(mainBox, Color.TRANSPARENT));
        stage.getScene().getStylesheets().add(getClass().getResource("/resources/css/modal-dialog.css").toExternalForm());
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

    public void showMessage(String title,JSONObject form, Listener listener) {
        this.listener = listener;
        messageLabel.setText(title);
        textArea.setText(form.toString(3));
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
    }

    class Delta { double x, y; }

}
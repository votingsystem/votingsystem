package org.votingsystem.client.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.votingsystem.client.Browser;
import org.votingsystem.client.util.Utils;
import org.votingsystem.util.ContextVS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class JSONFormDialog extends VBox {

    private static Logger log = Logger.getLogger(JSONFormDialog.class.getSimpleName());

    public interface Listener {
        public void processJSONForm(Map jsonForm);
    }

    private final Stage stage;
    private TextArea textArea;
    private Listener listener;
    private Label messageLabel;
    private static JSONFormDialog dialog;

    public JSONFormDialog() {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(true);
        stage.initOwner(Browser.getInstance().getScene().getWindow());
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
        messageLabel = new Label();
        messageLabel.setWrapText(true);
        textArea = new TextArea();
        textArea.setPrefHeight(300);
        textArea.setPrefWidth(600);
        HBox.setHgrow(textArea, Priority.ALWAYS);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        textArea.setStyle("-fx-word-wrap:break-word;");

        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(actionEvent -> {
            try {
                Map<String, Object> dataMap = new ObjectMapper().readValue(textArea.getText(),
                        new TypeReference<HashMap<String, Object>>() {});
                if(listener != null) listener.processJSONForm(dataMap);
                else log.info("No listeners to send JSON form");
                stage.hide();
            } catch (IOException ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }

        });
        acceptButton.setGraphic(Utils.getIcon(FontAwesomeIconName.CHECK));
        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> stage.hide());
        cancelButton.setGraphic(Utils.getIcon(FontAwesomeIconName.TIMES, Utils.COLOR_RED_DARK));

        HBox footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), cancelButton, acceptButton);

        getChildren().addAll(messageLabel, textArea, footerButtonsBox);
        getStyleClass().add("modal-dialog");
        stage.setScene(new Scene(this));
        stage.getScene().getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        Utils.addMouseDragSupport(stage);
    }

    public void showMessage(String title, Map dataMap, Listener listener) throws JsonProcessingException {
        this.listener = listener;
        messageLabel.setText(title);
        textArea.setText(new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT,true).writeValueAsString(dataMap));
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
    }

    public static void show(Map formData, Listener listener) {
        PlatformImpl.runLater(() -> {
            try {
                if(dialog == null) dialog = new JSONFormDialog();
                dialog.showMessage(ContextVS.getMessage("enterReceptorMsg"), formData, listener);
            } catch (JsonProcessingException ex) {
               log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

}
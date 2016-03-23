package org.votingsystem.client.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.util.Utils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class JSONFormDialog extends DialogVS {

    private static Logger log = Logger.getLogger(JSONFormDialog.class.getName());

    public interface Listener {
        public void processJSONForm(Map jsonForm);
    }

    private TextArea textArea;
    private Listener listener;
    private Label messageLabel;
    private static JSONFormDialog dialog;

    public JSONFormDialog() {
        super(new VBox(10));
        VBox mainDialog = (VBox) getContentPane();
        mainDialog.getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        mainDialog.getStyleClass().add("modal-dialog");
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
                Map<String, Object> dataMap = JSON.getMapper().readValue(textArea.getText(),
                        new TypeReference<HashMap<String, Object>>() {});
                if(listener != null) listener.processJSONForm(dataMap);
                else log.info("No listeners to send JSON form");
                hide();
            } catch (IOException ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }

        });
        acceptButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));
        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> hide());
        cancelButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        HBox footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), cancelButton, acceptButton);
        mainDialog.getChildren().addAll(messageLabel, textArea, footerButtonsBox);
    }

    public void showMessage(String title, Map dataMap, Listener listener) throws JsonProcessingException {
        this.listener = listener;
        messageLabel.setText(title);
        textArea.setText(JSON.getMapper().configure(SerializationFeature.INDENT_OUTPUT, true).writeValueAsString(dataMap));
        show();
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
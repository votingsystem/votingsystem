package org.votingsystem.client.dialog;

import javafx.event.EventHandler;
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
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;

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
        stage.setResizable(true);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent event) {
                log.debug("browserStage.setOnCloseRequest");
            }
        });
        //stage.initOwner(window);
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
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
        acceptButton.setOnAction(actionEvent -> {
            if(listener != null) listener.processJSONForm((JSONObject) JSONSerializer.toJSON(textArea.getText()));
            else log.debug("No listeners to send JSON form");
            stage.hide();
        });
        acceptButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));
        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> stage.hide());
        cancelButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));

        HBox footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), cancelButton, acceptButton);

        mainBox.getChildren().addAll(messageLabel, textArea, footerButtonsBox);
        mainBox.getStyleClass().add("modal-dialog");
        stage.setScene(new Scene(mainBox));
        stage.getScene().getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        Utils.addMouseDragSupport(stage);
    }

    public void showMessage(String title,JSONObject form, Listener listener) {
        this.listener = listener;
        messageLabel.setText(title);
        textArea.setText(form.toString(3));
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
    }

}
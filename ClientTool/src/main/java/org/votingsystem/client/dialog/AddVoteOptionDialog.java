package org.votingsystem.client.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AddVoteOptionDialog extends VBox {

    private static Logger log = Logger.getLogger(AddVoteOptionDialog.class.getSimpleName());

    public interface Listener {
        public void addOption(String optionContent);
    }

    private final Stage stage;
    private TextArea textArea;
    private Listener listener;
    private Label messageLabel;
    private static AddVoteOptionDialog dialog;

    public AddVoteOptionDialog() {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        if(Browser.getInstance() != null) stage.initOwner(Browser.getInstance().getScene().getWindow());
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
        messageLabel = new Label();
        messageLabel.setWrapText(true);
        textArea = new TextArea();
        textArea.setPrefHeight(100);
        textArea.setPrefWidth(400);
        HBox.setHgrow(textArea, Priority.ALWAYS);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        textArea.setStyle("-fx-word-wrap:break-word;");

        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(actionEvent -> {
            listener.addOption(textArea.getText());
            stage.hide();
        });
        acceptButton.setGraphic(Utils.getIcon(FontAwesomeIcons.CHECK));
        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> stage.hide());
        cancelButton.setGraphic(Utils.getIcon(FontAwesomeIcons.TIMES, Utils.COLOR_RED_DARK));

        HBox footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), cancelButton, acceptButton);

        getChildren().addAll(messageLabel, textArea, footerButtonsBox);
        getStyleClass().add("modal-dialog");
        stage.setScene(new Scene(this));
        stage.getScene().getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        Utils.addMouseDragSupport(stage);
    }

    public void showMessage(String title, Listener listener) throws JsonProcessingException {
        this.listener = listener;
        messageLabel.setText(title);
        textArea.setText("");
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
    }

    public static void show(Listener listener) {
        PlatformImpl.runLater(() -> {
            try {
                if(dialog == null) dialog = new AddVoteOptionDialog();
                dialog.showMessage(ContextVS.getMessage("enterVoteOptionMsg"), listener);
            } catch (JsonProcessingException ex) {
               log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

}
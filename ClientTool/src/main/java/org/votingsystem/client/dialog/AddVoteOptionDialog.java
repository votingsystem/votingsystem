package org.votingsystem.client.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AddVoteOptionDialog extends DialogVS {

    private static Logger log = Logger.getLogger(AddVoteOptionDialog.class.getSimpleName());

    public interface Listener {
        public void addOption(String optionContent);
    }

    private VBox mainPane;
    private TextArea textArea;
    private Listener listener;
    private Label messageLabel;
    private static AddVoteOptionDialog dialog;

    public AddVoteOptionDialog() {
        super(new VBox(10));
        mainPane = (VBox) getContentPane();
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
            hide();
        });
        acceptButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));
        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> hide());
        cancelButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));

        HBox footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), cancelButton, acceptButton);

        mainPane.getChildren().addAll(messageLabel, textArea, footerButtonsBox);
        mainPane.getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        mainPane.getStyleClass().add("modal-dialog");
    }

    public void showMessage(String title, Listener listener) throws JsonProcessingException {
        this.listener = listener;
        messageLabel.setText(title);
        textArea.setText("");
        show();
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
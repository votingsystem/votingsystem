package org.votingsystem.client.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.votingsystem.client.Browser;
import org.votingsystem.client.pane.DecoratedPane;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AddTagVSDialog extends DialogVS {

    private static Logger log = Logger.getLogger(AddTagVSDialog.class.getSimpleName());

    public interface Listener {
        public void addTagVS(String tagName);
    }

    private VBox mainPane;
    private TextField textField;
    private Listener listener;
    private Label messageLabel;
    private static AddTagVSDialog dialog;

    public AddTagVSDialog() {
        super(new VBox(10));
        mainPane = (VBox) ((DecoratedPane) getParent()).getContentPane();
        messageLabel = new Label();
        messageLabel.setWrapText(true);
        textField = new TextField();
        HBox.setHgrow(textField, Priority.ALWAYS);
        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(actionEvent -> {
            if ("".equals(textField.getText().trim())) {
                Browser.getInstance().showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("emptyFieldErrorMsg"));
                return;
            }
            listener.addTagVS(textField.getText());
            hide();
        });
        textField.setOnKeyPressed(ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                acceptButton.fire();
            }
        });
        acceptButton.setGraphic(Utils.getIcon(FontAwesomeIcons.CHECK));
        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> hide());
        cancelButton.setGraphic(Utils.getIcon(FontAwesomeIcons.TIMES, Utils.COLOR_RED_DARK));

        HBox footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(Utils.getSpacer(), cancelButton, acceptButton);

        mainPane.getChildren().addAll(messageLabel, textField, footerButtonsBox);
        mainPane.getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        mainPane.getStyleClass().add("modal-dialog");
    }

    public void showMessage(String title, Listener listener) throws JsonProcessingException {
        this.listener = listener;
        messageLabel.setText(title);
        textField.setText("");
        show();
    }

    public static void show(String message, Listener listener) {
        PlatformImpl.runLater(() -> {
            try {
                if(dialog == null) dialog = new AddTagVSDialog();
                dialog.showMessage(message, listener);
            } catch (JsonProcessingException ex) {
               log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

}
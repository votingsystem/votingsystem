package org.votingsystem.client.webextension.dialog;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.util.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RepresentativeAccreditationsDialog extends DialogVS {

    private static Logger log = Logger.getLogger(RepresentativeAccreditationsDialog.class.getSimpleName());

    @FXML private DatePicker datePicker;
    @FXML TextField emailText;
    @FXML private Button acceptButton;
    private static RepresentativeAccreditationsDialog INSTANCE;
    private OperationVS operationVS;

    public RepresentativeAccreditationsDialog(OperationVS operationVS) throws IOException {
        super("/fxml/RepresentativeAccreditations.fxml", ContextVS.getMessage("requestAccreditationsLbl"));
        this.operationVS = operationVS;
    }

    @FXML void initialize() {
        acceptButton.setOnAction(actionEvent -> submitForm());
        acceptButton.setText(ContextVS.getMessage("requestLbl"));
        datePicker.setPromptText(ContextVS.getMessage("selectDateLbl"));
        emailText.setPromptText(ContextVS.getMessage("emailLbl"));
    }

    public void submitForm() {
        if(!StringUtils.validateMail(emailText.getText())) {
            emailText.getStyleClass().add("text-field-error");
            BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterValidEmailMsg"));
            return;
        } else emailText.getStyleClass().add("text-field-ok");
        LocalDate isoDate = datePicker.getValue();
        if(isoDate == null) {
            datePicker.getStyleClass().add("text-field-error");
            BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("selectDateLbl"));
            return;
        }
        Instant instant = isoDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
        Date selectedDate = Date.from(instant);
        if(selectedDate.after(new Date())) {
            datePicker.getStyleClass().add("text-field-error");
            BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("dateAfterCurrentDateErrorMsg"));
            return;
        }
        datePicker.getStyleClass().add("text-field-ok");

        Map mapToSign = operationVS.getDocumentToSign();
        mapToSign.put("selectedDate", selectedDate.getTime());
        mapToSign.put("email", emailText.getText());
        mapToSign.put("UUID", UUID.randomUUID().toString());
        operationVS.setCallerCallback(null);
        operationVS.processOperationWithPassword(ContextVS.getMessage("requestAccreditationsLbl"));
        hide();
    }

    public static void show(OperationVS operationVS) {
        Platform.runLater(() -> {
            try {
                if (INSTANCE == null) INSTANCE = new RepresentativeAccreditationsDialog(operationVS);
                INSTANCE.show();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }
}

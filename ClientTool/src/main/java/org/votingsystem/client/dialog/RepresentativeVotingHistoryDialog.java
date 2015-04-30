package org.votingsystem.client.dialog;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.votingsystem.client.Browser;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
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

import static org.votingsystem.client.Browser.showMessage;


public class RepresentativeVotingHistoryDialog extends DialogVS {

    private static Logger log = Logger.getLogger(RepresentativeAccreditationsDialog.class.getSimpleName());

    @FXML TextField emailText;
    @FXML private DatePicker dateToPicker;
    @FXML private DatePicker dateFromPicker;
    @FXML private Button acceptButton;
    private OperationVS operationVS;
    private static RepresentativeVotingHistoryDialog INSTANCE;

    public RepresentativeVotingHistoryDialog(OperationVS operationVS) throws IOException {
        super("/fxml/RepresentativeVotingHistory.fxml", ContextVS.getMessage("requestVotingHistoryLbl"));
        this.operationVS = operationVS;
    }

    @FXML void initialize() {
        acceptButton.setOnAction(actionEvent -> submitForm());
        acceptButton.setText(ContextVS.getMessage("requestLbl"));
        dateFromPicker.setPromptText(ContextVS.getMessage("dateFromLbl"));
        dateToPicker.setPromptText(ContextVS.getMessage("dateToLbl"));
        emailText.setPromptText(ContextVS.getMessage("emailLbl"));
    }

    private void submitForm(){
        if(!StringUtils.validateMail(emailText.getText())) {
            emailText.getStyleClass().add("text-field-error");
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterValidEmailMsg"));
            return;
        } else emailText.getStyleClass().add("text-field-ok");
        LocalDate isoDate = dateFromPicker.getValue();
        if(isoDate == null) {
            dateFromPicker.getStyleClass().add("text-field-error");
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("selectDateLbl"));
            return;
        } else dateFromPicker.getStyleClass().add("text-field-ok");
        Instant instant = isoDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
        Date dateFrom = Date.from(instant);
        isoDate = dateToPicker.getValue();
        if(isoDate == null) {
            dateToPicker.getStyleClass().add("text-field-error");
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("selectDateLbl"));
            return;
        } else dateToPicker.getStyleClass().add("text-field-ok");
        instant = isoDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
        Date dateTo = Date.from(instant);
        if(dateFrom.after(dateTo)) {
            dateFromPicker.getStyleClass().add("text-field-error");
            dateToPicker.getStyleClass().add("text-field-error");
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("dateRangeErrorMsg", DateUtils.getDateStr(dateFrom),
                    DateUtils.getDateStr(dateTo)));
            return;
        } else {
            dateFromPicker.getStyleClass().add("text-field-ok");
            dateToPicker.getStyleClass().add("text-field-ok");
        }
        Map mapToSign = operationVS.getDocumentToSign();
        mapToSign.put("dateFrom", dateFrom.getTime());
        mapToSign.put("dateTo", dateTo.getTime());
        mapToSign.put("email", emailText.getText());
        mapToSign.put("UUID", UUID.randomUUID().toString());
        operationVS.setCallerCallback(null);
        Browser.getInstance().processOperationVS(operationVS, null);
        hide();
    }

    public static void show(OperationVS operationVS, Window owner) {
        Platform.runLater(() -> {
            try {
                if (INSTANCE == null) INSTANCE = new RepresentativeVotingHistoryDialog(operationVS);
                INSTANCE.show();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

}
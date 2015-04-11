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
import org.votingsystem.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.Browser.showMessage;

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

    }
    public void submitForm() {
        LocalDate isoDate = datePicker.getValue();
        Instant instant = isoDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
        Date selectedDate = Date.from(instant);
        try {
            if(!StringUtils.validateMail(emailText.getText())) {
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterValidEmailMsg"));
                return;
            }
            Map mapToSign = operationVS.getDocumentToSignMap();
            mapToSign.put("selectedDate", selectedDate.getTime());
            mapToSign.put("email", emailText.getText());
            operationVS.setCallerCallback(null);
            Browser.getInstance().processOperationVS(operationVS, null);
            hide();
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static void show(OperationVS operationVS, Window owner) {
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

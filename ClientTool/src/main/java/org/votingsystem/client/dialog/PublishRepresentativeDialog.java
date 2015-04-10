package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.votingsystem.client.Browser;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.StringUtils;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.Browser.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PublishRepresentativeDialog {

    private static Logger log = Logger.getLogger(PublishRepresentativeDialog.class.getSimpleName());

    private static Stage stage;
    private OperationVS operationVS;
    private String representativeImagePath;

    @FXML private TextField caption;
    @FXML private DatePicker datePicker;
    @FXML private Button selectImageButton;
    @FXML private Button publishButton;
    @FXML private HTMLEditor editor;
    @FXML private VBox optionsVBox;

    public PublishRepresentativeDialog(OperationVS operationVS) {
        this.operationVS = operationVS;
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        caption.setPromptText(ContextVS.getMessage("electionSubjectLbl"));
        selectImageButton.setText(ContextVS.getMessage("addVoteOptionLbl"));
        selectImageButton.setOnAction(actionEvent -> selectImage());
        publishButton.setText(ContextVS.getMessage("publishElectionLbl"));
        publishButton.setOnAction(actionEvent -> {
            submitForm();
        });
        publishButton.setVisible(true);
        editor.setHtmlText("<html><body></body></html>");
    }

    private void submitForm(){
        try {
            if("".equals(caption.getText().trim())) {
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterSubjectLbl"));
                return;
            }
            if(datePicker.getValue() == null) {
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterDateLbl"));
                return;
            }
            if(StringUtils.isHTMLEmpty(editor.getHtmlText().trim())) {
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterDataLbl"));
            }
            LocalDate isoDate = datePicker.getValue();
            Instant instant = isoDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            Date dateBegin = Date.from(instant);
            Map mapToSign = new HashMap<>();
            mapToSign.put("subject", caption.getText().trim());
            mapToSign.put("content", editor.getHtmlText());
            mapToSign.put("dateBegin", dateBegin);
            List<Map> optionList = new ArrayList<>();


            mapToSign.put("fieldsEventVS", optionList);
            operationVS.setDocumentToSignMap(mapToSign);
            Browser.getInstance().processOperationVS(operationVS, null);
            stage.hide();
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void selectImage() {
        PlatformImpl.runLater(() -> {
            try {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter(
                        "JPG (*.jpg)", Arrays.asList("*.jpg", "*.JPG"));
                FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter(
                        "PNG (*.png)", Arrays.asList("*.png", "*.PNG"));
                fileChooser.getExtensionFilters().addAll(extFilterJPG, extFilterPNG);
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                File selectedImage = fileChooser.showOpenDialog(null);
                if (selectedImage != null) {
                    byte[] imageFileBytes = FileUtils.getBytesFromFile(selectedImage);
                    log.info(" - imageFileBytes.length: " + imageFileBytes.length);
                    if (imageFileBytes.length > ContextVS.IMAGE_MAX_FILE_SIZE) {
                        log.info(" - MAX_FILE_SIZE exceeded ");
                        showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("fileSizeExceeded", ContextVS.IMAGE_MAX_FILE_SIZE_KB));
                    } else representativeImagePath = selectedImage.getAbsolutePath();
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
        });
    }

    public static void show(OperationVS operationVS, Window owner) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    PublishRepresentativeDialog dialog = new PublishRepresentativeDialog(operationVS);
                    if (stage == null) {
                        stage = new Stage(StageStyle.DECORATED);
                        stage.initOwner(owner);
                        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
                    }
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/RepresentativeEditor.fxml"));
                    fxmlLoader.setController(dialog);
                    stage.setScene(new Scene(fxmlLoader.load()));
                    stage.getScene().setFill(null);
                    Utils.addMouseDragSupport(stage);
                    stage.centerOnScreen();
                    stage.toFront();
                    stage.show();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        });
    }


}
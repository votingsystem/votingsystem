package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.votingsystem.client.Browser;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.dto.RepresentativeDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.*;
    import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.votingsystem.client.Browser.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PublishRepresentativeDialog extends DialogVS {

    private static Logger log = Logger.getLogger(PublishRepresentativeDialog.class.getSimpleName());

    private OperationVS operationVS;
    @FXML private Label adviceLbl;
    @FXML private Button selectImageButton;
    @FXML private Button publishButton;
    @FXML private HTMLEditor editor;
    @FXML private VBox optionsVBox;
    @FXML private ImageView imageView;
    private File selectedImage;
    private static PublishRepresentativeDialog INSTANCE;

    public PublishRepresentativeDialog(String caption, OperationVS operationVS) throws IOException {
        super("/fxml/RepresentativeEditor.fxml", caption);
        this.operationVS = operationVS;
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        selectImageButton.setText(ContextVS.getMessage("selectRepresentativeImgLbl"));
        selectImageButton.setOnAction(actionEvent -> selectImage());
        adviceLbl.setWrapText(true);
        adviceLbl.setText(" - " + ContextVS.getMessage("newRepresentativeAdviceMsg3") + "\n" +
                " - " + ContextVS.getMessage("newRepresentativeAdviceMsg2") + "\n" +
                " - " + ContextVS.getMessage("newRepresentativeAdviceMsg1"));
        publishButton.setOnAction(actionEvent -> {
            submitForm();
        });
        publishButton.setVisible(true);
        editor.setHtmlText("<html><body></body></html>");
    }

    private void loadOperationData() {
        log.info("loadOperationData - type: " + operationVS.getType());
        switch (operationVS.getType()) {
            case NEW_REPRESENTATIVE:
                publishButton.setText(ContextVS.getMessage("publishRepresentativeLbl"));
                setCaption(ContextVS.getMessage("publishRepresentativeLbl"));
                break;
            case EDIT_REPRESENTATIVE:
                publishButton.setText(ContextVS.getMessage("editRepresentativeLbl"));
                setCaption(ContextVS.getMessage("editRepresentativeLbl"));
                ProgressDialog.showDialog(new FetchRepresentativeDataTask(operationVS.getNif()),
                        ContextVS.getMessage("editRepresentativeLbl"), getStage());
                break;
        }
        show();
    }

    private void submitForm(){
        try {
            if(StringUtils.isHTMLEmpty(editor.getHtmlText().trim())) {
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterDataLbl"));
                return;
            }
            if(selectedImage == null) {
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterImageLbl"));
                return;
            }
            Map mapToSign = new HashMap<>();
            mapToSign.put("operation", TypeVS.NEW_REPRESENTATIVE);
            //this is to allow parsing json html fields with javascript
            mapToSign.put("representativeInfo", Base64.getEncoder().encodeToString(editor.getHtmlText().getBytes()));
            operationVS.setDocumentToSignMap(mapToSign);
            operationVS.setFile(selectedImage);
            operationVS.setSignedMessageSubject(ContextVS.getMessage("publishRepresentativeLbl"));
            operationVS.setServiceURL(ContextVS.getInstance().getAccessControl().getRepresentativeServiceURL());
            operationVS.setCallerCallback(null);
            Browser.getInstance().processOperationVS(operationVS, null);
            hide();
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
                selectedImage = fileChooser.showOpenDialog(null);
                if (selectedImage != null) {
                    byte[] imageFileBytes = FileUtils.getBytesFromFile(selectedImage);
                    log.info(" - imageFileBytes.length: " + imageFileBytes.length);
                    if (imageFileBytes.length > ContextVS.IMAGE_MAX_FILE_SIZE) {
                        log.info(" - MAX_FILE_SIZE exceeded ");
                        showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("fileSizeExceeded",
                                ContextVS.IMAGE_MAX_FILE_SIZE_KB));
                        selectedImage = null;
                    }
                }
                imageView.setImage(new Image(selectedImage.toURI().toURL().toString()));
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
        });
    }

    public static void show(OperationVS operationVS, Window owner) {
        Platform.runLater(() -> {
            try {
                String caption = null;
                switch (operationVS.getType()) {
                    case NEW_REPRESENTATIVE:
                        caption = ContextVS.getMessage("publishRepresentativeLbl");
                        break;
                    case EDIT_REPRESENTATIVE:
                        caption = ContextVS.getMessage("editRepresentativeLbl");
                        break;
                }
                if(INSTANCE == null) INSTANCE =  new PublishRepresentativeDialog(caption, operationVS);
                INSTANCE.loadOperationData();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

    public class FetchRepresentativeDataTask extends Task<ResponseVS> {

        private String nif;

        public FetchRepresentativeDataTask(String nif) {
            this.nif = nif;
        }

        @Override protected ResponseVS call() throws Exception {
            updateProgress(1, 10);
            updateMessage(ContextVS.getMessage("editRepresentativeLbl"));
            String serviceURL = ContextVS.getInstance().getAccessControl().getRepresentativeByNifServiceURL(nif);
            updateProgress(3, 10);
            ResponseVS responseVS = HttpHelper.getInstance().getData(serviceURL, ContentTypeVS.JSON);
            updateProgress(8, 10);
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                showMessage(responseVS);
            } else {
                RepresentativeDto representativeDto = (RepresentativeDto) responseVS.getDto(RepresentativeDto.class);
                String description = new String(Base64.getDecoder().decode(representativeDto.getDescription()),
                        StandardCharsets.UTF_8);
                Platform.runLater(() -> editor.setHtmlText(description));
            }
            return responseVS;
        }
    }
}
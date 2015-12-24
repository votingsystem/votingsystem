package org.votingsystem.client.webextension.dialog;

import com.google.common.eventbus.Subscribe;
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
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.service.EventBusService;
import org.votingsystem.client.webextension.util.OperationVS;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeEditorDialog extends DialogVS {

    private static Logger log = Logger.getLogger(RepresentativeEditorDialog.class.getSimpleName());

    private OperationVS operationVS;
    @FXML private Label adviceLbl;
    @FXML private Button selectImageButton;
    @FXML private Button publishButton;
    @FXML private HTMLEditor editor;
    @FXML private VBox optionsVBox;
    @FXML private ImageView imageView;
    private File representativeImage;
    private static RepresentativeEditorDialog INSTANCE;

    static class OperationVSListener {
        @Subscribe public void call(ResponseVS  responseVS) {
            switch(responseVS.getType()) {
                case CANCELED:
                    if(responseVS.getData() instanceof OperationVS) {
                        OperationVS operationVS = (OperationVS) responseVS.getData();
                        if((operationVS.getType() == TypeVS.EDIT_REPRESENTATIVE ||
                                operationVS.getType() == TypeVS.NEW_REPRESENTATIVE) && operationVS != null) {
                            show(operationVS);
                            INSTANCE.refreshImage();
                        }
                    }
                    break;
            }
        }
    }

    public RepresentativeEditorDialog() throws IOException {
        super("/fxml/RepresentativeEditor.fxml");
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

    private void loadOperationData(OperationVS operationVS) {
        log.info("loadOperationData - type: " + operationVS.getType());
        this.operationVS = operationVS;
        editor.setHtmlText("<html><body></body></html>");
        switch (operationVS.getType()) {
            case NEW_REPRESENTATIVE:
                publishButton.setText(ContextVS.getMessage("saveLbl"));
                setCaption(ContextVS.getMessage("publishRepresentativeLbl"));
                break;
            case EDIT_REPRESENTATIVE:
                publishButton.setText(ContextVS.getMessage("saveLbl"));
                setCaption(ContextVS.getMessage("editRepresentativeLbl"));
                ProgressDialog.showDialog(new FetchRepresentativeDataTask(operationVS.getNif()),
                        ContextVS.getMessage("editRepresentativeLbl"));
                break;
        }
        if(operationVS.getJsonStr() != null) {
            try {
                UserVSDto dto = operationVS.getData(UserVSDto.class);
                editor.setHtmlText(new String(Base64.getDecoder().decode(dto.getDescription()), StandardCharsets.UTF_8));
                byte[] imageBytes = Base64.getDecoder().decode(dto.getBase64Image().getBytes());
                FileUtils.copyBytesToFile(imageBytes, representativeImage);
                refreshImage();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        imageView.setImage(null);
        show();
    }

    private void submitForm(){
        try {
            if(representativeImage == null) {
                BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("enterImageLbl"));
                return;
            }
            UserVSDto dto = new UserVSDto();
            dto.setOperation(TypeVS.NEW_REPRESENTATIVE);
            dto.setDescription(Base64.getEncoder().encodeToString(editor.getHtmlText().getBytes()));
            dto.setBase64Image(Base64.getEncoder().encodeToString(FileUtils.getBytesFromFile(representativeImage)));
            dto.setUUID(UUID.randomUUID().toString());
            operationVS.setOperation(TypeVS.NEW_REPRESENTATIVE);
            operationVS.setJsonStr(JSON.getMapper().writeValueAsString(dto));
            operationVS.setSignedMessageSubject(ContextVS.getMessage("publishRepresentativeLbl"));
            operationVS.setServiceURL(ContextVS.getInstance().getAccessControl().getRepresentativeServiceURL());
            operationVS.setCallerCallback(null);
            BrowserHost.getInstance().processOperationWithPassword(operationVS, null);
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
                representativeImage = fileChooser.showOpenDialog(null);
                if (representativeImage != null) {
                    byte[] imageFileBytes = FileUtils.getBytesFromFile(representativeImage);
                    log.info(" - imageFileBytes.length: " + imageFileBytes.length);
                    if (imageFileBytes.length > ContextVS.IMAGE_MAX_FILE_SIZE) {
                        log.info(" - MAX_FILE_SIZE exceeded ");
                        BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("fileSizeExceeded",
                                ContextVS.IMAGE_MAX_FILE_SIZE_KB));
                        representativeImage = null;
                    }
                    refreshImage();
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
        });
    }

    private void refreshImage() {
        if(representativeImage != null) PlatformImpl.runLater(() -> {
            try {
                imageView.setImage(new Image(representativeImage.toURI().toURL().toString()));
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

    public static void show(OperationVS operationVS) {
        Platform.runLater(() -> {
            try {
                if(INSTANCE == null) {
                    INSTANCE =  new RepresentativeEditorDialog();
                    EventBusService.getInstance().register(new OperationVSListener());
                }
                INSTANCE.loadOperationData(operationVS);
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
                BrowserHost.showMessage(responseVS);
            } else {
                UserVSDto representativeDto = (UserVSDto) responseVS.getMessage(UserVSDto.class);
                String description = new String(Base64.getDecoder().decode(representativeDto.getDescription()),
                        StandardCharsets.UTF_8);
                Platform.runLater(() -> editor.setHtmlText(description));
            }
            return responseVS;
        }
    }
}
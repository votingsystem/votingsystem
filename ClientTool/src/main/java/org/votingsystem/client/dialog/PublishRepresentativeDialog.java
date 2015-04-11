package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
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
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
    @FXML private Label requestInfoLbl;
    @FXML private Button selectImageButton;
    @FXML private Button publishButton;
    @FXML private HTMLEditor editor;
    @FXML private VBox optionsVBox;
    @FXML private ImageView imageView;
    private File selectedImage;

    public PublishRepresentativeDialog(OperationVS operationVS) throws IOException {
        super("/fxml/RepresentativeEditor.fxml", ContextVS.getMessage("publishRepresentativeLbl"));
        this.operationVS = operationVS;
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        requestInfoLbl.setText(ContextVS.getMessage("enterRepresentativeDescriptionMsg"));
        selectImageButton.setText(ContextVS.getMessage("selectRepresentativeImgLbl"));
        selectImageButton.setOnAction(actionEvent -> selectImage());
        publishButton.setText(ContextVS.getMessage("publishRepresentativeLbl"));
        publishButton.setOnAction(actionEvent -> {
            submitForm();
        });
        publishButton.setVisible(true);
        editor.setHtmlText("<html><body></body></html>");
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
            mapToSign.put("representativeInfo", editor.getHtmlText());
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
                        showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("fileSizeExceeded", ContextVS.IMAGE_MAX_FILE_SIZE_KB));
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
                PublishRepresentativeDialog dialog = new PublishRepresentativeDialog(operationVS);
                dialog.show();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

}
package org.votingsystem.client.pane;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.javafx.application.PlatformImpl;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.MainApp;
import org.votingsystem.client.util.DocumentVS;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.voting.MetaInf;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.SignedFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DocumentBrowserPane extends VBox {

    private static Logger log = Logger.getLogger(DocumentBrowserPane.class.getName());

    private HBox buttonsHBox;
    private HBox navigateButtonsHBox;
    private Button saveButton;
    private Label navLabel;
    private MetaInf metaInf;
    private int selectedFileIndex;
    private Node mainPane;
    private File selectedFile;
    private List<String> fileList = new ArrayList<>();
    private String caption;

    public DocumentBrowserPane() {
        buttonsHBox = new HBox();
        saveButton = new Button(ContextVS.getMessage("saveLbl"), Utils.getIcon(FontAwesome.Glyph.SAVE));
        saveButton.setOnAction(actionEvent ->  saveMessage());
        navigateButtonsHBox = new HBox(10);
        Button nextButton = new Button(null, Utils.getIcon(FontAwesome.Glyph.CHEVRON_RIGHT));
        nextButton.setOnAction(event -> goNext());
        Button prevButton =  new Button(null, Utils.getIcon(FontAwesome.Glyph.CHEVRON_LEFT));
        prevButton.setOnAction(event -> goPrevious());
        navLabel = new Label();
        navLabel.setStyle("-fx-padding: 0 0 0 30px;-fx-fill: #888; -fx-font-size: 1.4em;");
        navigateButtonsHBox.getChildren().addAll(prevButton, nextButton, navLabel);
        navigateButtonsHBox.setStyle("-fx-padding: 0 0 0 10px;");
        navigateButtonsHBox.setVisible(false);
        HBox.setMargin(saveButton, new Insets(5, 10, 0, 0));
        buttonsHBox.getChildren().addAll(navigateButtonsHBox, Utils.getSpacer(), saveButton);
        getChildren().addAll(buttonsHBox);
        saveButton.setVisible(false);
    }

    public void goNext() {
        if (selectedFileIndex == fileList.size() -1)  selectedFileIndex = 0;
        else ++selectedFileIndex;
        navLabel.setText((selectedFileIndex + 1) + "/" + fileList.size());
        PlatformImpl.runLater(() -> {
            load(new File(fileList.get(selectedFileIndex)));
            navigateButtonsHBox.setVisible(true);
        });

    }

    public void goPrevious() {
        if (selectedFileIndex == 0)  selectedFileIndex = fileList.size() -1;
        else --selectedFileIndex;
        navLabel.setText((selectedFileIndex + 1) + "/" + fileList.size());
        PlatformImpl.runLater(() -> {
            load(new File(fileList.get(selectedFileIndex)));
            navigateButtonsHBox.setVisible(true);
        });
    }

    public void load (File file) {
        navigateButtonsHBox.setVisible(false);
        this.selectedFile = file;
        try {
            int fileIndex = fileList.indexOf(file.getPath());
            log.info("load - file: " + file.getAbsolutePath() + " - fileIndex: " + fileIndex);
            String fileName = file.getName().endsWith("temp") ? "":file.getName();
            SignedFile signedFile = new SignedFile(FileUtils.getBytesFromFile(file), fileName);
            if(mainPane != null) getChildren().remove(mainPane);
            mainPane = new CMSPane(signedFile);
            getChildren().add(mainPane);
            VBox.setVgrow(mainPane, Priority.ALWAYS);
            saveButton.setVisible(true);
            this.caption = ((CMSPane)mainPane).getCaption();
            getScene().getWindow().sizeToScene();
        } catch (Exception ex) {
            MainApp.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("openFileErrorMsg", file.getAbsolutePath()));
            String message =  ex.getMessage();
            if(file != null) message = message + " - file: " + file.getAbsolutePath();
            log.log(Level.SEVERE, message, ex);
        }
    }

    public void processUnzippedFile(ResponseVS response) {
        log.info("processUnzippedFile - statusCode:" + response.getStatusCode());
        if (ResponseVS.SC_OK == response.getStatusCode()) {
            String decompressedBackupBaseDir = (String) response.getData();
            File decompressedBackupDir = new File(decompressedBackupBaseDir);
            log.info("processDecompressedFile - decompressedBackupBaseDir: " + decompressedBackupBaseDir);
            File metaInfFile = new File(decompressedBackupBaseDir + File.separator + "meta.inf");
            if (!metaInfFile.exists()) {
                String message = ContextVS.getMessage("metaInfNotFoundMsg", metaInfFile.getAbsolutePath());
                log.log(Level.SEVERE, message);
                MainApp.showMessage(ResponseVS.SC_ERROR, "Error - " + message);
                return;
            }
            try {
                metaInf = JSON.getMapper().readValue(metaInfFile, MetaInf.class);
                switch (metaInf.getType()) {
                    case TRANSACTION_INFO:
                        fileList = new ArrayList<>();
                        for(File file : decompressedBackupDir.listFiles()) {
                            if(file.getName().toLowerCase().endsWith(ContentType.JSON_SIGNED.getExtension()))
                                fileList.add(file.getAbsolutePath());
                        }
                        if(fileList.size() > 0) {
                            selectedFileIndex = -1;
                            goNext();
                        }
                        break;
                    default:
                        File representativeMetaInfFile = new File(decompressedBackupBaseDir + File.separator +
                                "REPRESENTATIVE_DATA" + File.separator + "meta.inf");
                        Map representativeDataMap = JSON.getMapper().readValue(representativeMetaInfFile,
                                new TypeReference<HashMap<String, Object>>() { });
                        if (representativeMetaInfFile.exists()) metaInf.loadRepresentativeData(representativeDataMap);
                        if(mainPane != null) getChildren().remove(mainPane);
                        mainPane = new EventVSInfoPane(metaInf, decompressedBackupBaseDir);
                        getChildren().add(1, mainPane);
                        if (buttonsHBox.getChildren().contains(saveButton)) buttonsHBox.getChildren().remove(buttonsHBox);
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
            saveButton.setVisible(true);
        }
    }

    public void saveMessage () {
        try {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showSaveDialog(new Stage());
            if(file != null) {
                Object content = getChildren().get(1);
                String fileName = file.getAbsolutePath();
                if(content instanceof DocumentVS) {
                    if(!fileName.contains(".")) fileName = fileName + ((DocumentVS)content).getContentTypeVS().getExtension();
                    FileOutputStream fos = new FileOutputStream(new File(fileName));
                    fos.write(((DocumentVS)content).getDocumentBytes());
                    fos.close();
                } else if(content instanceof EventVSInfoPane) {
                    if(!fileName.contains(".")) fileName = fileName + ContentType.ZIP.getExtension();
                    FileOutputStream fos = new FileOutputStream(new File(fileName));
                    fos.write(FileUtils.getBytesFromFile(selectedFile));
                    fos.close();
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private String checkFileSize (File file) {
        log.info("checkFileSize");
        String result = null;
        if (file.length() > ContextVS.SIGNED_MAX_FILE_SIZE) {
            result = ContextVS.getInstance().getMessage("fileSizeExceededMsg", file.length(),
                    ContextVS.SIGNED_MAX_FILE_SIZE_KB);
        }
        return result;
    }

    private String checkByteArraySize (byte[] signedFileBytes) {
        String result = null;
        if (signedFileBytes.length > ContextVS.SIGNED_MAX_FILE_SIZE) {
            result = ContextVS.getInstance().getMessage("fileSizeExceededMsg",
                    signedFileBytes.length, ContextVS.SIGNED_MAX_FILE_SIZE_KB);
        }
        return result;
    }

    public String getSignatureMessage (Date date) {
        return "<html><b>" + ContextVS.getMessage("signatureDateLbl") + DateUtils.getDateStr(date) + "</html>";
    }

    public String getCaption() {
        if(caption == null) caption = ContextVS.getMessage("backupCaption");
        return caption;
    }

}

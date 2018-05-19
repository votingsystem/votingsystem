package org.votingsystem.client.pane;

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
import org.votingsystem.client.util.DocumentContainer;
import org.votingsystem.client.util.Utils;
import org.votingsystem.crypto.SignedFile;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.MetaInfDto;
import org.votingsystem.http.ContentType;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.Messages;
import org.votingsystem.xml.XML;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private MetaInfDto metaInf;
    private int selectedFileIndex;
    private Node mainPane;
    private File selectedFile;
    private List<String> fileList = new ArrayList<>();
    private String caption;

    public DocumentBrowserPane() {
        buttonsHBox = new HBox();
        saveButton = new Button(Messages.currentInstance().get("saveLbl"), Utils.getIcon(FontAwesome.Glyph.SAVE));
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
            String fileName = file.getName().endsWith("temp") ? "" : file.getName();
            SignedFile signedFile = new SignedFile(FileUtils.getBytesFromFile(file), fileName);
            if(mainPane != null)
                getChildren().remove(mainPane);
            mainPane = new SignedXMLPane(signedFile);
            getChildren().add(mainPane);
            VBox.setVgrow(mainPane, Priority.ALWAYS);
            saveButton.setVisible(true);
            this.caption = ((SignedXMLPane)mainPane).getCaption();
            getScene().getWindow().sizeToScene();
        } catch (Exception ex) {
            MainApp.showMessage(ResponseDto.SC_ERROR, Messages.currentInstance().get("openFileErrorMsg",
                    file.getAbsolutePath()));
            String message =  ex.getMessage();
            if(file != null)
                message = message + " - file: " + file.getAbsolutePath();
            log.log(Level.SEVERE, message, ex);
        }
    }

    public void processUnzippedFile(ResponseDto response) {
        log.info("processUnzippedFile - statusCode:" + response.getStatusCode());
        if (ResponseDto.SC_OK == response.getStatusCode()) {
            String decompressedBackupBaseDir = (String) response.getData();
            File decompressedBackupDir = new File(decompressedBackupBaseDir);
            log.info("processDecompressedFile - decompressedBackupBaseDir: " + decompressedBackupBaseDir);
            File metaInfFile = new File(decompressedBackupBaseDir + File.separator + "meta.inf");
            if (!metaInfFile.exists()) {
                String message = Messages.currentInstance().get("metaInfNotFoundMsg", metaInfFile.getAbsolutePath());
                log.log(Level.SEVERE, message);
                MainApp.showMessage(ResponseDto.SC_ERROR, "Error - " + message);
                return;
            }
            try {
                metaInf = new XML().getMapper().readValue(metaInfFile, MetaInfDto.class);
                fileList = new ArrayList<>();
                for(File file : decompressedBackupDir.listFiles()) {
                    if(file.getName().toLowerCase().endsWith(ContentType.XML.getExtension()))
                        fileList.add(file.getAbsolutePath());
                }
                if(fileList.size() > 0) {
                    selectedFileIndex = -1;
                    goNext();
                }
                log.info("processDecompressedFile - decompressedBackupBaseDir: " + metaInf.getOperation());
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
                if(content instanceof DocumentContainer) {
                    if(!fileName.contains(".")) fileName = fileName + ((DocumentContainer)content).getContentType().getExtension();
                    FileOutputStream fos = new FileOutputStream(new File(fileName));
                    fos.write(((DocumentContainer)content).getDocumentBytes());
                    fos.close();
                } else if(content instanceof ElectionInfoPane) {
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
        if (file.length() > Constants.SIGNED_MAX_FILE_SIZE) {
            result = Messages.currentInstance().get("fileSizeExceededMsg", file.length(),
                    Constants.SIGNED_MAX_FILE_SIZE_KB);
        }
        return result;
    }

    private String checkByteArraySize (byte[] signedFileBytes) {
        String result = null;
        if (signedFileBytes.length > Constants.SIGNED_MAX_FILE_SIZE) {
            result = Messages.currentInstance().get("fileSizeExceededMsg",
                    signedFileBytes.length, Constants.SIGNED_MAX_FILE_SIZE_KB);
        }
        return result;
    }

    public String getSignatureMessage (LocalDateTime date) {
        return "<html><b>" + Messages.currentInstance().get("signatureDateLbl") + DateUtils.getDateStr(date) + "</html>";
    }

    public String getCaption() {
        if(caption == null) caption = Messages.currentInstance().get("backupCaption");
        return caption;
    }

}

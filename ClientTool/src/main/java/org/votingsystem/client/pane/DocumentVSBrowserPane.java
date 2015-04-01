package org.votingsystem.client.pane;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.dialog.CurrencyDialog;
import org.votingsystem.client.model.MetaInf;
import org.votingsystem.client.util.DocumentVS;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.util.SignedFile;
import org.votingsystem.util.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DocumentVSBrowserPane extends VBox implements DecompressBackupPane.Listener {

    private static Logger log = Logger.getLogger(DocumentVSBrowserPane.class.getSimpleName());

    private HBox buttonsHBox;
    private Button saveButton;
    private MetaInf metaInf;
    private int selectedFileIndex;
    private File backup;
    private List<String> fileList = new ArrayList<String>();
    private String caption;

    public DocumentVSBrowserPane(final String signedDocumentStr, File fileParam, Map operationDocument) {
        this.backup = fileParam;
        buttonsHBox = new HBox();
        saveButton = new Button(ContextVS.getMessage("saveLbl"));
        saveButton.setGraphic((Utils.getIcon(FontAwesomeIconName.SAVE)));
        saveButton.setOnAction(actionEvent ->  saveMessage());
        HBox.setMargin(saveButton, new Insets(5, 10, 0, 0));
        buttonsHBox.getChildren().addAll(Utils.getSpacer(), saveButton);
        getChildren().addAll(buttonsHBox);
        saveButton.setVisible(false);
        if(backup == null) {
            if(signedDocumentStr == null) {
                FileChooser fileChooser = new FileChooser();
                    /*FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                            ContextVS.getMessage("signedFileFileFilterMsg"), "*" + ContentTypeVS.SIGNED.getExtension());
                    fileChooser.getExtensionFilters().add(extFilter);*/
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                //fileChooser.setInitialFileName(ContextVS.getMessage("genericReceiptFileName"));
                backup = fileChooser.showOpenDialog(new Stage());
            } else backup = FileUtils.getFileFromString(signedDocumentStr);
        }
        if(backup != null) {
            try {
                if(FileUtils.isZipFile(backup)){
                    DecompressBackupPane.showDialog(DocumentVSBrowserPane.this, backup);
                    return;
                }
                if(backup.getName().endsWith(ContentTypeVS.CURRENCY.getExtension())) {
                    CurrencyDialog.show((Currency) ObjectUtils.deSerializeObject(FileUtils.getBytesFromFile(backup)),
                            BrowserVS.getInstance().getScene().getWindow());
                } else {
                    openFile(backup, operationDocument);
                }
            } catch (IOException ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        } else log.info("backup null");
    }

    public void goNext() {
        if (selectedFileIndex == fileList.size() -1)  selectedFileIndex = 0;
        else ++selectedFileIndex;
        showSignedFile(fileList.get(selectedFileIndex));
    }

    public void goPrevious() {
        if (selectedFileIndex == 0)  selectedFileIndex = fileList.size() -1;
        else --selectedFileIndex;
        showSignedFile(fileList.get(selectedFileIndex));
    }

    private void showSignedFile(String signedFile) {
        log.info("TODO - showSignedFile: " + signedFile);
    }

    private void openFile (File file, Map operationDocument) {
        try {
            int fileIndex = fileList.indexOf(file.getPath());
            log.info("openFile - file: " + file.getAbsolutePath() + " - fileIndex: " + fileIndex);
            fileList.add(file.getPath());
            String fileName = file.getName().endsWith("temp") ? "":file.getName();
            SignedFile signedFile = new SignedFile(FileUtils.getBytesFromFile(file), fileName, operationDocument);
            SMIMEPane SMIMEPane = new SMIMEPane(signedFile);
            getChildren().add(1, SMIMEPane);
            VBox.setVgrow(SMIMEPane, Priority.ALWAYS);
            saveButton.setVisible(true);
            this.caption = SMIMEPane.getCaption();
        } catch (Exception ex) {
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("openFileErrorMsg", file.getAbsolutePath()));
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override public void processDecompressedFile(ResponseVS response) {
        log.info("processDecompressedFile - statusCode:" + response.getStatusCode());
        PlatformImpl.runLater(() -> {
            if (ResponseVS.SC_OK == response.getStatusCode()) {
                String decompressedBackupBaseDir = (String) response.getData();
                log.info("processDecompressedFile - decompressedBackupBaseDir: " + decompressedBackupBaseDir);
                File metaInfFile = new File(decompressedBackupBaseDir + File.separator + "meta.inf");
                File representativeMetaInfFile = new File(decompressedBackupBaseDir + File.separator + "REPRESENTATIVE_DATA" +
                        File.separator + "meta.inf");
                if (!metaInfFile.exists()) {
                    String message = ContextVS.getMessage("metaInfNotFoundMsg", metaInfFile.getAbsolutePath());
                    log.log(Level.SEVERE, message);
                    showMessage(ResponseVS.SC_ERROR, "Error - " + message);
                    return;
                }
                try {
                    metaInf = MetaInf.parse(new ObjectMapper().readValue(metaInfFile,
                            new TypeReference<HashMap<String, Object>>() {}));
                    Map representativeDataMap = new ObjectMapper().readValue(representativeMetaInfFile,
                            new TypeReference<HashMap<String, Object>>() {});
                    if (representativeMetaInfFile.exists()) metaInf.loadRepresentativeData(representativeDataMap);
                    EventVSInfoPane eventPanel = new EventVSInfoPane(metaInf, decompressedBackupBaseDir);
                    getChildren().add(1, eventPanel);
                    if (buttonsHBox.getChildren().contains(saveButton)) buttonsHBox.getChildren().remove(buttonsHBox);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
                saveButton.setVisible(true);
            }
        });
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
                    if(!fileName.contains(".")) fileName = fileName + ContentTypeVS.ZIP.getExtension();
                    FileOutputStream fos = new FileOutputStream(new File(fileName));
                    fos.write(FileUtils.getBytesFromFile(backup));
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

    public File getBackup() {
        return backup;
    }

    public void setBackup(File backup) {
        this.backup = backup;
    }

    private class CMSFilter implements java.io.FileFilter {
        @Override public boolean accept(File file) {
            return (checkFileSize(file) == null);
        }
    }

    public String getSignatureMessage (Date date) {
        return "<html><b>" + ContextVS.getMessage("signatureDateLbl") + DateUtils.getDateStr(date) + "</html>";
    }

    public String getCaption() {
        if(caption == null) caption = ContextVS.getMessage("backupCaption");
        return caption;
    }

}

package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.dialog.CooinDialog;
import org.votingsystem.client.model.MetaInf;
import org.votingsystem.client.util.DocumentVS;
import org.votingsystem.client.util.Utils;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.SignedFile;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.ObjectUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.votingsystem.client.VotingSystemApp.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DocumentVSBrowserStackPane extends StackPane implements DecompressBackupPane.Listener {

    private static Logger log = Logger.getLogger(DocumentVSBrowserStackPane.class);

    private TabPane tabPane;
    private static Stage dialogStage;
    private HBox buttonsHBox;
    private Button saveButton;
    private String dialogTitle = null;
    private MetaInf metaInf;
    private int selectedFileIndex;
    private Text progressMessageText;
    private VBox mainVBox;
    private VBox progressBox;
    private File backup;
    private List<String> fileList = new ArrayList<String>();

    public DocumentVSBrowserStackPane() {
        progressBox = new VBox();
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPrefWidth(400);
        progressBox.setPrefHeight(300);
        progressMessageText = new Text();
        progressMessageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #f9f9f9;");
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setLayoutY(10);
        progressBox.getChildren().addAll(progressMessageText, progressBar);
        setProgressVisible(false, null);
        getChildren().addAll(progressBox);
        mainVBox = new VBox();
        mainVBox.setPrefWidth(800);
        buttonsHBox = new HBox();
        HBox navigateButtonsHBox = new HBox();
        Button nextButton = new Button(ContextVS.getMessage("buttonNextLbl"));
        nextButton.setOnAction(actionEvent ->  goNext());
        nextButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHEVRON_RIGHT));
        Button prevButton =  new Button(ContextVS.getMessage("buttonPreviousLbl"));
        prevButton.setOnAction(actionEvent ->  goPrevious());
        prevButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHEVRON_LEFT));
        navigateButtonsHBox.getChildren().addAll(prevButton, nextButton);
        saveButton = new Button(ContextVS.getMessage("saveLbl"));
        saveButton.setGraphic((Utils.getImage(FontAwesome.Glyph.SAVE)));
        saveButton.setOnAction(actionEvent ->  saveMessage());
        HBox.setMargin(saveButton, new Insets(5, 10, 5, 0));
        navigateButtonsHBox.setVisible(false);
        buttonsHBox.getChildren().addAll(navigateButtonsHBox, Utils.getSpacer(), saveButton);
        tabPane = new TabPane();
        tabPane.setRotateGraphic(false);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        tabPane.setSide(Side.TOP);
        HBox.setHgrow(tabPane, Priority.ALWAYS);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        mainVBox.getChildren().addAll(buttonsHBox, tabPane);
        getChildren().add(0, mainVBox);
    }

    public void init() {
        getScene().getStylesheets().add(Utils.getResource("/css/cooin-pane.css"));
    }

    private void goNext() {
        if (selectedFileIndex == fileList.size() -1)  selectedFileIndex = 0;
        else ++selectedFileIndex;
        showSignedFile(fileList.get(selectedFileIndex));
    }

    private void goPrevious() {
        if (selectedFileIndex == 0)  selectedFileIndex = fileList.size() -1;
        else --selectedFileIndex;
        showSignedFile(fileList.get(selectedFileIndex));
    }

    private void showSignedFile(String signedFile) {
        log.debug("TODO - showSignedFile: " + signedFile);
    }

    private void setProgressVisible(boolean isVisible, String message) {
        progressBox.setVisible(isVisible);
        if(message != null) progressMessageText.setText(message);
        else progressMessageText.setText("");
    }

    private void openFile (File file, Map operationDocument) {
        log.debug("openFile - file: " + file.getAbsolutePath());
        try {
            int fileIndex = fileList.indexOf(file.getPath());
            if (fileIndex != -1 && !tabPane.getSelectionModel().isEmpty()) {
                tabPane.getSelectionModel().select(fileIndex);
                return;
            }
            fileList.add(file.getPath());
            String fileName = file.getName().endsWith("temp") ? "":file.getName();
            SignedFile signedFile = new SignedFile(FileUtils.getBytesFromFile(file), fileName, operationDocument);
            Tab newTab = new Tab();
            newTab.setText(ContextVS.getMessage("signedDocumentLbl"));
            SMIMEPane SMIMEPane = new SMIMEPane(signedFile);
            newTab.setContent(SMIMEPane);
            tabPane.getTabs().add(newTab);
            tabPane.getSelectionModel().select(newTab);
        } catch (Exception ex) {
            showMessage(null, ContextVS.getMessage("openFileErrorMsg", file.getAbsolutePath()));
            getScene().getWindow().hide();
            log.error(ex.getMessage(), ex);
        }
    }

    public static void showDialog(final String signedDocumentStr, File fileParam, Map operationDocument) {
        Platform.runLater(() -> {
            DocumentVSBrowserStackPane documentVSBrowserStackPane = new DocumentVSBrowserStackPane();
            dialogStage = new Stage();
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(documentVSBrowserStackPane));
            dialogStage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
            documentVSBrowserStackPane.init();
            dialogStage.setTitle(ContextVS.getMessage("signedDocumentBrowserCaption"));
            dialogStage.setResizable(true);
            File file = fileParam;
            if(file == null) {
                if(signedDocumentStr == null) {
                    FileChooser fileChooser = new FileChooser();
                    /*FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                            ContextVS.getMessage("signedFileFileFilterMsg"), "*" + ContentTypeVS.SIGNED.getExtension());
                    fileChooser.getExtensionFilters().add(extFilter);*/
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                    //fileChooser.setInitialFileName(ContextVS.getMessage("genericReceiptFileName"));
                    file = fileChooser.showOpenDialog(dialogStage);
                } else file = FileUtils.getFileFromString(signedDocumentStr);
            }
            if(file != null){
                try {
                    if(FileUtils.isZipFile(file)){
                        DecompressBackupPane.showDialog(documentVSBrowserStackPane, file);
                        documentVSBrowserStackPane.setBackup(file);
                        return;
                    }
                    if(file.getName().endsWith(ContentTypeVS.COOIN.getExtension())) {
                        CooinDialog.show((Cooin) ObjectUtils.deSerializeObject(FileUtils.getBytesFromFile(file)));
                    } else {
                        documentVSBrowserStackPane.openFile(file, operationDocument);
                        dialogStage.show();
                    }
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            } else log.debug("File null dialog will not be opened");
        });
    }
    @Override public void processDecompressedFile(ResponseVS response) {
        log.debug("processDecompressedFile - statusCode:" + response.getStatusCode());
        if(ResponseVS.SC_OK == response.getStatusCode()) {
            Platform.runLater(() -> {
                setVisible((String) response.getData());
            });
        }
    }

    public void saveMessage () {
        try {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showSaveDialog(getScene().getWindow());
            Object tabContent = tabPane.getSelectionModel().getSelectedItem().getContent();
            if(file != null) {
                String fileName = file.getAbsolutePath();
                if(tabContent instanceof DocumentVS) {
                    if(!fileName.contains(".")) fileName = fileName + ((DocumentVS)tabContent).getContentTypeVS().getExtension();
                    FileOutputStream fos = new FileOutputStream(new File(fileName));
                    fos.write(((DocumentVS)tabContent).getDocumentBytes());
                    fos.close();
                } else if(tabContent instanceof EventVSInfoPane) {
                    if(!fileName.contains(".")) fileName = fileName + ContentTypeVS.ZIP.getExtension();
                    FileOutputStream fos = new FileOutputStream(new File(fileName));
                    fos.write(FileUtils.getBytesFromFile(backup));
                    fos.close();
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private String checkFileSize (File file) {
        log.debug("checkFileSize");
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

    public void setVisible(String decompressedBackupBaseDir) {
        log.debug("setVisible - decompressedBackupBaseDir: " + decompressedBackupBaseDir);
        File metaInfFile = new File(decompressedBackupBaseDir + File.separator + "meta.inf");
        File representativeMetaInfFile = new File(decompressedBackupBaseDir + File.separator + "REPRESENTATIVE_DATA"  +
                File.separator + "meta.inf");
        if(!metaInfFile.exists()) {
            String message = ContextVS.getMessage("metaInfNotFoundMsg", metaInfFile.getAbsolutePath());
            log.error(message);
            showMessage(ResponseVS.SC_ERROR, "Error - " + message);
            return;
        }
        try {
            metaInf = MetaInf.parse((JSONObject) JSONSerializer.toJSON(FileUtils.getStringFromFile(metaInfFile)));
            if(representativeMetaInfFile.exists()) metaInf.loadRepresentativeData((JSONObject)
                    JSONSerializer.toJSON(FileUtils.getStringFromFile(representativeMetaInfFile)));
            EventVSInfoPane eventPanel = new EventVSInfoPane(metaInf, decompressedBackupBaseDir);
            tabPane.getTabs().removeAll(tabPane.getTabs());
            dialogTitle = null;
            switch(metaInf.getType()) {
                case CLAIM_EVENT:
                    dialogTitle = ContextVS.getMessage("claimEventTabTitle");
                    break;
                case MANIFEST_EVENT:
                    dialogTitle = ContextVS.getMessage("manifestEventTabTitle");
                    break;
                case VOTING_EVENT:
                    dialogTitle = ContextVS.getMessage("electionEventTabTitle");
                    break;
            }
            if(TypeVS.VOTING_EVENT == metaInf.getType() || TypeVS.MANIFEST_EVENT == metaInf.getType() ||
                    TypeVS.CLAIM_EVENT == metaInf.getType()) {
            }
            tabPane.getTabs().add(Utils.getTab(dialogTitle, eventPanel));
            if(buttonsHBox.getChildren().contains(saveButton)) buttonsHBox.getChildren().remove(buttonsHBox);
            dialogStage.show();
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}

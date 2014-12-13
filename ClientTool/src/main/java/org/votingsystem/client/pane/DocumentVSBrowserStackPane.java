package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.dialog.CooinDialog;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.model.MetaInf;
import org.votingsystem.client.model.SignedFile;
import org.votingsystem.client.util.DocumentVS;
import org.votingsystem.client.util.Utils;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
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
public class DocumentVSBrowserStackPane extends StackPane {

    private static Logger log = Logger.getLogger(DocumentVSBrowserStackPane.class);

    private TabPane tabPane;
    private String fileDir = null;
    private String dialogTitle = null;
    private String decompressedBackupBaseDir = null;
    private MessageDialog messageDialog = null;
    private MetaInf metaInf;
    private Button validateBackupButton;
    private int selectedFileIndex;
    private Text progressMessageText;
    private VBox progressBox;
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

        VBox mainVBox = new VBox();
        mainVBox.setPrefWidth(560);
        HBox buttonsHBox = new HBox();
        HBox navigateButtonsHBox = new HBox();

        Button nextButton = new Button(ContextVS.getMessage("buttonNextLbl"));
        nextButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                goNext();
            }
        });

        nextButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHEVRON_RIGHT));
        Button prevButton =  new Button(ContextVS.getMessage("buttonPreviousLbl"));
        prevButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                goPrevious();
            }
        });
        prevButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHEVRON_LEFT));
        navigateButtonsHBox.getChildren().addAll(prevButton, nextButton);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        validateBackupButton = new Button(ContextVS.getMessage("validateBackupLbl"));
        validateBackupButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                BackupValidatorPane.showDialog(decompressedBackupBaseDir, metaInf);
            }
        });

        validateBackupButton.setGraphic(Utils.getImage(FontAwesome.Glyph.FILE_TEXT_ALT));

        Button saveButton = new Button(ContextVS.getMessage("saveLbl"));
        saveButton.setGraphic((Utils.getImage(FontAwesome.Glyph.SAVE)));
        saveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                saveMessage();
            }
        });
        HBox.setMargin(saveButton, new Insets(5, 10, 5, 0));
        HBox.setMargin(validateBackupButton, new Insets(0, 10, 0, 0));

        navigateButtonsHBox.setVisible(false);
        buttonsHBox.getChildren().addAll(navigateButtonsHBox, spacer, saveButton);

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
        getScene().getStylesheets().add(((Object)this).getClass().getResource("/css/cooin-pane.css").toExternalForm());
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
        fileDir = file.getParent();
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

    public static void showDialog(final String signedDocumentStr, Map operationDocument) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                DocumentVSBrowserStackPane documentVSBrowserStackPane = new DocumentVSBrowserStackPane();
                Stage stage = new Stage();
                stage.setScene(new Scene(documentVSBrowserStackPane));
                documentVSBrowserStackPane.init();
                stage.initModality(Modality.WINDOW_MODAL);
                stage.setTitle(ContextVS.getMessage("signedDocumentBrowserCaption"));
                stage.setResizable(true);
                File file = null;
                if(signedDocumentStr == null) {
                    FileChooser fileChooser = new FileChooser();
                    /*FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                            ContextVS.getMessage("signedFileFileFilterMsg"), "*" + ContentTypeVS.SIGNED.getExtension());
                    fileChooser.getExtensionFilters().add(extFilter);*/
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                    //fileChooser.setInitialFileName(ContextVS.getMessage("genericReceiptFileName"));
                    file = fileChooser.showOpenDialog(stage);
                } else file = FileUtils.getFileFromString(signedDocumentStr);
                if(file != null){
                    try {
                        if(FileUtils.isZipFile(file)){
                            DecompressBackupPane.showDialog(VotingSystemApp.getInstance(), file);
                            return;
                        }
                        if(file.getName().endsWith(ContentTypeVS.COOIN.getExtension())) {
                            CooinDialog.show((Cooin) ObjectUtils.deSerializeObject(FileUtils.getBytesFromFile(file)));
                        } else {
                            documentVSBrowserStackPane.openFile(file, operationDocument);
                            stage.centerOnScreen();
                            stage.show();
                        }
                    } catch (IOException ex) {
                        log.error(ex.getMessage(), ex);
                    }
                } else log.debug("File null dialog will not be opened");
            }
        });
    }


    public void saveMessage () {
        try {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showSaveDialog(getScene().getWindow());
            DocumentVS selectedDocumentVS = ((DocumentVS)tabPane.getSelectionModel().getSelectedItem().getContent());
            if(file != null) {
                String fileName = file.getAbsolutePath();
                if(!fileName.contains(".")) fileName = fileName + selectedDocumentVS.getContentTypeVS().getExtension();
                FileOutputStream fos = new FileOutputStream(new File(fileName));
                fos.write(selectedDocumentVS.getDocumentBytes());
                fos.close();
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
        this.decompressedBackupBaseDir = decompressedBackupBaseDir;
        File metaInfFile = new File(decompressedBackupBaseDir + File.separator + "meta.inf");
        if(!metaInfFile.exists()) {
            String message = ContextVS.getMessage("metaInfNotFoundMsg", metaInfFile.getAbsolutePath());
            log.error(message);
            showMessage(null, "Error - " + message);
            return;
        }
        try {
            metaInf = MetaInf.parse(FileUtils.getStringFromFile(metaInfFile));
            EventVSInfoPane eventPanel = new EventVSInfoPane(metaInf);
            tabPane.getTabs().removeAll(tabPane.getTabs());
            String dialogTitle = null;
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
                validateBackupButton.setVisible(true);
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        /*try {
            ZipFile backupZip = new ZipFile(zip);
            new File(ClientToolContext.DEFAULTS.APPTEMPDIR + File.separator +
                    zip.getName()).mkdirs();
            setTitle(zip.getName());
            Enumeration entries = backupZip.entries();
            while(entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)entries.nextElement();
                if(entry.isDirectory()) {
                    //archivoDestino.mkdirs();
                    //continue;
                } else {
                    byte[] signedFileBytes = FileUtils.getBytesFromInputStream(
                        backupZip.getInputStream(entry));
                    String msg = checkByteArraySize(signedFileBytes);
                    if(msg == null) {
                        if ("meta.inf".equals(entry.getName())) {
                            byte[] metaInfBytes = FileUtils.getBytesFromInputStream(
                                    backupZip.getInputStream(entry));
                            InformacionEventoPanel informacionEventoPanel =
                                new InformacionEventoPanel(metaInfBytes);
                            tabbedPane.removeAll();
                            tabbedPane.addTab("<html><b>" + entry.getName() +
                                    "</b>&nbsp;&nbsp;&nbsp;&nbsp;</html>",
                                    informacionEventoPanel);
                        } else {
                            SignedFile signedFile = new SignedFile(
                                    signedFileBytes, entry.getName());
                            signedFileList.add(signedFile);

                            showProgress(false);
                        }
                    } else {
                        log.error("ERROR ZipEntry '" + entry.getName() + "'  -> " + msg);
                    }
                }
            }
            backupZip.close();
            log.debug("Numero archivos en lista: " + signedFileList.size());
            selectedFileIndex = 0;
            SignedFile primerArchivo = signedFileList.get(selectedFileIndex++);
            SignedFilePanel signedFilePanel =
                    new SignedFilePanel(primerArchivo);
            signedFilePanel.setContentFormated(checkBox.isSelected());
            tabbedPane.addTab(primerArchivo.getName(), signedFilePanel);
            tabbedPane.setSelectedIndex(0);
            tabbedPane.setVisible(true);
            navegacionPanel.setVisible(true);
        } catch (ZipException ex) {/*
            File fileDir = archivo.getParentFile();
            signedFileListCMS = Arrays.asList(fileDir.listFiles(new CMSFilter()));
            selectedFileIndex = signedFileListCMS.indexOf(archivo);
            if (signedFileListCMS.size() < 2) habilitarBotones(false);
            else habilitarBotones(true);
            contexto = ClientToolContext.EXPLORANDO_DIRECTORIO;
            openFile(archivo);
            showProgress(false);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            showProgress(false);
        } */
    }

}

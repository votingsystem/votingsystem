package org.votingsystem.client.util;

import com.sun.javafx.application.PlatformImpl;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.model.MetaInf;
import org.votingsystem.client.model.SignedFile;
import org.votingsystem.client.pane.BackupValidatorPane;
import org.votingsystem.client.pane.EventVSInfoPane;
import org.votingsystem.client.pane.SignedFilePane;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SignedDocumentsBrowser extends StackPane{

    private static Logger logger = Logger.getLogger(SignedDocumentsBrowser.class);

    private TabPane tabPane;
    private String fileDir = null;
    private String dialogTitle = null;
    private String decompressedBackupBaseDir = null;
    private MessageDialog messageDialog = null;
    private MetaInf metaInf;
    private Button validateBackupButton;
    private String mensajeMime;
    private int selectedFileIndex;
    private Text progressMessageText;
    private VBox progressBox;
    private List<String> fileList = new ArrayList<String>();

    public SignedDocumentsBrowser() {
        Region progressRegion = new Region();
        progressRegion.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4)");
        progressRegion.setPrefSize(240, 160);

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
        HBox buttonsHBox = new HBox();
        HBox navigateButtonsHBox = new HBox();

        Button nextButton = new Button(ContextVS.getMessage("buttonNextLbl"));
        nextButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                goNext();
            }
        });

        nextButton.setGraphic(new ImageView(Utils.getImage(this, "fa-chevron-right")));
        Button prevButton =  new Button(ContextVS.getMessage("buttonPreviousLbl"));
        prevButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                goPrevious();
            }
        });
        prevButton.setGraphic(new ImageView(Utils.getImage(this, "fa-chevron-left")));
        navigateButtonsHBox.getChildren().addAll(prevButton, nextButton);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        validateBackupButton = new Button(ContextVS.getMessage("validateBackupLbl"));
        validateBackupButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                BackupValidatorPane.showDialog(decompressedBackupBaseDir, metaInf);
            }
        });
        validateBackupButton.setGraphic(new ImageView(Utils.getImage(this, "document-properties")));

        Button saveButton = new Button(ContextVS.getMessage("saveLbl"));
        saveButton.setGraphic((new ImageView(Utils.getImage(this, "save_data"))));
        saveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                saveMessage();
            }
        });
        HBox.setMargin(saveButton, new Insets(0, 10, 0, 0));
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
        logger.debug("TODO - showSignedFile: " + signedFile);
    }

    private void setProgressVisible(boolean isVisible, String message) {
        progressBox.setVisible(isVisible);
        if(message != null) progressMessageText.setText(message);
        else progressMessageText.setText("");
    }

    private void openFile (File file) {
        logger.debug("openFile - file: " + file.getAbsolutePath());
        fileDir = file.getParent();
        try {
            int fileIndex = fileList.indexOf(file.getPath());
            if (fileIndex != -1 && !tabPane.getSelectionModel().isEmpty()) {
                tabPane.getSelectionModel().select(fileIndex);
                return;
            }
            fileList.add(file.getPath());
            byte[] fileBytes = FileUtils.getBytesFromFile(file);
            SignedFile signedFile = new SignedFile(fileBytes, file.getName());
            Tab newTab = new Tab();
            newTab.setText(file.getName());
            SignedFilePane signedFilePane = new SignedFilePane(signedFile);
            newTab.setContent(signedFilePane);
            tabPane.getTabs().add(newTab);
            tabPane.getSelectionModel().select(newTab);
        } catch (Exception ex) {
            showMessage(ContextVS.getMessage("openFileErrorMsg", file.getAbsolutePath()));
            logger.error(ex.getMessage(), ex);
        }
    }

    public void showMessage(final String message) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                if (messageDialog == null) messageDialog = new MessageDialog();
                messageDialog.showMessage(message);
            }
        });
    }

    public static void showDialog() {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                SignedDocumentsBrowser signedDocumentsBrowser = new SignedDocumentsBrowser();
                Stage primaryStage = new Stage();
                primaryStage.setScene(new Scene(signedDocumentsBrowser));
                primaryStage.initModality(Modality.WINDOW_MODAL);
                primaryStage.setTitle(ContextVS.getMessage("signedDocumentBrowserCaption"));
                primaryStage.setResizable(true);
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                        ContextVS.getMessage("signedFileFileFilterMsg"), "*" + ContentTypeVS.SIGNED.getExtension());
                fileChooser.getExtensionFilters().add(extFilter);
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                //fileChooser.setInitialFileName(ContextVS.getMessage("genericReceiptFileName"));
                File file = fileChooser.showOpenDialog(primaryStage);
                if(file != null){
                    signedDocumentsBrowser.openFile(file);
                    primaryStage.centerOnScreen();
                    primaryStage.show();
                }
            }
        });
    }

    public void saveMessage () {
        try {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showSaveDialog(getScene().getWindow());
            String fileName = file.getAbsolutePath() + ContentTypeVS.SIGNED.getExtension();
            FileOutputStream fos = new FileOutputStream(new File(fileName));
            fos.write(mensajeMime.getBytes());
            fos.close();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private String checkFileSize (File file) {
        logger.debug("checkFileSize");
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
        return "<html><b>" + ContextVS.getMessage("signatureDateLbl") +
                DateUtils.getLongDate_Es(date) + "</html>";
    }

    public void setVisible(String decompressedBackupBaseDir) {
        logger.debug("setVisible - decompressedBackupBaseDir: " + decompressedBackupBaseDir);
        this.decompressedBackupBaseDir = decompressedBackupBaseDir;
        File metaInfFile = new File(decompressedBackupBaseDir + File.separator + "meta.inf");
        if(!metaInfFile.exists()) {
            String message = ContextVS.getMessage("metaInfNotFoundMsg", metaInfFile.getAbsolutePath());
            logger.error(message);
            showMessage("Error - " + message);
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
            logger.error(ex.getMessage(), ex);
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
                        logger.error("ERROR ZipEntry '" + entry.getName() + "'  -> " + msg);
                    }
                }
            }
            backupZip.close();
            logger.debug("Numero archivos en lista: " + signedFileList.size());
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
            logger.error(ex.getMessage(), ex);
            showProgress(false);
        } */
    }

}

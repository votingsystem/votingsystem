package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.*;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DecompressBackupPane extends VBox {

    private static Logger log = Logger.getLogger(DecompressBackupPane.class.getSimpleName());

    public interface Listener {
        public void processDecompressedFile(ResponseVS response);
    }

    private DecompressBackupTask runningTask;
    private Listener decompressListener;


    public DecompressBackupPane(Listener listener, final String zipFilePath, final String outputFolder) {
        decompressListener = listener;
        VBox progressBox = new VBox();
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPrefWidth(300);
        Text progressMessageText = new Text();
        progressMessageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #555;");
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setLayoutY(10);
        Button cancelButton = new Button(ContextVS.getMessage("cancelLbl"));
        cancelButton.setOnAction(actionEvent -> {
            runningTask.cancel();
            getScene().getWindow().hide();
        });
        cancelButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        HBox footerButtonBox = new HBox();
        footerButtonBox.getChildren().addAll(Utils.getSpacer(), cancelButton);
        progressBox.setMargin(footerButtonBox, new Insets(30, 20, 0, 10));
        progressBox.setMargin(progressMessageText, new Insets(0, 0, 10, 0));
        progressBox.getChildren().addAll(progressMessageText, progressBar, footerButtonBox);
        runningTask = new DecompressBackupTask(zipFilePath, outputFolder);
        progressMessageText.textProperty().bind(runningTask.messageProperty());
        progressBar.progressProperty().bind(runningTask.progressProperty());
        progressBox.visibleProperty().bind(runningTask.runningProperty());
        getChildren().add(progressBox);
    }

    private void init() {
        new Thread(runningTask).start();
    }

    public static void showDialog(final Listener listener, final File fileToOpen) {
        final String outputFolder = ContextVS.APPTEMPDIR + File.separator + UUID.randomUUID();
        log.info("validateBackup - outputFolder: " + outputFolder);
        Platform.runLater(() -> {
            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.initModality(Modality.WINDOW_MODAL);
            //stage.initOwner(window);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
            stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
            File file = fileToOpen;
            if(file == null) {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                        ContextVS.getMessage("backupFileFilterMsg"), "*" + ContentTypeVS.ZIP.getExtension());
                fileChooser.getExtensionFilters().add(extFilter);
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                //fileChooser.setInitialFileName(ContextVS.getMessage("genericReceiptFileName"));
                file = fileChooser.showOpenDialog(stage);
            } else {
                log.info("validateBackup - zipFilePath: " + file.getAbsolutePath() + " - outputFolder: " + outputFolder);
                DecompressBackupPane decompressBackupPane = new DecompressBackupPane(listener,
                        file.getAbsolutePath(), outputFolder);
                decompressBackupPane.getStyleClass().add("modal-dialog");
                decompressBackupPane.init();
                stage.setScene(new Scene(decompressBackupPane));
                stage.getScene().getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
                stage.setTitle(ContextVS.getMessage("decompressBackupCaption"));
                stage.show();
            }
        });
    }

    public class DecompressBackupTask extends Task<ResponseVS> {

        String zipFilePath;
        String outputFolder;

        public DecompressBackupTask(String zipFilePath, String outputFolder) {
            this.zipFilePath = zipFilePath;
            this.outputFolder = outputFolder;
        }

        @Override protected ResponseVS call() throws Exception {
            ResponseVS responseVS = null;
            updateMessage(ContextVS.getMessage("openingFileMsg"));
            updateMessage(ContextVS.getMessage("decompressProgressBarLabel", zipFilePath));
            byte[] buffer = new byte[2048];
            try{
                File folder = new File(outputFolder);
                if(!folder.exists()) folder.mkdir();
                ZipFile zipFile = new ZipFile(zipFilePath);
                int zipFileSize = zipFile.size();
                updateProgress(0, zipFileSize);
                ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
                ZipEntry zipEntry = zis.getNextEntry();
                int fileIndex = 0;
                while( zipEntry != null && !runningTask.isDone()){
                    fileIndex++;
                    updateProgress(fileIndex, zipFileSize);
                    String msg = ContextVS.getMessage("decompressProgressBarMsg", fileIndex, zipFileSize);
                    updateMessage(msg);
                    String fileName = zipEntry.getName();
                    File newFile = new File(outputFolder + File.separator + fileName);
                    if(zipEntry.isDirectory()) {
                        newFile.mkdirs();
                        log.info("mkdirs : " + newFile.getAbsoluteFile());
                    } else {
                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                    zipEntry = zis.getNextEntry();
                }
                zis.closeEntry();
                zis.close();
            } catch(IOException ex){
                log.log(Level.SEVERE, ex.getMessage(), ex);
                setVisible(false);
                return new ResponseVS(ResponseVS.SC_ERROR);
            }
            responseVS = new ResponseVS(ResponseVS.SC_OK);
            responseVS.setData(outputFolder);
            decompressListener.processDecompressedFile(responseVS);
            Platform.runLater(() -> DecompressBackupPane.this.getScene().getWindow().hide());
            return responseVS;
        }
    }
}

package org.votingsystem.client.dialog;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.votingsystem.client.MainApp;
import org.votingsystem.client.pane.DocumentBrowserPane;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.Messages;
import org.votingsystem.util.ObjectUtils;

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
public class DocumentBrowserDialog extends AppDialog {

    private static Logger log = Logger.getLogger(DocumentBrowserDialog.class.getName());

    private static DocumentBrowserDialog INSTANCE;
    private static DocumentBrowserPane documentBrowserPane;

    public DocumentBrowserDialog(DocumentBrowserPane pane) {
        super(pane);
        this.documentBrowserPane = pane;
    }

    private void processUnzippedFile(ResponseDto responseVS) {
        Platform.runLater(() -> {
            documentBrowserPane.processUnzippedFile(responseVS);
            show();
        });
    }

    private void showFile(final File cmsFile) {
        Platform.runLater(() -> {
            log.info("showFile: " + cmsFile.getAbsolutePath());
            documentBrowserPane.load(cmsFile);
            setCaption(documentBrowserPane.getCaption());
            show();
        });
    }

    public static DocumentBrowserDialog showDialog(File selectedFile) {
        if(INSTANCE == null) INSTANCE = new DocumentBrowserDialog(new DocumentBrowserPane());
        if(selectedFile == null) return INSTANCE;
        try {
            if(new ZipInputStream(new FileInputStream(selectedFile)).getNextEntry() != null){
                final String outputFolder = MainApp.instance().getTempDir() + File.separator + UUID.randomUUID();
                ProgressDialog.show(new DecompressBackupTask(selectedFile.getAbsolutePath(), outputFolder), null);
            } else {
                log.severe("--- TODO ---");
                if(selectedFile.getName().contains("_curr_")) {
                    CurrencyDialog.show((Currency) ObjectUtils.deSerializeObject(FileUtils.getBytesFromFile(selectedFile)),
                            documentBrowserPane.getScene().getWindow(), null);
                } else {
                    INSTANCE.showFile(selectedFile);
                }
            }
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return INSTANCE;
    }

    public static void showDialog( ) {
        if(INSTANCE == null) INSTANCE = new DocumentBrowserDialog(new DocumentBrowserPane());
        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            /*FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                    Messages.currentInstance().get("signedFileFileFilterMsg"), "*" + ContentType.SIGNED.getExtension());
            fileChooser.getExtensionFilters().add(extFilter);*/
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            //fileChooser.setInitialFileName(Messages.currentInstance().get("genericReceiptFileName"));
            showDialog(fileChooser.showOpenDialog(new Stage()));
        });
    }



    public static void showDialog(byte[] signedDocument, EventHandler closeDialogHandler) {
        Platform.runLater(() -> {
            if(INSTANCE == null)
                INSTANCE = new DocumentBrowserDialog(new DocumentBrowserPane());
            File selectedFile = FileUtils.getFileFromBytes(signedDocument);
            if(closeDialogHandler != null)
                INSTANCE.addCloseListener(closeDialogHandler);
            INSTANCE.showFile(selectedFile);
        });
    }

    public static class DecompressBackupTask extends Task<ResponseDto> {

        String zipFilePath;
        String outputFolder;

        public DecompressBackupTask(String zipFilePath, String outputFolder) {
            this.zipFilePath = zipFilePath;
            this.outputFolder = outputFolder;
        }

        @Override protected ResponseDto call() throws Exception {
            ResponseDto responseVS = null;
            updateMessage(Messages.currentInstance().get("openingFileMsg"));
            updateMessage(Messages.currentInstance().get("decompressProgressBarLabel", zipFilePath));
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
                while( zipEntry != null && !this.isDone()){
                    fileIndex++;
                    updateProgress(fileIndex, zipFileSize);
                    String msg = Messages.currentInstance().get("decompressProgressBarMsg", fileIndex, zipFileSize);
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
                return new ResponseDto(ResponseDto.SC_ERROR);
            }
            responseVS = new ResponseDto(ResponseDto.SC_OK).setData(outputFolder);
            INSTANCE.processUnzippedFile(responseVS);
            return responseVS;
        }
    }

}
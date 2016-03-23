package org.votingsystem.client.dialog;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.votingsystem.client.pane.DocumentBrowserPane;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
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
public class DocumentBrowserDialog extends DialogVS {

    private static Logger log = Logger.getLogger(DocumentBrowserDialog.class.getName());

    private static DocumentBrowserDialog INSTANCE;
    private static DocumentBrowserPane documentBrowserPane;

    public DocumentBrowserDialog(DocumentBrowserPane pane) {
        super(pane);
        this.documentBrowserPane = pane;
    }

    private void processUnzippedFile(ResponseVS responseVS) {
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
                final String outputFolder = ContextVS.getInstance().getTempDir() + File.separator + UUID.randomUUID();
                ProgressDialog.show(new DecompressBackupTask(selectedFile.getAbsolutePath(), outputFolder), null);
            } else {
                if(selectedFile.getName().endsWith(ContentType.CURRENCY.getExtension())) {
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
                    ContextVS.getMessage("signedFileFileFilterMsg"), "*" + ContentType.SIGNED.getExtension());
            fileChooser.getExtensionFilters().add(extFilter);*/
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            //fileChooser.setInitialFileName(ContextVS.getMessage("genericReceiptFileName"));
            showDialog(fileChooser.showOpenDialog(new Stage()));
        });
    }

    public static void showDialog(CMSSignedMessage cmsMessage, EventHandler closeDialogHandler) {
        try {
            showDialog(cmsMessage.toPEM(), closeDialogHandler);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static void showDialog(byte[] cmsMessageBytes, EventHandler closeDialogHandler) {
        Platform.runLater(() -> {
            if(INSTANCE == null) INSTANCE = new DocumentBrowserDialog(new DocumentBrowserPane());
            File selectedFile = FileUtils.getFileFromBytes(cmsMessageBytes);
            if(closeDialogHandler != null) INSTANCE.addCloseListener(closeDialogHandler);
            INSTANCE.showFile(selectedFile);
        });
    }

    public static class DecompressBackupTask extends Task<ResponseVS> {

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
                while( zipEntry != null && !this.isDone()){
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
                return new ResponseVS(ResponseVS.SC_ERROR);
            }
            responseVS = new ResponseVS(ResponseVS.SC_OK).setData(outputFolder);
            INSTANCE.processUnzippedFile(responseVS);
            return responseVS;
        }
    }

}

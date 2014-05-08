package org.votingsystem.admintool.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.admintool.panel.ProgressBarPanel;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class DecompressFileDialog extends JDialog {

    private static Logger logger = Logger.getLogger(DecompressFileDialog.class);

    public interface Listener {
        public void processDecompressedFile(ResponseVS response);
    }
    
    private Container container;
    private ProgressBarPanel progressBarPanel;
    private Future<ResponseVS> runningTask;
    private Listener decompressListener;

    public DecompressFileDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug(" - window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
            }
        });
        setTitle(ContextVS.getMessage("decompressBackupCaption"));
        pack();
        setLocationRelativeTo(null);
    }

    public void unZipBackup(Listener listener, final String zipFilePath, final String outputFolder) 
            throws Exception {
        this.decompressListener = listener;
        DecompressWorker decompressWorker = new DecompressWorker(zipFilePath, outputFolder);
        runningTask = decompressWorker;
        decompressWorker.execute();
        setVisible(true);
    }


    private void initComponents() {
        container = getContentPane();
        container.setLayout(new MigLayout("fill", "", "[]20[]"));
        progressBarPanel = new ProgressBarPanel();
        container.add(progressBarPanel, "cell 0 0, width 400::, growx, wrap");

        JButton cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel_16"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel();
            }
        });
        container.add(cancelButton, "width :150:, align right");
    }

    private void cancel() {
        logger.debug("cancel");
        if(runningTask != null) runningTask.cancel(true);
        decompressListener.processDecompressedFile(new ResponseVS(ResponseVS.SC_CANCELLED));
        dispose();
    }

     class DecompressWorker extends SwingWorker<ResponseVS, Object> {

         String zipFilePath;
         String outputFolder;

        public DecompressWorker(String zipFilePath, String outputFolder) {
            this.zipFilePath = zipFilePath;
            this.outputFolder = outputFolder;
        }

        @Override public ResponseVS doInBackground() {
            logger.debug("worker.doInBackground: " + zipFilePath + " - outputFolder: " + outputFolder);
            progressBarPanel.setMessage(ContextVS.getMessage("decompressProgressBarLabel", zipFilePath));
            byte[] buffer = new byte[2048];
            try{
                File folder = new File(outputFolder);
                if(!folder.exists()) folder.mkdir();
                ZipFile zipFile = new ZipFile(zipFilePath);
                int zipFileSize = zipFile.size();
                progressBarPanel.setMaximum(zipFileSize);
                ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
                ZipEntry zipEntry = zis.getNextEntry();
                int fileIndex = 0;
                while( zipEntry != null && !runningTask.isDone()){
                    fileIndex++;
                    progressBarPanel.setValue(fileIndex);
                    String msg = ContextVS.getInstance().getMessage("decompressProgressBarMsg", fileIndex, zipFileSize);
                    progressBarPanel.setBarMessage(msg);
                    String fileName = zipEntry.getName();
                    File newFile = new File(outputFolder + File.separator + fileName);
                    if(zipEntry.isDirectory()) {
                        newFile.mkdirs();
                        logger.debug("mkdirs : "+ newFile.getAbsoluteFile());
                    } else {
                        new File(newFile.getParent()).mkdirs();
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
                logger.error(ex.getMessage(), ex);
                setVisible(false);
                return new ResponseVS(ResponseVS.SC_ERROR);
            }
            setVisible(false);
            return new ResponseVS(ResponseVS.SC_OK);
        }

       @Override protected void done() {
           logger.debug("worker.done");
           ResponseVS response = new ResponseVS(ResponseVS.SC_OK);
           response.setData(outputFolder);
           decompressListener.processDecompressedFile(response);
       }
   }
    
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ContextVS.init(null, "log4jAdminTool.properties", "adminToolMessages_", "es");
                    String zipFile = "./representative_00000001R.zip";
                    String outputFolder = ContextVS.APPTEMPDIR +  File.separator + UUID.randomUUID();
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    DecompressFileDialog dialog = new DecompressFileDialog(new JFrame(), true);
                    dialog.setVisible(true);
                    //dialog.unZipBackup(zipFile, outputFolder);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }

}
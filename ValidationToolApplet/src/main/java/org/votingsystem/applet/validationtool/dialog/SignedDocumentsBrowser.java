package org.votingsystem.applet.validationtool.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.ClosableTabbedPane;
import org.votingsystem.applet.validationtool.model.MetaInf;
import org.votingsystem.applet.validationtool.model.SignedFile;
import org.votingsystem.applet.validationtool.panel.EventVSInfoPanel;
import org.votingsystem.applet.validationtool.panel.MessagePanel;
import org.votingsystem.applet.validationtool.panel.ProgressBarPanel;
import org.votingsystem.applet.validationtool.panel.SignedFilePanel;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignedDocumentsBrowser extends JDialog {

    private static Logger logger = Logger.getLogger(SignedDocumentsBrowser.class);


    private Container container;
    private ProgressBarPanel progressBarPanel;
    private MessagePanel messagePanel;
    private String fileDir;
    private boolean isProgressBarVisible = false;
    private String mensajeMime;
    private List<SignedFile> signedFileList = new ArrayList<SignedFile>();

    private ClosableTabbedPane tabbedPane;
    private int selectedFileIndex;
    private String dialogTitle;
    private MetaInf metaInf;
    private String decompressedBackupBaseDir = null;
    private JButton validateBackupButton;
    private JButton cancelButton;
    private JPanel buttonsPanel;

    public SignedDocumentsBrowser(Frame parent, boolean modal) {
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
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        container = getContentPane();
        container.setLayout(new MigLayout("fill, insets 10 10 10 10", "[800:800:]", "[grow][]20[]"));

        tabbedPane = new ClosableTabbedPane();
        container.add(tabbedPane, "cell 0 0, grow, wrap");
        
        //messagePanel = new MessagePanel();
        //container.add(messagePanel, "cell 0 1, growx, wrap");

        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new MigLayout("fill"));

        validateBackupButton = new JButton(ContextVS.getMessage("validateBackupLbl"));
        validateBackupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validateBackup();
            }
        });
        validateBackupButton.setIcon(ContextVS.getIcon(this, "check_box_list"));
        validateBackupButton.setVisible(false);
        buttonsPanel.add(validateBackupButton);

        JButton previousButton = new JButton(ContextVS.getMessage("buttonPreviousLbl"));
        previousButton.setIcon(ContextVS.getIcon(this, "resultset_previous"));
        previousButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goPrevious();
            }
        });
        previousButton.setVisible(false);
        buttonsPanel.add(previousButton, "gapleft 80");

        JButton nextButton = new JButton(ContextVS.getMessage("buttonNextLbl"));
        nextButton.setIcon(ContextVS.getIcon(this, "resultset_next"));
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goNext();
            }
        });
        nextButton.setVisible(false);
        buttonsPanel.add(nextButton);

        JButton saveButton = new JButton(ContextVS.getMessage("saveLbl"));
        saveButton.setIcon(ContextVS.getIcon(this, "save_data"));
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMessage();
            }
        });
        saveButton.setVisible(false);
        buttonsPanel.add(saveButton, "gapleft 20");

        cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dispose();
            }
        });
        buttonsPanel.add(cancelButton, "width :150:, align right");

        container.add(buttonsPanel, "cell 0 2, growx");
    }

    public void openSignedFile() {
        File file = null;
        try {
            final JFileChooser chooser = new JFileChooser();
            int returnVal = chooser.showOpenDialog(new JFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) file = chooser.getSelectedFile();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        if(file != null) {
            openFile(file);
            setVisible(true);
        } else logger.debug("Not file selected");
    }


    private void goNext() {
        if (selectedFileIndex == signedFileList.size() -1)  selectedFileIndex = 0;
        else ++selectedFileIndex;
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        showSignedFile(signedFileList.get(selectedFileIndex));
    }

    private void goPrevious() {
        if (selectedFileIndex == 0)  selectedFileIndex = signedFileList.size() -1;
        else --selectedFileIndex;
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        showSignedFile(signedFileList.get(selectedFileIndex));
    }

    private void validateBackup() {
        BackupValidationDialog validationDialog = new BackupValidationDialog(new JFrame(), metaInf, false);
        try {
            validationDialog.initValidation(decompressedBackupBaseDir);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void openFile (File file) {
        fileDir = file.getParent();
        String title = (dialogTitle == null ? file.getName() : dialogTitle + " - " + file.getName());
        setTitle(title);
        try {
            int fileIndex = tabbedPane.indexOfFile (file);
            if (fileIndex != -1 && tabbedPane.getTabCount() >1) {
                tabbedPane.setSelectedIndex(fileIndex);
                return;
            }
            byte[] fileBytes = FileUtils.getBytesFromFile(file);
            SignedFile signedFile = new SignedFile(fileBytes, file.getName());
            if(signedFile.isPDF()) tabbedPane.addTab(file, new SignedFilePanel(signedFile));
            else  tabbedPane.addTab(file, new SignedFilePanel(signedFile));
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
            tabbedPane.setVisible(true);
            pack();
        } catch (Exception ex) {
            showErrorMessage(ContextVS.getMessage("openFileErrorMsg", file.getAbsolutePath()));
            logger.error(ex.getMessage(), ex);

        }
    }

    private void showSignedFile (SignedFile signedFile) {
        try {
            tabbedPane.setTitleAt(1, "<html>" + signedFile.getName() + "&nbsp;&nbsp;&nbsp;&nbsp;</html>");
            SignedFilePanel signedFilePanel = (SignedFilePanel) tabbedPane.getComponentAt(1);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void showProgress (boolean visibility) {
        isProgressBarVisible = visibility;
        if(visibility) { 
            if(progressBarPanel == null) progressBarPanel = new ProgressBarPanel();;
            container.add(progressBarPanel, "cell 0 2, width 400::, growx, wrap");
            container.remove(tabbedPane);
            container.remove(buttonsPanel);
        } else {
            container.remove(progressBarPanel);
            container.add(tabbedPane, "cell 0 0, width 400::, growx, wrap");
            container.add(tabbedPane, "cell 0 0, width 400::, growx, wrap");
        }
        pack();
    }

    private void showErrorMessage (String message) {
        buttonsPanel.setVisible(false);
        tabbedPane.setVisible(false);
        messagePanel = new MessagePanel();
        messagePanel.setMessage(message, ContextVS.getIcon(this, "cancel_32"));
        container.add(messagePanel, "cell 0 0, wrap");
        pack();
    }

    public void showEventVS () {
        showProgress(true);
        setVisible(true);
    }

    public void saveMessage () {
        try {
            final JFileChooser chooser = new JFileChooser();
            int returnVal = chooser.showSaveDialog(new JFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (file.getName().indexOf(".") == -1) {
                    String fileName = file.getAbsolutePath() + ContextVS.SIGNED_PART_EXTENSION;
                    FileOutputStream fos = new FileOutputStream(new File(fileName));
                    fos.write(mensajeMime.getBytes());
                    fos.close();
                }
            }
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

    private class FiltroCMS implements java.io.FileFilter {
        @Override public boolean accept(File file) {
            return (checkFileSize(file) == null);
        }
    }

    public String getSignatureMessage (Date date) {
        return "<html><b>" + ContextVS.getMessage("signatureDateLbl") +
                DateUtils.getSpanishFormattedStringFromDate(date) + "</html>";
    }

    public void setVisible(String decompressedBackupBaseDir) {
        logger.debug("setVisible - decompressedBackupBaseDir: " + decompressedBackupBaseDir);
        this.decompressedBackupBaseDir = decompressedBackupBaseDir;
        File metaInfFile = new File(decompressedBackupBaseDir + File.separator + "meta.inf");
        if(!metaInfFile.exists()) {
            String message = ContextVS.getMessage("metaInfNotFoundMsg", metaInfFile.getAbsolutePath());
            logger.error(message);
            MessageDialog messageDialog = new MessageDialog(new JFrame(), true);
            messageDialog.showMessage(message, "Error");
            return;
        }
        try {
            metaInf = MetaInf.parse(FileUtils.getStringFromFile(metaInfFile));
            EventVSInfoPanel eventPanel = new EventVSInfoPanel(metaInf);
            tabbedPane.removeAll();
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
            setTitle(dialogTitle);
            tabbedPane.addTab("<html><div style='margin:0 20px 0 0;'><b>" + dialogTitle + "</b></div></html>",
                    eventPanel);
            if(TypeVS.VOTING_EVENT == metaInf.getType() || TypeVS.MANIFEST_EVENT == metaInf.getType() ||
                    TypeVS.CLAIM_EVENT == metaInf.getType()) {
                validateBackupButton.setVisible(true);
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        /*try {
            ZipFile backupZip = new ZipFile(zip);
            new File(ValidationToolContext.DEFAULTS.APPTEMPDIR + File.separator +
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
            signedFileListCMS = Arrays.asList(fileDir.listFiles(new FiltroCMS()));
            selectedFileIndex = signedFileListCMS.indexOf(archivo);
            if (signedFileListCMS.size() < 2) habilitarBotones(false);
            else habilitarBotones(true);
            contexto = ValidationToolContext.EXPLORANDO_DIRECTORIO;
            openFile(archivo);
            showProgress(false);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            showProgress(false);
        } */
        pack();
        setVisible(true);
    }


    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ContextVS.init(null, "log4jValidationTool.properties", "validationToolMessages_", "es");
                    String zipFile = "./representative_00000001R.zip";
                    String outputFolder = ContextVS.APPTEMPDIR +  File.separator + UUID.randomUUID();
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    //BackupValidationDialog dialog = new BackupValidationDialog(new JFrame(), true);
                    //dialog.setVisible(true);
                    //dialog.unZipBackup(zipFile, outputFolder);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }

}
package org.votingsystem.applet.validationtool.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.html.parser.ParserDelegator;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class PasswordDialog extends JDialog {

    private static Logger logger = Logger.getLogger(PasswordDialog.class);

    private Container container;
    private JLabel messageLabel;
    private JPanel messagePanel;
    private JPasswordField password1Field;
    private JPasswordField password2Field;
    private JButton cancelButton;
    private JButton acceptButton;
    private String password;
    private String mainMessage = null;
    boolean isCapsLockPressed = false;

    public PasswordDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        password1Field.addKeyListener(new KeyListener(){
            boolean shiftPressed = false;
            @Override public void keyPressed(KeyEvent e){
                if(!shiftPressed) {
                    if(Character.isUpperCase(e.getKeyChar())) setCapsLockState(true);
                    else setCapsLockState(false);
                } else {
                    if(Character.isUpperCase(e.getKeyChar())) setCapsLockState(false);
                    else setCapsLockState(true);
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) acceptButton.doClick();
                if (e.getKeyCode() == KeyEvent.VK_SHIFT) shiftPressed = true;
            }
            @Override public void keyTyped(KeyEvent ke) {}
            @Override public void keyReleased(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_SHIFT) {
                    shiftPressed = false;
                }
            }
        });
        password2Field.addKeyListener(new KeyListener(){
            @Override public void keyPressed(KeyEvent e){
                if (e.getKeyCode() == KeyEvent.VK_ENTER) acceptButton.doClick();
            }
            @Override public void keyTyped(KeyEvent ke) {}
            @Override public void keyReleased(KeyEvent ke) {}
        });
        //Bug similar to -> http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6993691
        ParserDelegator workaround = new ParserDelegator();

        password = null;
        String dialogTitle = null;
        if(ContextVS.getInstance().getBoolProperty(ContextVS.WITH_KEYSTORE_PROPERTY, false)) {
            mainMessage = ContextVS.getInstance().getMessage("adviceKeyStore");
            dialogTitle = ContextVS.getInstance().getMessage("passwordDialogKeyStoreCaption");
        } else {
            mainMessage = ContextVS.getInstance().getMessage("adviceDNIE");
            dialogTitle = ContextVS.getInstance().getMessage("passwordDialogDNIeCaption");
        }
        setTitle(dialogTitle);
        /*boolean check = false;
        try {//NOT SUPPORTED IN APPLET
            check = Toolkit.getDefaultToolkit().
                getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        if(check) changeCapsLockState();
        else messageLabel.setText(getMessage("adviceDNIE")); */

        setMessage(mainMessage);
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        logger.debug("initComponents");
        container = getContentPane();
        container.setLayout(new MigLayout("fill", "", "[][]20[]"));

        messagePanel = new JPanel();
        Border messagePanelBorder = BorderFactory.createLineBorder(Color.GRAY, 1);
        messagePanel.setBorder(messagePanelBorder);
        messagePanel.setLayout(new MigLayout("fill"));
        messageLabel = new JLabel();

        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messagePanel.add(messageLabel, "growx, wrap");
        container.add(messagePanel, "cell 0 0, growx, wrap");


        JPanel formPanel = new JPanel();
        formPanel.setLayout(new MigLayout("fill", "15[grow]15"));
        JLabel password1Label = new JLabel(ContextVS.getMessage("password1Lbl"));
        password1Field = new JPasswordField();
        formPanel.add(password1Label, "wrap");
        formPanel.add(password1Field, "growx, wrap");

        JLabel password2Label = new JLabel(ContextVS.getMessage("password2Lbl"));
        password2Field = new JPasswordField();
        formPanel.add(password2Label, "wrap");
        formPanel.add(password2Field, "growx, wrap");
        container.add(formPanel, "cell 0 1, growx, wrap");


        acceptButton = new JButton(ContextVS.getMessage("acceptLbl"));
        acceptButton.setIcon(ContextVS.getIcon(this, "accept"));
        acceptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { checkPasswords();}
        });
        container.add(acceptButton, "width :150:, cell 0 2, split2, align right");

        cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { dispose();}
        });
        container.add(cancelButton, "width :150:, align right");


    }

    public String getPassword() {
        return password;
    }

    private void checkPasswords() {
        logger.debug("checkPasswords");
        String password1 = new String(password1Field.getPassword());
        String password2 = new String(password2Field.getPassword());
        if(password1.trim().isEmpty() && password2.trim().isEmpty()) setMessage(ContextVS.getMessage("passwordMissing"));
        else {
            if (password1.equals(password2)) {
                password = password1;
                dispose();
            } else {
                setMessage(ContextVS.getMessage("passwordError"));
                password1Field.setText("");
                password2Field.setText("");
                pack();
            }
        }
    }

    public void setMainMessage(String mainMessage, String caption) {
        this.mainMessage = mainMessage;
        setMessage(mainMessage);
        if(caption != null) setTitle(caption);
    }

    private void setCapsLockState (boolean pressed) {
        this.isCapsLockPressed = pressed;
        setMessage(null);
    }

    private void setMessage (String mensaje) {
        if (mensaje == null) {
            if(isCapsLockPressed) {
                messageLabel.setText("<html><b>" +
                        ContextVS.getMessage("capsLockKeyPressed") + "</b><br/><br/>" + mainMessage + "</html>");
            } else {
                messageLabel.setText(mainMessage);
            }
        } else {
            if(isCapsLockPressed) {
                messageLabel.setText("<html><b>" + ContextVS.getMessage("capsLockKeyPressed")+ "</b><br/>" +
                        mensaje + "</html>");
            }  else messageLabel.setText(mensaje);
        }
        pack();
    }

    public static void main(String args[]) {

        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                    ContextVS.initSignatureApplet(null, "log4j.properties", "messages_", "es");
                    final PasswordDialog dialog = new PasswordDialog(new JFrame(), true);
                    dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override public void windowClosing(java.awt.event.WindowEvent e) { dialog.dispose(); }
                    });
                    logger.debug("--- Mostrando diálogo ---");
                    dialog.setVisible(true);
                    logger.debug("--- Diálogo mostrado ---");
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }


}
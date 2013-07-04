package org.sistemavotacion.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import static java.awt.Frame.getFrames;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import org.sistemavotacion.herramientavalidacion.VisualizadorDeEventoFirmadoDialog;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.test.dialogo.InfoServidorDialog;
import org.sistemavotacion.test.dialogo.MensajeDialog;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.callable.InfoSender;
import org.sistemavotacion.callable.InfoGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class MainFrame extends JFrame  implements KeyListener, FocusListener {
    
    private static Logger logger = LoggerFactory.getLogger(MainFrame.class);
        
    private static final int ACCESS_CONTROL_GETTER = 0;
    private static final int CONTROL_CENTER_GETTER = 1;
    private static final int CA_CERT_INITIALIZER   = 2;
    
    private static BlockingQueue<Future<Respuesta>> queue = 
            new LinkedBlockingQueue<Future<Respuesta>>(10);

    public enum Estado {CONECTADO_CONTROL_ACCESO , ERROR_CONEXION_CONTROL_ACCESO,
        DESCONECTADO, CONECTANDO;}
    
    private Estado estado = Estado.DESCONECTADO;
    private Future<Respuesta> tareaEnEjecucion;
    private Border normalTextBorder;
    private Frame mainFrame;
    private final AtomicBoolean done = new AtomicBoolean(false);
    
    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        initComponents();
        controlAccesoTextField.addFocusListener(this);
        setLocationRelativeTo(null);
        tabbedPane.setVisible(false);
        mensajePanel.setVisible(false);
        normalTextBorder = new JTextField().getBorder();
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                done.set(true);
                if (tareaEnEjecucion != null) {
                    tareaEnEjecucion.cancel(true);
                }
            }
            public void windowClosing(WindowEvent e) { 
                done.set(true);
            }
        });
        controlAccesoTextField.addKeyListener(this);
        mainFrame = getFrames()[0];
        pack();
        votacionesPanel.setMainFrame(this);
        ContextoPruebas.INSTANCE.setVotingPanel(votacionesPanel);
        ContextoPruebas.INSTANCE.submit(new Runnable() {
            @Override public void run() {
                try {
                    readFutures();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        setTitle(ContextoPruebas.INSTANCE.getString("mainFrameCaptionLbl"));
    }
                
    public void readFutures () {
        logger.debug(" - readFutures");
        while (!done.get()) {
            try {
                Future<Respuesta> future = queue.take();
                Respuesta respuesta = future.get();
                switch(respuesta.getId()) {
                    case ACCESS_CONTROL_GETTER:
                        estado = Estado.DESCONECTADO;
                        infoServidorButton.setIcon(null);
                        infoServidorButton.setText("Conectar");
                        infoServidorButton.setIcon(new ImageIcon(getClass().getResource("/images/pair_16x16.png")));            
                        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                            try {
                                ActorConIP controlAcceso = ActorConIP.parse(
                                        respuesta.getMensaje());
                                if(ActorConIP.Tipo.CONTROL_ACCESO != 
                                        controlAcceso.getTipo()) {
                                    mostrarMensajeUsuario(ContextoPruebas.INSTANCE.
                                            getString("accessControlErrorMsg"));
                                    controlAccesoTextField.setBorder(new LineBorder(Color.RED,2));
                                    return;
                                }
                                if(ActorConIP.EnvironmentMode.DEVELOPMENT !=  
                                        controlAcceso.getEnvironmentMode()) {
                                    String msg = "SERVER NOT IN DEVELOPMENT MODE. Server mode:" + 
                                            controlAcceso.getEnvironmentMode();
                                    logger.error("### ERROR - " + msg);
                                    mostrarMensajeUsuario(msg);
                                    return;
                                }
                                mostrarMensajeUsuario(null);
                                controlAccesoTextField.setBorder(normalTextBorder);
                                ContextoPruebas.INSTANCE.setControlAcceso(controlAcceso);
                                votacionesPanel.setControlAcceso(controlAcceso);
                                byte[] caPemCertificateBytes = CertUtil.fromX509CertToPEM (
                                    ContextoPruebas.INSTANCE.getRootCACert());
                                String urlAnyadirCertificadoCA = ContextoPruebas.getRootCAServiceURL(
                                    controlAcceso.getServerURL());
                                estado = Estado.CONECTANDO;
                                infoServidorButton.setIcon(new javax.swing.ImageIcon(
                                getClass().getResource("/images/loading.gif")));
                                infoServidorButton.setText("Añadiendo Autoridad Certificadora");
                                InfoSender infoSender = new InfoSender(CA_CERT_INITIALIZER, 
                                        caPemCertificateBytes, null, urlAnyadirCertificadoCA);
                                Future<Respuesta> futureCaInitializer = ContextoPruebas.INSTANCE.submit(infoSender);
                                queue.add(futureCaInitializer);
                                tareaEnEjecucion = futureCaInitializer;
                            } catch(Exception ex) {
                                logger.error(ex.getMessage(), ex);
                                mostrarMensajeUsuario(ex.getMessage());
                            }
                        } else if (Respuesta.SC_NOT_FOUND == respuesta.getCodigoEstado()) { 
                            mostrarMensajeUsuario("Página no encontrada");
                        } else {
                            String mensaje = "Error - " + respuesta.getMensaje();
                            mostrarMensajeUsuario(mensaje);
                        }
                        break;
                    case CONTROL_CENTER_GETTER:
                        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                            try {
                                ActorConIP actorConIP = ActorConIP.parse(
                                        respuesta.getMensaje());
                                if(!(ActorConIP.Tipo.CENTRO_CONTROL == actorConIP.getTipo())) {
                                    mostrarMensajeUsuario("El servidor no es un Centro Control");
                                    return;
                                }
                                mostrarMensajeUsuario(null);
                                ContextoPruebas.INSTANCE.setControlCenter(actorConIP);
                            } catch(Exception ex) {
                                String mensaje = "Error Cargando centro Control <br/>" + ex.getMessage();
                                mostrarMensajeUsuario(mensaje);
                            }
                        } else {
                            String mensaje = "Error Cargando centro Control <br/>" + 
                                    respuesta.getMensaje();
                            mostrarMensajeUsuario(mensaje);
                        }
                        break;
                    case CA_CERT_INITIALIZER:
                        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                            infoServidorButton.setText("Información del servidor");
                            infoServidorButton.setIcon(new ImageIcon(getClass()
                                    .getResource("/images/information-white.png")));
                            estado = Estado.CONECTADO_CONTROL_ACCESO;
                            tabbedPane.setVisible(true);
                            pack();
                        } else {
                            estado = Estado.DESCONECTADO;
                            infoServidorButton.setText("Conectar");
                            infoServidorButton.setIcon(new ImageIcon(getClass().getResource("/images/pair_16x16.png")));
                            String mensaje = "Error añadiendo Autoridad Certificadora de pruebas - " + 
                                            respuesta.getMensaje();
                            mostrarMensajeUsuario(mensaje);
                            logger.debug("mostrarResultadoOperacion - multipartEntityWorker - message: " 
                                + mensaje);
                        } 
                        break;
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex); 
            }
       }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        urlsPanel = new javax.swing.JPanel();
        controlAccesoLabel = new javax.swing.JLabel();
        controlAccesoTextField = new javax.swing.JTextField();
        estadoControlAccesoLabel = new javax.swing.JLabel();
        infoServidorButton = new javax.swing.JButton();
        tabbedPane = new javax.swing.JTabbedPane();
        votacionesTabPanel = new org.sistemavotacion.test.panel.FirmasPanel();
        votacionesPanel = new org.sistemavotacion.test.panel.VotacionesPanel();
        firmasPanel1 = new org.sistemavotacion.test.panel.FirmasPanel();
        mensajePanel = new javax.swing.JPanel();
        mensajeLabel = new javax.swing.JLabel();
        closeLabel = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        archivoMenu = new javax.swing.JMenu();
        salirMenuItem = new javax.swing.JMenuItem();
        abrirArchivoMenuItem = new javax.swing.JMenuItem();
        ayudaMenu = new javax.swing.JMenu();
        informacionSistemaMenuItem = new javax.swing.JMenuItem();
        razonesMenuItem = new javax.swing.JMenuItem();
        comoAuditarMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(null);

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/test/Bundle"); // NOI18N
        controlAccesoLabel.setText(bundle.getString("MainFrame.controlAccesoLabel.text")); // NOI18N

        controlAccesoTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controlAccesoTextFieldActionPerformed(evt);
            }
        });

        estadoControlAccesoLabel.setText(bundle.getString("MainFrame.estadoControlAccesoLabel.text")); // NOI18N

        infoServidorButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/pair_16x16.png"))); // NOI18N
        infoServidorButton.setText(bundle.getString("MainFrame.infoServidorButton.text")); // NOI18N
        infoServidorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                infoServidorButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout urlsPanelLayout = new javax.swing.GroupLayout(urlsPanel);
        urlsPanel.setLayout(urlsPanelLayout);
        urlsPanelLayout.setHorizontalGroup(
            urlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(urlsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(controlAccesoLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(controlAccesoTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 426, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(infoServidorButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(estadoControlAccesoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        urlsPanelLayout.setVerticalGroup(
            urlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(urlsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(urlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(urlsPanelLayout.createSequentialGroup()
                        .addGroup(urlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(controlAccesoLabel)
                            .addComponent(controlAccesoTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(infoServidorButton))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(estadoControlAccesoLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout votacionesTabPanelLayout = new javax.swing.GroupLayout(votacionesTabPanel);
        votacionesTabPanel.setLayout(votacionesTabPanelLayout);
        votacionesTabPanelLayout.setHorizontalGroup(
            votacionesTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(votacionesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 1056, Short.MAX_VALUE)
        );
        votacionesTabPanelLayout.setVerticalGroup(
            votacionesTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(votacionesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)
        );

        tabbedPane.addTab(bundle.getString("MainFrame.votacionesTabPanel.TabConstraints.tabTitle"), votacionesTabPanel); // NOI18N

        javax.swing.GroupLayout firmasPanel1Layout = new javax.swing.GroupLayout(firmasPanel1);
        firmasPanel1.setLayout(firmasPanel1Layout);
        firmasPanel1Layout.setHorizontalGroup(
            firmasPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1056, Short.MAX_VALUE)
        );
        firmasPanel1Layout.setVerticalGroup(
            firmasPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 412, Short.MAX_VALUE)
        );

        tabbedPane.addTab(null, firmasPanel1);

        mensajePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        mensajeLabel.setFont(new java.awt.Font("DejaVu Sans", 1, 15)); // NOI18N
        mensajeLabel.setForeground(java.awt.Color.red);
        mensajeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mensajeLabel.setText(bundle.getString("MainFrame.mensajeLabel.text")); // NOI18N

        closeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        closeLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/close.gif"))); // NOI18N
        closeLabel.setText(bundle.getString("MainFrame.closeLabel.text")); // NOI18N
        closeLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                closeMensajeUsuario(evt);
            }
        });

        javax.swing.GroupLayout mensajePanelLayout = new javax.swing.GroupLayout(mensajePanel);
        mensajePanel.setLayout(mensajePanelLayout);
        mensajePanelLayout.setHorizontalGroup(
            mensajePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mensajePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mensajeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 865, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(closeLabel))
        );
        mensajePanelLayout.setVerticalGroup(
            mensajePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mensajePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mensajeLabel)
                .addContainerGap(16, Short.MAX_VALUE))
            .addGroup(mensajePanelLayout.createSequentialGroup()
                .addComponent(closeLabel)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        archivoMenu.setText(bundle.getString("MainFrame.archivoMenu.text")); // NOI18N

        salirMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        salirMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/exit_16x16.png"))); // NOI18N
        salirMenuItem.setText(bundle.getString("MainFrame.salirMenuItem.text")); // NOI18N
        salirMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                salirMenuItemActionPerformed(evt);
            }
        });
        archivoMenu.add(salirMenuItem);

        abrirArchivoMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/fileopen16x16.png"))); // NOI18N
        abrirArchivoMenuItem.setText(bundle.getString("MainFrame.abrirArchivoMenuItem.text")); // NOI18N
        abrirArchivoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                abrirArchivoMenuItemActionPerformed(evt);
            }
        });
        archivoMenu.add(abrirArchivoMenuItem);

        menuBar.add(archivoMenu);

        ayudaMenu.setText(bundle.getString("MainFrame.ayudaMenu.text")); // NOI18N

        informacionSistemaMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/information-white.png"))); // NOI18N
        informacionSistemaMenuItem.setText(bundle.getString("MainFrame.informacionSistemaMenuItem.text")); // NOI18N
        informacionSistemaMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                informacionSistemaMenuItemActionPerformed(evt);
            }
        });
        ayudaMenu.add(informacionSistemaMenuItem);

        razonesMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/information-white.png"))); // NOI18N
        razonesMenuItem.setText(bundle.getString("MainFrame.razonesMenuItem.text")); // NOI18N
        razonesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                razonesMenuItemActionPerformed(evt);
            }
        });
        ayudaMenu.add(razonesMenuItem);

        comoAuditarMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/information-white.png"))); // NOI18N
        comoAuditarMenuItem.setText(bundle.getString("MainFrame.comoAuditarMenuItem.text")); // NOI18N
        comoAuditarMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comoAuditarMenuItemActionPerformed(evt);
            }
        });
        ayudaMenu.add(comoAuditarMenuItem);

        menuBar.add(ayudaMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(urlsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPane)
                .addContainerGap())
            .addComponent(mensajePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(mensajePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(urlsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabbedPane)
                .addContainerGap())
        );

        tabbedPane.getAccessibleContext().setAccessibleName(null);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void salirMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_salirMenuItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_salirMenuItemActionPerformed

    private void controlAccesoTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlAccesoTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_controlAccesoTextFieldActionPerformed

    private void infoServidorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_infoServidorButtonActionPerformed
        logger.debug("infoServidorButtonActionPerformed - estado: " + estado.toString());
        if("".equals(controlAccesoTextField.getText().trim())) return;        
        switch(estado) {
            case DESCONECTADO:
                logger.debug("Conectando con Control Acceso");
                cargarControlAcceso();
                break;
            case CONECTANDO:
                tareaEnEjecucion.cancel(true);
                infoServidorButton.setIcon(null);
                infoServidorButton.setText("Conectar");
                infoServidorButton.setIcon(new ImageIcon(getClass().getResource("/images/pair_16x16.png")));
                estado = Estado.DESCONECTADO;
                break;
            case CONECTADO_CONTROL_ACCESO:
                InfoServidorDialog infoServidorDialog = new InfoServidorDialog(
                    getFrames()[0], false, ContextoPruebas.INSTANCE.getAccessControl());
                infoServidorDialog.setVisible(true);
                break;
        }
    }//GEN-LAST:event_infoServidorButtonActionPerformed

    private void closeMensajeUsuario(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_closeMensajeUsuario
        mostrarMensajeUsuario(null);
    }//GEN-LAST:event_closeMensajeUsuario

    private void comoAuditarMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comoAuditarMenuItemActionPerformed

    }//GEN-LAST:event_comoAuditarMenuItemActionPerformed

    private void informacionSistemaMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_informacionSistemaMenuItemActionPerformed
        String theString = new Scanner(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("InformacionSistema.html")).useDelimiter("\\A").next();
        MensajeDialog mensajeDialog = new MensajeDialog(mainFrame, true,
                new Dimension(600, 600));
        mensajeDialog.setMessage(theString, "Información del sistema");
    }//GEN-LAST:event_informacionSistemaMenuItemActionPerformed

    private void razonesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_razonesMenuItemActionPerformed
        String theString = new Scanner(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("RazonesParaSistemaVotacionOnline.html")).useDelimiter("\\A").next();
        MensajeDialog mensajeDialog = new MensajeDialog(mainFrame, true,
                new Dimension(600, 550));
        mensajeDialog.setMessage(theString, "Razones para un sistema de votación en red");
    }//GEN-LAST:event_razonesMenuItemActionPerformed

    private void abrirArchivoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_abrirArchivoMenuItemActionPerformed
        logger.debug("Abriendo archivo firmado");
        VisualizadorDeEventoFirmadoDialog visualizador =
                        new VisualizadorDeEventoFirmadoDialog(mainFrame, false);
        File file = visualizador.abrirArchivoFirmado();
        if (file != null) visualizador.setVisible(true);
    }//GEN-LAST:event_abrirArchivoMenuItemActionPerformed

    public void setControlAccesoTextFieldEditable(boolean isEditable) {
        controlAccesoTextField.setEditable(isEditable);
    }
    
    public void cargarControlAcceso() {
        estado = Estado.CONECTANDO;
        infoServidorButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/loading.gif")));
        infoServidorButton.setText("Conectando");
        String urlServidor = StringUtils.prepararURL(controlAccesoTextField.getText());
        controlAccesoTextField.setText(urlServidor);
        String urlInfoServidor = ContextoPruebas.getURLInfoServidor(urlServidor);
        InfoGetter infoGetter = new InfoGetter(
                ACCESS_CONTROL_GETTER, urlInfoServidor, null);
        Future<Respuesta> future = ContextoPruebas.INSTANCE.submit(infoGetter);
        queue.add(future);
        tareaEnEjecucion = future;
    }
    
    public void cargarCentroControl(String urlCentroControl){
        String urlServidor = StringUtils.prepararURL(urlCentroControl);
        String urlInfoServidor = ContextoPruebas.getURLInfoServidor(urlServidor);
        InfoGetter infoGetter = new InfoGetter(
                CONTROL_CENTER_GETTER, urlInfoServidor, null);
        controlAccesoTextField.setText(controlAccesoTextField.getText().trim());
        Future<Respuesta> future = ContextoPruebas.INSTANCE.submit(infoGetter);
        queue.add(future);
        tareaEnEjecucion = future;      
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }                 
                MainFrame mainFrame = new MainFrame();
                mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                mainFrame.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem abrirArchivoMenuItem;
    private javax.swing.JMenu archivoMenu;
    private javax.swing.JMenu ayudaMenu;
    private javax.swing.JLabel closeLabel;
    private javax.swing.JMenuItem comoAuditarMenuItem;
    private javax.swing.JLabel controlAccesoLabel;
    private javax.swing.JTextField controlAccesoTextField;
    private javax.swing.JLabel estadoControlAccesoLabel;
    private org.sistemavotacion.test.panel.FirmasPanel firmasPanel1;
    private javax.swing.JButton infoServidorButton;
    private javax.swing.JMenuItem informacionSistemaMenuItem;
    private javax.swing.JLabel mensajeLabel;
    private javax.swing.JPanel mensajePanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem razonesMenuItem;
    private javax.swing.JMenuItem salirMenuItem;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JPanel urlsPanel;
    private org.sistemavotacion.test.panel.VotacionesPanel votacionesPanel;
    private org.sistemavotacion.test.panel.FirmasPanel votacionesTabPanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void keyTyped(KeyEvent ke) { }

    @Override
    public void keyPressed(KeyEvent ke) {
        int key = ke.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            Toolkit.getDefaultToolkit().beep();
            infoServidorButton.doClick();
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) { }
    
    @Override
    public void focusGained(FocusEvent fe) {   }

    @Override
    public void focusLost(FocusEvent fe) {
        /*logger.debug("focusLost");
        if(controlAccesoTextField.equals(fe.getSource())) {
            if(!urlControlAcceso.equals(controlAccesoTextField.getText())) {
                logger.debug("foco perdido en campo control de acceso - texto ha cambiado" );
                logger.debug("controlAccesoTextField.getText(): " + controlAccesoTextField.getText());
            } else {
                logger.debug("foco perdido en campo control de acceso - mismo texto");
            }
            urlControlAcceso = controlAccesoTextField.getText();
        }*/
    }
    
    public void packMainFrame() {
        if(SwingUtilities.isEventDispatchThread()) {
            pack();
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {pack();}
                });
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }
    
    public void mostrarMensajeUsuario(final String mensaje) {
        logger.debug("mostrarMensajeUsuario :" + mensaje);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if(mensaje == null || "".equals(mensaje)) {
                     mensajeLabel.setText("");
                     mensajePanel.setVisible(false);
                } else {
                    mensajeLabel.setText(mensaje);
                    mensajePanel.setVisible(true);
                }
                pack();
            }
        });
    }

}

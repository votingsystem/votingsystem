package org.sistemavotacion.herramientavalidacion;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.sistemavotacion.herramientavalidacion.MetaInfoDeEvento;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/HerramientaValidacionCopiasDeSeguridad/blob/master/licencia.txt
*/
public class InformacionEventoPanel extends javax.swing.JPanel {
    
    private static Logger logger = LoggerFactory.getLogger(InformacionEventoPanel.class);

    /** Creates new form InformacionEventoPanel */
    public InformacionEventoPanel() {
        initComponents();
        valorURLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                URI uri = null;
                try {
                    uri = new URI("");
                } catch (URISyntaxException ex) {
                    logger.error(ex.getMessage(), ex);
                }
                open(uri);
            }
        });
    }
    
    public InformacionEventoPanel(File metaInfFile) {
        try {
            initComponents();
            String metaInfoString = FileUtils.getStringFromFile(metaInfFile);
            MetaInfoDeEvento metaInfo = MetaInfoDeEvento.parse(metaInfoString);
            valorAsuntoLabel.setText(metaInfo.getAsunto());
            String valorTipoEvento = null;
            switch (metaInfo.getTipoEvento()) {
                case EVENTO_FIRMA:
                    valorTipoEvento = AppletHerramienta.getResourceBundle().
                        getString("manfiestoLabel");
                    documentosFirmadosLabel.setText("<html><b>" + 
                            AppletHerramienta.getResourceBundle().
                            getString("numeroFirmasLabel") + ": </b></html>");
                    valorNumeroDocumentosLabel.setText(String.valueOf(metaInfo.getNumeroFirmas()));
                    break;
                case EVENTO_RECLAMACION:
                    valorTipoEvento = AppletHerramienta.getResourceBundle().
                        getString("reclamacionLabel");
                    documentosFirmadosLabel.setText("<html><b>" + 
                            AppletHerramienta.getResourceBundle().
                            getString("numeroFirmasLabel") + ": </b></html>");
                    valorNumeroDocumentosLabel.setText(String.valueOf(metaInfo.getNumeroFirmas()));
                    break;
                case EVENTO_VOTACION:
                    valorTipoEvento = AppletHerramienta.getResourceBundle().
                        getString("votacionLabel");
                    documentosFirmadosLabel.setText("<html><b>" + 
                            AppletHerramienta.getResourceBundle().
                            getString("numeroVotosLabel") + ": </b></html>");
                    valorNumeroDocumentosLabel.setText(String.valueOf(metaInfo.getNumeroVotos()));
                    solicitudesAccesoLabel.setText("<html><b>" + 
                            AppletHerramienta.getResourceBundle().
                        getString("solicitudesAccesoLabel") + ": </b></html>");
                    valorSolicitudesAccesoLabel.setText(String.valueOf(
                            metaInfo.getNumeroSolicitudesAcceso()));                            
                    break;                    
            }
            tipoEventoLabel.setText("<html><h2> -  " + valorTipoEvento + " -</h2></html>");
            
            final String metaInfoURL = metaInfo.getURL();
            valorURLButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    URI uri = null;
                    try {
                        uri = new URI(metaInfoURL);
                    } catch (URISyntaxException ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                    open(uri);
                }
            });
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }        
    }
    

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        asuntoLabel = new javax.swing.JLabel();
        valorAsuntoLabel = new javax.swing.JLabel();
        tipoEventoLabel = new javax.swing.JLabel();
        documentosFirmadosLabel = new javax.swing.JLabel();
        valorNumeroDocumentosLabel = new javax.swing.JLabel();
        valorURLButton = new javax.swing.JButton();
        solicitudesAccesoLabel = new javax.swing.JLabel();
        valorSolicitudesAccesoLabel = new javax.swing.JLabel();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/herramientavalidacion/Bundle"); // NOI18N
        asuntoLabel.setText(bundle.getString("InformacionEventoPanel.asuntoLabel.text")); // NOI18N
        asuntoLabel.setName("asuntoLabel"); // NOI18N

        valorAsuntoLabel.setText(bundle.getString("InformacionEventoPanel.valorAsuntoLabel.text")); // NOI18N
        valorAsuntoLabel.setName("valorAsuntoLabel"); // NOI18N

        tipoEventoLabel.setText(bundle.getString("InformacionEventoPanel.tipoEventoLabel.text")); // NOI18N
        tipoEventoLabel.setName("tipoEventoLabel"); // NOI18N

        documentosFirmadosLabel.setText(bundle.getString("InformacionEventoPanel.documentosFirmadosLabel.text")); // NOI18N
        documentosFirmadosLabel.setName("documentosFirmadosLabel"); // NOI18N

        valorNumeroDocumentosLabel.setText(bundle.getString("InformacionEventoPanel.valorNumeroDocumentosLabel.text")); // NOI18N
        valorNumeroDocumentosLabel.setName("valorNumeroDocumentosLabel"); // NOI18N

        valorURLButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/agt_internet.png"))); // NOI18N
        valorURLButton.setText(bundle.getString("InformacionEventoPanel.valorURLButton.text")); // NOI18N
        valorURLButton.setName("valorURLButton"); // NOI18N

        solicitudesAccesoLabel.setText(bundle.getString("InformacionEventoPanel.solicitudesAccesoLabel.text")); // NOI18N
        solicitudesAccesoLabel.setName("solicitudesAccesoLabel"); // NOI18N

        valorSolicitudesAccesoLabel.setText(bundle.getString("InformacionEventoPanel.valorSolicitudesAccesoLabel.text")); // NOI18N
        valorSolicitudesAccesoLabel.setName("valorSolicitudesAccesoLabel"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(asuntoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(valorAsuntoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 607, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tipoEventoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 431, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(valorURLButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(documentosFirmadosLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                            .addComponent(solicitudesAccesoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(valorSolicitudesAccesoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 406, Short.MAX_VALUE)
                            .addComponent(valorNumeroDocumentosLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 406, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tipoEventoLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(asuntoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valorAsuntoLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(documentosFirmadosLabel)
                    .addComponent(valorNumeroDocumentosLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(solicitudesAccesoLabel)
                    .addComponent(valorSolicitudesAccesoLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 157, Short.MAX_VALUE)
                .addComponent(valorURLButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel asuntoLabel;
    private javax.swing.JLabel documentosFirmadosLabel;
    private javax.swing.JLabel solicitudesAccesoLabel;
    private javax.swing.JLabel tipoEventoLabel;
    private javax.swing.JLabel valorAsuntoLabel;
    private javax.swing.JLabel valorNumeroDocumentosLabel;
    private javax.swing.JLabel valorSolicitudesAccesoLabel;
    private javax.swing.JButton valorURLButton;
    // End of variables declaration//GEN-END:variables

    private static void open(URI uri) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(uri);
            } catch (IOException e) {
            // TODO: error handling
            }
        } else {
        // TODO: error handling
        }
    }

}

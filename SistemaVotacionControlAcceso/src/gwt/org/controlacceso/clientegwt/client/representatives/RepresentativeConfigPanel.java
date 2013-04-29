package org.controlacceso.clientegwt.client.representatives;

import java.util.List;
import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.dialogo.ErrorDialog;
import org.controlacceso.clientegwt.client.modelo.ConsultaEventosSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.EventosSistemaVotacionJso;
import org.controlacceso.clientegwt.client.panel.BarraNavegacion;
import org.controlacceso.clientegwt.client.panel.PanelEncabezado;
import org.controlacceso.clientegwt.client.panel.PanelEvento;
import org.controlacceso.clientegwt.client.util.RequestHelper;
import org.controlacceso.clientegwt.client.util.ServerPaths;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class RepresentativeConfigPanel extends Composite {

    private static Logger logger = Logger.getLogger("RepresentativeConfigPanel");

    @UiField PushButton removeRepresentativeButton;
    @UiField PushButton newRepresentativeButton;
 

    private static RepresentativeConfigPanelUiBinder uiBinder = GWT.create(RepresentativeConfigPanelUiBinder.class);

    interface RepresentativeConfigPanelUiBinder extends UiBinder<VerticalPanel, RepresentativeConfigPanel> { }

    public RepresentativeConfigPanel() {
    	initWidget(uiBinder.createAndBindUi(this));
    }

    @UiHandler("newRepresentativeButton")
    void onClickNewRepresentativeButton(ClickEvent e) {
    	History.newItem(HistoryToken.NEW_REPRESENTATIVE.toString());
    }
    
 
    
    private void showErrorDialog (String text, String body) {
    	ErrorDialog errorDialog = new ErrorDialog();
    	errorDialog.show(text, body);	
    }


}
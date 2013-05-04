package org.controlacceso.clientegwt.client.representatives;

import java.util.List;
import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.dialogo.ConfirmacionListener;
import org.controlacceso.clientegwt.client.dialogo.NifDialog;
import org.controlacceso.clientegwt.client.dialogo.ResultDialog;
import org.controlacceso.clientegwt.client.modelo.RepresentativesMapJso;
import org.controlacceso.clientegwt.client.modelo.UsuarioJso;
import org.controlacceso.clientegwt.client.panel.BarraNavegacion;
import org.controlacceso.clientegwt.client.util.RequestHelper;
import org.controlacceso.clientegwt.client.util.ServerPaths;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class RepresentativesPanel extends Composite 
	implements BarraNavegacion.Listener, ConfirmacionListener {

    private static Logger logger = Logger.getLogger("RepresentativesPanel");

    public static final int CHECK_REPRESENTATIVE = 0;
  
    @UiField BarraNavegacion barraNavegacion;
    @UiField VerticalPanel representativesPanelContainer;
    @UiField VerticalPanel panelBarrarProgreso;
    @UiField FlowPanel representativesPanel;
    @UiField HTML emptySearchLabel;
    @UiField PushButton checkRepresentativeButton;
    @UiField PushButton representativeConfigButton;
 
    private String userNif = null;

    private static RepresentativesPanelUiBinder uiBinder = GWT.create(RepresentativesPanelUiBinder.class);

    interface RepresentativesPanelUiBinder extends UiBinder<VerticalPanel, RepresentativesPanel> { }

    public RepresentativesPanel() {
    	initWidget(uiBinder.createAndBindUi(this));
    	representativesPanel.clear();
        //representativesPanel.setVisible(false);
    	representativesPanel.setVisible(true);
        emptySearchLabel.setVisible(false);
        panelBarrarProgreso.setVisible(false);
    }

    @UiHandler("representativeConfigButton")
    void onClickRepresentativeConfigButton(ClickEvent e) {
    	History.newItem(HistoryToken.REPRESENTATIVE_CONFIG.toString());
    }
    
    @UiHandler("checkRepresentativeButton")
    void onClickCheckRepresentativeButton(ClickEvent e) {
    	NifDialog nifDialog = new NifDialog(CHECK_REPRESENTATIVE, this, 
    			Constantes.INSTANCIA.checkRepresentativeCaption(),
    			Constantes.INSTANCIA.checkRepresentativeMsg());
    	nifDialog.show();
	}
    
    private class RepresentativesRequestCallback implements RequestCallback {

        @Override
        public void onError(Request request, Throwable exception) {
        	showErrorDialog (Constantes.INSTANCIA.exceptionLbl(), 
        			exception.getMessage());                
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
            if (response.getStatusCode() == Response.SC_OK) {
            	RepresentativesMapJso representativesMap = 
            			RepresentativesMapJso.create(response.getText());
            	updateRepresentativesMap(representativesMap);
            } else {
            	if(response.getStatusCode() == 0) {//Magic Number!!! -> network problem
            		showErrorDialog (Constantes.INSTANCIA.errorLbl() , 
            				Constantes.INSTANCIA.networkERROR());
            	} else showErrorDialog (String.valueOf(
            			response.getStatusCode()), response.getText());
            }
        }

    }
    
    private void showErrorDialog (String text, String body) {
		ResultDialog resultDialog = new ResultDialog();
		resultDialog.show(text, body, Boolean.FALSE);	
    }

	public void updateRepresentativesMap(RepresentativesMapJso representativesMap) {
		logger.info("- updateRepresentativesMap - ");
        barraNavegacion.addListener(this, representativesMap.getOffset(), 
        		Constantes.REPRESENTATIVES_RANGE, 
        		representativesMap.getRepresentativesTotalNumber());
	    List<UsuarioJso> representativesList = 
	    		representativesMap.getRepresentativesList();
		logger.info("- updateRepresentativesMap - representativesList.size(): " + 
				representativesList.size());
	    representativesPanel.clear();
		if (representativesList != null &&
				representativesList.size() > 0) {
			emptySearchLabel.setVisible(false);
			for(UsuarioJso usuario: representativesList) {
				RepresentativeSmallPanel representativePanel = 
						new RepresentativeSmallPanel(usuario);
				representativesPanel.add(representativePanel);
			}
	    } else {
	        emptySearchLabel.setVisible(true);
	    	barraNavegacion.setVisible(false);
	    }
		panelBarrarProgreso.setVisible(false);
		representativesPanel.setVisible(true);
		barraNavegacion.setVisible(true);
	}

	@Override
	public void gotoPage(int offset, int range) {
		logger.info("--- gotoPage ---");
		RequestHelper.doGet(ServerPaths.getRepresentativesUrl(range, offset), 
				new RepresentativesRequestCallback());
		panelBarrarProgreso.setVisible(true);
		representativesPanel.setVisible(false);
		barraNavegacion.setVisible(false);
	}

	@Override
	public void confirmed(Integer id, Object param) {
		logger.info(" - confirmed + id: " + id + " - param: " + param);
		switch(id) {
			case CHECK_REPRESENTATIVE:
				logger.info(" - Lanzando comprobación de representante " + param);
				userNif = (String)param;
				RequestHelper.doGet(ServerPaths.getRepresentativeByUserNif(userNif), 
						new RepresentativeCheckRequestCallback());
				break;
		}
	}

    public class RepresentativeCheckRequestCallback implements RequestCallback {

        @Override
        public void onError(Request request, Throwable exception) {
        	showErrorDialog(Constantes.INSTANCIA.exceptionLbl(), 
        			exception.getMessage());                
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
			ResultDialog resultDialog = new ResultDialog();
            if (response.getStatusCode() == Response.SC_OK) {
            	logger.info("response.getText(): " + response.getText());
            	JSONValue jsonValue = JSONParser.parseLenient(response.getText());
            	JSONObject jsonObj = jsonValue.isObject();
            	Double representativeId = jsonObj.get("representativeId").isNumber().doubleValue();
            	String representativeName = jsonObj.get("representativeName").isString().stringValue();
            	String representativeURL = ServerPaths.getRepresentativeDetailsUrl(representativeId.intValue());
            	resultDialog.show(null, Constantes.INSTANCIA.checkrepresentativeResultMessage(
            			userNif, representativeURL, representativeName), Boolean.TRUE);
            } else {
            	logger.info("ERROR: " +response.getStatusCode() 
            			+ " - message: " + response.getText());
            	if(response.getStatusCode() == 0) {//Magic Number!!! -> network problem
            		resultDialog.show(Constantes.INSTANCIA.errorLbl() , 
            				Constantes.INSTANCIA.networkERROR(), Boolean.FALSE);
            	} else {
            		resultDialog.show(Constantes.INSTANCIA.errorLbl(), 
            				response.getText(), Boolean.FALSE);
            	} 
            }
        }

    }

}
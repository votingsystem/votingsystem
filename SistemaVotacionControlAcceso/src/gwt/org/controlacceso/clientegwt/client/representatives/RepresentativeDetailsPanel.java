package org.controlacceso.clientegwt.client.representatives;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.PuntoEntrada;
import org.controlacceso.clientegwt.client.PuntoEntradaEditor;
import org.controlacceso.clientegwt.client.dialogo.ConfirmacionListener;
import org.controlacceso.clientegwt.client.dialogo.DialogoConfirmacion;
import org.controlacceso.clientegwt.client.dialogo.DialogoOperacionEnProgreso;
import org.controlacceso.clientegwt.client.dialogo.DialogoSolicitudEmail;
import org.controlacceso.clientegwt.client.dialogo.ErrorDialog;
import org.controlacceso.clientegwt.client.dialogo.SolicitanteEmail;
import org.controlacceso.clientegwt.client.evento.BusEventos;
import org.controlacceso.clientegwt.client.evento.EventGWTRepresentativeDetails;
import org.controlacceso.clientegwt.client.evento.EventoGWTMensajeClienteFirma;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso;
import org.controlacceso.clientegwt.client.modelo.UsuarioJso;
import org.controlacceso.clientegwt.client.modelo.MensajeClienteFirmaJso.Operacion;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.DateUtils;
import org.controlacceso.clientegwt.client.util.RequestHelper;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import org.controlacceso.clientegwt.client.votaciones.DialogoAnulacionSolicitudAcceso;
import org.controlacceso.clientegwt.client.votaciones.DialogoResultadoVotacion;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

public class RepresentativeDetailsPanel extends Composite implements 
	EventGWTRepresentativeDetails.Handler, EventoGWTMensajeClienteFirma.Handler, 
	ConfirmacionListener, SolicitanteEmail {
	
    private static Logger logger = Logger.getLogger("RepresentativeDetailsPanel");

	private static RepresentativeSmallPanelUiBinder uiBinder = GWT
			.create(RepresentativeSmallPanelUiBinder.class);

	interface RepresentativeSmallPanelUiBinder extends UiBinder<Widget, RepresentativeDetailsPanel> {}
	
    interface Style extends CssResource {
    }

	@UiField Label representativeNameLabel;
    @UiField Style style;
	@UiField Image image;
	@UiField Label representationsNumber;
	@UiField HTMLPanel info;
	@UiField HorizontalPanel detailsPanel;
	@UiField HorizontalPanel historyPanel;
	@UiField PushButton requestRepresentativeAccreditations;
	@UiField PushButton requestRepresentativeVotingHistoryButton;
	
	private static Map<Integer, UsuarioJso> representativesMap = 
			new HashMap<Integer, UsuarioJso>();
	
	private UsuarioJso representative;
    private DialogoOperacionEnProgreso dialogoProgreso;
    private String dateFromStr = null;
    private String dateToStr = null;
    private String selectedDateStr = null;
    private String email = null;
    private String representativeName;
	private static final int CONFIRM_REPRESENTATIVE_SELECTION_DIALOG = 0;
	private static final int REPRESENTATIVE_VOTING_HISTORY_DIALOG = 1;
	private static final int REPRESENTATIVE_ACCREDITATIONS_DIALOG = 2;	
	private static final long ONE_YEARS_IN_MILLI_SEC = 365l*24l*3600l*1000l;

	
	public RepresentativeDetailsPanel(Integer id) {
		logger.info("RepresentativeDetailsPanel - representativeId: " + id);
		initWidget(uiBinder.createAndBindUi(this));
		//DOM.setStyleAttribute(label.getElement(),"border", "1px solid #00f");
        sinkEvents(Event.ONCLICK);
        //sinkEvents(Event.ONMOUSEOVER);
        //sinkEvents(Event.ONMOUSEOUT);
        BusEventos.addHandler(EventGWTRepresentativeDetails.TYPE, this);
        BusEventos.addHandler(
        		EventoGWTMensajeClienteFirma.TYPE, this);
		representative = representativesMap.get(id);
		historyPanel.setVisible(false);
		if(representative == null) {
			RequestHelper.doGet(ServerPaths.getRepresentativeDetailesMapUrl(id), 
	        		new ServerRequestCallback());
		} else setRepresentativeDetails(representative);
	}
    
	public void onBrowserEvent(Event event){
		switch(DOM.eventGetType(event)) {
			case Event.ONCLICK:
	    		logger.info("onBrowserEvent - onBrowserEvent");
	       		break;
		}
	}
	
    @UiHandler("selectRepresentativeButton")
    void handleSelectRepresentativeButton(ClickEvent e) {
    	representativeName = representative.getNombre() + " " + 
    			representative.getPrimerApellido();
    	DialogoConfirmacion dialogoConfirmacion = new DialogoConfirmacion(
    			CONFIRM_REPRESENTATIVE_SELECTION_DIALOG, this);
    	dialogoConfirmacion.show(Constantes.INSTANCIA.selectRepresentativeCaption(), 
    			Constantes.INSTANCIA.selectRepresentativeConfirmMsg(representativeName));
    }
	
  	
	@Override
	public void setRepresentativeDetails(UsuarioJso usuario) {
		logger.info(" - setRepresentativeDetails - id: " + usuario.getId());
		this.representative = usuario;
		representativesMap.put(usuario.getId(), usuario);
		image.setUrl(representative.getImageURL());
		representativeNameLabel.setText(representative.getNombre() + " " + representative.getPrimerApellido());

		representationsNumber.setText(Constantes.INSTANCIA.representationsNumberLbl(
				representative.getRepresentationsNumber()));
		info.add(new HTML(representative.getInfo()));
		//info.setHTML(representative.getInfo());
		
	}

	@UiHandler("tabPanel")
	void onTabSelection(SelectionEvent<Integer> event) {
		logger.info("onTabSelection - event.getSelectedItem: " + event.getSelectedItem());
		if (event.getSelectedItem() == 0) {
			detailsPanel.setVisible(true);
			historyPanel.setVisible(false);
		}
		if (event.getSelectedItem() == 1) {
			detailsPanel.setVisible(false);
			historyPanel.setVisible(true);
		}
	}
    
    @UiHandler("requestRepresentativeAccreditations")
    void handleRequestRepresentativeAccreditationsButton(ClickEvent e) {
    	DialogoConfirmacion dialogoConfirmacion = new DialogoConfirmacion(
    			REPRESENTATIVE_ACCREDITATIONS_DIALOG, this);
    	dialogoConfirmacion.setDates(DateUtils.getTodayDate(), null);
    	dialogoConfirmacion.show(Constantes.INSTANCIA.
    			requestRepresentativeAccreditationsCaption(), 
    			Constantes.INSTANCIA.requestRepresentativeAccreditationsMsg());
    }
    
    @UiHandler("requestRepresentativeVotingHistoryButton")
    void handleRequestRepresentativeVotingHistoryButton(ClickEvent e) {
    	DialogoConfirmacion dialogoConfirmacion = new DialogoConfirmacion(
    			REPRESENTATIVE_VOTING_HISTORY_DIALOG, this);
    	dialogoConfirmacion.setDates(new Date(System.currentTimeMillis() -
    			ONE_YEARS_IN_MILLI_SEC), DateUtils.getTodayDate());
    	dialogoConfirmacion.show(Constantes.INSTANCIA.
    			requestRepresentativeVotingHistoryCaption(),Constantes.INSTANCIA.
    			requestRepresentativeVotingHistoryMsg());
    }
	
    
    
	@Override
	public void procesarMensajeClienteFirma(MensajeClienteFirmaJso mensaje) {
		logger.info(" - procesarMensajeClienteFirma - mensajeClienteFirma: " + mensaje.toJSONString());
		switch(mensaje.getOperacionEnumValue()) {
			case SELECT_REPRESENTATIVE:
				if(MensajeClienteFirmaJso.SC_OK == mensaje.getCodigoEstado()) {
					representationsNumber.setText(Constantes.INSTANCIA.representationsNumberLbl(
							representative.getRepresentationsNumber() + 1));
					dialogoProgreso.showFinishMessage(Constantes.INSTANCIA.
							selectRepresentativeCaption(), 
							Constantes.INSTANCIA.selectRepresentativeOKMsg(representativeName), Boolean.TRUE);
				} else {
					dialogoProgreso.showFinishMessage(Constantes.INSTANCIA.
							selectRepresentativeCaption(), 
			    			mensaje.getMensaje(), Boolean.FALSE);
				}
				break;
			case REPRESENTATIVE_ACCREDITATIONS_REQUEST:
				if(MensajeClienteFirmaJso.SC_OK == mensaje.getCodigoEstado()) {
					dialogoProgreso.showFinishMessage(Constantes.INSTANCIA.
			    			requestRepresentativeAccreditationsCaption(), 
			    			Constantes.INSTANCIA.backupRequestOK(email), Boolean.TRUE);
				} else {
					dialogoProgreso.showFinishMessage(Constantes.INSTANCIA.
			    			requestRepresentativeAccreditationsCaption(), 
			    			mensaje.getMensaje(), Boolean.FALSE);
				}
				break;
			case REPRESENTATIVE_VOTING_HISTORY_REQUEST:
				if(MensajeClienteFirmaJso.SC_OK == mensaje.getCodigoEstado()) {
					dialogoProgreso.showFinishMessage(Constantes.INSTANCIA.
			    			requestRepresentativeVotingHistoryCaption(), 
			    			Constantes.INSTANCIA.backupRequestOK(email), Boolean.TRUE);
				} else {
					dialogoProgreso.showFinishMessage(Constantes.INSTANCIA.
			    			requestRepresentativeAccreditationsCaption(), 
			    			mensaje.getMensaje(), Boolean.FALSE);
				}
				break;
			default:
				break;
		}
		
	}
	
    private void showErrorDialog (String text, String body) {
    	ErrorDialog errorDialog = new ErrorDialog();
    	errorDialog.show(text, body);	
    }
    
    public class ServerRequestCallback implements RequestCallback {

        @Override
        public void onError(Request request, Throwable exception) {
        	new ErrorDialog().show (Constantes.INSTANCIA.exceptionLbl(), 
        			exception.getMessage());                
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
            if (response.getStatusCode() == Response.SC_OK) {
            	logger.info("response.getText(): " + response.getText());
                UsuarioJso usuario = UsuarioJso.create(response.getText());
                setRepresentativeDetails(usuario);
                /*if(usuario == null) {
                	History.newItem(HistoryToken.REPRESENTATIVES_PAGE.toString());
                } else {
                	BusEventos.fireEvent(new EventGWTRepresentativeDetails(usuario));
                }*/
            } else {
            	if(response.getStatusCode() == 0) {//Magic Number!!! -> network problem
            		showErrorDialog (Constantes.INSTANCIA.errorLbl() , 
            				Constantes.INSTANCIA.networkERROR());
            	} else {
            		showErrorDialog (String.valueOf(
                			response.getStatusCode()), response.getText());
                	History.newItem(HistoryToken.REPRESENTATIVES_PAGE.toString());
            	} 
            }
        }

    }
    
	private void setAppletOperationMode(boolean publicando) {
		if(publicando) {
			if(PuntoEntradaEditor.INSTANCIA != null && 
					PuntoEntradaEditor.INSTANCIA.getAndroidClientLoaded()) {
				Browser.showProgressDialog(Constantes.INSTANCIA.publishingDocument());
			} else {
				dialogoProgreso = new DialogoOperacionEnProgreso();
				dialogoProgreso.show();
			}
		} else {
			if(PuntoEntradaEditor.INSTANCIA != null && 
					PuntoEntradaEditor.INSTANCIA.getAndroidClientLoaded()) {
				MensajeClienteFirmaJso mensaje = MensajeClienteFirmaJso.create();
				mensaje.setCodigoEstado(MensajeClienteFirmaJso.SC_PING);
				Browser.setAndroidClientMessage(mensaje.toJSONString());
			} else if(dialogoProgreso != null) dialogoProgreso.hide();
		}
	}

	@Override
	public void confirmed(Integer confirmDialogId, Object param) {
		DialogoSolicitudEmail dialogoEmail = null;
		switch (confirmDialogId) {
			case CONFIRM_REPRESENTATIVE_SELECTION_DIALOG:
				logger.info("confirmed representative selection - representativeId: " + 
						representative.getId());
		    	MensajeClienteFirmaJso mensajeClienteFirma = MensajeClienteFirmaJso.create();
		    	mensajeClienteFirma.setCodigoEstado(MensajeClienteFirmaJso.SC_PROCESANDO);
		    	mensajeClienteFirma.setOperacion(MensajeClienteFirmaJso.Operacion.
		    			SELECT_REPRESENTATIVE.toString());
		    	JSONObject contenidoFirma = new JSONObject();
		    	contenidoFirma.put("operation", new JSONString(MensajeClienteFirmaJso.Operacion.
		    			SELECT_REPRESENTATIVE.toString()));
		    	contenidoFirma.put("representativeNif", new JSONString(representative.getNif()));
		    	contenidoFirma.put("representativeName", new JSONString(representative.getNombre() + 
		    			" " + representative.getPrimerApellido()));
		    	
		    	mensajeClienteFirma.setContenidoFirma(contenidoFirma.getJavaScriptObject());
		    	mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlSelectRepresentative());
		    	if(PuntoEntrada.INSTANCIA != null &&
		    			PuntoEntrada.INSTANCIA.servidor != null) {
		    		mensajeClienteFirma.setNombreDestinatarioFirma(
		    				PuntoEntrada.INSTANCIA.servidor.getNombre());
		    	}
		    	mensajeClienteFirma.setAsuntoMensajeFirmado(
		    			Constantes.INSTANCIA.selectRepresentativeSubject());	
		    	mensajeClienteFirma.setRespuestaConRecibo(true);
		    	mensajeClienteFirma.setUrlTimeStampServer(ServerPaths.getUrlTimeStampServer());
				if(!Browser.isAndroid()) setAppletOperationMode(true);
				Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
				break;
			case REPRESENTATIVE_VOTING_HISTORY_DIALOG:
				Date[] dates = (Date[])param;
				dateFromStr = DateUtils.getStringFromDate(dates[0]) ;
				dateToStr = DateUtils.getStringFromDate(dates[1]);
				logger.info("confirmed -VOTING_HISTORY_DIALOG - " + 
						"dateFromStr: " + dateFromStr + " - dateToStr: " + dateToStr);
				dialogoEmail = new DialogoSolicitudEmail(REPRESENTATIVE_VOTING_HISTORY_DIALOG, this, 
		    					Constantes.INSTANCIA.emailForVotingHistoryMsg());
		    	dialogoEmail.show();
				break;
			case REPRESENTATIVE_ACCREDITATIONS_DIALOG:
				selectedDateStr = DateUtils.getStringFromDate((Date)param);
				logger.info("confirmed - ACCREDITATIONS_DIALOG - " + 
						"selectedDateStr: " + selectedDateStr);
		    	dialogoEmail = new DialogoSolicitudEmail(REPRESENTATIVE_ACCREDITATIONS_DIALOG, this, 
		    					Constantes.INSTANCIA.emailForVRepresentativeAccreditationsMsg());
		    	dialogoEmail.show();
				break;				
			default:
				logger.info("confirmed - unknown dialog: " + confirmDialogId);
		}
		
	}

	@Override
	public void procesarEmail(Integer dialogId, String email) {
		logger.info("--- procesarEmail");
		MensajeClienteFirmaJso mensajeClienteFirma = null;
		JSONObject contenidoFirma = null;
		this.email = email;
		switch(dialogId) {
			case REPRESENTATIVE_ACCREDITATIONS_DIALOG:
				mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
						Operacion.REPRESENTATIVE_ACCREDITATIONS_REQUEST.toString(), 
						MensajeClienteFirmaJso.SC_PROCESANDO);
		    	contenidoFirma = new JSONObject();
		    	contenidoFirma.put("operation", new JSONString(
		    			Operacion.REPRESENTATIVE_ACCREDITATIONS_REQUEST.toString()));
		    	contenidoFirma.put("representativeNif", new JSONString(representative.getNif()));
		    	contenidoFirma.put("representativeName", new JSONString(representative.getNombre() + 
		    			" " + representative.getPrimerApellido()));
		    	contenidoFirma.put("selectedDate", new JSONString(selectedDateStr));
		    	contenidoFirma.put("email", new JSONString(email));
		    	
		    	if(PuntoEntrada.INSTANCIA != null &&
		    			PuntoEntrada.INSTANCIA.servidor != null) {
		    		mensajeClienteFirma.setNombreDestinatarioFirma(
		    				PuntoEntrada.INSTANCIA.servidor.getNombre());
		    	}
		    	mensajeClienteFirma.setContenidoFirma(contenidoFirma.getJavaScriptObject());
		    	mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlRepresentativeAccreditations());
				mensajeClienteFirma.setEmailSolicitante(email);
				break;
			case REPRESENTATIVE_VOTING_HISTORY_DIALOG:
				mensajeClienteFirma = MensajeClienteFirmaJso.create(null, 
						Operacion.REPRESENTATIVE_VOTING_HISTORY_REQUEST.toString(), 
						MensajeClienteFirmaJso.SC_PROCESANDO);
				contenidoFirma = new JSONObject();
		    	contenidoFirma.put("operation", new JSONString(
		    			Operacion.REPRESENTATIVE_VOTING_HISTORY_REQUEST.toString()));
		    	contenidoFirma.put("representativeNif", new JSONString(representative.getNif()));
		    	contenidoFirma.put("representativeName", new JSONString(representative.getNombre() + 
		    			" " + representative.getPrimerApellido()));
		    	contenidoFirma.put("dateFrom", new JSONString(dateFromStr));
		    	contenidoFirma.put("dateTo", new JSONString(dateToStr));
		    	contenidoFirma.put("email", new JSONString(email));
		    	
		    	if(PuntoEntrada.INSTANCIA != null &&
		    			PuntoEntrada.INSTANCIA.servidor != null) {
		    		mensajeClienteFirma.setNombreDestinatarioFirma(
		    				PuntoEntrada.INSTANCIA.servidor.getNombre());
		    	}
		    	
		    	mensajeClienteFirma.setContenidoFirma(contenidoFirma.getJavaScriptObject());
		    	mensajeClienteFirma.setUrlEnvioDocumento(ServerPaths.getUrlRepresentativeVotingHistory());
				mensajeClienteFirma.setEmailSolicitante(email);
				break;
		}
		if(mensajeClienteFirma != null) {
			if(!Browser.isAndroid()) setAppletOperationMode(true);
			Browser.ejecutarOperacionClienteFirma(mensajeClienteFirma);
		}
	}
}
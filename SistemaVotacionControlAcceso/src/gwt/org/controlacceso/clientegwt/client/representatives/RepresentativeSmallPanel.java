package org.controlacceso.clientegwt.client.representatives;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.dialogo.ResultDialog;
import org.controlacceso.clientegwt.client.modelo.UsuarioJso;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class RepresentativeSmallPanel extends Composite implements LoadHandler {
	
    private static Logger logger = Logger.getLogger("RepresentativeSmallPanel");

	private static RepresentativeSmallPanelUiBinder uiBinder = GWT
			.create(RepresentativeSmallPanelUiBinder.class);

	interface RepresentativeSmallPanelUiBinder extends UiBinder<Widget, RepresentativeSmallPanel> {}
	
    interface Style extends CssResource {
        String imageWidthLimit();
        String imageHeightLimit();
        String image();
    }
    
	@UiField Label representativeNameLabel;
    @UiField Style style;
	@UiField VerticalPanel mainPanel;
	@UiField VerticalPanel imagePanel;
	private Image image;
	@UiField Label numRepresentations;
	
	private UsuarioJso representative;

	public RepresentativeSmallPanel(UsuarioJso representative) {
		initWidget(uiBinder.createAndBindUi(this));
		image = new Image();
		image.addLoadHandler(this);
		image.setStyleName(style.image(), true);
		image.setUrl(representative.getImageURL());
		imagePanel.clear();
		imagePanel.add(image);

		this.representative = representative;
		representativeNameLabel.setText(representative.getNombre() + " " + representative.getPrimerApellido());
		numRepresentations.setText(Constantes.INSTANCIA.numRepresentationsLbl(
				representative.getNumRepresentations()));

		//DOM.setStyleAttribute(label.getElement(),"border", "1px solid #00f");
        sinkEvents(Event.ONCLICK);
        //sinkEvents(Event.ONMOUSEOVER);
        //sinkEvents(Event.ONMOUSEOUT);

	}
	
	public void onBrowserEvent(Event event){
		switch(DOM.eventGetType(event)) {
			case Event.ONCLICK:
	    		logger.info("onBrowserEvent - onBrowserEvent");
	        	History.newItem(HistoryToken.REPRESENTATIVE_DETAILS.toString()
	        			+ "&representativeId=" +  new Integer(representative.getId()));
	       		break;
		}
	}
	
  	private void showRepresentativePopup(int clientX, int clientY) {
  		logger.info("showRepresentativePopup");
  	}

  	
    private class ServerRequestCallback implements RequestCallback {

        @Override
    	public void onError(Request request, Throwable exception) {
        	logger.log(Level.SEVERE, exception.getMessage(), exception);
        	ResultDialog resultDialog = new ResultDialog();
    		resultDialog.show(Constantes.INSTANCIA.exceptionLbl(), 
        			exception.getMessage(),Boolean.FALSE);         	
    	}

        @Override
    	public void onResponseReceived(Request request, Response response) {
			logger.log(Level.INFO, "StatusCode: " + response.getStatusCode() + 
					", Response Text: " + response.getText());
    	}

    }

	@Override
	public void onLoad(LoadEvent event) {
		logger.info("onLoad image");
		image.setStyleName(style.image(), false);
		if(image.getWidth() > image.getHeight()) {
			image.setStyleName(style.imageWidthLimit(), true);
		} else image.setStyleName(style.imageHeightLimit(), true);

	}

}
package org.controlacceso.clientegwt.client.panel;

import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.HistoryToken;
import org.controlacceso.clientegwt.client.HtmlTemplates;
import org.controlacceso.clientegwt.client.util.Browser;
import org.controlacceso.clientegwt.client.util.ServerPaths;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

public class PanelInferior extends Composite {
	
    private static Logger logger = Logger.getLogger("PanelInferior");

	private static PanelInferiorUiBinder uiBinder = GWT
			.create(PanelInferiorUiBinder.class);

	interface PanelInferiorUiBinder extends UiBinder<Widget, PanelInferior> {	}

    @UiField HTML administracionHTML;
    @UiField Anchor herramientaValidacionAnchor;

	public PanelInferior() {
		initWidget(uiBinder.createAndBindUi(this));
		administracionHTML.setHTML(HtmlTemplates.INSTANCIA.enlaceDatosAplicacion(
    			ServerPaths.getUrlDatosAplicacion(), 
    			Constantes.INSTANCIA.administracionLabel()));
		if(Browser.isAndroid()) {
			herramientaValidacionAnchor.setVisible(false);
		}else {
			herramientaValidacionAnchor.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					History.newItem(HistoryToken.DUMMY_TOKEN.toString());
					History.newItem(HistoryToken.HERRAMIENTA_VALIDACION.toString());
				}
			});	
		}

	}
    
}
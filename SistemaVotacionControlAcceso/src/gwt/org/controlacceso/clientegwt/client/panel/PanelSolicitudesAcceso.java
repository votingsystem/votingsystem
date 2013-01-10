package org.controlacceso.clientegwt.client.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class PanelSolicitudesAcceso extends Composite {

	private static PanelSolicitudesAccesoUiBinder uiBinder = GWT
			.create(PanelSolicitudesAccesoUiBinder.class);

	interface PanelSolicitudesAccesoUiBinder extends UiBinder<Widget, PanelSolicitudesAcceso> {}

	public PanelSolicitudesAcceso() {
		initWidget(uiBinder.createAndBindUi(this));
	}

}

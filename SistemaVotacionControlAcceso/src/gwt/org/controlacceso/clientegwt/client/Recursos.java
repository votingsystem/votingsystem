package org.controlacceso.clientegwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

public interface Recursos extends ClientBundle {
	
	public static final Recursos INSTANCIA = GWT.create(Recursos.class);
	
	@Source("xit.gif")
	ImageResource tagLabelIcon();
	@Source("xitHover.gif")
	ImageResource tagLabelHoverIcon();
	@Source("Indeterminate_small.gif")
	ImageResource indeterminate_small();
	@Source("edit_add.png")
	ImageResource editAdd();
	@Source("edit_remove.png")
	ImageResource editRemove();
	@Source("accept_16x16.png")
	ImageResource accept_16x16();
	@Source("cancel_16x16.png")
	ImageResource cancel_16x16();	
	
	@Source("feed.png")
	ImageResource feedImage();
	@Source("Indeterminate.gif")
	ImageResource indeterminate();
	@Source("info_16x16.png")
	ImageResource info_16x16_Image();
	@Source("BotonFirmar.png")
	ImageResource botonFirmar();
	@Source("BotonVotar.png")
	ImageResource botonVotar();
	@Source("BotonReclamar.png")
	ImageResource botonReclamar();
	@Source("BotonPendiente.png")
	ImageResource botonPendiente();
	@Source("BotonPendiente.png")
	ImageResource borrarOpcion();
	@Source("signature-bad_16x16.png")
	ImageResource signatureBad_16x16();
	@Source("view_detailed_16x16.png")
	ImageResource view_detailed_16x16();
	@Source("filesave_16x16.png")
	ImageResource filesave_16x16();
	@Source("accept_48x48.png")
	ImageResource accept_48x48();
	@Source("cancel_48x48.png")
	ImageResource cancel_48x48();
	
	@Source("documentacion.html")
    TextResource documentacion();
}

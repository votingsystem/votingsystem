package org.centrocontrol.clientegwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.resources.client.ClientBundle.Source;

public interface Recursos extends ClientBundle {
	
	public static final Recursos INSTANCIA = GWT.create(Recursos.class);
	
	@Source("xit.gif")
	ImageResource tagLabelIcon();
	@Source("xitHover.gif")
	ImageResource tagLabelHoverIcon();
	@Source("procesando.gif")
	ImageResource procesando();
	@Source("edit_add.png")
	ImageResource editAdd();
	@Source("edit_remove.png")
	ImageResource editRemove();
	@Source("accept_16x16.png")
	ImageResource accept_16x16();
	
	
	@Source("documentacion.html")
    TextResource documentacion();
	@Source("feed.png")
	ImageResource feedImage();
	@Source("Indeterminate.gif")
	ImageResource indeterminate();
	@Source("view_detailed_16x16.png")
	ImageResource view_detailed_16x16();
	@Source("filesave_16x16.png")
	ImageResource filesave_16x16();
	@Source("signature-bad_16x16.png")
	ImageResource signatureBad_16x16();	
	
	@Source("BotonVotar.png")
	ImageResource botonVotar();
	
}

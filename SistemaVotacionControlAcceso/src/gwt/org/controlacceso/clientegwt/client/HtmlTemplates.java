package org.controlacceso.clientegwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;

public interface HtmlTemplates extends SafeHtmlTemplates {
	
	public static final HtmlTemplates INSTANCIA = GWT.create(HtmlTemplates.class);

    @Template("<div class=\"asuntoEvento\">{0}</div>")
    SafeHtml asuntoDiv(String asunto);
    @Template("<div class=\"tipoAbierto\">{0}</div>")
    SafeHtml abiertoDiv(String qn);
    @Template("<div class=\"tipoCerrado\">{0}</div>")
    SafeHtml cerradoDiv(String qn);
    @Template("<div class=\"tipoPendienteAbrir\">{0}</div>")
    SafeHtml pendienteAbrirDiv(String qn);  
    @Template("<a href=\"{0}\" class=\"votarLabel\">{1}</a>")
    SafeHtml enlaceClienteVoto(String url, String accion); 
    @Template("<a class=\"mensajeSMIME\">{0}</a>")
    SafeHtml enlaceMensajeSMIME(String tipoMensaje); 
    @Template("<a href=\"{0}\">{1}</div>")
    SafeHtml enlaceHerramientaPublicacion(String url, String mensaje); 
    @Template("<a href=\"{0}\" >{0}</a>")
    SafeHtml estadoEvento(String mensaje);
    @Template("<a href=\"{0}\" class=\"textoPanelInferior\">{1}</a>")
    SafeHtml enlaceDatosAplicacion(String url, String textoEnlace);
}

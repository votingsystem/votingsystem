package org.centrocontrol.clientegwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface Constantes extends Messages{
    
		public static final Constantes INSTANCIA = GWT.create(Constantes.class);
		
		public static final int EVENTS_RANGE = 20;
	    public static final String ID_FRAME_APPLET = "frameClienteFirma";
	    public static final String ID_FRAME_HERRAMIENTA_PUBLICACION = "frameHerramientaPublicacion";

		public static final int MAX_NUM_CARACTERES_SUBJECT = 100;
	    
	    String mensajeErrorConCodigoEstado(String errorCode, String message);
	    
	    String mensajeError(String message);
	    
	    @Key("headerPanel.LargeTitle")
		String headerPanelLargeTitle(String nombreServidor);
		  
		@Key("headerPanel.title")
		String headerPanelTitle();
		  
		@Key("tituloEncabezado")
		String tituloEncabezado();
		
		String cargaClienteAndroidCaption();
		String emptyFieldException();
		String cargaClienteAndroidMsg(String urlAction, String urlAppAndroid);
		String androidOperationMsg(String urlAppAndroid);
        String horasLabel();
        String duracionLabel();
        String publicadoPorLabel();
        String abiertoLabel();
        String finalizadoLabel();
        String canceladoLabel();
        String searchLabel();
        
        String mensajeNavegadorSinJava();
        String captionNavegadorSinJava();
        
        String tipoCampoLabelOpciones();
        String tipoCampoLabelCamposReclamacion();
        
        String cancelAccessRequestCaption();
        String cancelAccessRequestMsg();
        String accessRequestCanceledMsg();
        
        String advancedSearchCaption();
        String advancedSearchMsg();
        String searchQueryTextLabel();
        String beginFromLabel();
        String beginToLabel();
        String endFromLabel();
        String endToLabel();
        String remainLabel();

        String stateSelectionLabel();
        String allVotingsListboxOption();
        String onlyOpenVotingsListboxOption();
        String onlyPendingVotingsListboxOption();
        String onlyClosedVotingsListboxOption();
        
        String searchPanelTitle();
        String emptySearchLabel();
        String searchStringLabel();
        String beginAfterLabel();
        String beginBeforeLabel();
        String beginBeforeLabel1();
        String endAfterLabel();
        String endBeforeLabel();
        String endBeforeLabel1();
        String pendingLabel();
        
        String votingFinishedMsg();
        String votingCanceledMsg();
        String votingPendingMsg();
        

		String centroControlNotSelected();
		
		String documentoSinOpciones();
		
		String documentoSinCambios();
		String documentoSinTexto();
		
		String centroControlNoValido();
	
		String conectandoConServidor();
		
		String asociacionCentroControlOK();
		String asociacionCentroControlError();
		String asociandoCentroControl();
		String confirmarBorradoOpcion();	
		String votoConfirmLabel(String value);
		String administracionDocumentoLabel();
                String herramientaValidacionLabel();
		
		
		
		String asuntoAsociarCentroControl();
		String asuntoPublicarVotacion();
		String publicacionVotacionOK();
		String publicacionVotacionERROR();
		
		String administracionLabel();
				
		String asuntoPublicarManifiesto();
		String asuntoFirmaReclamacion(String value);
        String asuntoCancelarDocumento(String value);
	        
		String publicacionManifiestoOK();
		String publicacionManifiestoERROR();
	
	
		String asuntoPublicarReclamacion();
		String publicacionReclamacionOK();
		String publicacionReclamacionERROR();
		
		String tituloLabel();
		String validoHastaLabel();
		String fechaInicioLabel();
		String fechaFinalLabel();
	
		String piePaginaFirmarReclamacion();
		String piePaginaPublicarReclamacion();
		String piePaginaFirmarDocumento();
		String piePaginaPublicarVotacion();
		String piePaginaVotar();
		String piePaginaPublicarManifiesto();
		
		String sistemaVotacionLabel();
		String opcionesVotacionLabel();
		String crearOpcionCaption();
		String crearOpcionInfoLabel();
		String asociarImagenOpcionLabel();
		String mensajeAsociarCentroControlLabel();
		String centroControlListBoxFirstItem();
		String seleccionarCentroControlLabel();
		String sistemaFirmasLabel();
		String sistemaReclamacionesLabel();
	
		String salirSinSalvarConfirmLabel();
		String salirSinFirmarConfirmLabel();
				
		String publicarReclamacionTitleLabel();
		String camposReclamacionLabel();
		String crearCampoCaption();
		String anyadirCampoReclamacionLabel();
		String publicarManifiestoTitleLabel();
		String publicarVotacionTitleLabel();
		String textoPopupCentrosControl();
		String urlCentroControlLabel();
		String textoPopupCampoReclamacion();
		
		String multiplesRepresentacionesLabel();
		String permisoSolicitudBackupLabel();
		String crearCampoInfoLabel();
		
		String mensajeErrorEmail();
		String solicitudEmailCaption();
		String solicitudEmailLabel();
		
		String infoCopiaSeguridadFirmasManifiesto();
		String infoCopiaSeguridadReclamaciones();
		String mensajeSolicitudCopiaSeguridad();
		String mensajeSolicitudCopiaSeguridadOK();
                String mensajeSolicitudCopiaSeguridadVotacionAbierta();
		
		String mensajeInfoManifiestoUnaFirma();
		String mensajeInfoManifiesto(int value);
		
		String mensajeInfoReclamacionUnaFirma();
		String mensajeInfoReclamacion(int value);
		
		String mensajeInfoVotacionUnaSolicitud();
		String mensajeInfoVotacion(int value);
		
		String fechaLimiteFirmaslabel();
		String fechaLimiteReclamacioneslabel();
		String fechaLimiteVotoslabel();
	
		String publicadoPorlabel();
		
		String manifiestoLabel();
		String reclamacionLabel();
		String votacionLabel();
		
		String singleSelectionMessage();
                String singleSelectionFinishedMessage();
		String salirSinVotarConfirmLabel();
		
		String cargaAppletCaption();
		String cargaAppletTextoChrome();
		String cargaAppletTextoAdvertenciaApplet();
	
		String anularVotoLabel();
		String guardarVotoLabel();
		String votoEnSistemaLabel();
		String solicitudAccesoRepetida();
		String resultadoVotacionOKCaption();
		String resultadoVotacionMessage(String asunto, String opcionSeleccionada);
		String textoPopupVisualizarVotoEnSistema(String urlVoto);
		String textoPopupGuardarVoto();
		String textoPopupAnularVoto();
		String textoConfirmAnularVoto(String contenidoOpcionSeleccionada);
		String mensajeAnulacionVotoERROR();
		String mensajeAnulacionVotoOK();
	
		String mensajeGuardarReciboERROR();
		String mensajeGuardarReciboOK(String rutaArchivo);
                
        String numSolicitudesAccesoLabel();
        String numSolicitudesAccesoOKLabel();
        String numSolicitudesAccesoANULADASLabel();
        String numVotosLabel();
        String numVotosOKLabel();
        String numVotosANULADOSLabel();
        String recuentoVotosLabel();
        String solicitudCopiaSeguridadLabel();      
        String detallesRecuentoLabel();        
	
        String checkBoxAnularDocumento();
        String checkBoxCancelarDocumento();
        
        String dialogoProgresoCaption();
        String cargandoLabel();
        String mensajeCancelarEvento(String tipoDocumento);
        String cancelarDocumentoCheckBoxMsg();
        String firmaLabelCancelarMsg();
        String votacionLabelCancelarMsg();
        String reclamacionLabelCancelarMsg();
        String cancelarManifiestoCaption();
        String cancelarReclamacionCaption();
        String cancelarVotacionCaption();
        String continuaLabel();
        
        String reclamacionCanceladaMsg();
        String statusControlAccesoError();
        
        String opcionGraficoLabel();
        String numVotosGraficoLabel();
        
        String feedsVotacionesLabel();
        String busquedaAvanzadaLabel();
        
        String arrancandoClienteFirmaLabel();
        String arrancandoHerramientaValidacionLabel();
        String porcentajeDescargaLabel(String porcentaje);
        
        String eventoCanceladoOK();
        String eventoBorradoOK();
        
}
package org.sistemavotacion.modelo;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.sistemavotacion.android.R;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Date;
import java.util.Random;

import javax.mail.MessagingException;

import static org.sistemavotacion.android.Aplicacion.getAppString;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VoteReceipt {

	public static final String TAG = "VoteReceipt";
    
    private int id;
    private int notificationId;
    private int codigoEstado = 0;
    private String mensaje;
    private String eventoURL;    
    private Long eventoVotacionId;
    private Long opcionSeleccionadaId;
    private Tipo tipo;
    private ActorConIP actorConIP;
    private String controlAccesoServerURL;    
    private boolean esValido = false;
    private SMIMEMessageWrapper smimeMessage;
    private SMIMEMessageWrapper cancelVoteReceipt;
    private byte[] encryptedKey = null;
    private boolean isCanceled = false;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private PrivateKey certVotePrivateKey;
    private Evento voto;
    private Date dateCreated;
    private Date dateUpdated;
    
    public VoteReceipt (int codigoEstado, String mensaje) { 
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
    }
    
    public int initNotificationId() {
        Random randomGenerator = new Random();
        this.notificationId = randomGenerator.nextInt(100);
        return notificationId;
    }
    
    public int getNotificationId() {
        return notificationId;
    }
    
    public VoteReceipt (int codigoEstado, 
            SMIMEMessageWrapper votoValidado, Evento voto) throws Exception { 
        this.smimeMessage = votoValidado;
        this.codigoEstado = codigoEstado;
        this.voto = voto;
        String contenidoRecibo = votoValidado.getSignedContent();
        JSONObject contenidoReciboJSON = new JSONObject(contenidoRecibo);
        this.opcionSeleccionadaId = contenidoReciboJSON.getLong("opcionSeleccionadaId");
        this.eventoURL = contenidoReciboJSON.getString("eventoURL");
        comprobarRecibo();
    }
    
    public VoteReceipt (int codigoEstado, Evento voto) throws Exception { 
        this.codigoEstado = codigoEstado;
        this.voto = voto;
    }
    
    public String toJSONString() throws JSONException {
    	Log.d(TAG + ".toJSONString(...)", " --- voto.getHashSolicitudAccesoBase64(): " 
    			+ voto.getHashSolicitudAccesoBase64());
    	JSONObject jsonObject = new JSONObject();
    	jsonObject.put("codigoEstado", codigoEstado);
        if(voto != null) jsonObject.put("voto", voto.toJSON());
        jsonObject.put("isCanceled", isCanceled);
        return jsonObject.toString();
    }
    
    public static VoteReceipt parse(String jsonVoteReceipt) throws Exception {
    	//Log.d(TAG + ".parse(...)", "- jsonVoteReceipt: '" + jsonVoteReceipt + "'");
    	if(jsonVoteReceipt == null) return null;
    	int codigoEstado = 0;
    	boolean isCanceled = false;
    	Evento voto = null;
    	Log.d(TAG + ".parse(...)", " - parse(...)");
    	JSONObject jsonObject = new JSONObject (jsonVoteReceipt);
        if(jsonObject.has("codigoEstado"))
        	codigoEstado = jsonObject.getInt("codigoEstado");
        if(jsonObject.has("isCanceled"))
        	isCanceled = jsonObject.getBoolean("isCanceled");
        if(jsonObject.has("voto"))
        	voto = Evento.parse(jsonObject.getJSONObject("voto"));
        VoteReceipt voteReceipt = new VoteReceipt(codigoEstado, voto);
        voteReceipt.setCanceled(isCanceled);
    	return voteReceipt;
    }
    
    public boolean esValido () throws Exception {
        return esValido;
    }

    public void writoToFile(File file) throws Exception {
    	if(file == null) throw new Exception("File null");
    	if(smimeMessage == null) throw new Exception("Receipt null");
    	smimeMessage.writeTo(new FileOutputStream(file));
    }
    
    private void comprobarRecibo () throws Exception {
        if (Respuesta.SC_ERROR_VOTO_REPETIDO == codigoEstado) {//voto repetido
            esValido = true; 
            mensaje = getAppString(R.string.vote_repeated_msg, 
            		voto.getAsunto(), voto.getOpcionSeleccionada().getContenido());
            return;
        }
        if (!opcionSeleccionadaId.equals(voto.getOpcionSeleccionada().getId())) {
            Log.e("ReciboVoto", getAppString(R.string.option_error_msg));
            esValido = false; 
            mensaje = getAppString(R.string.option_error_msg);
            return;
        }
        if (smimeMessage.isValidSignature()) {
            esValido = true;
            mensaje = getAppString(R.string.vote_ok_msg, 
        		voto.getAsunto(), voto.getOpcionSeleccionada().getContenido());
        } 
    }
    
    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }

    /**
     * @return the eventoVotacionId
     */
    public Long getEventoVotacionId() {
        return eventoVotacionId;
    }

    /**
     * @param eventoVotacionId the eventoVotacionId to set
     */
    public void setEventoVotacionId(Long eventoVotacionId) {
        this.eventoVotacionId = eventoVotacionId;
    }

    /**
     * @return the opcionSeleccionadaId
     */
    public Long getOpcionSeleccionadaId() {
        return opcionSeleccionadaId;
    }

    /**
     * @param opcionSeleccionadaId the opcionSeleccionadaId to set
     */
    public void setOpcionSeleccionadaId(Long opcionSeleccionadaId) {
        this.opcionSeleccionadaId = opcionSeleccionadaId;
    }

    /**
     * @return the archivoRecibo
     */
    public File getArchivoRecibo() throws IOException, MessagingException {
        File archivoRecibo = null;
        if (smimeMessage != null) {
            archivoRecibo = new File("recibo");
            smimeMessage.writeTo(new FileOutputStream(archivoRecibo));
        }
        return archivoRecibo;
    }


    /**
     * @return the actorConIP
     */
    public ActorConIP getActorConIP() {
        return actorConIP;
    }

    /**
     * @param actorConIP the actorConIP to set
     */
    public void setActorConIP(ActorConIP actorConIP) {
        this.actorConIP = actorConIP;
    }

    /**
     * @return the controlAccesoServerURL
     */
    public String getControlAccesoServerURL() {
        return controlAccesoServerURL;
    }

    /**
     * @param controlAccesoServerURL the controlAccesoServerURL to set
     */
    public void setControlAccesoServerURL(String controlAccesoServerURL) {
        this.controlAccesoServerURL = controlAccesoServerURL;
    }

    /**
     * @return the tipo
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * @param tipo the tipo to set
     */
    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    /**
     * @return the codigoEstado
     */
    public int getCodigoEstado() {
        return codigoEstado;
    }

    /**
     * @param codigoEstado the codigoEstado to set
     */
    public void setCodigoEstado(int codigoEstado) {
        this.codigoEstado = codigoEstado;
    }

    /**
     * @return the mensaje
     */
    public String getMensaje() {
        return mensaje;
    }

    /**
     * @param mensaje the mensaje to set
     */
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    /**
     * @return the smimeMessage
     */
    public SMIMEMessageWrapper getSmimeMessage() {
        return smimeMessage;
    }

    /**
     * @param smimeMessage the smimeMessage to set
     */
    public void setSmimeMessage(SMIMEMessageWrapper smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    /**
     * @return the eventoURL
     */
    public String getEventoURL() {
        return eventoURL;
    }

    /**
     * @param eventoURL the eventoURL to set
     */
    public void setEventoURL(String eventoURL) {
        this.eventoURL = eventoURL;
    }

    /**
     * @return the voto
     */
    public Evento getVoto() {
        return voto;
    }

    /**
     * @param voto the voto to set
     */
    public void setVoto(Evento voto) {
        this.voto = voto;
    }

	public SMIMEMessageWrapper getCancelVoteReceipt() {
		return cancelVoteReceipt;
	}

	public void setCancelVoteReceipt(SMIMEMessageWrapper cancelVoteReceipt) {
		this.cancelVoteReceipt = cancelVoteReceipt;
		if(cancelVoteReceipt != null) isCanceled = true;
	}

	public Date getDateUpdated() {
		return dateUpdated;
	}

	public void setDateUpdated(Date dateUpdated) {
		this.dateUpdated = dateUpdated;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public boolean isCanceled() {
		return isCanceled;
	}

	public void setCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
	}

	public byte[] getEncryptedKey() {
		return encryptedKey;
	}

	public void setEncryptedKey(byte[] encryptedKey) {
		this.encryptedKey = encryptedKey;
	}

	public PKCS10WrapperClient getPkcs10WrapperClient() {
		return pkcs10WrapperClient;
	}

	public void setPkcs10WrapperClient(PKCS10WrapperClient pkcs10WrapperClient) {
		this.pkcs10WrapperClient = pkcs10WrapperClient;
	}

	public PrivateKey getCertVotePrivateKey() {
		return certVotePrivateKey;
	}

	public void setCertVotePrivateKey(PrivateKey certVotePrivateKey) {
		this.certVotePrivateKey = certVotePrivateKey;
	}
	
}
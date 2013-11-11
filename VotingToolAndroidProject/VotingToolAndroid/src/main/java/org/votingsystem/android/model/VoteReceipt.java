package org.votingsystem.android.model;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.PKCS10WrapperClient;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Date;
import java.util.Random;

import javax.mail.MessagingException;


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
    private TypeVS typeVS;
    private ActorVS actorVS;
    private String controlAccesoServerURL;    
    private boolean esValido = false;
    private SMIMEMessageWrapper smimeMessage;
    private SMIMEMessageWrapper cancelVoteReceipt;
    private byte[] encryptedKey = null;
    private boolean isCanceled = false;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private PrivateKey certVotePrivateKey;
    private EventVSAndroid voto;
    private Date dateCreated;
    private Date dateUpdated;
    
    public int initNotificationId() {
        Random randomGenerator = new Random();
        this.notificationId = randomGenerator.nextInt(100);
        return notificationId;
    }
    
    public int getNotificationId() {
        return notificationId;
    }
    
    public VoteReceipt (int codigoEstado, 
            SMIMEMessageWrapper votoValidado, EventVSAndroid voto) throws Exception {
        this.smimeMessage = votoValidado;
        this.codigoEstado = codigoEstado;
        this.voto = voto;
        String contenidoRecibo = votoValidado.getSignedContent();
        JSONObject contenidoReciboJSON = new JSONObject(contenidoRecibo);
        this.opcionSeleccionadaId = contenidoReciboJSON.getLong("opcionSeleccionadaId");
        this.eventoURL = contenidoReciboJSON.getString("eventoURL");
        if (smimeMessage.isValidSignature()) {
            esValido = true;
        }
        if (ResponseVS.SC_ERROR_VOTO_REPETIDO == codigoEstado) {//voto repetido
            esValido = false;
        }
        if (!opcionSeleccionadaId.equals(voto.getOpcionSeleccionada().getId())) {
            esValido = false;
        }
    }
    
    public VoteReceipt (int codigoEstado, EventVSAndroid voto) throws Exception {
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
    	EventVSAndroid voto = null;
    	Log.d(TAG + ".parse(...)", " - parse(...)");
    	JSONObject jsonObject = new JSONObject (jsonVoteReceipt);
        if(jsonObject.has("codigoEstado"))
        	codigoEstado = jsonObject.getInt("codigoEstado");
        if(jsonObject.has("isCanceled"))
        	isCanceled = jsonObject.getBoolean("isCanceled");
        if(jsonObject.has("voto"))
        	voto = EventVSAndroid.parse(jsonObject.getJSONObject("voto"));
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
     * @return the actorVS
     */
    public ActorVS getActorVS() {
        return actorVS;
    }

    /**
     * @param actorVS the actorVS to set
     */
    public void setActorVS(ActorVS actorVS) {
        this.actorVS = actorVS;
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
     * @return the typeVS
     */
    public TypeVS getTypeVS() {
        return typeVS;
    }

    /**
     * @param typeVS the typeVS to set
     */
    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
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
    public String getMensaje(Context context) {
        if (ResponseVS.SC_ERROR_VOTO_REPETIDO == codigoEstado) {//voto repetido
            return context.getString(R.string.vote_repeated_msg,
                    voto.getAsunto(), voto.getOpcionSeleccionada().getContent());
        }
        if (!opcionSeleccionadaId.equals(voto.getOpcionSeleccionada().getId())) {
            return context.getString(R.string.option_error_msg);
        }
        if (smimeMessage.isValidSignature()) {
            return context.getString(R.string.vote_ok_msg,
                    voto.getAsunto(), voto.getOpcionSeleccionada().getContent());
        }
        return null;
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
    public EventVSAndroid getVoto() {
        return voto;
    }

    /**
     * @param voto the voto to set
     */
    public void setVoto(EventVSAndroid voto) {
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
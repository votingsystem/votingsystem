package org.votingsystem.android.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.UserVSBase;
import org.votingsystem.util.DateUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Estadisticas {
	
	public static final String TAG = "Estadisticas";
   
    private Long id;
    private EventVSAndroid.Estado estado;
    private TypeVS typeVS;
    private UserVS userVSBase;
    private Integer numeroFirmasRecibidas;
    private Integer numeroSolicitudesDeAcceso;
    private Integer numeroVotosContabilizados;
    private String solicitudPublicacionValidadaURL;
    private String solicitudPublicacionURL;
    private String strFechaInicio;
    private String strFechaFin;
    private Date fechaInicio;
    private Date fechaFin;
    
    public void setEstado(EventVSAndroid.Estado estado) {
        this.estado = estado;
    }
    public EventVSAndroid.Estado getEstado() {
        return estado;
    }
    public void setUserVSBase(UserVS userVSBase) {
        this.userVSBase = userVSBase;
    }
    public UserVS getUserVSBase() {
        return userVSBase;
    }
    public void setNumeroFirmasRecibidas(int numeroFirmasRecibidas) {
        this.numeroFirmasRecibidas = numeroFirmasRecibidas;
    }
    public Integer getNumeroFirmasRecibidas() {
        return numeroFirmasRecibidas;
    }
    public void setStrFechaInicio(String strFechaInicio) throws ParseException {
        this.strFechaInicio = strFechaInicio;
        fechaInicio = DateUtils.getDateFromString(strFechaInicio);
    }
    public String getStrFechaInicio() {
        return strFechaInicio;
    }
    public void setStrFechaFin(String strFechaFin) throws ParseException {
        this.strFechaFin = strFechaFin;
        fechaFin = DateUtils.getDateFromString(strFechaFin);
    }
    public String getStrFechaFin() {
        return strFechaFin;
    }
    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    public Date getFechaInicio() {
            return fechaInicio;
    }
    public void setFechaFin(Date fechaFin) {
        this.fechaFin = fechaFin;
    }
    public Date getFechaFin() {
        return fechaFin;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
    public TypeVS getTypeVS() {
        return typeVS;
    }
    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    /**
     * @return the numeroSolicitudesDeAcceso
     */
    public Integer getNumeroSolicitudesDeAcceso() {
        return numeroSolicitudesDeAcceso;
    }

    /**
     * @param numeroSolicitudesDeAcceso the numeroSolicitudesDeAcceso to set
     */
    public void setNumeroSolicitudesDeAcceso(Integer numeroSolicitudesDeAcceso) {
        this.numeroSolicitudesDeAcceso = numeroSolicitudesDeAcceso;
    }
	public String getSolicitudPublicacionValidadaURL() {
		return solicitudPublicacionValidadaURL;
	}
	public void setSolicitudPublicacionValidadaURL(
			String solicitudPublicacionValidadaURL) {
		this.solicitudPublicacionValidadaURL = solicitudPublicacionValidadaURL;
	}
	public String getSolicitudPublicacionURL() {
		return solicitudPublicacionURL;
	}
	public void setSolicitudPublicacionURL(String solicitudPublicacionURL) {
		this.solicitudPublicacionURL = solicitudPublicacionURL;
	}

    /**
     * @return the numeroVotosContabilizados
     */
    public Integer getNumeroVotosContabilizados() {
        return numeroVotosContabilizados;
    }

    /**
     * @param numeroVotosContabilizados the numeroVotosContabilizados to set
     */
    public void setNumeroVotosContabilizados(Integer numeroVotosContabilizados) {
        this.numeroVotosContabilizados = numeroVotosContabilizados;
    }

    public static Estadisticas parse (String strEstadisticas) throws IOException, Exception {
    	Log.d(TAG + ".parse(...)", "parse(...)");
        Estadisticas estadisticas = new Estadisticas();
        JSONObject estadisticaJSON = new JSONObject(strEstadisticas);
        JSONObject jsonObject = null;
        JSONArray jsonArray = null;
        if (estadisticaJSON.has("typeVS"))
            estadisticas.setTypeVS(TypeVS.valueOf(estadisticaJSON.getString("typeVS")));
        if (estadisticaJSON.has("id"))
             estadisticas.setId(estadisticaJSON.getLong("id"));
        if (estadisticaJSON.has("estado"))
             estadisticas.setEstado(EventVSAndroid.Estado.valueOf(estadisticaJSON.getString("estado")));
        if (estadisticaJSON.has("userVSBase")) {
            UserVS userVSBase = new UserVSBase();
            userVSBase.setNif(estadisticaJSON.getString("userVSBase"));
            estadisticas.setUserVSBase(userVSBase);
        }
        if (estadisticaJSON.has("numeroSolicitudesDeAcceso"))
             estadisticas.setNumeroSolicitudesDeAcceso(estadisticaJSON.getInt("numeroSolicitudesDeAcceso"));
        if (estadisticaJSON.has("numeroFirmasRecibidas"))
             estadisticas.setNumeroFirmasRecibidas(estadisticaJSON.getInt("numeroFirmasRecibidas"));
        if (estadisticaJSON.has("solicitudPublicacionValidadaURL"))
             estadisticas.setSolicitudPublicacionValidadaURL(estadisticaJSON.getString("solicitudPublicacionValidadaURL"));
        if (estadisticaJSON.has("solicitudPublicacionURL"))
             estadisticas.setSolicitudPublicacionURL(estadisticaJSON.getString("solicitudPublicacionURL"));
        if (estadisticaJSON.has("fechaInicio"))
             estadisticas.setFechaInicio(DateUtils.getDateFromString(estadisticaJSON.getString("fechaInicio")));
        if (estadisticaJSON.has("fechaFin"))
             estadisticas.setFechaInicio(DateUtils.getDateFromString(estadisticaJSON.getString("fechaFin")));        
        if (estadisticaJSON.has("centroControl")) {
            jsonObject = estadisticaJSON.getJSONObject("controlCenter");
            ControlCenter controlCenter = new ControlCenter();
            controlCenter.setNombre(jsonObject.getString("nombre"));
            controlCenter.setServerURL(jsonObject.getString("serverURL"));
            controlCenter.setId(jsonObject.getLong("id"));
        } 
        if (estadisticaJSON.has("numeroSolicitudesDeAcceso"))
             estadisticas.setNumeroSolicitudesDeAcceso(estadisticaJSON.getInt("numeroSolicitudesDeAcceso"));          
        return estadisticas;	
    }
}

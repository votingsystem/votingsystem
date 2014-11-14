package org.votingsystem.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.util.DateUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class StatisticsVS {
	
	public static final String TAG = "StatisticsVS";
   
    private Long id;
    private EventVS.State state;
    private TypeVS typeVS;
    private UserVS userVS;
    private Integer numSignatures;
    private Integer numAccessRequests;
    private Integer numVotesVSAccounted;
    private String validatedPublishRequestURL;
    private String publishRequestURL;
    private String strDateBegin;
    private String strdateFinish;
    private Date dateBegin;
    private Date dateFinish;
    
    public void setState(EventVS.State state) {
        this.state = state;
    }
    public EventVS.State getState() {
        return state;
    }
    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }
    public UserVS getUserVS() {
        return userVS;
    }
    public void setNumSignatures(int numSignatures) {
        this.numSignatures = numSignatures;
    }
    public Integer getNumSignatures() {
        return numSignatures;
    }
    public void setStrDateBegin(String strDateBegin) throws ParseException {
        this.strDateBegin = strDateBegin;
        dateBegin = DateUtils.getDateFromString(strDateBegin);
    }
    public String getStrDateBegin() {
        return strDateBegin;
    }
    public void setStrdateFinish(String strdateFinish) throws ParseException {
        this.strdateFinish = strdateFinish;
        dateFinish = DateUtils.getDateFromString(strdateFinish);
    }
    public String getStrdateFinish() {
        return strdateFinish;
    }
    public void setDateBegin(Date dateBegin) {
        this.dateBegin = dateBegin;
    }
    public Date getDateBegin() {
            return dateBegin;
    }
    public void setDateFinish(Date dateFinish) {
        this.dateFinish = dateFinish;
    }
    public Date getDateFinish() {
        return dateFinish;
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
     * @return the numAccessRequests
     */
    public Integer getNumeroSolicitudesDeAcceso() {
        return numAccessRequests;
    }

    /**
     * @param numAccessRequests the numAccessRequests to set
     */
    public void setNumeroSolicitudesDeAcceso(Integer numAccessRequests) {
        this.numAccessRequests = numAccessRequests;
    }
	public String getSolicitudPublicacionValidadaURL() {
		return validatedPublishRequestURL;
	}
	public void setSolicitudPublicacionValidadaURL(
			String validatedPublishRequestURL) {
		this.validatedPublishRequestURL = validatedPublishRequestURL;
	}
	public String getSolicitudPublicacionURL() {
		return publishRequestURL;
	}
	public void setSolicitudPublicacionURL(String publishRequestURL) {
		this.publishRequestURL = publishRequestURL;
	}

    /**
     * @return the numVotesVSAccounted
     */
    public Integer getNumVotesVSAccounted() {
        return numVotesVSAccounted;
    }

    /**
     * @param numVotesVSAccounted the numVotesVSAccounted to set
     */
    public void setNumeroVotosContabilizados(Integer numVotesVSAccounted) {
        this.numVotesVSAccounted = numVotesVSAccounted;
    }

    public static StatisticsVS parse (String strEstadisticas) throws IOException, Exception {
    	Log.d(TAG + ".parse", "parse");
        StatisticsVS statisticsVS = new StatisticsVS();
        JSONObject estadisticaJSON = new JSONObject(strEstadisticas);
        JSONObject jsonObject = null;
        JSONArray jsonArray = null;
        if (estadisticaJSON.has("typeVS"))
            statisticsVS.setTypeVS(TypeVS.valueOf(estadisticaJSON.getString("typeVS")));
        if (estadisticaJSON.has("id"))
             statisticsVS.setId(estadisticaJSON.getLong("id"));
        if (estadisticaJSON.has("state"))
             statisticsVS.setState(EventVS.State.valueOf(estadisticaJSON.getString("state")));
        if (estadisticaJSON.has("userVS")) {
            UserVS userVS = new UserVS();
            userVS.setNif(estadisticaJSON.getString("userVS"));
            statisticsVS.setUserVS(userVS);
        }
        if (estadisticaJSON.has("numAccessRequests"))
             statisticsVS.setNumeroSolicitudesDeAcceso(estadisticaJSON.getInt("numAccessRequests"));
        if (estadisticaJSON.has("numSignatures"))
             statisticsVS.setNumSignatures(estadisticaJSON.getInt("numSignatures"));
        if (estadisticaJSON.has("validatedPublishRequestURL"))
             statisticsVS.setSolicitudPublicacionValidadaURL(estadisticaJSON.getString("validatedPublishRequestURL"));
        if (estadisticaJSON.has("publishRequestURL"))
             statisticsVS.setSolicitudPublicacionURL(estadisticaJSON.getString("publishRequestURL"));
        if (estadisticaJSON.has("dateBegin"))
             statisticsVS.setDateBegin(DateUtils.getDateFromString(estadisticaJSON.getString("dateBegin")));
        if (estadisticaJSON.has("dateFinish"))
             statisticsVS.setDateBegin(DateUtils.getDateFromString(estadisticaJSON.getString("dateFinish")));
        if (estadisticaJSON.has("controlCenter")) {
            jsonObject = estadisticaJSON.getJSONObject("controlCenterVS");
            ControlCenterVS controlCenterVS = new ControlCenterVS();
            controlCenterVS.setName(jsonObject.getString("name"));
            controlCenterVS.setServerURL(jsonObject.getString("serverURL"));
            controlCenterVS.setId(jsonObject.getLong("id"));
        } 
        if (estadisticaJSON.has("numAccessRequests"))
             statisticsVS.setNumeroSolicitudesDeAcceso(estadisticaJSON.getInt("numAccessRequests"));
        return statisticsVS;
    }
}

package org.sistemavotacion.test.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import static org.sistemavotacion.test.modelo.SimulationData.parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class UserBaseData {
    
    private static Logger logger = LoggerFactory.getLogger(UserBaseData.class);

    private int codigoEstado = Respuesta.SC_ERROR_EJECUCION;
    private String message = null;

    private Integer numRepresentatives = 0;
    private Integer numVotesRepresentatives = 0;

    private Integer numUsersWithoutRepresentative =  0;
    private Integer numVotesUsersWithoutRepresentative =  0;

    private Integer numUsersWithRepresentative =  0;
    private Integer numVotesUsersWithRepresentative =  0;
    

    private Integer numAltasOK = 0;
    private Integer numAltasERROR = 0;

    private Integer numDelegacionesOK = 0;
    private Integer numDelegacionesERROR = 0;
    
    private static Integer horasDuracionVotacion;
    private static Integer minutosDuracionVotacion;
    
    private static boolean votacionAleatoria = true;
    private static boolean simulacionConTiempos = false;
    
    private Evento evento = null;
    
    private AtomicLong userIndex = new AtomicLong(0);
    
    private List<String> representativeNifList = new ArrayList<String>();
    private List<String> usersWithoutRepresentativeList = new ArrayList<String>();
    private List<String> usersWithRepresentativeList = new ArrayList<String>();

    
    private static long comienzo;
    private static long duracion;
    
    public Integer getNumberElectors() {
        return numVotesRepresentatives + numVotesUsersWithRepresentative + 
                numVotesUsersWithoutRepresentative;
    }
    
    public Integer getNumAltasOK() {
        return this.numAltasOK;
    }
    
    public Integer getAndIncrementNumAltas() {
        this.numAltasOK++;
        return this.numAltasOK;
    }
    
    public Integer getNumAltasERROR() {
        return this.numAltasERROR;
    }
    
    public Integer getAndIncrementNumAltasERROR() {
        this.numAltasERROR++;
        return this.numAltasERROR;
    }
    
    public Integer getNumDelegacionesOK() {
        return this.numDelegacionesOK;
    }
    
    public Integer getAndIncrementNumDelegacionesOK() {
        this.numDelegacionesOK++;
        return this.numDelegacionesOK;
    }
    
    
    public Integer getNumDelegacionesERROR() {
        return this.numDelegacionesERROR;
    }
    
    public Integer getAndIncrementNumDelegacionesERROR() {
        this.numDelegacionesERROR++;
        return this.numDelegacionesERROR;
    }
    
    
    public UserBaseData(int status, String message) {
        this.codigoEstado = status;
        this.message = message;
    }
    
    public UserBaseData() {}
    
    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the numRepresentatives
     */
    public Integer getNumRepresentatives() {
        return numRepresentatives;
    }

    /**
     * @param numRepresentatives the numRepresentatives to set
     */
    public void setNumRepresentatives(Integer numRepresentatives) {
        this.numRepresentatives = numRepresentatives;
    }


    /**
     * @return the duracion
     */
    public long getDuracion() {
        return duracion;
    }

    /**
     * @param aDuracion the duracion to set
     */
    public void setDuracion(long aDuracion) {
        duracion = aDuracion;
    }

    
    /**
     * @return the comienzo
     */
    public long getComienzo() {
        return comienzo;
    }

    /**
     * @param aComienzo the comienzo to set
     */
    public void setComienzo(long aComienzo) {
        comienzo = aComienzo;
    }
    
    public String operationResultHtml() {
        return new StringBuffer("<html><b>Altas OK:</b>").append(this.numAltasOK)
            .append("<br/><b>Altas con error:</b>").append(this.numAltasERROR)
            .append("<br/><b>Delegaciones OK:</b>").append(this.numDelegacionesOK)
            .append("<br/><b>Delegaciones ERROR:</b>").append(this.numDelegacionesERROR)
             .append("</html>").toString();
    }
        
    public String toHtml() {
        return new StringBuffer("<html><b>Número representantes:</b>")
                .append(this.numRepresentatives)
                .append("<br/><b>Número de votos de representantes:</b>")
                .append(this.numVotesRepresentatives)
                .append("<br/><b>Número de usuarios representados:</b>")
                .append(this.numUsersWithRepresentative)
                .append("<br/><b>Número de votos de usuarios representados:</b>")
                .append(this.numVotesUsersWithRepresentative)
                .append("<br/><b>Número de usuarios sin representante:</b>")
                .append(this.numUsersWithoutRepresentative)
                .append("<br/><b>Número de votos de usuarios sin representante:</b>")
                .append(this.numVotesUsersWithoutRepresentative)
                .append("</html>").toString();
    }
    

    /**
     * @return the userIndex
     */
    public long getUserIndex() {
        return userIndex.get();
    }
    
    /**
     * @return the userIndex
     */
    public long getAndIncrementUserIndex() {
        return userIndex.getAndIncrement();
    }

    /**
     * @param userIndex the userIndex to set
     */
    public void setUserIndex(long userIndex) {
        this.userIndex = new AtomicLong(userIndex);
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
     * @return the representativeNifList
     */
    public List<String> getRepresentativeNifList() {
        return representativeNifList;
    }

    /**
     * @param representativeNifList the representativeNifList to set
     */
    public void setRepresentativeNifList(List<String> representativeNifList) {
        this.representativeNifList = representativeNifList;
    }

    /**
     * @return the userNifList
     */
    public List<String> getUserNifList() {
        List<String> result = new ArrayList<String>();
        if(getUsersWithRepresentativeList() != null && 
                !usersWithRepresentativeList.isEmpty()) 
            result.addAll(getUsersWithRepresentativeList());
        if(getUsersWithoutRepresentativeList() != null && 
                !usersWithoutRepresentativeList.isEmpty()) 
            result.addAll(getUsersWithoutRepresentativeList());        
        if(representativeNifList != null && !representativeNifList.isEmpty()) 
            result.addAll(representativeNifList);
        return result;
    }

    /**
     * @return the evento
     */
    public Evento getEvento() {
        return evento;
    }

    /**
     * @param evento the evento to set
     */
    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    /**
     * @return the horasDuracionVotacion
     */
    public static Integer getHorasDuracionVotacion() {
        return horasDuracionVotacion;
    }

    /**
     * @param aHorasDuracionVotacion the horasDuracionVotacion to set
     */
    public static void setHorasDuracionVotacion(Integer aHorasDuracionVotacion) {
        horasDuracionVotacion = aHorasDuracionVotacion;
    }

    /**
     * @return the minutosDuracionVotacion
     */
    public static Integer getMinutosDuracionVotacion() {
        return minutosDuracionVotacion;
    }

    /**
     * @param aMinutosDuracionVotacion the minutosDuracionVotacion to set
     */
    public static void setMinutosDuracionVotacion(Integer aMinutosDuracionVotacion) {
        minutosDuracionVotacion = aMinutosDuracionVotacion;
    }

    /**
     * @return the votacionAleatoria
     */
    public static boolean isVotacionAleatoria() {
        return votacionAleatoria;
    }

    /**
     * @param aVotacionAleatoria the votacionAleatoria to set
     */
    public static void setVotacionAleatoria(boolean aVotacionAleatoria) {
        votacionAleatoria = aVotacionAleatoria;
    }
    
    
    /**
     * @return the simulacionConTiempos
     */
    public static boolean isSimulacionConTiempos() {
        return simulacionConTiempos;
    }

    /**
     * @param aSimulacionConTiempos the simulacionConTiempos to set
     */
    public static void setSimulacionConTiempos(boolean aSimulacionConTiempos) {
        simulacionConTiempos = aSimulacionConTiempos;
    }

    /**
     * @return the numVotesRepresentatives
     */
    public Integer getNumVotesRepresentatives() {
        return numVotesRepresentatives;
    }

    /**
     * @param numVotesRepresentatives the numVotesRepresentatives to set
     */
    public void setNumVotesRepresentatives(Integer numVotesRepresentatives) {
        this.numVotesRepresentatives = numVotesRepresentatives;
    }

    /**
     * @return the numUsersWithoutRepresentative
     */
    public Integer getNumUsersWithoutRepresentative() {
        return numUsersWithoutRepresentative;
    }

    /**
     * @param numUsersWithoutRepresentative the numUsersWithoutRepresentative to set
     */
    public void setNumUsersWithoutRepresentative(Integer numUsersWithoutRepresentative) {
        this.numUsersWithoutRepresentative = numUsersWithoutRepresentative;
    }

    /**
     * @return the numVotesUsersWithoutRepresentative
     */
    public Integer getNumVotesUsersWithoutRepresentative() {
        return numVotesUsersWithoutRepresentative;
    }

    /**
     * @param numVotesUsersWithoutRepresentative the numVotesUsersWithoutRepresentative to set
     */
    public void setNumVotesUsersWithoutRepresentative(Integer numVotesUsersWithoutRepresentative) {
        this.numVotesUsersWithoutRepresentative = numVotesUsersWithoutRepresentative;
    }

    /**
     * @return the numUsersWithRepresentative
     */
    public Integer getNumUsersWithRepresentative() {
        return numUsersWithRepresentative;
    }

    /**
     * @param numUsersWithRepresentative the numUsersWithRepresentative to set
     */
    public void setNumUsersWithRepresentative(Integer numUsersWithRepresentative) {
        this.numUsersWithRepresentative = numUsersWithRepresentative;
    }

    /**
     * @return the numVotesUsersWithRepresentative
     */
    public Integer getNumVotesUsersWithRepresentative() {
        return numVotesUsersWithRepresentative;
    }

    /**
     * @param numVotesUsersWithRepresentative the numVotesUsersWithRepresentative to set
     */
    public void setNumVotesUsersWithRepresentative(Integer numVotesUsersWithRepresentative) {
        this.numVotesUsersWithRepresentative = numVotesUsersWithRepresentative;
    }

    /**
     * @return the usersWithoutRepresentativeList
     */
    public List<String> getUsersWithoutRepresentativeList() {
        return usersWithoutRepresentativeList;
    }

    /**
     * @param usersWithoutRepresentativeList the usersWithoutRepresentativeList to set
     */
    public void setUsersWithoutRepresentativeList(List<String> usersWithoutRepresentativeList) {
        this.usersWithoutRepresentativeList = usersWithoutRepresentativeList;
    }

    /**
     * @return the usersWithRepresentativeList
     */
    public List<String> getUsersWithRepresentativeList() {
        return usersWithRepresentativeList;
    }

    /**
     * @param usersWithRepresentativeList the usersWithRepresentativeList to set
     */
    public void setUsersWithRepresentativeList(List<String> usersWithRepresentativeList) {
        this.usersWithRepresentativeList = usersWithRepresentativeList;
    }
    
    public static UserBaseData parse (String dataStr) {
        logger.debug("- parse");
        if(dataStr == null) return null;
        JSONObject dataJSON = (JSONObject)JSONSerializer.toJSON(dataStr);
        return parse(dataJSON);
    }
   
    
    public static UserBaseData parse (JSONObject dataJSON) {
        logger.debug("- parse - json "  + dataJSON.toString());
        if(dataJSON == null) return null;
        UserBaseData userBaseData = new UserBaseData();
        if (dataJSON.containsKey("userIndex")) {
            userBaseData.setUserIndex(dataJSON.getInt("userIndex"));
        }                
        if (dataJSON.containsKey("numRepresentatives")) {
            logger.debug("dataJSON.get(\"numRepresentatives\").getClass(): " + dataJSON.get("numRepresentatives").getClass());
            
            userBaseData.setNumRepresentatives(dataJSON.getInt("numRepresentatives"));
        }
        if (dataJSON.containsKey("numUsersWithoutRepresentative")) {
            userBaseData.setNumUsersWithoutRepresentative(dataJSON.
                    getInt("numUsersWithoutRepresentative"));
        }
        if (dataJSON.containsKey("numVotesUsersWithoutRepresentative")) {
            userBaseData.setNumVotesUsersWithoutRepresentative(dataJSON.
                    getInt("numVotesUsersWithoutRepresentative"));
        }
        if (dataJSON.containsKey("numUsersWithRepresentative")) {
            userBaseData.setNumUsersWithRepresentative(dataJSON.
                    getInt("numUsersWithRepresentative"));
        }
        if (dataJSON.containsKey("numVotesUsersWithRepresentative")) {
            userBaseData.setNumVotesUsersWithRepresentative(dataJSON.
                    getInt("numVotesUsersWithRepresentative"));
        }        
        return userBaseData;
    }


}

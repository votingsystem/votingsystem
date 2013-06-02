package org.sistemavotacion.test.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.simulation.SimulatorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class UserBaseData implements SimulatorData {
    
    private static Logger logger = LoggerFactory.getLogger(UserBaseData.class);

    private int statusCode = Respuesta.SC_ERROR_EJECUCION;
    private String message = null;

    private Integer numRepresentatives = 0;
    private Integer numVotesRepresentatives = 0;

    private Integer numUsersWithoutRepresentative =  0;
    private Integer numVotesUsersWithoutRepresentative =  0;

    private Integer numUsersWithRepresentative =  0;
    private Integer numVotesUsersWithRepresentative =  0;
    

    private AtomicLong numRepresentativeRequestsOK = new AtomicLong(0);
    private AtomicLong numRepresentativeRequestssERROR = new AtomicLong(0);

    private AtomicLong numDelegationsOK = new AtomicLong(0);
    private AtomicLong numDelegationsERROR = new AtomicLong(0);
    
    private final AtomicLong representativeRequests = new AtomicLong(0);
    private final AtomicLong delegationRequests = new AtomicLong(0);
    
    private Integer horasDuracionVotacion;
    private Integer minutosDuracionVotacion;
    
    private boolean withRandomVotes = true;
    private boolean timerBased = false;

    private AtomicLong userIndex = new AtomicLong(0);
    
    private List<String> representativeNifList = new ArrayList<String>();
    private List<String> usersWithoutRepresentativeList = new ArrayList<String>();
    private List<String> usersWithRepresentativeList = new ArrayList<String>();

    
    private long begin;
    private long finish;
    
    private String durationStr = null;

    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getNumDelegationRequestsColected() {
        return (numDelegationsERROR.get() + numDelegationsOK.get());
    }
    
    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getNumDelegationRequests() {
        return delegationRequests.get();
    }
    
    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getAndIncrementNumDelegationRequests() {
        return delegationRequests.getAndIncrement();
    }
    
    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getNumRepresentativeRequests() {
        return representativeRequests.get();
    }
    
    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getAndIncrementNumRepresentativeRequests() {
        return representativeRequests.getAndIncrement();
    }
    
    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getNumRepresentativeRequestsColected() {
        return (numRepresentativeRequestsOK.get() + 
                numRepresentativeRequestssERROR.get());
    }
    
    
    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getNumRepresentativeRequestsOK() {
        return numRepresentativeRequestsOK.get();
    }
    
    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getAndIncrementNumRepresentativeRequestsOK() {
        return numRepresentativeRequestsOK.getAndIncrement();
    }

    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getNumRepresentativeRequestsERROR() {
        return numRepresentativeRequestssERROR.get();
    }
    
    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getAndIncrementNumRepresentativeRequestsERROR() {
        return numRepresentativeRequestssERROR.get();
    }

    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getNumDelegationsOK() {
        return numDelegationsOK.get();
    }
    
    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getAndIncrementNumDelegationsOK() {
        return numDelegationsOK.getAndIncrement();
    }
    
    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getNumDelegationsERROR() {
        return numDelegationsERROR.get();
    }
    
    /**
     * @return the numRepresentativeRequestsOK
     */
    public Long getAndIncrementNumDelegationsERROR() {
        return numDelegationsERROR.getAndIncrement();
    }
    
    
    public Integer getNumberElectors() {
        return numVotesRepresentatives + numVotesUsersWithRepresentative + 
                numVotesUsersWithoutRepresentative;
    }

    public UserBaseData(int status, String message) {
        this.statusCode = status;
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
     * @return the horasDuracionVotacion
     */
    public Integer getHorasDuracionVotacion() {
        return horasDuracionVotacion;
    }

    /**
     * @param aHorasDuracionVotacion the horasDuracionVotacion to set
     */
    public void setHorasDuracionVotacion(Integer aHorasDuracionVotacion) {
        horasDuracionVotacion = aHorasDuracionVotacion;
    }

    /**
     * @return the minutosDuracionVotacion
     */
    public Integer getMinutosDuracionVotacion() {
        return minutosDuracionVotacion;
    }

    /**
     * @param aMinutosDuracionVotacion the minutosDuracionVotacion to set
     */
    public void setMinutosDuracionVotacion(Integer aMinutosDuracionVotacion) {
        minutosDuracionVotacion = aMinutosDuracionVotacion;
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
    
    
    /**
     * @return the statusCode
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
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

    /**
     * @return the begin
     */
    public long getBegin() {
        return begin;
    }

    /**
     * @param begin the begin to set
     */
    public void setBegin(long begin) {
        this.begin = begin;
    }

    /**
     * @return the finish
     */
    public long getFinish() {
        return finish;
    }

    /**
     * @param finish the finish to set
     */
    public void setFinish(long finish) {
        this.finish = finish;
    }

    public String getDurationStr() {
        return durationStr;
    }

    /**
     * @return the timerBased
     */
    public boolean isTimerBased() {
        return timerBased;
    }

    /**
     * @param timerBased the timerBased to set
     */
    public void setTimerBased(boolean timerBased) {
        this.timerBased = timerBased;
    }

    /**
     * @return the withRandomVotes
     */
    public boolean isWithRandomVotes() {
        return withRandomVotes;
    }

    /**
     * @param withRandomVotes the withRandomVotes to set
     */
    public void setWithRandomVotes(boolean withRandomVotes) {
        this.withRandomVotes = withRandomVotes;
    }

}

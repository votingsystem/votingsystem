package org.votingsystem.simulation.model

import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ResponseVS

import java.util.concurrent.atomic.AtomicLong
/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class UserBaseSimulationData extends SimulationData {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(UserBaseSimulationData.class);

    private int statusCode = ResponseVS.SC_ERROR;
    private String message = null;

    private Integer numRepresentatives = 0;
    private Integer numRepresentativesWithVote = 0;

    private Integer numUsersWithoutRepresentative = 0;
    private Integer numUsersWithoutRepresentativeWithVote =  0;

    private Integer numUsersWithRepresentative =  0;
    private Integer numUsersWithRepresentativeWithVote =  0;


    private AtomicLong numRepresentativeRequestsOK = new AtomicLong(0);
    private AtomicLong numRepresentativeRequestsERROR = new AtomicLong(0);

    private AtomicLong numDelegationsOK = new AtomicLong(0);
    private AtomicLong numDelegationsERROR = new AtomicLong(0);

    private final AtomicLong representativeRequests = new AtomicLong(0);
    private final AtomicLong delegationRequests = new AtomicLong(0);


    private boolean withRandomVotes = true;

    private AtomicLong userIndex = new AtomicLong(0);

    private List<String> representativeNifList = new ArrayList<String>();
    private List<String> usersWithoutRepresentativeList = new ArrayList<String>();
    private List<String> usersWithRepresentativeList = new ArrayList<String>();

    public Long getNumDelegationRequestsColected() {
        return (numDelegationsERROR.get() + numDelegationsOK.get());
    }

    public Long getNumDelegationRequests() {
        return delegationRequests.get();
    }

    public Long getAndIncrementNumDelegationRequests() {
        return delegationRequests.getAndIncrement();
    }

    public Long getNumRepresentativeRequests() {
        return representativeRequests.get();
    }

    public Long getAndIncrementNumRepresentativeRequests() {
        return representativeRequests.getAndIncrement();
    }

    public Long getNumRepresentativeRequestsColected() {
        return (numRepresentativeRequestsOK.get() +
                numRepresentativeRequestsERROR.get());
    }

    public Long getNumRepresentativeRequestsOK() {
        return numRepresentativeRequestsOK.get();
    }

    public boolean hasRepresesentativeRequestsPending() {
        return (numRepresentatives > representativeRequests.get());
    }

    public boolean waitingForRepresesentativeRequests() {
        return (getMaxPendingResponses() < (representativeRequests.get() - getNumRepresentativeRequestsColected()));
    }

    public Long getAndIncrementNumRepresentativeRequestsOK() {
        return numRepresentativeRequestsOK.getAndIncrement();
    }

    public Long getNumRepresentativeRequestsERROR() {
        return numRepresentativeRequestsERROR.get();
    }

    public Long getAndIncrementNumRepresentativeRequestsERROR() {
        return numRepresentativeRequestsERROR.getAndIncrement();
    }

    public Long getNumDelegationsOK() {
        return numDelegationsOK.get();
    }

    public Long getAndIncrementNumDelegationsOK() {
        return numDelegationsOK.getAndIncrement();
    }

    public Long getNumDelegationsERROR() {
        return numDelegationsERROR.get();
    }

    public Long getAndIncrementNumDelegationsERROR() {
        return numDelegationsERROR.getAndIncrement();
    }

    public Integer getNumberElectors() {
        return numRepresentativesWithVote + numUsersWithRepresentativeWithVote + numUsersWithoutRepresentativeWithVote;
    }

    public UserBaseSimulationData(int status, String message) {
        this.statusCode = status;
        this.message = message;
    }

    public UserBaseSimulationData() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getNumRepresentatives() {
        return numRepresentatives;
    }

    public void setNumRepresentatives(Integer numRepresentatives) {
        this.numRepresentatives = numRepresentatives;
    }

    public long getUserIndex() {
        return userIndex.get();
    }

    public long getAndIncrementUserIndex() {
        return userIndex.getAndIncrement();
    }

    public void setUserIndex(long userIndex) {
        this.userIndex = new AtomicLong(userIndex);
    }

    public List<String> getRepresentativeNifList() {
        return representativeNifList;
    }

    public void setRepresentativeNifList(List<String> representativeNifList) {
        this.representativeNifList = representativeNifList;
    }

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

    public Integer getNumRepresentativesWithVote() {
        return numRepresentativesWithVote;
    }

    public void setNumRepresentativesWithVote(Integer numRepresentativesWithVote) {
        this.numRepresentativesWithVote = numRepresentativesWithVote;
    }

    public Integer getNumUsersWithoutRepresentativeWithVote() {
        return numUsersWithoutRepresentativeWithVote;
    }

    public void setNumUsersWithoutRepresentativeWithVote(Integer numUsersWithoutRepresentativeWithVote) {
        this.numUsersWithoutRepresentativeWithVote = numUsersWithoutRepresentativeWithVote;
    }

    public Integer getNumUsersWithoutRepresentative() {
        return numUsersWithoutRepresentative;
    }

    public void setNumUsersWithoutRepresentative(Integer numUsersWithoutRepresentative) {
        this.numUsersWithoutRepresentative = numUsersWithoutRepresentative;
    }

    public Integer getNumUsersWithRepresentative() {
        return numUsersWithRepresentative;
    }

    public void setNumUsersWithRepresentative(Integer numUsersWithRepresentative) {
        this.numUsersWithRepresentative = numUsersWithRepresentative;
    }

    public Integer getNumUsersWithRepresentativeWithVote() {
        return numUsersWithRepresentativeWithVote;
    }

    public void setNumUsersWithRepresentativeWithVote(Integer numUsersWithRepresentativeWithVote) {
        this.numUsersWithRepresentativeWithVote = numUsersWithRepresentativeWithVote;
    }

    public List<String> getUsersWithoutRepresentativeList() {
        return usersWithoutRepresentativeList;
    }

    public void setUsersWithoutRepresentativeList(List<String> usersWithoutRepresentativeList) {
        this.usersWithoutRepresentativeList = usersWithoutRepresentativeList;
    }

    public List<String> getUsersWithRepresentativeList() {
        return usersWithRepresentativeList;
    }

    public void setUsersWithRepresentativeList(List<String> usersWithRepresentativeList) {
        this.usersWithRepresentativeList = usersWithRepresentativeList;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public boolean isWithRandomVotes() {
        return withRandomVotes;
    }

    public void setWithRandomVotes(boolean withRandomVotes) {
        this.withRandomVotes = withRandomVotes;
    }

    public static UserBaseSimulationData parse (String dataStr) {
        logger.debug("- parse");
        if(dataStr == null) return null;
        JSONObject dataJSON = new JSONObject(dataStr);
        return parse(dataJSON);
    }

    public static UserBaseSimulationData parse (JSONObject dataJSON) {
        logger.debug("- parse - json "  + dataJSON.toString());
        if(dataJSON == null) return null;
        UserBaseSimulationData userBaseData = new UserBaseSimulationData();
        if (dataJSON.containsKey("userIndex")) {
            userBaseData.setUserIndex(dataJSON.getInt("userIndex"));
        }
        if (dataJSON.containsKey("numUsersWithoutRepresentative")) {
            userBaseData.setNumUsersWithoutRepresentative(dataJSON.getInt("numUsersWithoutRepresentative"));
        }
        if (dataJSON.containsKey("numUsersWithoutRepresentativeWithVote")) {
            userBaseData.setNumUsersWithoutRepresentativeWithVote(dataJSON.getInt("numUsersWithoutRepresentativeWithVote"));
        }
        if (dataJSON.containsKey("numRepresentatives")) {
            userBaseData.setNumRepresentatives(dataJSON.getInt("numRepresentatives"));
        }
        if (dataJSON.containsKey("numRepresentativesWithVote")) {
            userBaseData.setNumRepresentativesWithVote(dataJSON.getInt("numRepresentativesWithVote"));
        }
        if (dataJSON.containsKey("numUsersWithRepresentative")) {
            userBaseData.setNumUsersWithRepresentative(dataJSON.getInt("numUsersWithRepresentative"));
        }
        if (dataJSON.containsKey("numUsersWithRepresentativeWithVote")) {
            userBaseData.setNumUsersWithRepresentativeWithVote(dataJSON.getInt("numUsersWithRepresentativeWithVote"));
        }
        return userBaseData;
    }

}

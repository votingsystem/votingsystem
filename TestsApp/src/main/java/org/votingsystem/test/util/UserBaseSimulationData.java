package org.votingsystem.test.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.test.callable.RepresentativeDelegatorDataSender;
import org.votingsystem.test.callable.RepresentativeTestDataSender;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.NifUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserBaseSimulationData extends SimulationData {

    private static Logger log = Logger.getLogger(UserBaseSimulationData.class.getSimpleName());

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

    private final AtomicBoolean representativeDataTerminated = new AtomicBoolean(false);
    private final AtomicBoolean usersWithouRepresentativeTerminated = new AtomicBoolean(false);
    private CompletionService<ResponseVS>  requestCompletionService;
    private ExecutorService requestExecutor;

    private List<String> representativeNifList;
    private List<String> usersWithRepresentativeList;
    private List<String> usersWithoutRepresentativeList;
    private CountDownLatch countDownLatch;

    private boolean withRandomVotes = true;

    private AtomicLong userIndex = new AtomicLong(0);

    public Long getNumDelegationRequestsCollected() {
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

    public Long getNumRepresentativeRequestsCollected() {
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
        return (getMaxPendingResponses() < (representativeRequests.get() - getNumRepresentativeRequestsCollected()));
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

    public Long getUserIndex() {
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

    public List<String> getUserNifList() {
        List<String> result = new ArrayList<String>();
        if(usersWithRepresentativeList != null && !usersWithRepresentativeList.isEmpty())
            result.addAll(getUsersWithRepresentativeList());
        if(usersWithoutRepresentativeList != null && !usersWithoutRepresentativeList.isEmpty())
            result.addAll(usersWithoutRepresentativeList);
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

    public void sendData(CountDownLatch countDownLatch) {
        log.info("sendData");
        this.countDownLatch = countDownLatch;
        requestExecutor = Executors.newFixedThreadPool(100);
        representativeDataTerminated.set(false);
        usersWithouRepresentativeTerminated.set(false);
        requestCompletionService = new ExecutorCompletionService<ResponseVS>(requestExecutor);
        representativeNifList = Collections.synchronizedList(new ArrayList<String>());
        usersWithRepresentativeList = Collections.synchronizedList(new ArrayList<String>());
        usersWithoutRepresentativeList = Collections.synchronizedList(new ArrayList<String>());

        requestExecutor.execute(new Runnable() {@Override public void run() {
            try {
                createRepresentatives();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }});
        requestExecutor.execute(new Runnable() {@Override public void run() {
            try {
                waitForRepresentativeResponses();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }});
        requestExecutor.execute(new Runnable() {@Override public void run() {
            try {
                createUsersWithoutRepresentative();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }});

    }
    private void createUsersWithoutRepresentative() throws Exception {
        log.info("createUsersWithoutRepresentative - Num. users without representative:" +
                getNumUsersWithoutRepresentativeWithVote());
        for(int i = 1; i <= getNumUsersWithoutRepresentativeWithVote(); i++ ) {
            int userIndex = new Long(getAndIncrementUserIndex()).intValue();
            String userNif = NifUtils.getNif(userIndex);
            SignatureService signatureService = SignatureService.genUserVSSignatureService(userNif);
            usersWithoutRepresentativeList.add(userNif);
            byte[] usertCertPEMBytes = CertUtils.getPEMEncoded(signatureService.getCertSigner());
            String certServiceURL = ContextVS.getInstance().getAccessControl().getUserCertServiceURL();
            ResponseVS responseVS = HttpHelper.getInstance().sendData(
                    usertCertPEMBytes,ContentTypeVS.X509_USER,certServiceURL);
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            if((i % 50) == 0) log.info("createUsersWithoutRepresentative - " + i + " of " +
                    getNumUsersWithoutRepresentativeWithVote());
        }
        usersWithouRepresentativeTerminated.set(true);
        if(representativeDataTerminated.get()) countDownLatch.countDown();
    }

    public void createRepresentatives () throws Exception {
        log.info("createRepresentatives - NumRepresentatives: " + getNumRepresentatives());
        if(getNumRepresentatives() > 0) {
            byte[] imageBytes = ContextVS.getInstance().getResourceBytes("./images/icon_64/fa-user.png");
            while (hasRepresesentativeRequestsPending()){
                if(!waitingForRepresesentativeRequests()) {
                    requestCompletionService.submit(new RepresentativeTestDataSender(NifUtils.getNif(
                            new Long(getAndIncrementUserIndex()).intValue()), imageBytes));
                    getAndIncrementNumRepresentativeRequests();
                } else Thread.sleep(500);
            }
        } else log.info("createRepresentatives - without representative requests");
    }

    public void waitForRepresentativeResponses() throws InterruptedException, ExecutionException, Exception {
        log.info("waitForRepresentativeResponses - NumRepresentatives: " + getNumRepresentatives());
        for (int v = 0; v < getNumRepresentatives(); v++) {
            Future<ResponseVS> f = requestCompletionService.take();
            final ResponseVS responseVS = f.get();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                representativeNifList.add(responseVS.getMessage());
                getAndIncrementNumRepresentativeRequestsOK();
            } else throw new ExceptionVS(responseVS.getMessage());
        }
        initDelegations ();
    }

    private void initDelegations() {
        log.info("initDelegations");
        requestExecutor.execute(new Runnable() { @Override public void run() {
            try {
                createDelegations();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }});
        requestExecutor.execute(new Runnable() { @Override public void run() {
            try {
                waitForDelegationResponses();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }});
    }

    public void createDelegations () throws Exception {
        log.info("createDelegations - Num. users with representative: " + getNumUsersWithRepresentative());
        if(getNumUsersWithRepresentative() > 0) {
            String serviceURL = ContextVS.getInstance().getAccessControl().getDelegationServiceURL();
            while (getNumDelegationRequests() < getNumUsersWithRepresentative()) {
                log.info("getNumDelegationRequests: " + getNumDelegationRequests() +
                        "- getNumUsersWithRepresentative: " + getNumUsersWithRepresentative());
                if((getNumDelegationRequests() - getNumDelegationRequestsCollected()) < getMaxPendingResponses()) {
                    String userNIF = NifUtils.getNif(new Long(getAndIncrementUserIndex()).intValue());
                    String representativeNIF = getRandomRepresentative();
                    requestCompletionService.submit(new RepresentativeDelegatorDataSender(
                            userNIF,representativeNIF, serviceURL));
                    getAndIncrementNumDelegationRequests();
                } else Thread.sleep(200);
            }
        } else log.info("createDelegations - without representative delegations");
    }

    public void waitForDelegationResponses() throws Exception {
        log.info("waitForDelegationResponses - Num. users with representative: " + getNumUsersWithRepresentative());
        if(getNumRepresentatives() > 0) {
            for (int v = 0; v < getNumUsersWithRepresentative(); v++) {
                Future<ResponseVS> f = requestCompletionService.take();
                final ResponseVS response = f.get();
                log.info("Delegation response '" + v + "' - statusCode:" +
                        response.getStatusCode() + " - msg: " + response.getMessage());
                if(ResponseVS.SC_OK == response.getStatusCode()) {
                    usersWithRepresentativeList.add(response.getMessage());
                    getAndIncrementNumDelegationsOK();
                } else throw new ExceptionVS(response.getMessage());
            }
        }
        representativeDataTerminated.set(true);
        if(usersWithouRepresentativeTerminated.get()) countDownLatch.countDown();
    }

    public List<String> getElectorList() {
        List<String> result = new ArrayList<String>(getNumberElectors());
        List<String> tempList = new ArrayList<String>(this.representativeNifList);
        if(getNumRepresentativesWithVote() > 0) {
            for(int i = 0; i < getNumRepresentativesWithVote(); i++) {
                result.add(tempList.remove(ThreadLocalRandom.current().nextInt(tempList.size())));
            }
            log.info("getElectorList - '" + getNumRepresentativesWithVote() + "' representatives with vote");
        }
        tempList = new ArrayList<String>(this.usersWithRepresentativeList);
        for(int i = 0; i < getNumUsersWithRepresentativeWithVote(); i++) {
            result.add(tempList.remove(ThreadLocalRandom.current().nextInt(tempList.size())));
        }
        log.info("getElectorList - '" +  getNumUsersWithRepresentativeWithVote() + "' users WITH representative and vote");
        tempList = new ArrayList<String>(this.usersWithoutRepresentativeList);
        for(int i = 0; i < getNumUsersWithoutRepresentativeWithVote(); i++) {
            result.add(tempList.remove(ThreadLocalRandom.current().nextInt(tempList.size())));
        }
        log.info("getElectorList - '" + getNumUsersWithoutRepresentativeWithVote() +
                "' users WITHOUT representative and vote");
        return result;
    }

    private String getRandomRepresentative() {
        if(representativeNifList.isEmpty()) return null;
        // ThreadLocalRandom are not cryptographically secure
        return representativeNifList.get(ThreadLocalRandom.current().nextInt(representativeNifList.size()));
    }

}

package org.votingsystem.simulation

import com.sun.mail.pop3.POP3Store

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.EventVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.StatusVS
import org.votingsystem.simulation.model.POP3SimulationData
import org.votingsystem.simulation.model.SimulationData
import org.votingsystem.util.DateUtils

import java.util.concurrent.*

class MailSimulationService {

    public enum Status implements StatusVS<Status> {INIT_SIMULATION, SEND_REQUEST, FINISH_SIMULATION}

    private Long broadcastMessageInterval = 10000;
    private Locale locale = new Locale("es")

    def webSocketService
    def contextService
    def messageSource
    private String simulationStarter

    private POP3Store pop3Store;
    private Session session;

    private List<String> errorList = new ArrayList<String>();
    private Set<String> synchronizedListenerSet;

    private static ExecutorService simulatorExecutor;
    private static CompletionService<ResponseVS> signCompletionService;
    private Timer broadcastTimer;
    private POP3SimulationData simulationData;
    private EventVS eventVS;

    public void processRequest(JSONObject messageJSON) {
        log.debug("--- processRequest - status: '${messageJSON?.status}'")
        try {
            Status status = Status.valueOf(messageJSON?.status);
            switch(status) {
                case Status.INIT_SIMULATION:
                    if(simulationData?.isRunning()) {
                        log.error("INIT_SIMULATION ERROR - Simulation Running")
                        Map responseMap = [userId: messageJSON.userId, message:"Simulation already running",
                                statusCode:ResponseVS.SC_ERROR, service:this.getClass().getSimpleName()]
                        webSocketService.processResponse(new JSONObject(responseMap))
                    } else initSimulation(messageJSON)
                    break;
                case Status.FINISH_SIMULATION:
                    if(simulationStarter?.equals(messageJSON.userId)) {
                        String message = messageSource.getMessage("simulationCancelledByUserMsg", null, locale) +
                                " - message: ${messageJSON.message}"
                        finishSimulation(new ResponseVS(ResponseVS.SC_CANCELLED, message));
                    }
                    break;
                default:
                    log.error("UNKNOWN STATUS ${messageJSON.status}")
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
        }
    }

    private void initSimulation(JSONObject simulationDataJSON) {
        log.debug("initSimulation ### Enter status INIT_SIMULATION")
        simulationStarter = simulationDataJSON.userId
        synchronizedListenerSet = Collections.synchronizedSet(new HashSet<String>())
        synchronizedListenerSet.add(simulationStarter)
        simulationData = POP3SimulationData.parse(simulationDataJSON)
        log.debug("initSimulation - numRequestsProjected: " + simulationData.numRequestsProjected)

        Properties properties = new Properties();
        properties.put("mail.smtp.host", simulationData.getSmtpHostName());
        properties.put("mail.host", simulationData.getPop3HostName());
        properties.put("mail.store.protocol", "pop3");


        session = Session.getInstance(properties, null);
        pop3Store = (POP3Store) session.getStore();
        pop3Store.connect(simulationData.getUserName(),
                simulationData.getPassword());
        System.out.println("store.isConnected(): " + pop3Store.isConnected());

        Folder remoteInbox = pop3Store.getFolder("INBOX");
        remoteInbox.open(Folder.READ_WRITE);

        contextService.init();
        simulationData.init(System.currentTimeMillis());
        startBroadcatsTimer();
        errorList = new ArrayList<String>();
        changeSimulationStatus(new ResponseVS(ResponseVS.SC_OK, Status.INIT_SIMULATION, null));
    }

    public void startBroadcatsTimer() throws Exception {
        log.debug("startBroadcatsTimer - interval between broadcasts: '${broadcastMessageInterval}' milliseconds");
        broadcastTimer = new Timer();
        broadcastTimer.schedule(new BroadcastTimerTask(simulationData, broadcastTimer), 0, broadcastMessageInterval);
    }

    class BroadcastTimerTask extends TimerTask {

        private SimulationData simulationData;
        private Timer launcher;

        public BroadcastTimerTask(SimulationData simulationData, Timer launcher) {
            this.simulationData = simulationData;
            this.launcher = launcher;
        }

        public void run() {
            //log.debug("======== BroadcastTimer run")
            if(ResponseVS.SC_PROCESSING == simulationData.getStatusCode()) {
                Map messageMap = [statusCode:ResponseVS.SC_PROCESSING, simulationData:simulationData.getDataMap()]
                Map broadcastResul = webSocketService.broadcastList(messageMap, synchronizedListenerSet);
                if(ResponseVS.SC_OK != broadcastResul.statusCode) {
                    broadcastResul.errorList.each {synchronizedListenerSet.remove(it)}
                }
            } else {
                Logger.getLogger(AccessRequestSimulationService.class).debug("Cancelling BroadcastTimerTask - statusCode(): "
                        + simulationData.getStatusCode() + " - message: " + simulationData.getMessage())
                launcher.cancel();
            }
        }
    }

    private void sendRequests(){
        log.debug("sendVotes ### Enter status SEND_REQUEST");
        if(!(simulationData.getNumRequestsProjected() > 0)) {
            log.debug("WITHOUT NumberOfRequestsProjected");
            return;
        }
        log.debug("--------------- launchRequests - NumRequestsProjected: " + simulationData.getNumRequestsProjected());

        simulatorExecutor = Executors.newFixedThreadPool(100);
        signCompletionService = new ExecutorCompletionService<ResponseVS>(simulatorExecutor);
        try {
            simulatorExecutor.execute(new Runnable() {
                @Override public void run() { sendMails(); }
            });
            simulatorExecutor.execute(new Runnable() {
                @Override public void run() { readMails(); }
            });
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
            changeSimulationStatus(new ResponseVS(ResponseVS.SC_ERROR, Status.SEND_REQUEST, ex.getMessage()));
        }
    }

    private void sendMails () throws Exception {
        log.debug("sendMails - num. mails to send: " + simulationData.getNumRequestsProjected());
        for(int i = 0; i < simulationData.getNumRequestsProjected(); i++) {
            launchEmail("ControlAcceso@sistemavotacion.org", simulationData.getUserName() + "@" +
                            simulationData.getDomainname(), "Message content", "Message subject");
        }
        log.debug("sendMails - sent ${simulationData.getNumRequestsProjected()} mails");
    }

    public void launchEmail(String fromUser, String touser, String textoSign,
                            String subject, Header... headers) throws Exception {
        //logger.debug("launchEmail - fromUser: " + fromUser + " - touser: " + touser);
        MimeMessage message = new MimeMessage(session);
        InternetAddress from = new InternetAddress(fromUser);
        //Set from address to message
        message.setFrom(from);
        InternetAddress to = new InternetAddress(touser);
        message.addRecipient(Message.RecipientType.TO, to);
        message.setSubject(subject);
        message.setText(textoSign);
        Transport.send(message);
    }


    private void readMails () throws Exception {
        log.debug(" - readMails");
        int numIterations = 1;
        while (simulationData.getNumRequestsColected() < simulationData.getNumRequestsProjected()) {
            log.debug("simulationData.getNumRequestsColected(): " + simulationData.getNumRequestsColected());
            try {
                Folder remoteInbox = pop3Store.getFolder("INBOX");
                remoteInbox.open(Folder.READ_WRITE);
                Message[] messages = remoteInbox.getMessages();
                if(messages.length > 100 || numIterations == 3) {
                    System.out.println("messages.length: " + messages.length);
                    remoteInbox.setFlags(messages, new Flags(Flags.Flag.DELETED), true);
                    simulationData.getAndAddNumRequestsOK(messages.length);
                } else {
                    numIterations++;
                    //Thread.sleep(20000);
                    Thread.sleep(1000);
                }
                remoteInbox.close(true);
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        log.debug("======= simulationData.getNumRequestsColected(): " + simulationData.getNumRequestsColected());
    }

    private void finishSimulation(ResponseVS responseVS) {
        log.debug(" --- finishSimulation Enter status FINISH_SIMULATION - status: ${responseVS.statusCode}")

        simulationData.finish(responseVS.getStatusCode(), System.currentTimeMillis());
        if(broadcastTimer != null) broadcastTimer.cancel();

        if(simulatorExecutor != null) simulatorExecutor.shutdown();

        log.info("Begin: " + DateUtils.getStringFromDate( simulationData.getBeginDate())  + " - Duration: " +
                simulationData.getDurationStr());
        log.info("------- SIMULATION RESULT for EventVS: " + eventVS?.getId());
        log.info("Number of projected requests: " + simulationData.getNumRequestsProjected());
        log.info("Number of completed requests: " + simulationData.getNumRequests());
        log.info("Number of signatures OK: " + simulationData.getNumRequestsOK());
        log.info("Number of signatures ERROR: " + simulationData.getNumRequestsERROR());

        String message = responseVS.getMessage();
        if(!errorList.isEmpty()) {
            String errorsMsg = StringUtils.getFormattedErrorList(errorList);
            if(message == null) message = errorsMsg;
            else message = message + "\n" + errorsMsg;
            log.info(" ************* " + errorList.size() + " ERRORS: \n" + errorsMsg);
        }
        simulationData.statusCode = responseVS.getStatusCode()
        simulationData.message = message
        responseVS.setStatus(Status.FINISH_SIMULATION)
        changeSimulationStatus(responseVS);
    }

    private void changeSimulationStatus (ResponseVS statusFromResponse) {
        log.debug("changeSimulationStatus - statusFrom: '${statusFromResponse.getStatus()}' " +
                " - statusCode: ${statusFromResponse.getStatusCode()}")
        if(ResponseVS.SC_OK != statusFromResponse.getStatusCode())
            log.debug("statusFromResponse message: ${statusFromResponse.getMessage()}")
        try {
            switch(statusFromResponse.getStatus()) {
                case Status.INIT_SIMULATION:
                    if(ResponseVS.SC_OK == statusFromResponse.getStatusCode()) {
                        sendRequests();
                    } else finishSimulation(statusFromResponse);
                    break;
                case Status.SEND_REQUEST:
                    finishSimulation(statusFromResponse);
                    break;
                case Status.FINISH_SIMULATION:
                    Map messageMap = [statusCode:statusFromResponse.statusCode,
                            status:statusFromResponse.status.toString(),
                            message:statusFromResponse.message, simulationData:simulationData.getDataMap()]
                    webSocketService.broadcastList(messageMap, synchronizedListenerSet)
                    break;
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex)
            finishSimulation(new ResponseVS(ResponseVS.SC_ERROR, statusFromResponse.getStatus() , ex.getMessage()));
        }
    }


}
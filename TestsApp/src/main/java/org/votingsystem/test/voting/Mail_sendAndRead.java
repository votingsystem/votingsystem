package org.votingsystem.test.voting;

import com.sun.mail.pop3.POP3Store;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.util.POP3SimulationData;
import org.votingsystem.test.util.TestUtils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class Mail_sendAndRead {

    private static Logger log;
    private static Session session;
    private static POP3Store pop3Store;
    private static POP3SimulationData simulationData;
    private static ExecutorCompletionService completionService;
    
    public static void main(String[] args) throws Exception {
        Map simulationDataMap = new HashMap<>();
        simulationDataMap.put("smtpHostName", "localhost");
        simulationDataMap.put("pop3HostName", "localhost");
        simulationDataMap.put("userName", "voting_system_access_control");
        simulationDataMap.put("domainName", "votingsystem.org");
        simulationDataMap.put("password", "1234");
        simulationDataMap.put("maxPendingResponses", 10);
        simulationDataMap.put("numRequestsProjected", 2);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationDataMap.put("timer", timerMap);

        log = TestUtils.init(Mail_sendAndRead.class, simulationDataMap);
        simulationData = POP3SimulationData.parse(simulationDataMap);

        Properties properties = new Properties();
        properties.put("mail.smtp.host", simulationData.getSmtpHostName());
        properties.put("mail.host", simulationData.getPop3HostName());
        properties.put("mail.store.protocol", "pop3");

        session = Session.getInstance(properties, null);
        pop3Store = (POP3Store) session.getStore();
        pop3Store.connect(simulationData.getUserName(), simulationData.getPassword());
        System.out.println("store.isConnected(): " + pop3Store.isConnected());
        Folder remoteInbox = pop3Store.getFolder("INBOX");
        remoteInbox.open(Folder.READ_WRITE);
        initSimulation();
    }
    private static void initSimulation(){
        log.info("initSimulation");
        if(!(TestUtils.getSimulationData().getNumRequestsProjected() > 0)) {
            log.info("WITHOUT NumberOfRequestsProjected");
            return;
        }
        log.info("initSimulation - NumRequestsProjected: " + TestUtils.getSimulationData().getNumRequestsProjected());
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        completionService = new ExecutorCompletionService<ResponseVS>(executorService);
        executorService.execute(new Runnable() {
            @Override public void run() {
                try {
                    sendMails();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        executorService.execute(new Runnable() {
            @Override public void run() {
                try {
                    readMails();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void sendMails() throws Exception {
        log.info("sendMails - num. mails to send: " + simulationData.getNumRequestsProjected());
        for(int i = 0; i < simulationData.getNumRequestsProjected(); i++) {
            MimeMessage message = new MimeMessage(session);
            InternetAddress from = new InternetAddress("access_control@votingsystem.org");
            message.setFrom(from);
            InternetAddress to = new InternetAddress(simulationData.getUserName() + "@" + simulationData.getDomainname());
            message.addRecipient(Message.RecipientType.TO, to);
            message.setSubject("Message subject");
            message.setText("Message content");
            Transport.send(message);
        }
        log.info("sendMails - mails sended: " + simulationData.getNumRequestsProjected());
    }

    private static void readMails() throws Exception {
        log.info("readMails");
        int numIterations = 1;
        while (simulationData.getNumRequestsCollected() < simulationData.getNumRequestsProjected()) {
            log.info("Num. requests collected: " + simulationData.getNumRequestsCollected());
            Folder remoteInbox = pop3Store.getFolder("INBOX");
            remoteInbox.open(Folder.READ_WRITE);
            Message[] messages = remoteInbox.getMessages();
            if(messages.length > 100 || numIterations == 3) {
                remoteInbox.setFlags(messages, new Flags(Flags.Flag.DELETED), true);
                simulationData.getAndAddNumRequestsOK(messages.length);
            } else {
                numIterations++;
                //Thread.sleep(20000);
                Thread.sleep(1000);
            }
            remoteInbox.close(true);
        }
        TestUtils.finish("OK - Num. requests completed: " + simulationData.getNumRequestsCollected());
    }

}




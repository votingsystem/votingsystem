package org.votingsystem.test.voting

import com.sun.mail.pop3.POP3Store
import net.sf.json.JSONSerializer
import org.votingsystem.model.ResponseVS
import org.votingsystem.test.model.POP3SimulationData
import org.votingsystem.test.util.TestUtils

import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

Map simulationDataMap = [smtpHostName:"localhost", pop3HostName:"localhost", userName:"voting_system_access_control",
                         domainName:"votingsystem.org", password:"1234", maxPendingResponses:10,
                         numRequestsProjected:2, timer:[active:false, time:"00:00:10"]]


log = TestUtils.init(Multisign_send.class, simulationDataMap)

simulationData = POP3SimulationData.parse(JSONSerializer.toJSON(simulationDataMap))

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

initSimulation()

private void initSimulation(){
    log.debug("initSimulation");
    if(!(TestUtils.simulationData.getNumRequestsProjected() > 0)) {
        log.debug("WITHOUT NumberOfRequestsProjected");
        return;
    }
    log.debug("initSimulation - NumRequestsProjected: " + TestUtils.simulationData.getNumRequestsProjected());
    ExecutorService executorService = Executors.newFixedThreadPool(100);
    signCompletionService = new ExecutorCompletionService<ResponseVS>(executorService);
    executorService.execute(new Runnable() {
        @Override public void run() { sendMails(); }
    });
    executorService.execute(new Runnable() {
        @Override public void run() { readMails(); }
    });
}

private void sendMails() throws Exception {
    log.debug("sendMails - num. mails to send: " + simulationData.getNumRequestsProjected());
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
    log.debug("sendMails - '${simulationData.getNumRequestsProjected()}' mails sended");
}

private void readMails () throws Exception {
    log.debug("readMails");
    int numIterations = 1;
    while (simulationData.getNumRequestsColected() < simulationData.getNumRequestsProjected()) {
        log.debug("Num. requests colected: " + simulationData.getNumRequestsColected());
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
    TestUtils.finish("OK - Num. requests completed: " + simulationData.getNumRequestsColected())
}

package org.sistemavotacion.test.simulation;

import com.sun.mail.pop3.POP3Store;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.POP3SimulationData;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class POP3Simulator implements Callable {
            
    private static Logger logger = LoggerFactory.getLogger(POP3Simulator.class);
    
    private POP3SimulationData simulationData;
    private POP3Store pop3Store;
    private final Session session;
    private final AtomicBoolean done = new AtomicBoolean(false);
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    
    public POP3Simulator(POP3SimulationData simulationData) throws 
            NoSuchProviderException, MessagingException {
        this.simulationData = simulationData;
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
        
    }
    
    @Override public Respuesta call() { 
        logger.debug(" - call - process: " + ManagementFactory.
                getRuntimeMXBean().getName());
        try {
            simulationData.setBegin(System.currentTimeMillis());
            ContextoPruebas.INSTANCE.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        sendMails();
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    }
                });
            ContextoPruebas.INSTANCE.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        readMails();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            });
            countDownLatch.await();
            simulationData.setFinish(System.currentTimeMillis());
            logger.info("Begin: " + DateUtils.getStringFromDate(
                simulationData.getBeginDate())  + " - Duration: " + 
                simulationData.getDurationStr());
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        return null;
    }
    
    private void sendMails () throws Exception {
        logger.debug("sendMails - num. mails to send: " + 
                simulationData.getNumRequestsProjected());
        for(int i = 0; i < simulationData.getNumRequestsProjected(); i++) {
            launchEmail("ControlAcceso@sistemavotacion.org", 
                    simulationData.getUserName() + "@" + 
                    simulationData.getDomainname(), 
                    "Contenido del mensaje", "Asunto del mensaje");
        }
        logger.debug("sendMails - sent " + simulationData.getNumRequestsProjected() 
                + " mails");
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
        logger.debug(" - readMails");
        int numIterations = 1;
        while (simulationData.getNumRequestsColected() < 
                simulationData.getNumRequestsProjected()) {
            logger.debug("simulationData.getNumRequestsColected(): " + 
                simulationData.getNumRequestsColected());
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
        logger.debug("======= simulationData.getNumRequestsColected(): " + 
                simulationData.getNumRequestsColected());
        countDownLatch.countDown();
    }
    
    public static void main(String[] args) throws Exception {
        POP3SimulationData simulationData = null;
        if(args != null && args.length > 0) {
            logger.debug("args[0]");
            simulationData = POP3SimulationData.parse(args[0]);
        } else {
            File jsonFile = File.createTempFile("MBoxSimulationData", ".json");
            jsonFile.deleteOnExit();
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("simulatorFiles/pop3SimulationData.json"), jsonFile); 
            simulationData = POP3SimulationData.parse(FileUtils.getStringFromFile(jsonFile));
        }
        POP3Simulator simuHelper = new POP3Simulator(simulationData);
        simuHelper.call();
        System.exit(0);
    }
    
}

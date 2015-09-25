package org.votingsystem.test.ejb;

import org.votingsystem.service.VotingSystemRemote;
import org.votingsystem.test.util.IOUtils;
import org.votingsystem.util.NifUtils;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EJBClient {

    private static final Logger logger = Logger.getLogger(EJBClient.class.getName());

    public static void main(String[] args) throws Exception {
        Logger.getLogger("org.jboss").setLevel(Level.SEVERE);
        Logger.getLogger("org.votingsystem").setLevel(Level.FINE);
        new EJBClient().run();
    }

    private final Context context;
    private final List<Future<String>> lastCommands = new ArrayList<>();
    private VotingSystemRemote votingSystemRemote;

    public EJBClient() throws NamingException {
        final Properties jndiProperties = new Properties();
        jndiProperties.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        jndiProperties.put("jboss.naming.client.ejb.context", true);
        this.context = new InitialContext(jndiProperties);
    }

    private enum Command {
        VALIDATE_CSR, TEST_ASYNC, GET_MESSAGES, QUIT, INVALID;

        public static Command parseCommand(String stringCommand) {
            try {
                return valueOf(stringCommand.trim().toUpperCase());
            } catch (IllegalArgumentException iae) {
                return INVALID;
            }
        }
    }

    private void run() throws NamingException {
        this.votingSystemRemote = lookupVotingSystemRemoteEJB();
        showWelcomeMessage();
        while (true) {
            final String stringCommand = IOUtils.readLine("> ");
            final Command command = Command.parseCommand(stringCommand);
            switch (command) {
                case VALIDATE_CSR:
                    validateCSR();
                    break;
                case TEST_ASYNC:
                    testAsync();
                    break;
                case GET_MESSAGES:
                    getMessages();
                    break;
                case QUIT:
                    handleQuit();
                    break;
                default:
                    logger.warning("Unknown command " + stringCommand);
            }
        }
    }

    private void validateCSR() {
        String nif = null, deviceId;
        deviceId = IOUtils.readLine("enter deviceId: ");
        try {
            nif = NifUtils.validate(IOUtils.readLine("enter nif: "));;
        } catch (Exception e1) {
            logger.warning("wrong nif: " + nif);
            return;
        }
        try {
            final String retVal = votingSystemRemote.validateCSR(nif, deviceId);
            System.out.println(retVal);
        } catch (Exception e) {
            logger.warning(e.getMessage());
            return;
        }
    }

    private void testAsync() {
        String asyncMessage = IOUtils.readLine("Enter async message: ");
        lastCommands.add(votingSystemRemote.testAsync(asyncMessage));
        logger.info("async message - wait for response");
    }

    private void getMessages() {
        boolean displayed = false;
        final List<Future<String>> notFinished = new ArrayList<>();
        for (Future<String> booking : lastCommands) {
            if (booking.isDone()) {
                try {
                    final String result = booking.get();
                    logger.info("message received: " + result);
                    displayed = true;
                } catch (InterruptedException | ExecutionException e) {
                    logger.warning(e.getMessage());
                }
            } else {
                notFinished.add(booking);
            }
        }
        lastCommands.retainAll(notFinished);
        if (!displayed) {
            logger.info("no message received!");
        }
    }

    private void handleQuit() {
        logger.info("handleQuit");
        System.exit(0);
    }

    private VotingSystemRemote lookupVotingSystemRemoteEJB() throws NamingException {
        return (VotingSystemRemote) context.lookup("ejb:/AccessControl/RemoteTestBean!" + VotingSystemRemote.class.getName());

    }

    private void showWelcomeMessage() {
        System.out.println("voting system remote EJB client");
        System.out.println("------------------------------------------------------");
        System.out.println("Commands: validate_csr, test_async, get_messages, quit");
    }
}

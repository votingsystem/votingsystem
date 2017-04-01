package org.votingsystem.ejb;

import org.votingsystem.BaseTest;
import org.votingsystem.model.User;
import org.votingsystem.service.EJBAdminRemoteIdProvider;
import org.votingsystem.test.util.IOUtils;
import org.votingsystem.util.Constants;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.NifUtils;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

public class EJBClient extends BaseTest {


    private static final Logger log =  Logger.getLogger(EJBClient.class.getName());


    public static void main(String[] args) throws Exception {

        new EJBClient().run();
    }

    private final Context context;
    private EJBAdminRemoteIdProvider idProviderRemote;


    public EJBClient() throws NamingException {
        super();
        final Properties jndiProperties = new Properties();
        jndiProperties.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        jndiProperties.put("jboss.naming.client.ejb.context", true);
        this.context = new InitialContext(jndiProperties);
    }

    private enum Command {
        INIT_CURRENCY_PERIOD, VALIDATE_CSR, KEYSTORE_NEW, QUIT;

        public static Command parseCommand(String stringCommand) {
            try {
                return valueOf(stringCommand.trim().toUpperCase());
            } catch (IllegalArgumentException iae) {
                log.warning("Unknown command " + stringCommand);
                throw iae;
            }
        }
    }

    private void run() throws Exception {
        lookupRemoteBeans();
        showWelcomeMessage();
        while (true) {
            final String stringCommand = IOUtils.readLine("> ");
            try {
                final Command command = Command.parseCommand(stringCommand);
                switch (command) {
                    case VALIDATE_CSR:
                        validateCSR();
                        break;
                    case KEYSTORE_NEW:
                        newKeyStore();
                        break;
                    case QUIT:
                        handleQuit();
                        break;
                    default:
                        log.warning("Unknown command " + stringCommand);
                }
            } catch (Exception ex) {
                log.warning(ex.getMessage());
            }
        }
    }

    private void newKeyStore() throws Exception {
        User.Type type =  null;
        String givenName = null;
        String surname = null;
        String nif = null;
        while(type == null) {
            String typeInput = IOUtils.readLine("enter type (user, entity, timestamp_server): ");
            switch (typeInput) {
                case "user":
                    type = User.Type.USER;
                    givenName = IOUtils.readLine("enter givenName: ");
                    surname = IOUtils.readLine("enter surname: ");
                    nif = IOUtils.readLine("enter nif: ");
                    break;
                case "entity":
                    type = User.Type.ENTITY;
                    givenName = IOUtils.readLine("enter name: ");
                    break;
                case "timestamp_server":
                    type = User.Type.TIMESTAMP_SERVER;
                    givenName = IOUtils.readLine("enter name: ");
                    break;
                default:
                    type = null;
            }
        }
        char[] password = IOUtils.readPassword("enter key password: ");
        byte[] keyStoreBytes;
        if(type == User.Type.ENTITY || type == User.Type.TIMESTAMP_SERVER) {
            keyStoreBytes = idProviderRemote.generateSystemEntityKeyStore(type, givenName, Constants.USER_CERT_ALIAS, password);
        } else {
            keyStoreBytes = idProviderRemote.generateUserKeyStore(givenName, surname, nif, password);
        }
        File outputFile = FileUtils.copyBytesToFile(keyStoreBytes, new File(System.getProperty("user.home") +
                "/" + givenName.replace(" ", "") + ".jks"));
        System.out.println("KeyStore saved: " + outputFile.getAbsolutePath());
        System.exit(0);
    }
    
    private void validateCSR() {
        String nif = null, deviceId;
        deviceId = IOUtils.readLine("enter deviceId: ");
        try {
            nif = NifUtils.validate(IOUtils.readLine("enter nif: "));;
        } catch (Exception e1) {
            log.warning("wrong nif: " + nif);
            return;
        }
        try {
            final String retVal = idProviderRemote.validateCSR(nif, deviceId);
            System.out.println(retVal);
        } catch (Exception e) {
            log.warning(e.getMessage());
            return;
        }
    }

    private void handleQuit() {
        log.info("handleQuit");
        System.exit(0);
    }

    private void lookupRemoteBeans() throws NamingException {
        idProviderRemote =  (EJBAdminRemoteIdProvider) context.lookup(
                "ejb:/votingsystem-idprovider/AdminRemoteEJB!" + EJBAdminRemoteIdProvider.class.getName());

    }

    private void showWelcomeMessage() {
        System.out.println("------------------------------------------------------");
        System.out.println("voting system remote EJB client");
        System.out.println("------------------------------------------------------");
        String commands = Arrays.asList(Command.values()).stream().map(c -> c.toString().toLowerCase()).reduce(
                (t, u) -> t + ", " + u).get();
        System.out.println("Commands:" + commands);
    }

}

package org.votingsystem.test.ejb;

import org.votingsystem.model.ActorVS;
import org.votingsystem.model.KeyStoreVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.service.EJBRemoteAdminAccessControl;
import org.votingsystem.service.EJBRemoteAdminCurrencyServer;
import org.votingsystem.test.util.IOUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.crypto.KeyStoreUtil;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EJBClient {

    private static final Logger log =  Logger.getLogger(EJBClient.class.getName());

    private static final String ROOT_KEY_ALIAS     = "rootKeys";
    private static final String ROOT_KEY_PASSW     = "PemPass";
    private static final String ROOT_KEYSTORE_PATH = "./TestsApp/RootTest.jks";

    public static void main(String[] args) throws Exception {
        log.getLogger("org.jboss").setLevel(Level.SEVERE);
        log.getLogger("org.votingsystem").setLevel(Level.FINE);
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        new EJBClient().run();
    }

    private final Context context;
    private EJBRemoteAdminAccessControl votingSystemRemote;
    private EJBRemoteAdminCurrencyServer currencyServerAdmin;

    public EJBClient() throws NamingException {
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
                    case INIT_CURRENCY_PERIOD:
                        initWeekPeriod();
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
        ActorVS.Type type =  null;
        String givenName = null;
        String surname = null;
        String keyAlias = null;
        while(type == null) {
            String typeInput = IOUtils.readLine("enter type (user, server, timestamp_authority): ");
            switch (typeInput) {
                case "user":
                    type = ActorVS.Type.USER;
                    givenName = IOUtils.readLine("enter givenName: ");
                    surname = IOUtils.readLine("enter surname: ");
                    break;
                case "server":
                    type = ActorVS.Type.SERVER;
                    givenName = IOUtils.readLine("enter name: ");
                    keyAlias = IOUtils.readLine("enter keyAlias: ");
                    break;
                case "timestamp_authority":
                    type = ActorVS.Type.TIMESTAMP_SERVER;
                    givenName = IOUtils.readLine("enter name: ");
                    keyAlias = IOUtils.readLine("enter keyAlias: ");
                    break;
                default:
                    type = null;
            }
        }
        String nif = IOUtils.readLine("enter nif: ");
        char[] password = IOUtils.readLine("enter key password: ").toCharArray();
        byte[] keyStoreBytes = null;
        if(type == ActorVS.Type.SERVER || type == ActorVS.Type.TIMESTAMP_SERVER) {
            File rootKeyStoreFile = new File(ROOT_KEYSTORE_PATH);
            KeyStoreVS rootKeyStore = null;
            if(rootKeyStoreFile.exists()) {
                rootKeyStore = new KeyStoreVS(ROOT_KEY_ALIAS, FileUtils.getBytesFromFile(rootKeyStoreFile), null, null);
                rootKeyStore.setPassword(ROOT_KEY_PASSW);
                keyStoreBytes = votingSystemRemote.generateServerKeyStore(type, givenName, keyAlias, nif, password,
                        rootKeyStore);
            } else {
                rootKeyStore = generateRootKeyStore();
                keyStoreBytes = votingSystemRemote.generateServerKeyStore(type, givenName, keyAlias, nif, password,
                        rootKeyStore);
                log.info(" --- NEW ROOT KEYSTORE: " + FileUtils.copyBytesToFile(rootKeyStore.getBytes(),
                        new File(ROOT_KEYSTORE_PATH)).getAbsolutePath());
            }
        } else {
            //keyStoreBytes = votingSystemRemote.generateUserKeyStore(givenName, surname, nif, password);
            keyStoreBytes = currencyServerAdmin.generateUserKeyStore(givenName, surname, nif, password);
        }
        File outputFile = FileUtils.copyBytesToFile(keyStoreBytes, new File(System.getProperty("user.home") +
                "/" + givenName.replace(" ", "") + ".jks"));
        System.out.println("KeyStore saved: " + outputFile.getAbsolutePath());
        System.exit(0);
    }

    //This is only for testing
    private KeyStoreVS generateRootKeyStore() throws Exception {
        Date validFrom = Calendar.getInstance().getTime();
        Calendar today_plus_year = Calendar.getInstance();
        today_plus_year.add(Calendar.YEAR, 1);
        today_plus_year.set(Calendar.HOUR_OF_DAY, 0);
        today_plus_year.set(Calendar.MINUTE, 0);
        today_plus_year.set(Calendar.SECOND, 0);
        Date validTo = today_plus_year.getTime();
        KeyStoreVS keyStoreVS = new KeyStoreVS(ROOT_KEY_ALIAS, null, validFrom, validTo);
        keyStoreVS.setPassword(ROOT_KEY_PASSW);
        KeyStore keyStore = KeyStoreUtil.createRootKeyStore(validFrom, validTo,
                keyStoreVS.getPassword().toCharArray(), keyStoreVS.getKeyAlias(),
                "CN=Voting System Certificate Authority, OU=Certs");
        keyStoreVS.setBytes(KeyStoreUtil.getBytes(keyStore, keyStoreVS.getPassword().toCharArray()));
        return keyStoreVS;
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
            final String retVal = votingSystemRemote.validateCSR(nif, deviceId);
            System.out.println(retVal);
        } catch (Exception e) {
            log.warning(e.getMessage());
            return;
        }
    }

    private void initWeekPeriod() {
        try {
            CompletableFuture<ResponseVS> future = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            Future<ResponseVS> resp = currencyServerAdmin.initWeekPeriod(Calendar.getInstance());
                            return resp.get();
                        } catch (Exception e) {
                            log.log(Level.SEVERE, e.getMessage(), e);
                            return ResponseVS.ERROR(e.getMessage());
                        }
                    }).thenApply(result -> {
                        log.info(String.format("command: INIT_CURRENCY_PERIOD - statusCode: %S - message: %s",
                                result.getStatusCode(), result.getMessage()));
                        return result;
                    });
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void handleQuit() {
        log.info("handleQuit");
        System.exit(0);
    }

    private void lookupRemoteBeans() throws NamingException {
        votingSystemRemote =  (EJBRemoteAdminAccessControl) context.lookup(
                "ejb:/AccessControl/RemoteAdminBean!" + EJBRemoteAdminAccessControl.class.getName());
        currencyServerAdmin = (EJBRemoteAdminCurrencyServer)context.lookup(
                "ejb:/CurrencyServer/RemoteAdminBean!" + EJBRemoteAdminCurrencyServer.class.getName());
    }

    private void showWelcomeMessage() {
        System.out.println("voting system remote EJB admin");
        System.out.println("------------------------------------------------------");
        String commands = Arrays.asList(Command.values()).stream().map(c -> c.toString().toLowerCase()).reduce(
                (t, u) -> t + ", " + u).get();
        System.out.println("Commands:" + commands);
    }
}

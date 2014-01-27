package org.votingsystem.android;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.itextpdf.text.Context_iTextVS;

import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ControlCenterVS;
import org.votingsystem.model.CurrencyData;
import org.votingsystem.model.CurrencyVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.TicketAccount;
import org.votingsystem.model.TicketServer;
import org.votingsystem.model.TicketVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.VotingSystemKeyGenerator;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.ObjectUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.votingsystem.model.ContextVS.ALGORITHM_RNG;
import static org.votingsystem.model.ContextVS.KEY_SIZE;
import static org.votingsystem.model.ContextVS.NIF_KEY;
import static org.votingsystem.model.ContextVS.PROVIDER;
import static org.votingsystem.model.ContextVS.SIG_NAME;
import static org.votingsystem.model.ContextVS.STATE_KEY;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.USER_DATA_FILE_NAME;
import static org.votingsystem.model.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class AppContextVS extends Application {

    public static final String TAG = "AppContextVS";

    private State state = State.WITHOUT_CSR;
    private String ticketServerURL;
    private AccessControlVS accessControl;
    private ControlCenterVS controlCenter;
    private TicketServer ticketServer;
    private UserVS userVS;
    private Map<String, X509Certificate> certsMap = new HashMap<String, X509Certificate>();
    private OperationVS operationVS = null;


	@Override public void onCreate() {
        //System.setProperty("android.os.Build.ID", android.os.Build.ID);
        Log.d(TAG + ".onCreate()", "");
        try {
            Context_iTextVS.init(getApplicationContext());
            VotingSystemKeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
            Properties props = new Properties();
            props.load(getAssets().open("VotingSystem.properties"));
            ticketServerURL = props.getProperty(ContextVS.TICKET_SERVER_URL);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
	}


    public String getTicketServerURL() {
        return ticketServerURL;
    }

    public String getHostID() {
        return android.os.Build.ID;
    }

    public OperationVS getOperationVS() {
        return operationVS;
    }

    public void setOperationVS(OperationVS operationVS) {
        if(operationVS == null) Log.d(TAG + ".setOperationVS(...)", "- removing pending operationVS");
        else Log.d(TAG + ".setOperationVS(...)", "- operationVS: " + operationVS.getTypeVS());
        this.operationVS = operationVS;
    }

    public void setState(State state, String nif) {
        Log.d(TAG + ".setState(...)", STATE_KEY + "_" + accessControl.getServerURL() +
                " - state: " + state.toString());
        SharedPreferences settings = getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(STATE_KEY + "_" + accessControl.getServerURL() , state.toString());
        if(nif != null) editor.putString(NIF_KEY, nif);
        if(State.WITH_CERTIFICATE == state) loadUser();
        editor.commit();
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public X509Certificate getCert(String serverURL) {
        Log.d(TAG + ".getCert(...)", " - getCert - serverURL: " + serverURL);
        if(serverURL == null) return null;
        return certsMap.get(serverURL);
    }

    public void putCert(String serverURL, X509Certificate cert) {
        Log.d(TAG + ".putCert(...)", " serverURL: " + serverURL);
        certsMap.put(serverURL, cert);
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public AccessControlVS getAccessControl() {
        return accessControl;
    }

    public void setAccessControlVS(AccessControlVS accessControl) {
        Log.d(TAG + ".setAccessControlURL() ", " - setAccessControlURL: " +
                accessControl.getServerURL());
        SharedPreferences settings = getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String stateStr = settings.getString(
                STATE_KEY + "_" + accessControl.getServerURL(), State.WITHOUT_CSR.toString());
        state = State.valueOf(stateStr);
        this.accessControl = accessControl;
        loadUser();
    }

    public void loadUser() {
        try {
            File representativeDataFile = new File(getFilesDir(), USER_DATA_FILE_NAME);
            if(representativeDataFile.exists()) {
                byte[] serializedUserData = FileUtils.getBytesFromFile(
                        representativeDataFile);
                userVS = (UserVS) ObjectUtils.deSerializeObject(serializedUserData);
            } else Log.d(TAG + ".setAccessControlVS(...)", "user data not found");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getLapseWeekLbl(Calendar calendar) {
        Calendar thisWeekMonday = DateUtils.getMonday(calendar);
        calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return getString(R.string.week_lapse_lbl, DateUtils.getDate_Es(
                thisWeekMonday.getTime()), DateUtils.getDate_Es(calendar.getTime()));
    }

    public TicketAccount getTicketAccount() {
        String datePrefix =DateUtils.getDirPath(DateUtils.getMonday(
                Calendar.getInstance()).getTime());
        File ticketUserInfoDataFile = new File(getApplicationContext().getFilesDir(),
                datePrefix + ContextVS.TICKET_USER_INFO_DATA_FILE_NAME);
        TicketAccount ticketUserInfo = null;
        try {
            if(ticketUserInfoDataFile.exists()) {
                byte[] serializedTicketUserInfo = FileUtils.getBytesFromFile(ticketUserInfoDataFile);
                ticketUserInfo = (TicketAccount) ObjectUtils.deSerializeObject(
                        serializedTicketUserInfo);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return ticketUserInfo;
    }

    public void getTicketsAccounts(Integer year, Integer month) {
        Log.d(TAG + ".getTicketsAccounts(...)", "year: " + year + " - month: " + month);
        File pathDir = new File(getApplicationContext().getFilesDir(), "/" + year.toString());
        List<File> ticketsAccountsList = FileUtils.searchFiles(pathDir.getAbsolutePath(),
                ContextVS.TICKET_USER_INFO_DATA_FILE_NAME);
        for(File account: ticketsAccountsList) {
            Log.d(TAG + ".getTicketsAccounts(...)", "==== found account: " + account.getAbsolutePath());
        }
    }

    public void setTicketAccount(TicketAccount updatedTicketAccount) {
        try {
            TicketAccount ticketAccount = getTicketAccount();
            Map<CurrencyVS, CurrencyData> newCurrencyMap = null;
            Set<CurrencyVS> keySet = null;
            if(ticketAccount != null) {
                Map<CurrencyVS, CurrencyData> currencyMap = ticketAccount.getCurrencyMap();
                if(currencyMap != null) {
                    keySet = currencyMap.keySet();
                    newCurrencyMap = updatedTicketAccount.getCurrencyMap();
                    for(CurrencyVS currencyVS : keySet) {
                        if(newCurrencyMap != null && newCurrencyMap.containsKey(currencyVS)) {
                            newCurrencyMap.get(currencyVS).setTicketList(currencyMap.get(currencyVS).getTicketList());
                        } else {
                            Log.e(TAG + ".setTicketAccount(...)", "updatedTicketAccount " +
                                    "missing currency data" + currencyVS.toString());
                            CurrencyData currencyData = currencyMap.get(currencyVS);
                            if(currencyData == null) currencyData = new CurrencyData();
                            currencyData.setTransactionList(null);
                            currencyData.setTotalInputs(new BigDecimal(0));
                            currencyData.setTotalOutputs(new BigDecimal(0));
                            currencyData.setLastRequestDate(updatedTicketAccount.getLastRequestDate());
                            if(newCurrencyMap == null) newCurrencyMap =
                                    new HashMap<CurrencyVS, CurrencyData>();
                            newCurrencyMap.put(currencyVS, currencyData);
                            updatedTicketAccount.setCurrencyMap(newCurrencyMap);
                        }
                    }
                }
            }
            keySet = updatedTicketAccount.getCurrencyMap().keySet();
            for(CurrencyVS currencyVS : keySet) {
                CurrencyData currencyData = updatedTicketAccount.getCurrencyMap().get(currencyVS);
                for(TransactionVS transactionVS : currencyData.getTransactionList()) {
                    ContentValues values = new ContentValues();
                    values.put(TransactionVSContentProvider.SQL_INSERT_OR_REPLACE, true);
                    values.put(TransactionVSContentProvider.ID_COL, transactionVS.getId());
                    values.put(TransactionVSContentProvider.URL_COL, transactionVS.getMessageSMIMEURL());
                    values.put(TransactionVSContentProvider.FROM_USER_COL,
                            transactionVS.getFromUserVS().getNif());
                    values.put(TransactionVSContentProvider.TO_USER_COL,
                            transactionVS.getToUserVS().getNif());
                    values.put(TransactionVSContentProvider.SUBJECT_COL, transactionVS.getSubject());
                    values.put(TransactionVSContentProvider.AMOUNT_COL, transactionVS.getAmount().toPlainString());
                    values.put(TransactionVSContentProvider.CURRENCY_COL, transactionVS.getCurrencyVS().toString());
                    values.put(TransactionVSContentProvider.TYPE_COL, transactionVS.getType().toString());
                    values.put(TransactionVSContentProvider.SERIALIZED_OBJECT_COL,
                            ObjectUtils.serializeObject(transactionVS));
                    values.put(TransactionVSContentProvider.WEEK_LAPSE_COL,
                            DateUtils.getDirPath(updatedTicketAccount.getWeekLapse()));
                    values.put(TransactionVSContentProvider.TIMESTAMP_TRANSACTION_COL,
                            transactionVS.getDateCreated().getTime());
                    getContentResolver().insert(TransactionVSContentProvider.CONTENT_URI, values);
                }
                currencyData.setTransactionList(null);
            }
            byte[] ticketUserInfoBytes = ObjectUtils.serializeObject(updatedTicketAccount);
            String datePrefix =DateUtils.getDirPath(DateUtils.getMonday(
                    Calendar.getInstance()).getTime());
            File ticketUserInfoDir = new File(getApplicationContext().getFilesDir(), datePrefix);
            ticketUserInfoDir.mkdirs();
            File ticketUserInfoDataFile = new File(getApplicationContext().getFilesDir(),
                    datePrefix + ContextVS.TICKET_USER_INFO_DATA_FILE_NAME);
            ticketUserInfoDataFile.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(ticketUserInfoDataFile);
            outputStream.write(ticketUserInfoBytes);
            outputStream.close();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void updateTickets(Collection<TicketVS> tickets) {
        try {
            TicketAccount ticketAccount = getTicketAccount();
            for(TicketVS ticketVS : tickets) {
                Map<CurrencyVS, CurrencyData> currencyMap = ticketAccount.getCurrencyMap();
                CurrencyData currencyData = currencyMap.get(ticketVS.getCurrency());
                currencyData.addTicket(ticketVS);
            }
            byte[] ticketUserInfoBytes = ObjectUtils.serializeObject(ticketAccount);
            String datePrefix =DateUtils.getDirPath(DateUtils.getMonday(
                    Calendar.getInstance()).getTime());
            File ticketUserInfoDir = new File(getApplicationContext().getFilesDir(), datePrefix);
            ticketUserInfoDir.mkdirs();
            File ticketUserInfoDataFile = new File(getApplicationContext().getFilesDir(),
                    datePrefix + ContextVS.TICKET_USER_INFO_DATA_FILE_NAME);
            ticketUserInfoDataFile.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(ticketUserInfoDataFile);
            outputStream.write(ticketUserInfoBytes);
            outputStream.close();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setControlCenter(ControlCenterVS controlCenter) {
        this.controlCenter = controlCenter;
    }

    public ControlCenterVS getControlCenter() {
        return controlCenter;
    }

    public TicketServer getTicketServer() {
        return ticketServer;
    }

    public void setTicketServer(TicketServer ticketServer) {
        this.ticketServer = ticketServer;
    }
}

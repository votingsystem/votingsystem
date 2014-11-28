package org.votingsystem.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.vicket.model.Vicket;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletUtils {

    private static Logger log = Logger.getLogger(StringUtils.class);


    public static void saveVicketsToDir(Collection<Vicket> vicketCollection, String walletPath) throws Exception {
        for(Vicket vicket : vicketCollection) {
            byte[] vicketSerialized =  ObjectUtils.serializeObject(vicket);
            new File(walletPath).mkdirs();
            File vicketFile = FileUtils.copyStreamToFile(new ByteArrayInputStream(vicketSerialized), new File(
                    walletPath + UUID.randomUUID().toString() + ContextVS.SERIALIZED_OBJECT_EXTENSION));
            log.debug("stored vicket: " + vicketFile.getAbsolutePath());
        }
    }

    public static List<Map> getSerializedVicketList(Collection<Vicket> vicketCollection)
            throws UnsupportedEncodingException {
        List<Map> result = new ArrayList<>();
        for(Vicket vicket : vicketCollection) {
            Map vicketDataMap = vicket.getCertSubject().getDataMap();
            vicketDataMap.put("isTimeLimited", vicket.getIsTimeLimited());
            vicketDataMap.put("object", ObjectUtils.serializeObjectToString(vicket));
            result.add(vicketDataMap);
        }
        return result;
    }

    public static JSONArray getPlainWallet() throws Exception {
        File walletFile = new File(ContextVS.APPDIR + File.separator + ContextVS.PLAIN_WALLET_FILE_NAME);
        if(!walletFile.exists()) return new JSONArray();
        return (JSONArray) JSONSerializer.toJSON(new String(FileUtils.getBytesFromFile(walletFile), "UTF-8"));
    }

    public static void savePlainWallet(JSONArray walletJSON) throws Exception {
        File walletFile = new File(ContextVS.APPDIR + File.separator + ContextVS.PLAIN_WALLET_FILE_NAME);
        walletFile.createNewFile();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(walletJSON.toString().getBytes()), walletFile);
    }

    public static void saveToPlainWallet(List<Map> serializedVicketList) throws Exception {
        JSONArray storedWalletJSON = (JSONArray) getPlainWallet();
        storedWalletJSON.addAll(serializedVicketList);
        savePlainWallet(storedWalletJSON);
    }

    public static void saveWallet(Object walletJSON, String pin) throws Exception {
        if(walletJSON != null) {
            String pinHash = CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST);
            EncryptedWalletList encryptedWalletList = getEncryptedWalletList();
            WalletWrapper walletWrapper = encryptedWalletList.getWallet(pinHash);
            if(walletWrapper == null && encryptedWalletList.size() > 0)
                throw new ExceptionVS(ContextVS.getMessage("walletFoundErrorMsg"));
            File walletFile = null;
            if(walletWrapper == null && encryptedWalletList.size() == 0) {
                String walletFileName = ContextVS.WALLET_FILE_NAME + "_" + pinHash + ContextVS.WALLET_FILE_EXTENSION;
                walletFile = new File(ContextVS.APPDIR + File.separator + walletFileName);
                walletFile.createNewFile();
            }
            Encryptor.EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(pin, walletJSON.toString().getBytes());
            FileUtils.copyStreamToFile(new ByteArrayInputStream(bundle.toJSON().toString().getBytes("UTF-8")), walletFile);
        }
    }

    public static JSONArray getWallet(String pin) throws Exception {
        String pinHash = CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST);
        String walletFileName = ContextVS.WALLET_FILE_NAME + "_" + pinHash + ContextVS.WALLET_FILE_EXTENSION;
        File walletFile = new File(ContextVS.APPDIR + File.separator + walletFileName);
        if(!walletFile.exists())
            throw new ExceptionVS(ContextVS.getMessage("walletNotFoundErrorMsg"));
        JSONObject bundleJSON = (JSONObject) JSONSerializer.toJSON( FileUtils.getStringFromFile(walletFile));
        Encryptor.EncryptedBundle bundle = Encryptor.EncryptedBundle.parse(bundleJSON);
        byte[] decryptedWalletBytes = Encryptor.pbeAES_Decrypt(pin, bundle);
        return (JSONArray) JSONSerializer.toJSON(new String(decryptedWalletBytes, "UTF-8"));
    }

    public static void changeWalletPin(String newPin, String oldPin) throws Exception {
        JSONArray walletJSON = getWallet(oldPin);
        String oldPinHash = CMSUtils.getHashBase64(oldPin, ContextVS.VOTING_DATA_DIGEST);
        String newPinHash = CMSUtils.getHashBase64(newPin, ContextVS.VOTING_DATA_DIGEST);
        String newWalletFileName = ContextVS.WALLET_FILE_NAME + "_" + newPinHash + ContextVS.WALLET_FILE_EXTENSION;
        File newWalletFile = new File(ContextVS.APPDIR + File.separator + newWalletFileName);
        if(!newWalletFile.createNewFile())
            throw new ExceptionVS(ContextVS.getMessage("walletFoundErrorMsg"));
        Encryptor.EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(newPin, walletJSON.toString().getBytes());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(bundle.toJSON().toString().getBytes("UTF-8")), newWalletFile);
        String oldWalletFileName = ContextVS.WALLET_FILE_NAME + "_" + oldPinHash + ContextVS.WALLET_FILE_EXTENSION;
        File oldWalletFile = new File(ContextVS.APPDIR + File.separator + oldWalletFileName);
        oldWalletFile.delete();
    }

    public static EncryptedWalletList getEncryptedWalletList() {
        File directory = new File(ContextVS.APPDIR);
        String[] resultFiles = directory.list(new FilenameFilter() {
            public boolean accept(File directory, String fileName) {
                return fileName.startsWith(ContextVS.WALLET_FILE_NAME);
            }
        });
        if(resultFiles != null && resultFiles.length > 0) {
            EncryptedWalletList encryptedWalletList = new EncryptedWalletList();
            for(String filePath : resultFiles) {
                encryptedWalletList.addWallet(getWalletWrapper(filePath));
            }
            return encryptedWalletList;
        } else return null;
    }

    public static JSONObject getWalletState() throws Exception {
        JSONObject result = new JSONObject();
        result.put("plainWallet", getPlainWallet());
        EncryptedWalletList encryptedWalletList = getEncryptedWalletList();
        if(encryptedWalletList != null) result.put("encryptedWalletList", getEncryptedWalletList().toJSON());
        return result;
    }

    private static WalletWrapper getWalletWrapper(String filePath) {
        String[] nameParts = filePath.split("_");
        WalletWrapper result = null;
        try {
            result = new WalletWrapper(nameParts[1],  new File(filePath));
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return result;
    }

    private static class WalletWrapper {
        String hash;
        File walletFile;
        WalletWrapper(String hash, File walletFile) {
            this.hash = hash;
            this.walletFile = walletFile;
        }
    }

    private static class EncryptedWalletList {
        Map<String, WalletWrapper> walletList = new HashMap<String, WalletWrapper>();
        EncryptedWalletList() {}
        void addWallet(WalletWrapper walletWrapper) {
            walletList.put(walletWrapper.hash, walletWrapper);
        }
        WalletWrapper getWallet(String hash) {
            return walletList.get(hash);
        }
        JSONArray toJSON() {
            JSONArray result = new JSONArray();
            for(String hash : walletList.keySet()) {
                WalletWrapper walletWrapper = walletList.get(hash);
                JSONObject walletJSON = new JSONObject();
                //walletJSON.put("dateCreated", DateUtils.getDateStr(walletWrapper.dateCreated));
                walletJSON.put("hash", walletWrapper.hash);
                result.add(walletJSON);
            }
            return result;
        }

        int size() {
            return walletList.size();
        }

        File getEncryptedWallet(String hash) {
            return walletList.get(hash).walletFile;
        }
    }

}

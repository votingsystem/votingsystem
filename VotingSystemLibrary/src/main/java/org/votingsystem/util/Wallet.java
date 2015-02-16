package org.votingsystem.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.WalletException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Wallet {

    private static Logger log = Logger.getLogger(Wallet.class);

    private static JSONArray wallet;

    public static void saveCooinsToDir(Collection<Cooin> cooinCollection, String walletPath) throws Exception {
        for(Cooin cooin : cooinCollection) {
            byte[] cooinSerialized =  ObjectUtils.serializeObject(cooin);
            new File(walletPath).mkdirs();
            File cooinFile = FileUtils.copyStreamToFile(new ByteArrayInputStream(cooinSerialized), new File(
                    walletPath + UUID.randomUUID().toString() + ContextVS.SERIALIZED_OBJECT_EXTENSION));
            log.debug("stored cooin: " + cooinFile.getAbsolutePath());
        }
    }

    public static List<Map> getSerializedCooinList(Collection<Cooin> cooinCollection)
            throws UnsupportedEncodingException {
        List<Map> result = new ArrayList<>();
        for(Cooin cooin : cooinCollection) {
            Map cooinDataMap = cooin.getCertSubject().getDataMap();
            cooinDataMap.put("isTimeLimited", cooin.getIsTimeLimited());
            cooinDataMap.put("object", ObjectUtils.serializeObjectToString(cooin));
            result.add(cooinDataMap);
        }
        return result;
    }

    public static JSONArray getPlainWallet() throws Exception {
        File walletFile = new File(ContextVS.APPDIR + File.separator + ContextVS.PLAIN_WALLET_FILE_NAME);
        if(!walletFile.exists()) return new JSONArray();
        return (JSONArray) JSONSerializer.toJSON(new String(FileUtils.getBytesFromFile(walletFile), "UTF-8"));
    }

    public static List<Cooin> getCooinListFromPlainWallet() throws Exception {
        return getCooinListFromJSONArray(getPlainWallet());
    }

    public static List<Cooin> getCooinListFromJSONArray(JSONArray jsonArray) throws Exception {
        List<Cooin> cooinList = new ArrayList<Cooin>();
        for(int i = 0; i < jsonArray.size(); i++) {
            byte[] serializedCooin = jsonArray.getJSONObject(i).getString("object").getBytes();
            cooinList.add((Cooin) ObjectUtils.deSerializeObject(serializedCooin));
        }
        return cooinList;
    }

    public static void savePlainWallet(JSONArray walletJSON) throws Exception {
        File walletFile = new File(ContextVS.APPDIR + File.separator + ContextVS.PLAIN_WALLET_FILE_NAME);
        walletFile.createNewFile();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(walletJSON.toString().getBytes()), walletFile);
    }

    public static void saveToPlainWallet(List<Map> serializedCooinList) throws Exception {
        JSONArray storedWalletJSON = (JSONArray) getPlainWallet();
        storedWalletJSON.addAll(serializedCooinList);
        savePlainWallet(storedWalletJSON);
    }

    public static void saveToWallet(Collection<Cooin> cooinCollection, String pin) throws Exception {
        List<Map> serializedCooinList = getSerializedCooinList(cooinCollection);
        saveToWallet(serializedCooinList, pin);
    }

    public static void saveToWallet(List<Map> serializedCooinList, String pin) throws Exception {
        JSONArray storedWalletJSON = getWallet(pin);
        storedWalletJSON.addAll(serializedCooinList);
        List<String> cooinHashList = new ArrayList<>();
        JSONArray cooinsToSaveArray = new JSONArray();
        storedWalletJSON.stream().forEach(cooin -> {
            if (!cooinHashList.contains(((JSONObject) cooin).getString("hashCertVS"))) {
                cooinsToSaveArray.add(cooin);
                cooinHashList.add(((JSONObject) cooin).getString("hashCertVS"));
            } else log.debug("repeated cooin: " + ((JSONObject) cooin).getString("hashCertVS"));
        });
        log.debug("saving " + cooinsToSaveArray.size() + " cooins");
        saveWallet(cooinsToSaveArray, pin);
    }

    public static JSONArray saveWallet(Object walletJSON, String pin) throws Exception {
        String pinHashHex = StringUtils.toHex(CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST));
        EncryptedWalletList encryptedWalletList = getEncryptedWalletList();
        WalletFile walletFile = encryptedWalletList.getWallet(pinHashHex);
        if(walletFile == null || encryptedWalletList.size() == 0)
            throw new ExceptionVS(ContextVS.getMessage("walletFoundErrorMsg"));
        Encryptor.EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(pin, walletJSON.toString().getBytes());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(bundle.toJSON().toString().getBytes("UTF-8")), walletFile.file);
        wallet = (JSONArray) walletJSON;
        return wallet;
    }

    public static void createWallet(Object walletJSON, String pin) throws Exception {
        String pinHashHex = StringUtils.toHex(CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST));
        String walletFileName = ContextVS.WALLET_FILE_NAME + "_" + pinHashHex + ContextVS.WALLET_FILE_EXTENSION;
        File walletFile = new File(ContextVS.APPDIR + File.separator + walletFileName);
        walletFile.getParentFile().mkdirs();
        walletFile.createNewFile();
        Encryptor.EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(pin, walletJSON.toString().getBytes());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(bundle.toJSON().toString().getBytes("UTF-8")), walletFile);
        wallet = (JSONArray) walletJSON;
    }

    public static JSONArray getWallet() {
        return wallet;
    }

    public static JSONArray getWallet(String pin) throws Exception {
        String pinHashHex = StringUtils.toHex(CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST));
        String walletFileName = ContextVS.WALLET_FILE_NAME + "_" + pinHashHex + ContextVS.WALLET_FILE_EXTENSION;
        File walletFile = new File(ContextVS.APPDIR + File.separator + walletFileName);
        if(!walletFile.exists()) {
            EncryptedWalletList encryptedWalletList = getEncryptedWalletList();
            if(encryptedWalletList.size() > 0) throw new ExceptionVS(ContextVS.getMessage("walletNotFoundErrorMsg"));
            else throw new WalletException(ContextVS.getMessage("walletNotFoundErrorMsg"));
        }
        JSONObject bundleJSON = (JSONObject) JSONSerializer.toJSON( FileUtils.getStringFromFile(walletFile));
        Encryptor.EncryptedBundle bundle = Encryptor.EncryptedBundle.parse(bundleJSON);
        byte[] decryptedWalletBytes = Encryptor.pbeAES_Decrypt(pin, bundle);
        wallet = (JSONArray) JSONSerializer.toJSON(new String(decryptedWalletBytes, "UTF-8"));
        JSONArray plainWallet = getPlainWallet();
        if(plainWallet.size() > 0) {
            wallet.addAll(plainWallet);
            saveWallet(wallet, pin);
            savePlainWallet(new JSONArray());
        }
        return wallet;
    }

    public static void importPlainWallet(String password) throws Exception {
        JSONArray walletJSON = getWallet(password);
        walletJSON.addAll(getPlainWallet());
        saveWallet(walletJSON, password);
        savePlainWallet(new JSONArray());
    }

    public static void changePin(String newPin, String oldPin) throws Exception {
        JSONArray walletJSON = getWallet(oldPin);
        String oldPinHashHex = StringUtils.toHex(CMSUtils.getHashBase64(oldPin, ContextVS.VOTING_DATA_DIGEST));
        String newPinHashHex = StringUtils.toHex(CMSUtils.getHashBase64(newPin, ContextVS.VOTING_DATA_DIGEST));
        String newWalletFileName = ContextVS.WALLET_FILE_NAME + "_" + newPinHashHex + ContextVS.WALLET_FILE_EXTENSION;
        File newWalletFile = new File(ContextVS.APPDIR + File.separator + newWalletFileName);
        if(!newWalletFile.createNewFile()) throw new ExceptionVS(ContextVS.getMessage("walletFoundErrorMsg"));
        Encryptor.EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(newPin, walletJSON.toString().getBytes());
        FileUtils.copyStreamToFile(new ByteArrayInputStream(bundle.toJSON().toString().getBytes("UTF-8")), newWalletFile);
        String oldWalletFileName = ContextVS.WALLET_FILE_NAME + "_" + oldPinHashHex + ContextVS.WALLET_FILE_EXTENSION;
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
                encryptedWalletList.addWallet(getWalletWrapper(directory.getAbsolutePath() + File.separator + filePath));
            }
            return encryptedWalletList;
        } else return new EncryptedWalletList();
    }

    public static JSONObject getWalletState() throws Exception {
        JSONObject result = new JSONObject();
        result.put("plainWallet", getPlainWallet());
        EncryptedWalletList encryptedWalletList = getEncryptedWalletList();
        if(encryptedWalletList != null) result.put("encryptedWalletList", getEncryptedWalletList().toJSON());
        return result;
    }

    private static WalletFile getWalletWrapper(String filePath) {
        String[] nameParts = filePath.split("_");
        WalletFile result = null;
        try {
            result = new WalletFile(nameParts[1].split(ContextVS.WALLET_FILE_EXTENSION)[0],  new File(filePath));
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return result;
    }

    private static class WalletFile {
        String hash;
        File file;
        WalletFile(String hash, File file) {
            this.hash = hash;
            this.file = file;
        }
    }

    private static class EncryptedWalletList {
        Map<String, WalletFile> walletList = new HashMap<String, WalletFile>();
        EncryptedWalletList() {}
        void addWallet(WalletFile walletFile) {
            walletList.put(walletFile.hash, walletFile);
        }
        WalletFile getWallet(String hash) {
            return walletList.get(hash);
        }
        JSONArray toJSON() {
            JSONArray result = new JSONArray();
            for(String hash : walletList.keySet()) {
                WalletFile walletFile = walletList.get(hash);
                JSONObject walletJSON = new JSONObject();
                //walletJSON.put("dateCreated", DateUtils.getDateStr(walletWrapper.dateCreated));
                walletJSON.put("hash", walletFile.hash);
                result.add(walletJSON);
            }
            return result;
        }

        int size() {
            return walletList.size();
        }

        File getEncryptedWallet(String hash) {
            return walletList.get(hash).file;
        }
    }

}

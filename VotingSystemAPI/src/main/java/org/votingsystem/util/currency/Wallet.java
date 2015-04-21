package org.votingsystem.util.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.EncryptedBundleDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.EncryptedBundle;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toSet;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Wallet {

    private static Logger log = Logger.getLogger(Wallet.class.getSimpleName());

    private static Set<Currency> wallet;


    public static void saveCurrencyToDir(Collection<Currency> currencyCollection, String walletPath) throws Exception {
        for(Currency currency : currencyCollection) {
            byte[] currencySerialized =  ObjectUtils.serializeObject(currency);
            new File(walletPath).mkdirs();
            File currencyFile = FileUtils.copyStreamToFile(new ByteArrayInputStream(currencySerialized), new File(
                    walletPath + UUID.randomUUID().toString() + ContextVS.SERIALIZED_OBJECT_EXTENSION));
            log.info("stored currency: " + currencyFile.getAbsolutePath());
        }
    }

    public static List<CurrencyDto> getCurrencySerialized(Collection<Currency> currencyCollection)
            throws UnsupportedEncodingException {
        List<CurrencyDto> result = new ArrayList<>();
        for(Currency currency : currencyCollection) {
            result.add(new CurrencyDto(currency));
        }
        return result;
    }

    public static List<Map> getCertificationRequestSerialized(Collection<Currency> currencyCollection)
            throws UnsupportedEncodingException {
        List<Map> result = new ArrayList<Map>();
        for(Currency currency : currencyCollection) {
            Map currencyDataMap = currency.getCertSubject().getDataMap();
            byte[] serializedCertificationRequest =  ObjectUtils.serializeObject(currency.getCertificationRequest());
            currencyDataMap.put("certificationRequest", new String(serializedCertificationRequest, "UTF-8"));
            result.add(currencyDataMap);
        }
        return result;
    }

    public static Set<Currency> getCurrencySetFromCertificationRequest(List list) throws Exception {
        Set<Currency> currencySet = new HashSet<>();
        for(int i = 0; i < list.size(); i++) {
            byte[] serializedCertificationRequest = ((String)((Map)list.get(i)).get("certificationRequest")).getBytes();
            CertificationRequestVS certificationRequest = (CertificationRequestVS) ObjectUtils.deSerializeObject(
                    serializedCertificationRequest);
            currencySet.add(Currency.fromCertificationRequestVS(certificationRequest));
        }
        return currencySet;
    }

    public static List<Map> getPlainWallet() throws Exception {
        File walletFile = new File(ContextVS.APPDIR + File.separator + ContextVS.PLAIN_WALLET_FILE_NAME);
        if(!walletFile.exists()) return new ArrayList<>();
        return new ObjectMapper().readValue(new String(FileUtils.getBytesFromFile(walletFile), "UTF-8"),
                new TypeReference<List<Map>>() {
                });
    }

    public static Set<Currency> getCurrencySetFromPlainWallet() throws Exception {
        return getCurrencySetFromCertificationRequest(getPlainWallet());
    }

    public static Set<Currency> getCurrencySet(Collection<CurrencyDto> currencyDtoCollection) throws Exception {
        Set<Currency> currencySet = new HashSet<>();
        for(CurrencyDto currencyDto : currencyDtoCollection) {
            if(currencyDto.getObject() != null) {
                currencySet.add((Currency) ObjectUtils.deSerializeObject(currencyDto.getObject().getBytes()));
            } else if(currencyDto.getCertificationRequest() != null) {
                CertificationRequestVS certificationRequest = (CertificationRequestVS) ObjectUtils.deSerializeObject(
                        currencyDto.getCertificationRequest().getBytes());
                currencySet.add(Currency.fromCertificationRequestVS(certificationRequest));
            } else throw new ValidationExceptionVS("currency not serialized inside wallet");
        }
        return currencySet;
    }

    public static void savePlainWallet(List walletList) throws Exception {
        File walletFile = new File(ContextVS.APPDIR + File.separator + ContextVS.PLAIN_WALLET_FILE_NAME);
        walletFile.createNewFile();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(new ObjectMapper().writeValueAsString(walletList)
                .getBytes()), walletFile);
    }

    public static void saveToPlainWallet(List<Map> serializedCurrencyList) throws Exception {
        List storedWalletList = getPlainWallet();
        storedWalletList.addAll(serializedCurrencyList);
        savePlainWallet(storedWalletList);
    }

    public static void saveToWallet(Collection<Currency> currencyCollection, String pin) throws Exception {
        List<CurrencyDto> serializedCurrencyList = getCurrencySerialized(currencyCollection);
        saveToWallet(serializedCurrencyList, pin);
    }

    public static void saveToWallet(List<CurrencyDto> serializedCurrencyList, String pin) throws Exception {
        Set<Currency> storedWallet = getWallet(pin);
        List<CurrencyDto> storedWalletList = getCurrencySerialized(storedWallet);
        storedWalletList.addAll(serializedCurrencyList);
        Set<String> hashSet = new HashSet<>();
        List<CurrencyDto> currencyToSaveArray = new ArrayList<>();
        for(CurrencyDto currencyDto:  storedWalletList) {
            if(hashSet.add(currencyDto.getHashCertVS())) {
                currencyToSaveArray.add(currencyDto);
            } else log.log(Level.SEVERE, "repeated currency!!!: " + currencyDto.getHashCertVS());
        }
        log.info("saving '" + currencyToSaveArray.size() + "' currency items");
        saveWallet(currencyToSaveArray, pin);
    }

    public static Set<Currency> saveWallet(Set<Currency> wallet, String pin) throws Exception {
        Set<String> hashSet = new HashSet<>();
        wallet = wallet.stream().filter(currency -> {
            if (!hashSet.add(currency.getHashCertVS())) {
                log.log(Level.SEVERE, "removing repeated currency!!!: " + currency.getHashCertVS());
                return false;
            } else return true;
        }).collect(toSet());
        return saveWallet(getCurrencySerialized(wallet), pin);
    }

    public static Set<Currency> saveWallet(Collection<CurrencyDto> currencyDtoCollection, String pin) throws Exception {
        String pinHashHex = StringUtils.toHex(CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST));
        EncryptedWalletList encryptedWalletList = getEncryptedWalletList();
        WalletFile walletFile = encryptedWalletList.getWallet(pinHashHex);
        if(walletFile == null || encryptedWalletList.size() == 0)
            throw new ExceptionVS(ContextVS.getMessage("walletFoundErrorMsg"));
        EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(pin, JSON.getMapper().writeValueAsBytes(currencyDtoCollection));
        JSON.getMapper().writeValue(walletFile.file, new EncryptedBundleDto(bundle));
        wallet = getCurrencySet(currencyDtoCollection);
        return wallet;
    }

    public static void createWallet(List<CurrencyDto> walletDto, String pin) throws Exception {
        String pinHashHex = StringUtils.toHex(CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST));
        String walletFileName = ContextVS.WALLET_FILE_NAME + "_" + pinHashHex + ContextVS.WALLET_FILE_EXTENSION;
        File walletFile = new File(ContextVS.APPDIR + File.separator + walletFileName);
        walletFile.getParentFile().mkdirs();
        EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(pin, JSON.getMapper().writeValueAsBytes(walletDto));
        JSON.getMapper().writeValue(walletFile, new EncryptedBundleDto(bundle));
        wallet = getCurrencySet(walletDto);
    }

    public static Set<Currency> getWallet() {
        return wallet;
    }

    public static Set<Currency> getWallet(String pin) throws Exception {
        String pinHashHex = StringUtils.toHex(CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST));
        String walletFileName = ContextVS.WALLET_FILE_NAME + "_" + pinHashHex + ContextVS.WALLET_FILE_EXTENSION;
        File walletFile = new File(ContextVS.APPDIR + File.separator + walletFileName);
        if(!walletFile.exists()) {
            EncryptedWalletList encryptedWalletList = getEncryptedWalletList();
            if(encryptedWalletList.size() > 0) throw new ExceptionVS(ContextVS.getMessage("walletNotFoundErrorMsg"));
            else throw new WalletException(ContextVS.getMessage("walletNotFoundErrorMsg"));
        }
        EncryptedBundleDto bundleDto = JSON.getMapper().readValue(walletFile, EncryptedBundleDto.class);
        EncryptedBundle bundle = bundleDto.getEncryptedBundle();
        byte[] decryptedWalletBytes = Encryptor.pbeAES_Decrypt(pin, bundle);
        wallet = getCurrencySet(new ObjectMapper().readValue(
                new String(decryptedWalletBytes, "UTF-8"), new TypeReference<List>() {}));
        Set<Currency> plainWallet = getCurrencySetFromPlainWallet();
        if(plainWallet.size() > 0) {
            wallet.addAll(plainWallet);
            saveWallet(wallet, pin);
            savePlainWallet(new ArrayList<>());
        }
        return wallet;
    }

    public static void changePin(String newPin, String oldPin) throws Exception {
        Set<Currency> wallet = getWallet(oldPin);
        List<CurrencyDto> walletDto = getCurrencySerialized(wallet);
        String oldPinHashHex = StringUtils.toHex(CMSUtils.getHashBase64(oldPin, ContextVS.VOTING_DATA_DIGEST));
        String newPinHashHex = StringUtils.toHex(CMSUtils.getHashBase64(newPin, ContextVS.VOTING_DATA_DIGEST));
        String newWalletFileName = ContextVS.WALLET_FILE_NAME + "_" + newPinHashHex + ContextVS.WALLET_FILE_EXTENSION;
        File newWalletFile = new File(ContextVS.APPDIR + File.separator + newWalletFileName);
        if(!newWalletFile.createNewFile()) throw new ExceptionVS(ContextVS.getMessage("walletFoundErrorMsg"));
        EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(newPin, JSON.getMapper().writeValueAsBytes(walletDto));
        JSON.getMapper().writeValue(newWalletFile, new EncryptedBundleDto(bundle));
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

    public static Map getWalletState() throws Exception {
        Map result = new HashMap<>();
        result.put("plainWallet", getPlainWallet());
        EncryptedWalletList encryptedWalletList = getEncryptedWalletList();
        if(encryptedWalletList != null) result.put("encryptedWalletList", getEncryptedWalletList().toMap());
        return result;
    }

    private static WalletFile getWalletWrapper(String filePath) {
        String[] nameParts = filePath.split("_");
        WalletFile result = null;
        try {
            result = new WalletFile(nameParts[1].split(ContextVS.WALLET_FILE_EXTENSION)[0],  new File(filePath));
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
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
        List<Map> toMap() {
            List<Map> result = new ArrayList<>();
            for(String hash : walletList.keySet()) {
                WalletFile walletFile = walletList.get(hash);
                Map walletMap = new HashMap<>();
                //walletMap.put("dateCreated", DateUtils.getDateStr(walletWrapper.dateCreated));
                walletMap.put("hash", walletFile.hash);
                result.add(walletMap);
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

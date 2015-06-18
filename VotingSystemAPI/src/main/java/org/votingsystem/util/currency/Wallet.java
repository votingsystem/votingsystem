package org.votingsystem.util.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.dto.EncryptedBundleDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.WalletDto;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.EncryptedBundle;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Wallet {

    private static Logger log = Logger.getLogger(Wallet.class.getSimpleName());

    private static Set<Currency> wallet;

    public static List<CurrencyDto> getPlainWalletDto() throws Exception {
        File walletFile = new File(ContextVS.APPDIR + File.separator + ContextVS.PLAIN_WALLET_FILE_NAME);
        if(!walletFile.exists()) return new ArrayList<>();
        return JSON.getMapper().readValue(walletFile, new TypeReference<List<CurrencyDto>>() { });
    }

    public static Set<Currency> getPlainWallet() throws Exception {
        return CurrencyDto.deSerialize(getPlainWalletDto());
    }

    public static void savePlainWalletDto(Collection<CurrencyDto> walletList) throws Exception {
        File walletFile = new File(ContextVS.APPDIR + File.separator + ContextVS.PLAIN_WALLET_FILE_NAME);
        walletFile.createNewFile();
        JSON.getMapper().writeValue(walletFile, walletList);
    }

    public static void saveToPlainWallet(Collection<Currency> currencyCollection) throws Exception {
        List<CurrencyDto> plainWallet = getPlainWalletDto();
        plainWallet.addAll(CurrencyDto.serializeCollection(currencyCollection));
        savePlainWalletDto(plainWallet);
    }

    public static void saveToWallet(Collection<Currency> currencyCollection, String pin) throws Exception {
        Set<CurrencyDto> serializedCurrencyList = CurrencyDto.serializeCollection(currencyCollection);
        saveToWalletDto(serializedCurrencyList, pin);
    }

    public static void saveToWalletDto(Collection<CurrencyDto> currencyDtoCollection, String pin) throws Exception {
        Set<Currency> storedWallet = getWallet(pin);
        Set<CurrencyDto> storedWalletDto = CurrencyDto.serializeCollection(storedWallet);
        storedWalletDto.addAll(currencyDtoCollection);
        Set<String> hashSet = new HashSet<>();
        //check if duplicated
        List<CurrencyDto> walletDtoToSave = new ArrayList<>();
        for(CurrencyDto currencyDto:  storedWalletDto) {
            if(hashSet.add(currencyDto.getHashCertVS())) {
                walletDtoToSave.add(currencyDto);
            } else log.log(Level.SEVERE, "repeated currency!!!: " + currencyDto.getHashCertVS());
        }
        log.info("saving '" + walletDtoToSave.size() + "' currency items");
        saveWallet(walletDtoToSave, pin);
    }

    public static Set<Currency> saveWallet(Collection<CurrencyDto> currencyDtoCollection, String pin) throws Exception {
        String pinHashHex = StringUtils.toHex(CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST));
        EncryptedWalletList encryptedWalletList = getEncryptedWalletList();
        WalletFile walletFile = encryptedWalletList.getWallet(pinHashHex);
        if(walletFile == null || encryptedWalletList.size() == 0)
            throw new ExceptionVS(ContextVS.getMessage("walletFoundErrorMsg"));
        EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(pin, JSON.getMapper().writeValueAsBytes(currencyDtoCollection));
        JSON.getMapper().writeValue(walletFile.file, new EncryptedBundleDto(bundle));
        wallet = CurrencyDto.deSerialize(currencyDtoCollection);
        return wallet;
    }

    public static void createWallet(List<CurrencyDto> walletDto, String pin) throws Exception {
        String pinHashHex = StringUtils.toHex(CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST));
        String walletFileName = ContextVS.WALLET_FILE_NAME + "_" + pinHashHex + ContextVS.WALLET_FILE_EXTENSION;
        File walletFile = new File(ContextVS.APPDIR + File.separator + walletFileName);
        walletFile.getParentFile().mkdirs();
        EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(pin, JSON.getMapper().writeValueAsBytes(walletDto));
        JSON.getMapper().writeValue(walletFile, new EncryptedBundleDto(bundle));
        wallet = CurrencyDto.deSerialize(walletDto);
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
        List<CurrencyDto> walletDto = JSON.getMapper().readValue(decryptedWalletBytes, new TypeReference<List<CurrencyDto>>() {});
        wallet = CurrencyDto.deSerialize(walletDto);
        Set<Currency> plainWallet = getPlainWallet();
        if(plainWallet.size() > 0) {
            wallet.addAll(plainWallet);
            saveWallet(CurrencyDto.serializeCollection(wallet), pin);
            savePlainWalletDto(new ArrayList<>());
        }
        return wallet;
    }

    public static void changePin(String newPin, String oldPin) throws Exception {
        Set<Currency> wallet = getWallet(oldPin);
        Set<CurrencyDto> walletDto = CurrencyDto.serializeCollection(wallet);
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

    public static WalletDto getWalletDto() throws Exception {
        WalletDto walletDto = new WalletDto(getPlainWalletDto(), null, null);
        EncryptedWalletList encryptedWalletList = getEncryptedWalletList();
        if(encryptedWalletList != null) walletDto.setWalletFilesHashSet(encryptedWalletList.walletList.keySet());
        return walletDto;
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

        int size() {
            return walletList.size();
        }

        File getEncryptedWallet(String hash) {
            return walletList.get(hash).file;
        }
    }

}

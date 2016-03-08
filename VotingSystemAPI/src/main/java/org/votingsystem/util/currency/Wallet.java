package org.votingsystem.util.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.dto.EncryptedBundleDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.dto.currency.WalletDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.EncryptedBundle;
import org.votingsystem.util.crypto.Encryptor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Wallet {

    private static Logger log = Logger.getLogger(Wallet.class.getName());

    private Set<Currency> currencySet;
    private char[] password;

    public Wallet() { }

    private static Set<CurrencyDto> getPlainWalletDto() throws Exception {
        File walletFile = new File(ContextVS.getInstance().getAppDir() + File.separator + ContextVS.PLAIN_WALLET_FILE_NAME);
        if(!walletFile.exists()) return new HashSet<>();
        return JSON.getMapper().readValue(walletFile, new TypeReference<Set<CurrencyDto>>() { });
    }

    //this is the wallet we use to temporary store incoming 'currency messages', once the user enters de wallet password
    //theyŕe stored on the encrypted wallet
    public static Set<Currency> getPlainWallet() throws Exception {
        return CurrencyDto.deSerialize(getPlainWalletDto());
    }

    private static void savePlainWalletDto(Set<CurrencyDto> plainWallet) throws Exception {
        File walletFile = new File(ContextVS.getInstance().getAppDir() + File.separator + ContextVS.PLAIN_WALLET_FILE_NAME);
        walletFile.createNewFile();
        JSON.getMapper().writeValue(walletFile, plainWallet);
    }

    public static void saveToPlainWallet(Set<Currency> currencyCollection) throws Exception {
        Set<CurrencyDto> plainWallet = getPlainWalletDto();
        plainWallet.addAll(CurrencyDto.serializeCollection(currencyCollection));
        savePlainWalletDto(plainWallet);
    }

    public void saveToWallet(Set<Currency> currencySet, char[] password) throws Exception {
        Set<CurrencyDto> serializedCurrencyList = CurrencyDto.serializeCollection(currencySet);
        saveToWalletDto(serializedCurrencyList, password);
    }

    public void saveToWalletDto(Set<CurrencyDto> currencyDtoCollection, char[] password) throws Exception {
        Set<Currency> storedWallet = load(password).getCurrencySet();
        Set<CurrencyDto> storedWalletDto = CurrencyDto.serializeCollection(storedWallet);
        storedWalletDto.addAll(currencyDtoCollection);
        Set<String> hashSet = new HashSet<>();
        //check if duplicated
        Set<CurrencyDto> currencySetToSave = new HashSet<>();
        for(CurrencyDto currencyDto:  storedWalletDto) {
            if(hashSet.add(currencyDto.getHashCertVS())) {
                currencySetToSave.add(currencyDto);
            } else log.log(Level.SEVERE, "repeated currency!!!: " + currencyDto.getHashCertVS());
        }
        log.info("saving '" + currencySetToSave.size() + "' currency items");
        saveWallet(currencySetToSave, password);
    }

    public Set<Currency> saveWallet(Set<CurrencyDto> currencyToSave, char[] password) throws Exception {
        String passwordHashHex = StringUtils.toHex(StringUtils.getHashBase64(new String(password), ContextVS.VOTING_DATA_DIGEST));
        EncryptedWalletList encryptedWalletList = getEncryptedWalletList();
        WalletFile walletFile = encryptedWalletList.getWallet(passwordHashHex);
        if(walletFile == null || encryptedWalletList.size() == 0)
            throw new ExceptionVS(ContextVS.getMessage("walletFoundErrorMsg"));
        EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(password, JSON.getMapper().writeValueAsBytes(currencyToSave));
        JSON.getMapper().writeValue(walletFile.file, new EncryptedBundleDto(bundle));
        currencySet = CurrencyDto.deSerialize(currencyToSave);
        return currencySet;
    }

    public void createWallet(Set<CurrencyDto> setDto, char[] password) throws Exception {
        String passwordHashHex = StringUtils.toHex(StringUtils.getHashBase64(new String(password), ContextVS.VOTING_DATA_DIGEST));
        String walletFileName = ContextVS.WALLET_FILE_NAME + "_" + passwordHashHex + ContextVS.WALLET_FILE_EXTENSION;
        File walletFile = new File(ContextVS.getInstance().getAppDir() + File.separator + walletFileName);
        walletFile.getParentFile().mkdirs();
        EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(password, JSON.getMapper().writeValueAsBytes(setDto));
        JSON.getMapper().writeValue(walletFile, new EncryptedBundleDto(bundle));
        currencySet = CurrencyDto.deSerialize(setDto);
    }

    public Set<Currency> getCurrencySet() {
        return currencySet;
    }

    public static Wallet load(char[] password) throws Exception {
        String passwordHashHex = StringUtils.toHex(StringUtils.getHashBase64(new String(password), ContextVS.VOTING_DATA_DIGEST));
        String walletFileName = ContextVS.WALLET_FILE_NAME + "_" + passwordHashHex + ContextVS.WALLET_FILE_EXTENSION;
        File walletFile = new File(ContextVS.getInstance().getAppDir() + File.separator + walletFileName);
        if(!walletFile.exists()) {
            EncryptedWalletList encryptedWalletList = getEncryptedWalletList();
            if(encryptedWalletList.size() > 0) throw new ExceptionVS(ContextVS.getMessage("walletNotFoundErrorMsg"));
            else throw new WalletException(ContextVS.getMessage("walletNotFoundErrorMsg"));
        }
        EncryptedBundleDto bundleDto = JSON.getMapper().readValue(walletFile, EncryptedBundleDto.class);
        EncryptedBundle bundle = bundleDto.getEncryptedBundle();
        byte[] decryptedWalletBytes = Encryptor.pbeAES_Decrypt(password, bundle);
        Set<CurrencyDto> walletDto = JSON.getMapper().readValue(decryptedWalletBytes, new TypeReference<Set<CurrencyDto>>() {});
        Wallet wallet = new Wallet();
        wallet.currencySet = CurrencyDto.deSerialize(walletDto);
        Set<Currency> plainWallet = getPlainWallet();
        if(plainWallet.size() > 0) {
            wallet.currencySet.addAll(plainWallet);
            wallet.saveWallet(CurrencyDto.serializeCollection(wallet.currencySet), password);
            savePlainWalletDto(Collections.emptySet());
        }
        return wallet;
    }

    public void removeSet(Set<String> removedSet, char[] password) throws Exception {
        Set<String> clonedSet = removedSet.stream().collect(Collectors.toSet());
        Set<Currency> newCurrencySet = currencySet.stream().filter(currency -> {
            if (clonedSet.remove(currency.getHashCertVS())) {
                log.info("deleted currency with hash: " + currency.getHashCertVS());
                return false;
            } else return true;
        }).collect(toSet());
        if(!clonedSet.isEmpty()) throw new WalletException("trying to delete currencies not stored in wallet: [" +
                 clonedSet.stream().reduce((t, u) -> t + "," + u).get() + "]");
        saveWallet(CurrencyDto.serializeCollection(newCurrencySet), password);
    }

    public void changePassword(char[] newpassword, char[] oldpassword) throws Exception {
        Set<Currency> currencySet = load(oldpassword).getCurrencySet();
        Set<CurrencyDto> walletDto = CurrencyDto.serializeCollection(currencySet);
        String oldpasswordHashHex = StringUtils.toHex(StringUtils.getHashBase64(new String(oldpassword), ContextVS.VOTING_DATA_DIGEST));
        String newpasswordHashHex = StringUtils.toHex(StringUtils.getHashBase64(new String(newpassword), ContextVS.VOTING_DATA_DIGEST));
        String newWalletFileName = ContextVS.WALLET_FILE_NAME + "_" + newpasswordHashHex + ContextVS.WALLET_FILE_EXTENSION;
        File newWalletFile = new File(ContextVS.getInstance().getAppDir() + File.separator + newWalletFileName);
        if(!newWalletFile.createNewFile()) throw new ExceptionVS(ContextVS.getMessage("walletFoundErrorMsg"));
        EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(newpassword, JSON.getMapper().writeValueAsBytes(walletDto));
        JSON.getMapper().writeValue(newWalletFile, new EncryptedBundleDto(bundle));
        String oldWalletFileName = ContextVS.WALLET_FILE_NAME + "_" + oldpasswordHashHex + ContextVS.WALLET_FILE_EXTENSION;
        File oldWalletFile = new File(ContextVS.getInstance().getAppDir() + File.separator + oldWalletFileName);
        oldWalletFile.delete();
    }

    public static EncryptedWalletList getEncryptedWalletList() {
        File directory = new File(ContextVS.getInstance().getAppDir());
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

    public static CurrencyCheckResponse validateWithServer(Set<Currency> currencySet) throws Exception {
        CurrencyCheckResponse response = null;
        try {
            Map<String, Currency> currencyMap = new HashMap<>();
            Set<String> hashCertVSSet = currencySet.stream().map(currency -> {
                currencyMap.put(currency.getHashCertVS(), currency);
                return currency.getHashCertVS();}).collect(toSet());
            ResponseVS responseVS = ContextVS.getInstance().checkServer(
                    ContextVS.getInstance().getCurrencyServer().getServerURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                response =  CurrencyCheckResponse.load(responseVS);
                return response;
            }
            CurrencyServer currencyServer = (CurrencyServer) responseVS.getData();
            Set<CurrencyStateDto> responseDto =  HttpHelper.getInstance().sendData(
                    new TypeReference<Set<CurrencyStateDto>>() {},
                    JSON.getMapper().writeValueAsBytes(hashCertVSSet),
                    currencyServer.getCurrencyBundleStateServiceURL(),
                    ContentTypeVS.JSON);
            response = CurrencyCheckResponse.load(responseDto, currencyMap);
        } catch (Exception ex) {
            ex.printStackTrace();
            response = CurrencyCheckResponse.load(new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage()));
        } finally {
            return response;
        }
    }

}

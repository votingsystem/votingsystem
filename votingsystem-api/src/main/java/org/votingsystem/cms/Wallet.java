package org.votingsystem.cms;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.crypto.EncryptedBundle;
import org.votingsystem.dto.EncryptedBundleDto;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.HttpConn;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;
import org.votingsystem.crypto.Encryptor;
import org.votingsystem.crypto.HashUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.*;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private String walletDirPath;
    private char[] password;

    public Wallet(String walletDirPath, char[] password) throws Exception {
        this.password = password;
        this.walletDirPath = walletDirPath;
        load();
    }
    
    public void addToWallet(Set<Currency> currencySetAdded) throws Exception {
        Set<CurrencyDto> currencyDtoSetAdded = CurrencyDto.serializeCollection(currencySetAdded);
        Set<CurrencyDto> walletDto = CurrencyDto.serializeCollection(currencySet);
        walletDto.addAll(currencyDtoSetAdded);
        Set<String> hashSet = new HashSet<>();
        //check if duplicated
        Set<CurrencyDto> currencySetToSave = new HashSet<>();
        for(CurrencyDto currencyDto:  walletDto) {
            if(hashSet.add(currencyDto.getRevocationHashBase64())) {
                currencySetToSave.add(currencyDto);
            } else log.log(Level.SEVERE, "repeated currency!!!: " + currencyDto.getRevocationHashBase64());
        }
        log.info("saving '" + currencySetToSave.size() + "' currency items");
        saveWallet(currencySetToSave);
    }


    public Set<Currency> saveWallet(Set<CurrencyDto> currencyDtoSet) throws Exception {
        File walletFile = getWalletFile(password);
        if(walletFile == null) walletFile.createNewFile();
        EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(password, JSON.getMapper().writeValueAsBytes(currencyDtoSet));
        JSON.getMapper().writeValue(walletFile, new EncryptedBundleDto(bundle));
        this.currencySet = CurrencyDto.deSerialize(currencyDtoSet);
        return this.currencySet;
    }

    public void createWallet(Set<CurrencyDto> currencySet) throws Exception {
        File walletFile = getWalletFile(password);
        walletFile.getParentFile().mkdirs();
        log.info("new walletFile: " + walletFile.getAbsolutePath());
        EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(password, JSON.getMapper().writeValueAsBytes(currencySet));
        JSON.getMapper().writeValue(walletFile, new EncryptedBundleDto(bundle));
        this.currencySet = CurrencyDto.deSerialize(currencySet);
    }

    private File getWalletFile(char[] password) throws NoSuchAlgorithmException {
        String passwordHashHex = StringUtils.toHex(
                HashUtils.getHashBase64(new String(password).getBytes(), Constants.DATA_DIGEST_ALGORITHM));
        String walletFileName = Constants.WALLET_FILE_NAME + "_" + passwordHashHex + Constants.WALLET_FILE_EXTENSION;
        return new File(walletDirPath + File.separator + walletFileName);
    }
    
    
    public Set<Currency> getCurrencySet() {
        return currencySet;
    }

    public Wallet load() throws Exception {
        File walletFile = getWalletFile(password);
        if(walletFile.exists()) {
            EncryptedBundleDto bundleDto = JSON.getMapper().readValue(walletFile, EncryptedBundleDto.class);
            EncryptedBundle bundle = bundleDto.getEncryptedBundle();
            byte[] decryptedWalletBytes = Encryptor.pbeAES_Decrypt(password, bundle);
            Set<CurrencyDto> currencyDtoSet = JSON.getMapper().readValue(
                    decryptedWalletBytes, new TypeReference<Set<CurrencyDto>>() {});
            this.currencySet = CurrencyDto.deSerialize(currencyDtoSet);
        } else createWallet(new HashSet<>());
        return this;
    }

    public void removeCurrencySet(Set<String> currencySet, char[] password) throws Exception {
        Set<String> clonedSet = currencySet.stream().collect(Collectors.toSet());
        Set<Currency> newCurrencySet = this.currencySet.stream().filter(currency -> {
            if (clonedSet.remove(currency.getRevocationHash())) {
                log.info("deleted currency revocation hash: " + currency.getRevocationHash());
                return false;
            } else return true;
        }).collect(toSet());
        if(!clonedSet.isEmpty()) throw new WalletException("trying to delete currencies not stored in wallet: [" +
                 clonedSet.stream().reduce((t, u) -> t + "," + u).get() + "]");
        saveWallet(CurrencyDto.serializeCollection(newCurrencySet));
    }

    public void changePassword(char[] newpassword) throws Exception {
        Set<CurrencyDto> walletDto = CurrencyDto.serializeCollection(currencySet);
        File oldWalletFile = getWalletFile(password);
        File newWalletFile = getWalletFile(newpassword);
        if(!newWalletFile.createNewFile())
            throw new ValidationException(Messages.currentInstance().get("walletFoundErrorMsg"));
        EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(newpassword, JSON.getMapper().writeValueAsBytes(walletDto));
        JSON.getMapper().writeValue(newWalletFile, new EncryptedBundleDto(bundle));
        oldWalletFile.delete();
    }

    public static CurrencyCheckResponse validateWithServer(Set<Currency> currencySet, String currencyServerEntityId)
            throws Exception {
        CurrencyCheckResponse response = null;
        try {
            Map<String, Currency> currencyMap = new HashMap<>();
            Set<String> hashSet = currencySet.stream().map(currency -> {
                currencyMap.put(currency.getRevocationHash(), currency);
                return currency.getRevocationHash();}).collect(toSet());
            Set<CurrencyStateDto> responseDto =  HttpConn.getInstance().doPostRequest(
                    new TypeReference<Set<CurrencyStateDto>>() {},
                    JSON.getMapper().writeValueAsBytes(hashSet),
                    CurrencyOperation.GET_CURRENCY_BUNDLE_STATUS.getUrl(currencyServerEntityId),
                    ContentType.JSON);
            response = CurrencyCheckResponse.load(responseDto, currencyMap);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            response = new CurrencyCheckResponse(ResponseDto.SC_ERROR, ex.getMessage());
        } finally {
            return response;
        }
    }

}
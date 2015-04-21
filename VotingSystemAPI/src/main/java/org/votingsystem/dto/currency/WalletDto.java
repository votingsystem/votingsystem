package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collection;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletDto {

    //plain wallet -> currency items stored without password
    private Collection<CurrencyDto> plainWallet;
    private Collection<CurrencyDto> wallet;
    private Set<String> walletFilesHashSet;

    public WalletDto () {}

    public WalletDto (Collection<CurrencyDto> plainWallet, Collection<CurrencyDto> wallet, Set<String> walletFilesHashSet) {
        this.plainWallet = plainWallet;
        this.wallet = wallet;
        this.walletFilesHashSet = walletFilesHashSet;
    }

    public Collection<CurrencyDto> getPlainWallet() {
        return plainWallet;
    }

    public void setPlainWallet(Collection<CurrencyDto> plainWallet) {
        this.plainWallet = plainWallet;
    }

    public Collection<CurrencyDto> getWallet() {
        return wallet;
    }

    public void setWallet(Collection<CurrencyDto> wallet) {
        this.wallet = wallet;
    }

    public Set<String> getWalletFilesHashSet() {
        return walletFilesHashSet;
    }

    public void setWalletFilesHashSet(Set<String> walletFilesHashSet) {
        this.walletFilesHashSet = walletFilesHashSet;
    }
}

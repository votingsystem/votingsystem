package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.currency.websocket.SessionVSManager;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
public class UserVSBean {

    private static Logger log = Logger.getLogger(UserVSBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject MessagesBean messages;
    @Inject ConfigVS config;
    @Inject CurrencyAccountBean currencyAccountBean;
    @Inject UserVSBean userVSBean;
    @Inject SystemBean systemBean;
    @Inject SignatureBean signatureBean;
    @Inject SubscriptionVSBean subscriptionVSBean;
    @Inject TransactionVSBean transactionVSBean;
    
    
    public UserVS saveUser(MessageSMIME messageSMIMEReq) throws Exception {
        UserVS signer = messageSMIMEReq.getUserVS();
        if(!signatureBean.isUserAdmin(signer.getNif())) throw new ExceptionVS(messages.get("userWithoutPrivilegesErrorMsg",
                signer.getNif(), TypeVS.CERT_CA_NEW.toString()));
        ObjectNode dataJSON =(ObjectNode)  new ObjectMapper().readTree(messageSMIMEReq.getSMIME().getSignedContent());
        if (dataJSON.get("info") == null || dataJSON.get("certChainPEM") == null || dataJSON.get("operation") == null ||
                (TypeVS.CERT_USER_NEW != TypeVS.valueOf(dataJSON.get("operation").asText()))) {
            throw new ExceptionVS(messages.get("paramsErrorMsg"));
        }
        Collection<X509Certificate> certChain = CertUtils.fromPEMToX509CertCollection(
                dataJSON.get("certChainPEM").asText().getBytes());
        UserVS newUser = UserVS.getUserVS(certChain.iterator().next());
        signatureBean.verifyUserCertificate(newUser);
        newUser = subscriptionVSBean.checkUser(newUser);
        dao.merge(newUser.setState(UserVS.State.ACTIVE).setReason(dataJSON.get("info").asText()));
        return newUser;
    }

    @Transactional
    public UserVSDto getUserVSDto(UserVS userVS, boolean withCerts) throws Exception {
        List<CertificateVS> certificates = null;
        if(withCerts) {
            Query query = dao.getEM().createQuery("SELECT c FROM CertificateVS c WHERE c.userVS =:userVS and c.state =:state")
                    .setParameter("userVS", userVS).setParameter("state", CertificateVS.State.OK);
            certificates = query.getResultList();
        }
        Set<DeviceVSDto> deviceVSDtoSet = SessionVSManager.getInstance().connectedDeviceMap(userVS.getId());
        return UserVSDto.DEVICES(userVS, deviceVSDtoSet, certificates);
    }

    public BalancesDto getBalancesDto(UserVS userVS, TimePeriod timePeriod, boolean validateResult) throws Exception {
        BalancesDto balancesDto = transactionVSBean.getBalancesDto(
                transactionVSBean.getTransactionFromList(userVS, timePeriod), TransactionVS.Source.FROM);
        balancesDto.setTimePeriod(timePeriod);
        balancesDto.setUserVS(getUserVSDto(userVS, false));

        BalancesDto balancesToDto = transactionVSBean.getBalancesDto(
                transactionVSBean.getTransactionToList(userVS, timePeriod), TransactionVS.Source.TO);
        balancesDto.setTo(balancesToDto);

        balancesToDto.calculateCash();
        if(validateResult && UserVS.Type.SYSTEM != userVS.getType() && timePeriod.isCurrentWeekPeriod())
            currencyAccountBean.checkBalancesMap(userVS, balancesDto.getBalancesCash());
        return balancesDto;
    }

    public BalancesDto getBalancesDto(UserVS userVS, TimePeriod timePeriod) throws Exception {
        return getBalancesDto(userVS, timePeriod, true);
    }
    
}
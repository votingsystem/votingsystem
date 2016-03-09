package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.currency.websocket.SessionVSManager;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

@Stateless
public class UserVSBean {

    private static Logger log = Logger.getLogger(UserVSBean.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject CurrencyAccountBean currencyAccountBean;
    @Inject UserVSBean userVSBean;
    @Inject CMSBean cmsBean;
    @Inject SubscriptionVSBean subscriptionVSBean;
    @Inject TransactionVSBean transactionVSBean;
    
    
    public UserVS saveUser(CMSMessage cmsReq) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        UserVS signer = cmsReq.getUserVS();
        if(!cmsBean.isAdmin(signer.getNif())) throw new ExceptionVS(messages.get("userWithoutPrivilegesErrorMsg",
                signer.getNif(), TypeVS.CERT_CA_NEW.toString()));

        Map<String, String> dataMap = cmsReq.getCMS().getSignedContent(new TypeReference<HashMap<String, String>>() {});;
        if (dataMap.containsKey("info") || dataMap.containsKey("certChainPEM") || dataMap.containsKey("operation") ||
                (TypeVS.CERT_USER_NEW != TypeVS.valueOf(dataMap.get("operation")))) {
            throw new ExceptionVS(messages.get("paramsErrorMsg"));
        }
        Collection<X509Certificate> certChain = PEMUtils.fromPEMToX509CertCollection(
                dataMap.get("certChainPEM").getBytes());
        UserVS newUser = UserVS.FROM_X509_CERT(certChain.iterator().next());
        cmsBean.verifyUserCertificate(newUser);
        newUser = subscriptionVSBean.checkUser(newUser);
        dao.merge(newUser.setState(UserVS.State.ACTIVE).setReason(dataMap.get("info")));
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
    
}
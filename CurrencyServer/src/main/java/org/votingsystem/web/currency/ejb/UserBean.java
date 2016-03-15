package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.currency.websocket.SessionManager;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SubscriptionBean;
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
public class UserBean {

    private static Logger log = Logger.getLogger(UserBean.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject CurrencyAccountBean currencyAccountBean;
    @Inject
    UserBean userBean;
    @Inject CMSBean cmsBean;
    @Inject
    SubscriptionBean subscriptionBean;
    @Inject
    TransactionBean transactionBean;
    
    
    public User saveUser(CMSMessage cmsReq) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        User signer = cmsReq.getUser();
        if(!cmsBean.isAdmin(signer.getNif())) throw new ExceptionVS(messages.get("userWithoutPrivilegesErrorMsg",
                signer.getNif(), TypeVS.CERT_CA_NEW.toString()));

        Map<String, String> dataMap = cmsReq.getCMS().getSignedContent(new TypeReference<HashMap<String, String>>() {});;
        if (dataMap.containsKey("info") || dataMap.containsKey("certChainPEM") || dataMap.containsKey("operation") ||
                (TypeVS.CERT_USER_NEW != TypeVS.valueOf(dataMap.get("operation")))) {
            throw new ExceptionVS(messages.get("paramsErrorMsg"));
        }
        Collection<X509Certificate> certChain = PEMUtils.fromPEMToX509CertCollection(
                dataMap.get("certChainPEM").getBytes());
        User newUser = User.FROM_X509_CERT(certChain.iterator().next());
        cmsBean.verifyUserCertificate(newUser);
        newUser = subscriptionBean.checkUser(newUser);
        dao.merge(newUser.setState(User.State.ACTIVE).setReason(dataMap.get("info")));
        return newUser;
    }

    @Transactional
    public UserDto getUserDto(User user, boolean withCerts) throws Exception {
        List<Certificate> certificates = null;
        if(withCerts) {
            Query query = dao.getEM().createQuery("SELECT c FROM Certificate c WHERE c.user =:user and c.state =:state")
                    .setParameter("user", user).setParameter("state", Certificate.State.OK);
            certificates = query.getResultList();
        }
        Set<DeviceDto> deviceDtoSet = SessionManager.getInstance().connectedDeviceMap(user.getId());
        return UserDto.DEVICES(user, deviceDtoSet, certificates);
    }
    
}
package org.currency.web.ejb;

import org.currency.web.websocket.SessionManager;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.model.User;
import org.votingsystem.model.Certificate;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

@Stateless
public class UserEJB {

    private static Logger log = Logger.getLogger(UserEJB.class.getName());

    @PersistenceContext
    private EntityManager em;

    @TransactionAttribute(REQUIRES_NEW)
    public UserDto getUserDto(User user, boolean withCerts) throws Exception {
        List<Certificate> certificates = null;
        if(withCerts) {
            certificates = em.createQuery("SELECT c FROM Certificate c WHERE c.signer =:signer and c.state =:state")
                    .setParameter("signer", user).setParameter("state", Certificate.State.OK).getResultList();
        }
        Set<DeviceDto> deviceDtoSet = SessionManager.getInstance().connectedDeviceMap(user.getId());
        return UserDto.DEVICES(user, deviceDtoSet, certificates);
    }
    
}
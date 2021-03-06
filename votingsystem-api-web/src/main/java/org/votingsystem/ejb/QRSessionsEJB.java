package org.votingsystem.ejb;

import org.votingsystem.dto.OperationDto;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@Lock(LockType.READ)
public class QRSessionsEJB {

    private static Logger log = Logger.getLogger(QRSessionsEJB.class.getName());

    private static final Map<String, OperationDto> operationRequestMap = new ConcurrentHashMap<>();

    public void putOperation(String uuid, OperationDto operation) {
        operationRequestMap.put(uuid, operation);
    }

    public OperationDto getOperation(String uuid) {
        return operationRequestMap.get(uuid);
    }

    public void removeOperation(String uuid) {
        if(operationRequestMap.containsKey(uuid))
            operationRequestMap.remove(uuid);
    }

    public Set<String> getSessionKeys() {
        return operationRequestMap.keySet();
    }

}
package org.votingsystem.client.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.dto.OperationVS;

import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface WebKitHost {

    public void invokeBrowserCallback(Map dataMap, String callerCallback) throws JsonProcessingException;
    public void processOperationVS(OperationVS operationVS, String passwordDialogMessage);
    public void processOperationVS(String password, OperationVS operationVS);
    public void processSignalVS(Map signalData);
}

package org.votingsystem.client.util;

import net.sf.json.JSON;
import org.votingsystem.model.OperationVS;

import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface WebKitHost {

    public void invokeBrowserCallback(JSON messageJSON, String callerCallback);
    public void processOperationVS(OperationVS operationVS, String passwordDialogMessage);
    public void processOperationVS(String password, OperationVS operationVS);
    public void processSignalVS(Map signalData);
}

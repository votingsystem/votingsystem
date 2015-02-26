package org.votingsystem.client.util;

import javafx.concurrent.Task;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.CooinServer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.HttpHelper;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinCheckerTask extends Task<CooinCheckResponse> {

    private static Logger log = Logger.getLogger(CooinCheckerTask.class);

    public interface Listener {
        public void processCooinStatus(CooinCheckResponse response);
    }

    private Set<Cooin> cooinSet;
    private Listener listener;

    public CooinCheckerTask(Set<Cooin> cooinSet, Listener listener) {
        this.cooinSet = cooinSet;
        this.listener = listener;
    }

    @Override protected CooinCheckResponse call() throws Exception {
        try {
            updateMessage(ContextVS.getMessage("checkingCooinsMsg"));
            ResponseVS responseVS = Utils.checkServer(cooinSet.iterator().next().getCooinServerURL());
            CooinCheckResponse response;
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                response =  CooinCheckResponse.load(responseVS);
                if(listener != null) listener.processCooinStatus(response);
                return response;
            }
            CooinServer cooinServer = (CooinServer) responseVS.getData();
            Set<String> hashCertSet = cooinSet.stream().map(cooin -> {return cooin.getHashCertVS();}).collect(toSet());
            JSONArray resquestArray = new JSONArray();
            resquestArray.addAll(hashCertSet);
            responseVS = HttpHelper.getInstance().sendData(resquestArray.toString().getBytes(),
                    ContentTypeVS.JSON, cooinServer.getCooinBundleStateServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONArray result = (JSONArray) responseVS.getMessageJSON();
                Set<String> errorSet = new HashSet<>();
                Set<String> OKSet = new HashSet<>();
                for(int i = 0; i < result.size(); i++) {
                    JSONObject cooinData = result.getJSONObject(i);
                    if(Cooin.State.OK == Cooin.State.valueOf(cooinData.getString("state"))) {
                        OKSet.add(cooinData.getString("hashCertVS"));
                    } else errorSet.add(cooinData.getString("hashCertVS"));
                }
                Integer statusCode = errorSet.size() > 0? ResponseVS.SC_ERROR : ResponseVS.SC_OK;
                response = new CooinCheckResponse(statusCode, null, OKSet, errorSet);
            } else response = CooinCheckResponse.load(responseVS);
            if(listener != null) listener.processCooinStatus(response);
            return response;
        } catch (Exception ex) {
            ex.printStackTrace();
            return CooinCheckResponse.load(new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage()));
        }
    }
}

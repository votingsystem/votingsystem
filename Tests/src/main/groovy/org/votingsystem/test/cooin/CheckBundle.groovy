package org.votingsystem.test.cooin

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import net.sf.json.util.JSONUtils
import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.CooinServer
import org.votingsystem.model.ResponseVS
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.HttpHelper


Logger log = TestUtils.init(Test.class)

CooinServer cooinServer = TestUtils.fetchCooinServer(ContextVS.getInstance().config.cooinServerURL)
Set<String> hashCertSet = new HashSet<>(Arrays.asList("g8SAcWHXZ4GeZGyIciedc6zBPoXyS1HFDUCvhw8W5h0="))
JSONArray resquestArray = new JSONArray();
resquestArray.addAll(hashCertSet);

ResponseVS responseVS = HttpHelper.getInstance().sendData(resquestArray.toString().getBytes(),
        ContentTypeVS.JSON, cooinServer.getCooinBundleStateServiceURL());
log.debug("mayBeJSON: " + JSONUtils.mayBeJSON(responseVS.getMessage().trim()))
JSONObject result = responseVS.getMessageJSON();
log.debug("result: " + result.toString())

System.exit(0)
package org.votingsystem.applet.votingtool;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.votingsystem.applet.util.HttpHelper;
import org.votingsystem.applet.util.OSValidator;
import static org.votingsystem.model.ContextVS.CERT_RAIZ_PATH;
import org.votingsystem.model.AppHostVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.FileUtils;
import com.itextpdf.text.pdf.PdfName;
import iaik.pkcs.pkcs11.Mechanism;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.votingsystem.model.EventVSBase;
import org.votingsystem.model.VoteVS;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public enum VotingToolContext {
    
    INSTANCE;

    private static Logger logger = Logger.getLogger(VotingToolContext.class);
    
    public static final Mechanism DNIe_SESSION_MECHANISM = Mechanism.SHA1_RSA_PKCS;
    public static final PdfName PDF_SIGNATURE_NAME = PdfName.ADBE_PKCS7_SHA1;    
    
    private static ExecutorService executor;
    private static CompletionService<ResponseVS> completionService;
    
    private Map<String, VoteVS> receiptMap;
    
    public static class DEFAULTS {
        public static String BASEDIR =  System.getProperty("user.home");
        public static String APPDIR =  BASEDIR + File.separator + 
                ".VotingSystem"  + File.separator;
        public static String APPTEMPDIR =  APPDIR + File.separator + "tempVotingTool"
             + File.separator;
    }
    
    public static String TEMPDIR =  System.getProperty("java.io.tmpdir");
    
    public static void init (AppHostVS appHost, String logPropertiesFile, 
            String localizatedMessagesFileName, String locale){
        try {
            logger.debug("------------- init ----------------- ");
            new File(DEFAULTS.APPDIR).mkdir();
            new File(DEFAULTS.APPTEMPDIR).mkdir();
            ContextVS.init(appHost, logPropertiesFile, 
                    localizatedMessagesFileName, locale);
            HttpHelper.init();
            File copiaRaizDNI = new File(DEFAULTS.APPDIR + CERT_RAIZ_PATH);
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(CERT_RAIZ_PATH), copiaRaizDNI);
            OSValidator.initClassPath();
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(VotingToolContext.class.getName()).log(
                    Level.SEVERE, null, ex);
        }
    }
    
    public void shutdown() {
        try {
            logger.debug("------------- shutdown ----------------- ");
            if(executor != null) executor.shutdown();
            HttpHelper.INSTANCE.shutdown();
            FileUtils.deleteRecursively(new File(DEFAULTS.APPTEMPDIR));
        } catch (IOException ex) {
           logger.error(ex.getMessage(), ex);
        }
    }
    
    public void submit(Runnable runnable) {
        if(executor == null || executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(10);
            completionService = 
                    new ExecutorCompletionService<ResponseVS>(executor);
        }
        executor.submit(runnable);
    }
    
    public Future<ResponseVS> submit(Callable<ResponseVS> callable) {
        return completionService.submit(callable);
    }
    
    public JSONObject getVoteCancelationInSession(String hashCertVoteBase64) {
        logger.debug("getVoteCancelationInSession");
        if(receiptMap == null) return null;
        VoteVS recibo = receiptMap.get(hashCertVoteBase64);
        if(recibo == null) return null;
        EventVSBase voto = (EventVSBase)recibo.getVote();
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(
                voto.getCancelVoteDataMap());
        return jsonObject;
    } 
    
    public VoteVS getVoteReceipt(String hashCertVoteBase64) {
        if(receiptMap == null || hashCertVoteBase64 == null) return null;
        return receiptMap.get(hashCertVoteBase64);
    }
    
    public void addVoteReceipt(String hashCertVoteBase64, VoteVS receipt) {
        if(receiptMap == null) receiptMap = new HashMap<String, VoteVS>();
        receiptMap.put(hashCertVoteBase64, receipt);
    }
}

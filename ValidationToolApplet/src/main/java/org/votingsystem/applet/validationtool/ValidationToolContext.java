package org.votingsystem.applet.validationtool;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import org.votingsystem.model.AppHostVS;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public enum ValidationToolContext {
     
    INSTANCE;
    
    private static Logger logger = Logger.getLogger(ValidationToolContext.class);
    
    private static ExecutorService executor;
    private static CompletionService<ResponseVS> completionService;
    
    public static class DEFAULTS {
        public static String BASEDIR =  System.getProperty("user.home");
        public static String APPDIR =  BASEDIR + File.separator + 
                ".VotingSystem"  + File.separator;
        public static String APPTEMPDIR =  APPDIR + File.separator + "tempValidationTool"
             + File.separator;
    }

    public static void init (AppHostVS appHost, String logPropertiesFile, 
            String localizatedMessagesFileName, String locale){
        try {
            logger.debug("------------- init ----------------- ");

            ContextVS.init(appHost, logPropertiesFile, 
                    localizatedMessagesFileName, locale);
            //HttpHelper.init();
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(ValidationToolContext.class.getName()).log(
                    Level.SEVERE, null, ex);
        }
    }
    

 
    public void shutdown() {
        try {
            logger.debug("------------- shutdown ----------------- ");
            if(executor != null) executor.shutdown();
            //HttpHelper.INSTANCE.shutdown();
        } catch (Exception ex) {
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
    
}
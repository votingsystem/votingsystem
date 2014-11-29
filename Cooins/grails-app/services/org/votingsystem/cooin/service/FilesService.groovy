package org.votingsystem.cooin.service

import grails.converters.JSON
import grails.transaction.Transactional
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class FilesService {

	def grailsApplication
	def messageSource

	public void init() {
        new File("${grailsApplication.config.vs.errorsBaseDir}").mkdirs()
        new File("${grailsApplication.config.vs.backupCopyPath}").mkdirs()
        File polymerPlatform = grailsApplication.mainContext.getResource("bower_components/polymer/polymer.js").getFile()
        if(!polymerPlatform.exists()) {
            log.error "Have you executed 'bower install' from web-app dir ???"
        }
	}

    private static final POLL_INTERVAL = 1000

    private ExecutorService executorService = null

    public void monitorFile(final File file) throws IOException {
        if(executorService) executorService.shutdownNow()
        executorService = Executors.newSingleThreadExecutor()
        executorService.execute(new Runnable() {
            @Override void run() {
                log.debug("monitorFile - runnable - monitoring file: ${file.absolutePath}")
                FileReader reader = new FileReader(file);
                BufferedReader buffered = new BufferedReader(reader);
                //File destFile = new File('./CooinReports/eventTest.log')
                FileWriter fileWritter = new FileWriter('./CooinReports/eventTest.log', true);
                BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
                //bufferWritter.close();
                try {
                    while(true) {
                        String line = buffered.readLine();
                        if(line == null) {
                            // end of file, start polling
                            Thread.sleep(POLL_INTERVAL);
                        } else {
                            line = line.substring(0, line.length() - 1);
                            def messageJSON = JSON.parse(line)
                            /*if(Integer.valueOf(messageJSON.status) > 50) {
                                bufferWritter.write(line + ",\n");
                                bufferWritter.flush();
                            }*/
                            log.debug(line);
                        }
                    }
                } catch(InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        })

    }

}
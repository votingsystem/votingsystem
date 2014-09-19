package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.votingsystem.util.DateUtils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class FilesService {

    private static final CLASS_NAME = CsrService.class.getSimpleName()

	def grailsApplication
	def messageSource

	public void init() {
        new File("${grailsApplication.config.VotingSystem.errorsBaseDir}").mkdirs()
        new File("${grailsApplication.config.VotingSystem.backupCopyPath}").mkdirs()
        File polymerPlatform = grailsApplication.mainContext.getResource("bower_components/polymer/polymer.js").getFile()
        if(!polymerPlatform.exists()) {
            log.error "Have you executed 'bower install' from web-app dir ???"
        }
	}

    public Map<String, File> getWeekReportFiles(DateUtils.TimePeriod timePeriod, String subPath){
        DateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        String dateFromPathPart = formatter.format(timePeriod.getDateFrom())
        String dateToPathPart = formatter.format(timePeriod.getDateTo())
        String baseDirPath ="${grailsApplication.config.VotingSystem.backupCopyPath}/weekReports/${dateFromPathPart}_${dateToPathPart}/"
        if(subPath) baseDirPath = "${baseDirPath}${subPath}"
        File baseDir = new File(baseDirPath)
        baseDir.mkdirs()
        File receiptFile = new File("${baseDirPath}/receipt.p7s")
        return [baseDir:baseDir, reportsFile:new File("${baseDirPath}/balances.json"), systemReceipt:receiptFile]
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


                //File destFile = new File('./VicketReports/eventTest.log')
                FileWriter fileWritter = new FileWriter('./VicketReports/eventTest.log', true);
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


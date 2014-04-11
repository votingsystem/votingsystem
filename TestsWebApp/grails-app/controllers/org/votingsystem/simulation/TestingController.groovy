package org.votingsystem.simulation

import org.codehaus.groovy.grails.web.json.JSONObject
import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.FieldEventVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.VoteVS
import org.votingsystem.util.HttpHelper

class TestingController {
	
	def testService
	def grailsApplication
	
	private Timer timer = null


	def index() {
        ResponseVS responseVS = HttpHelper.getInstance().getData(
                "http://sistemavotacion.org/AccessControl/backupVS/download/13", ContentTypeVS.BACKUP);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            render "TODO validate backup";
            /*FutureTask<ResponseVS> future = new FutureTask<ResponseVS>(
                new ZipBackupValidator(responseVS.getMessageBytes()));
            simulatorExecutor.execute(future);
            responseVS = future.get();
            log.debug("BackupRequestWorker - status: " + responseVS.getStatusCode());*/
        } else render "Error"
        return false
	}

    def vote() {
        VoteVS voteVS = new VoteVS();
        EventVS eventVS = new EventVS();
        eventVS.setUrl("http//sistemavotacion.org")
        FieldEventVS field = new FieldEventVS()
        field.setId(1)
        field.setContent("Opción a");
        voteVS.setOptionSelected(field)
        voteVS.setEventVS(eventVS);
        grails.converters.JSON jsonConv = new grails.converters.JSON(voteVS.getVoteDataMap())
        /*JSONObject object = new JSONObject(voteVS.getVoteDataMap())
        log.debug("========= object.optionSelected?.id: " + object.optionSelected?.id)
        StringWriter sw = new StringWriter()
        object.write(sw)
        //render voteVS.getVoteDataMap() as JSON
        render sw.toString()*/
        render new grails.converters.JSON(voteVS.getVoteDataMap()).toString()
        return false
    }
	
	private void initTimer() {
		log.debug("initTimer")
		timer = new Timer();
		TimerDemo timerDemo = new TimerDemo()
		timer.schedule(timerDemo, 1000, 1000);
		log.debug("TimerDemo started")
	}
	
	private void stopTimer() {
		log.debug("stopTimer")
		timer?.cancel();
	}
	
	
	class TimerDemo extends TimerTask {
		public void run() { log.debug("run timer ==========") }
	}
	

}

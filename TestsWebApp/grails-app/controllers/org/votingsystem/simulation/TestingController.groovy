package org.votingsystem.simulation

import java.util.Timer;
import java.util.TimerTask;

import grails.util.Metadata

import org.bouncycastle.jce.provider.BouncyCastleProvider

class TestingController {
	
	def testService
	def grailsApplication
	
	
	private Timer timer = null

	def index() {
		render (view:"test")
	}
	
	def stop() {
		stopTimer();
		render "OK - stopTimer"
		return false
	}
	
	private void initTimer() {
		log.debug("======== initTimer")
		timer = new Timer();
		TimerDemo timerDemo = new TimerDemo()
		timer.schedule(timerDemo, 1000, 1000);
		log.debug("TimerDemo started")
	}
	
	private void stopTimer() {
		log.debug("======== stopTimer")
		timer?.cancel();
	}
	
	
	class TimerDemo extends TimerTask {
		public void run() {
			log.debug("run timer ==========")
		}
	}
	

}

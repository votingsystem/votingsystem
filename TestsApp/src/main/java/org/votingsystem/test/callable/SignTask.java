package org.votingsystem.test.callable;


import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class SignTask extends TimerTask {

    public interface Launcher {
        public void processTask(String param);
    }

    private Timer simulationTimer;
    private List<String> synchronizedSignerList;
    private Launcher launcher;

    public SignTask(Timer simulationTimer,  List<String> synchronizedSignerList, Launcher launcher) {
        this.simulationTimer = simulationTimer;
        this.synchronizedSignerList = synchronizedSignerList;
        this.launcher = launcher;
    }
    public void run() {
        if(!synchronizedSignerList.isEmpty()) {
            int randomSigner = new Random().nextInt(synchronizedSignerList.size());
            launcher.processTask(synchronizedSignerList.remove(randomSigner));
        } else simulationTimer.cancel();
    }
}

package org.votingsystem.test.callable

/**
 * Created by jgzornoza on 17/10/14.
 */
class SignTask extends TimerTask  {

    public interface Launcher {
        public void processTask(String param);
    }

    Timer simulationTimer
    List<String> synchronizedSignerList
    Launcher launcher
    public SignTask(Timer simulationTimer,  List<String> synchronizedSignerList, Launcher launcher) {
        this.simulationTimer = simulationTimer
        this.synchronizedSignerList = synchronizedSignerList
        this.launcher = launcher
    }
    public void run() {
        if(!synchronizedSignerList.isEmpty()) {
            int randomSigner = new Random().nextInt(synchronizedSignerList.size());
            launcher.processTask(synchronizedSignerList.remove(randomSigner));
        } else simulationTimer.stop();
    }
}

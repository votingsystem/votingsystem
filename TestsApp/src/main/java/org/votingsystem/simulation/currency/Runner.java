package org.votingsystem.simulation.currency;

import desmoj.core.util.AccessPoint;
import desmoj.core.util.SimRunListener;
import desmoj.extensions.experimentation.ui.ExperimentStarterApplication;
import desmoj.extensions.experimentation.ui.GraphicalObserverContext;
import desmoj.extensions.experimentation.util.AccessUtil;
import desmoj.extensions.experimentation.util.ExperimentRunner;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Runner extends ExperimentRunner {

    public Runner() {
        super();
    }

    public Runner(UserToUserModel m) {
        super(m);
    }

    public SimRunListener[] createSimRunListeners(GraphicalObserverContext c) {
        UserToUserModel model = (UserToUserModel)getModel();
        //TimeSeriesPlotter tp1 = new TimeSeriesPlotter("Trucks",c, model.trucksArrived, 360,360);
        //tp1.addTimeSeries(model.trucksServiced);
        //HistogramPlotter hp = new HistogramPlotter("Truck Wait Times", c, model.waitTimeHistogram,"h", 360,360, 365,0);
        //return new SimRunListener[] {tp1, hp};
        return new SimRunListener[] {};
    }

    public Map<String,AccessPoint> createParameters() {
        Map<String,AccessPoint> pm = super.createParameters();
        AccessUtil.setValue(pm, EXP_STOP_TIME, 1500.0);
        AccessUtil.setValue(pm, EXP_TRACE_STOP, 100.0);
        AccessUtil.setValue(pm, EXP_REF_UNIT, TimeUnit.MINUTES);
        return pm;
    }

    public static void main(String[] args) throws Exception {
        new ExperimentStarterApplication(UserToUserModel.class, Runner.class).setVisible(true);
    }
}

package org.votingsystem.client.util;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * This class is because this method blocks child stages:
 * stage.setFullScreenExitHint("");
 * stage.setFullScreen(!stage.isFullScreen());
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class FullScreenHelper {
    
    private Stage stage;
    private boolean fullScreen = false;
    private double prevWidth = 800;
    private double prevHeight = 600;
    private double xPos = 0;
    private double yPos = 0;
    
    public FullScreenHelper(Stage stage) {
        this.stage = stage;
    }
    
    public void toggleFullScreen() {
        fullScreen = !fullScreen;
        if(fullScreen) {
            prevWidth = stage.getWidth();
            prevHeight = stage.getHeight();
            xPos = stage.getX();
            yPos = stage.getY();
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
        } else {
            stage.setX(xPos);
            stage.setY(yPos);
            stage.setWidth(prevWidth);
            stage.setHeight(prevHeight);
        }
    }
}

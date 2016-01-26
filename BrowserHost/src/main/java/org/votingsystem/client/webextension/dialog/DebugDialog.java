package org.votingsystem.client.webextension.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DebugDialog extends DialogVS {

    private static Logger log = Logger.getLogger(DebugDialog.class.getSimpleName());

    private Button testButton;
    private static DebugDialog dialog;
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public DebugDialog() {
        super(new VBox(10));
        VBox mainDialog = (VBox) getContentPane();
        testButton = new Button("TEST");
        testButton.setOnAction(actionEvent -> {
            try {
                File transactionsPlan = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("test.json"));
                OperationVS operationVS = JSON.getMapper().readValue(transactionsPlan, OperationVS.class);
                operationVS.initProcess();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
        });
        mainDialog.getChildren().addAll(testButton);
        setCaption("DebugDialog");
    }

    public static void showDialog() {
        PlatformImpl.runLater(() -> {
            if(dialog == null) {
                dialog = new DebugDialog();
                dialog.initModality(Modality.NONE);
            }
            dialog.show();
        });
    }

}
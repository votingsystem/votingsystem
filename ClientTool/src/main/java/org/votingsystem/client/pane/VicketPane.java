package org.votingsystem.client.pane;

import com.sun.javafx.application.PlatformImpl;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.client.model.SignedFile;
import org.votingsystem.client.util.Formatter;
import org.votingsystem.client.util.SignatureService;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.*;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.vicket.model.Vicket;

import java.io.File;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VicketPane extends GridPane {

    private static Logger log = Logger.getLogger(VicketPane.class);

    private Vicket vicket;
    private VicketServer vicketServer;
    private WebView signatureContentWebView;
    private Label vicketStatusLbl;
    private Runnable statusChecker = new Runnable() {
        @Override public void run() {
            try {
                ResponseVS responseVS = SignatureService.checkServer(vicket.getVicketServerURL());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    vicketServer = (VicketServer) responseVS.getData();
                    responseVS = HttpHelper.getInstance().getData(
                            vicketServer.getVicketStatusServiceURL(vicket.getHashCertVS()), null);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        vicketStatusLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #61a753");
                        vicketStatusLbl.setText(ContextVS.getMessage("vicketOKLbl"));
                    } else {
                        vicketStatusLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #6c0404;");
                        vicketStatusLbl.setText(responseVS.getMessage());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public VicketPane(final Vicket vicket) {
        super();
        this.vicket = vicket;
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(10, 10, 10, 10));

        vicketStatusLbl = new Label();
        vicketStatusLbl.setWrapText(true);


        Button openSignatureInfoButton = new Button(ContextVS.getMessage("openSignedFileButtonLbl"));
        openSignatureInfoButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                log.debug("openSignatureInfoButton");

            }
        });
        openSignatureInfoButton.setPrefWidth(200);

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        ColumnConstraints column2 = new ColumnConstraints();
        getColumnConstraints().addAll(column1, column2);
        setHalignment(openSignatureInfoButton, HPos.RIGHT);
        signatureContentWebView = new WebView();
        signatureContentWebView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));

        openSignatureInfoButton.setGraphic(new ImageView(Utils.getImage(this, "accept")));
        openSignatureInfoButton.setText(ContextVS.getMessage("signatureOKLbl"));

        signatureContentWebView.getEngine().loadContent(JSONSerializer.toJSON(vicket.getCertSubject().getDataMap()).toString(3),
                "application/json");
        signatureContentWebView.setPrefHeight(400);
        VBox.setVgrow(signatureContentWebView, Priority.ALWAYS);
        add(vicketStatusLbl, 0,0);
        add(openSignatureInfoButton, 1, 0);
        add(signatureContentWebView, 0, 1);
        setColumnSpan(signatureContentWebView, 2);

        PlatformImpl.runLater(statusChecker);
    }

    public Vicket getVicket () {
        return vicket;
    }

}
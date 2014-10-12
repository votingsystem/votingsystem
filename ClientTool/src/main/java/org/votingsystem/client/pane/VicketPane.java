package org.votingsystem.client.pane;

import com.sun.javafx.application.PlatformImpl;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.client.service.SignatureService;
import org.votingsystem.client.util.DocumentVS;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.*;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.vicket.model.Vicket;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VicketPane extends GridPane implements DocumentVS {

    private static Logger log = Logger.getLogger(VicketPane.class);

    private Vicket vicket;
    private VicketServer vicketServer;
    private WebView signatureContentWebView;
    private Label vicketStatusLbl;
    private Button sendVicketButton;
    private Runnable statusChecker = new Runnable() {
        @Override public void run() {
            try {
                ResponseVS responseVS = SignatureService.checkServer(vicket.getVicketServerURL());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    vicketServer = (VicketServer) responseVS.getData();
                    responseVS = HttpHelper.getInstance().getData(
                            vicketServer.getVicketStatusServiceURL(vicket.getHashCertVS()), null);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        sendVicketButton.setText(responseVS.getMessage());
                        sendVicketButton.setVisible(true);
                    } else {
                        vicketStatusLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #6c0404;");
                        vicketStatusLbl.setText(responseVS.getMessage());
                        sendVicketButton.setVisible(false);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public VicketPane(final Vicket vicket) throws ExceptionVS {
        super();
        this.vicket = vicket;



        CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                ContextVS.getInstance().getVicketServer().getTrustAnchors(), false, Arrays.asList(
                vicket.getCertificationRequest().getCertificate()));
        X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
        log.debug("VicketPane. Vicket issuer: " + certCaResult.getSubjectDN().toString());



        setHgap(10);
        setVgap(10);
        setPadding(new Insets(10, 10, 10, 10));

        vicketStatusLbl = new Label();
        vicketStatusLbl.setWrapText(true);

        Label serverLbl = new Label(vicket.getCertSubject().getVicketServerURL());
        Label hashLbl = new Label(vicket.getHashCertVS());
        serverLbl.getStyleClass().add("server");
        hashLbl.getStyleClass().add("server");

        Label vicketValueLbl = new Label(vicket.getAmount().toString() + " " + vicket.getCurrencyCode());
        vicketValueLbl.getStyleClass().add("vicketValue");
        setHalignment(vicketValueLbl, HPos.CENTER);
        Label vicketTagLbl = new Label(ContextVS.getMessage("forLbl") + " " + Utils.getTagDescription(vicket.getTag().getName()));
        vicketTagLbl.getStyleClass().add("tag");
        setHalignment(vicketTagLbl, HPos.CENTER);


        sendVicketButton = new Button();
        sendVicketButton.setGraphic(new ImageView(Utils.getImage(this, "accept")));
        sendVicketButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                log.debug("sendVicketButton");

            }
        });
        sendVicketButton.setPrefWidth(200);
        sendVicketButton.setVisible(false);

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        ColumnConstraints column2 = new ColumnConstraints();
        getColumnConstraints().addAll(column1, column2);
        setHalignment(sendVicketButton, HPos.RIGHT);
        signatureContentWebView = new WebView();
        signatureContentWebView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));




        signatureContentWebView.getEngine().loadContent(JSONSerializer.toJSON(vicket.getCertSubject().getDataMap()).toString(3),
                "application/json");
        signatureContentWebView.setPrefHeight(300);
        signatureContentWebView.setPrefWidth(400);
        VBox.setVgrow(signatureContentWebView, Priority.ALWAYS);

        add(serverLbl, 0,0);
        add(hashLbl, 1,0);
        add(vicketStatusLbl, 0,1);
        add(sendVicketButton, 1, 1);
        add(vicketValueLbl, 0, 2);
        add(vicketTagLbl, 0, 3);
        add(signatureContentWebView, 0, 5);
        setColumnSpan(signatureContentWebView, 2);
        setColumnSpan(vicketValueLbl, 2);
        setColumnSpan(vicketTagLbl, 2);

        PlatformImpl.runLater(statusChecker);
    }

    public Vicket getVicket () {
        return vicket;
    }

    @Override public byte[] getDocumentBytes() throws Exception {
        return ObjectUtils.serializeObject(vicket);
    }

    @Override public ContentTypeVS getContentTypeVS() {
        return ContentTypeVS.VICKET;
    }
}
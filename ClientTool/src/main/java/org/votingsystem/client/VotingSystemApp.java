package org.votingsystem.client;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.dialog.SettingsDialog;
import org.votingsystem.client.pane.DecompressBackupPane;
import org.votingsystem.client.pane.DocumentVSBrowserStackPane;
import org.votingsystem.client.pane.SignDocumentFormPane;
import org.votingsystem.client.util.BrowserVSSessionUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.VicketServer;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.HttpHelper;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VotingSystemApp extends Application implements DecompressBackupPane.Listener {

    private static Logger log = Logger.getLogger(VotingSystemApp.class);

    private VBox mainBox;
    private VBox votingSystemOptionsBox;
    private VBox vicketOptionsBox;
    private HBox headerButtonsBox;
    public static String locale = "es";
    private static VotingSystemApp INSTANCE;
    private Map<String, String> smimeMessageMap;

    static {
        //Without this WebView always send requests with 'en-us,en;q=0.5'
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            public URLStreamHandler createURLStreamHandler(String protocol) {
                if (protocol.toLowerCase().contains("http") || protocol.toLowerCase().contains("https")) {
                    return new URLStreamHandler() {
                        protected URLConnection openConnection(URL url) throws IOException {
                            URLConnection reConnection = new sun.net.www.protocol.http.HttpURLConnection(url, null);
                            reConnection.setRequestProperty("Accept-Language", Locale.getDefault().getLanguage());
                            reConnection.addRequestProperty("Accept-Language", Locale.getDefault().getLanguage());
                            return reConnection;
                        }
                    };
                }
                // Don't handle a non-http protocol, so just return null and let
                // the system return the default one.
                return null;
            }
        });

    }

    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return ContextVS.getInstance().getVotingSystemSSLCerts().toArray(new X509Certificate[]{});
            }
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                log.debug("trustAllCerts - checkClientTrusted");
            }
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType ) throws CertificateException {
                log.debug("trustAllCerts - checkServerTrusted");
                try {
                    CertUtils.verifyCertificate(ContextVS.getInstance().getVotingSystemSSLTrustAnchors(), false,
                            Arrays.asList(certs));
                } catch(Exception ex) {
                    throw new CertificateException(ex.getMessage());
                }
            }
        }
    };

    public String getSMIME(String smimeMessageURL) {
        if(smimeMessageMap ==  null) return null;
        else return smimeMessageMap.get(smimeMessageURL);
    }

    public void setSMIME(String smimeMessageURL, String smimeMessageStr) {
        if(smimeMessageMap ==  null) {
            smimeMessageMap = new HashMap<String, String>();
        }
        smimeMessageMap.put(smimeMessageURL, smimeMessageStr);
    }

    @Override public void stop() {
        log.debug("stop");
        //Platform.exit();
        System.exit(0);
    }

    public static VotingSystemApp getInstance() {
        return INSTANCE;
    }

    @Override public void start(final Stage primaryStage) throws Exception {
        INSTANCE = this;
        new Thread(new Runnable() {
            @Override public void run() {
                boolean loadedFromJar = false;
                if(VotingSystemApp.class.getResource(VotingSystemApp.this.getClass().getSimpleName() +
                        ".class").toString().contains("jar:file")) {
                    loadedFromJar = true;
                }
                log.debug("ServerLoaderTask - loadedFromJar: " + loadedFromJar);
                try {
                    SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                } catch (GeneralSecurityException ex) {
                    log.error(ex.getMessage(), ex);
                }
                String accessControlServerURL = null;
                String vicketsServerURL = null;
                if(loadedFromJar) {
                    HttpHelper.getInstance().initVotingSystemSSLMode();
                    accessControlServerURL = ContextVS.getMessage("prodAccessControlServerURL");
                    vicketsServerURL = ContextVS.getMessage("prodVicketsServerURL");
                } else {
                    accessControlServerURL = ContextVS.getMessage("devAccessControlServerURL");
                    vicketsServerURL = ContextVS.getMessage("devVicketsServerURL");
                }
                ResponseVS responseVS = null;
                try {
                    responseVS = Utils.checkServer(accessControlServerURL);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        setVotingSystemAvailable(true, primaryStage);
                        ContextVS.getInstance().setAccessControl((AccessControlVS) responseVS.getData());
                        BrowserVSSessionUtils.getInstance().checkCSRRequest();
                    }
                } catch(Exception ex) {log.error(ex.getMessage(), ex);}
                try {
                    responseVS = Utils.checkServer(vicketsServerURL);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        setVicketServerAvailable(true, primaryStage);
                        ContextVS.getInstance().setVicketServer((VicketServer) responseVS.getData());
                    }
                }
                catch(Exception ex) {log.error(ex.getMessage(), ex);}
            }
        }).start();
        mainBox = new VBox();
        headerButtonsBox = new HBox(20);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox.setMargin(headerButtonsBox, new Insets(0, 0, 20, 0));
        ChoiceBox documentChoiceBox = new ChoiceBox();
        documentChoiceBox.setPrefWidth(150);
        final String[] documentOptions = new String[]{ContextVS.getMessage("documentsLbl"),
                ContextVS.getMessage("openFileButtonLbl"),
                ContextVS.getMessage("signDocumentButtonLbl")};
        documentChoiceBox.getItems().addAll(documentOptions);
        documentChoiceBox.getSelectionModel().selectFirst();
        documentChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue ov, Number value, Number new_value) {
                log.debug("value: " + value + " -new_value: " + new_value + " - option: " + documentOptions[new_value.intValue()]);
                String selectedOption = documentOptions[new_value.intValue()];
                if(ContextVS.getMessage("openFileButtonLbl").equals(selectedOption)) {
                    DocumentVSBrowserStackPane.showDialog(null, null);
                } else if(ContextVS.getMessage("signDocumentButtonLbl").equals(selectedOption)) {
                    SignDocumentFormPane.showDialog();
                }
                documentChoiceBox.getSelectionModel().select(0);
            }
        });
        votingSystemOptionsBox = new VBox(15);
        Button voteButton = new Button(ContextVS.getMessage("voteButtonLbl"));
        voteButton.setGraphic(Utils.getImage(FontAwesome.Glyph.ENVELOPE));
        voteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openVotingSystemURL(ContextVS.getInstance().getAccessControl().getVotingPageURL(),
                        ContextVS.getMessage("voteButtonLbl"));
            }});
        voteButton.setPrefWidth(500);

        Button selectRepresentativeButton = new Button(ContextVS.getMessage("selectRepresentativeButtonLbl"));
        selectRepresentativeButton.setGraphic(Utils.getImage(FontAwesome.Glyph.HAND_ALT_RIGHT));
        selectRepresentativeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openVotingSystemURL(ContextVS.getInstance().getAccessControl().getSelectRepresentativePageURL(),
                        ContextVS.getMessage("selectRepresentativeButtonLbl"));
            }});
        selectRepresentativeButton.setPrefWidth(500);
        votingSystemOptionsBox.getChildren().addAll(voteButton, selectRepresentativeButton);
        vicketOptionsBox = new VBox(15);
        Button vicketUsersProceduresButton = new Button(ContextVS.getMessage("financesLbl"));
        vicketUsersProceduresButton.setGraphic(Utils.getImage(FontAwesome.Glyph.BAR_CHART_ALT));
        vicketUsersProceduresButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openVicketURL(ContextVS.getInstance().getVicketServer().getUserDashBoardURL(),
                        ContextVS.getMessage("vicketUsersLbl"));
            }});
        vicketUsersProceduresButton.setPrefWidth(500);
        Button walletButton = new Button(ContextVS.getMessage("walletLbl"));
        walletButton.setGraphic(Utils.getImage(FontAwesome.Glyph.MONEY));
        walletButton.setPrefWidth(500);
        walletButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openVicketURL(ContextVS.getInstance().getVicketServer().getWalletURL(),
                        ContextVS.getMessage("walletLbl"));
            }});
        vicketOptionsBox.getChildren().addAll(vicketUsersProceduresButton, walletButton);
        vicketOptionsBox.setStyle("-fx-alignment: center;");
        ChoiceBox adminChoiceBox = new ChoiceBox();
        adminChoiceBox.setPrefWidth(180);
        final String[] adminOptions = new String[]{ContextVS.getMessage("adminLbl"),
                ContextVS.getMessage("settingsLbl"),
                ContextVS.getMessage("vicketAdminLbl"),
                ContextVS.getMessage("votingSystemProceduresLbl")};
        adminChoiceBox.getItems().addAll(adminOptions);
        adminChoiceBox.getSelectionModel().selectFirst();
        adminChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue ov, Number value, Number new_value) {
                    log.debug("value: " + value + " - new_value: " + new_value + " - option: " +
                            adminOptions[new_value.intValue()]);
                    String selectedOption = adminOptions[new_value.intValue()];
                    if(ContextVS.getMessage("settingsLbl").equals(selectedOption)) {
                        openSettings();
                    } else if(ContextVS.getMessage("vicketAdminLbl").equals(selectedOption)) {
                        openVicketURL(ContextVS.getInstance().getVicketServer().getAdminDashBoardURL(),
                                ContextVS.getMessage("vicketAdminLbl"));
                    } else if(ContextVS.getMessage("votingSystemProceduresLbl").equals(selectedOption)) {
                        openVotingSystemURL(ContextVS.getInstance().getAccessControl().getDashBoardURL(),
                                ContextVS.getMessage("votingSystemProceduresLbl"));
                    }
                    adminChoiceBox.getSelectionModel().select(0);
                }
            });
        mainBox.getChildren().add(0, headerButtonsBox);
        headerButtonsBox.getChildren().addAll(adminChoiceBox, documentChoiceBox);
        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                VotingSystemApp.this.stop();
            }});
        HBox footerButtonsBox = new HBox(10);
        footerButtonsBox.getChildren().addAll(spacer, cancelButton);
        VBox.setMargin(footerButtonsBox, new Insets(20, 10, 0, 10));
        mainBox.getChildren().addAll(footerButtonsBox);
        mainBox.getStyleClass().add("modal-dialog");
        mainBox.setPrefWidth(550);
        primaryStage.setScene(new Scene(mainBox));
        primaryStage.getScene().getStylesheets().add(((Object)this).getClass().getResource(
                "/css/modal-dialog.css").toExternalForm());
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle(ContextVS.getMessage("mainDialogCaption"));
        // allow the UNDECORATED Stage to be dragged around.
        final Node root = primaryStage.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = primaryStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = primaryStage.getY() - mouseEvent.getScreenY();
            }
        });
        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                primaryStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                primaryStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
        primaryStage.show();
    }

    private void setVicketServerAvailable(final boolean available, Stage primaryStage) {
        PlatformImpl.runLater(new Runnable(){
            @Override public void run() {
                if(available) {
                    mainBox.getChildren().add((mainBox.getChildren().size() - 1), vicketOptionsBox);
                } else {
                    if(mainBox.getChildren().contains(vicketOptionsBox)) {
                        mainBox.getChildren().remove(vicketOptionsBox);
                    }
                }
                primaryStage.sizeToScene();
            }
        });
    }


    private void setVotingSystemAvailable(final boolean available, Stage primaryStage) {
        PlatformImpl.runLater(new Runnable(){
            @Override public void run() {
                if(available) {
                    mainBox.getChildren().add(1, votingSystemOptionsBox);
                } else {
                    if(mainBox.getChildren().contains(votingSystemOptionsBox)) {
                        mainBox.getChildren().remove(votingSystemOptionsBox);
                    }
                }
                primaryStage.sizeToScene();
            }
        });
    }

    private void openVotingSystemURL(final String URL, final String caption) {
        log.debug("openVotingSystemURL: " + URL);
        if(ContextVS.getInstance().getAccessControl() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"));
            return;
        }
        Platform.runLater(new Runnable() {
            @Override public void run() { BrowserVS.getInstance().newTab(URL, caption, null); }});
    }

    private void openVicketURL(final String URL, final String caption) {
        log.debug("openVicketURL: " + URL);
        if(ContextVS.getInstance().getVicketServer() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"));
            return;
        }
        Platform.runLater(new Runnable() {
            @Override public void run() { BrowserVS.getInstance().newTab(URL, caption, null); }});
    }

    private void openSettings() {
        log.debug("openSettings");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                SettingsDialog settingsDialog = new SettingsDialog();
                settingsDialog.show();
            }
        });
    }

    public void showMessage(final String message) {
        PlatformImpl.runLater(new Runnable() { @Override public void run() {
            new MessageDialog().showHtmlMessage(message);}});
    }

    class Delta { double x, y; }

    @Override public void processDecompressedFile(ResponseVS response) {
        log.debug("processDecompressedFile - statusCode:" + response.getStatusCode());
        if(ResponseVS.SC_OK == response.getStatusCode()) {
            DocumentVSBrowserStackPane documentBrowser = new DocumentVSBrowserStackPane();
            documentBrowser.setVisible((String) response.getData());
        }
    }

    public static void main(String[] args) {
        ContextVS.initSignatureClient("log4jClientTool.properties", "clientToolMessages.properties", locale);
        if(args.length > 0) {
            ContextVS.getInstance().initDirs(args[0]);
        }
        launch(args);
    }

}
package org.votingsystem.client;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.dialog.SettingsDialog;
import org.votingsystem.client.pane.DecompressBackupPane;
import org.votingsystem.client.pane.DocumentVSBrowserStackPane;
import org.votingsystem.client.pane.SignDocumentFormPane;
import org.votingsystem.client.service.WebSocketService;
import org.votingsystem.client.util.Utils;
import org.votingsystem.client.util.WebSocketListener;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
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
import java.util.concurrent.atomic.AtomicBoolean;
import org.controlsfx.glyphfont.FontAwesome;
/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VotingSystemApp extends Application implements DecompressBackupPane.Listener, WebSocketListener {

    private static Logger log = Logger.getLogger(VotingSystemApp.class);

    private BrowserVS browserVS;
    private VBox mainBox;
    private VBox votingSystemOptionsBox;
    private VBox vicketOptionsBox;
    private SettingsDialog settingsDialog;
    private GridPane headerButtonsPane;
    private Button connectButton;
    private Text messageText;
    private Stage primaryStage;
    private AtomicBoolean wsConnected = new AtomicBoolean(false);
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
        this.primaryStage = primaryStage;
        browserVS = new BrowserVS();
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
                        setVotingSystemAvailable(true);
                        ContextVS.getInstance().setDefaultServer((ActorVS) responseVS.getData());
                    }
                }
                catch(Exception ex) {log.error(ex.getMessage(), ex);}
                try {
                    responseVS = Utils.checkServer(vicketsServerURL);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        setVicketServerAvailable(true);
                        ContextVS.getInstance().setDefaultServer((ActorVS) responseVS.getData());
                    }
                }
                catch(Exception ex) {log.error(ex.getMessage(), ex);}
            }
        }).start();

        mainBox = new VBox();

        connectButton = new Button(ContextVS.getMessage("connectLbl"));
        connectButton.setGraphic(Utils.getImage(FontAwesome.Glyph.SQUARE, Utils.COLOR_BUTTON_ERROR));
        connectButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                connectButton.setDisable(true);
                if(!wsConnected.get()) {
                    connectButton.setText(ContextVS.getMessage("connectionMsg") + "...");
                }
                toggleConnection();
            }});

        messageText = new Text();
        messageText.setWrappingWidth(320);
        messageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #6c0404;");
        VBox.setMargin(messageText, new Insets(0, 0, 0, 0));
        messageText.setTextAlignment(TextAlignment.CENTER);

        headerButtonsPane = new GridPane();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerButtonsPane.getChildren().addAll(connectButton, messageText);
        headerButtonsPane.setColumnSpan(headerButtonsPane, 2);

        VBox.setMargin(headerButtonsPane, new Insets(0, 0, 10, 0));

        votingSystemOptionsBox = new VBox(10);

        Button voteButton = new Button(ContextVS.getMessage("voteButtonLbl"));

        voteButton.setGraphic(Utils.getImage(FontAwesome.Glyph.ENVELOPE));
        voteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openVotingPage();
            }});
        voteButton.setPrefWidth(500);

        Button selectRepresentativeButton = new Button(ContextVS.getMessage("selectRepresentativeButtonLbl"));
        selectRepresentativeButton.setGraphic(Utils.getImage(FontAwesome.Glyph.HAND_ALT_RIGHT));
        selectRepresentativeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openSelectRepresentativePage();
            }});
        selectRepresentativeButton.setPrefWidth(500);

        votingSystemOptionsBox.getChildren().addAll(voteButton, selectRepresentativeButton);


        Button openFileButton = new Button(ContextVS.getMessage("openFileButtonLbl"));
        openFileButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CERTIFICATE));
        openFileButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                DocumentVSBrowserStackPane.showDialog(null, null);

            }});
        openFileButton.setPrefWidth(500);

        Button signDocumentButton = new Button(ContextVS.getMessage("signDocumentButtonLbl"));
        signDocumentButton.setGraphic(Utils.getImage(FontAwesome.Glyph.PENCIL));
        signDocumentButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                SignDocumentFormPane.showDialog();

            }});
        signDocumentButton.setPrefWidth(500);

        vicketOptionsBox = new VBox(10);
        Button vicketUsersProceduresButton = new Button(ContextVS.getMessage("vicketUsersLbl"));
        vicketUsersProceduresButton.setGraphic(Utils.getImage(FontAwesome.Glyph.MONEY));
        vicketUsersProceduresButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openVicketUserProcedures();
            }});
        vicketUsersProceduresButton.setPrefWidth(500);

        Button walletButton = new Button(ContextVS.getMessage("settingsLbl"));
        walletButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CREDIT_CARD));
        walletButton.setPrefWidth(500);
        walletButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openSettings();
            }});

        vicketOptionsBox.getChildren().addAll(vicketUsersProceduresButton, walletButton);
        vicketOptionsBox.setStyle("-fx-alignment: center;");
        HBox footerButtonsBox = new HBox(10);
        ChoiceBox choiceBox = new ChoiceBox();
        choiceBox.setPrefWidth(200);
        final String[] adminOptions = new String[]{ContextVS.getMessage("adminLbl"),
                ContextVS.getMessage("settingsLbl"),
                ContextVS.getMessage("vicketAdminLbl"),
                ContextVS.getMessage("votingSystemProceduresLbl")};
        choiceBox.getItems().addAll(adminOptions);
        choiceBox.getSelectionModel().selectFirst();
        choiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue ov, Number value, Number new_value) {
                    log.debug("value: " + value + " -new_value: " + new_value + " - option: " + adminOptions[new_value.intValue()]);
                    String selectedOption = adminOptions[new_value.intValue()];
                    if(ContextVS.getMessage("settingsLbl").equals(selectedOption)) {
                        openSettings();
                    } else if(ContextVS.getMessage("vicketAdminLbl").equals(selectedOption)) {
                        openVicketAdminProcedures();
                    } else if(ContextVS.getMessage("votingSystemProceduresLbl").equals(selectedOption)) {
                        openVotingSystemProceduresPage();
                    }
                choiceBox.getSelectionModel().select(0);

                }
            });

        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_BUTTON_ERROR));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                VotingSystemApp.this.stop();
            }});

        footerButtonsBox.getChildren().addAll(choiceBox, spacer, cancelButton);
        VBox.setMargin(footerButtonsBox, new Insets(20, 10, 0, 10));

        mainBox.getChildren().addAll(openFileButton, signDocumentButton, footerButtonsBox);
        mainBox.getStyleClass().add("modal-dialog");
        mainBox.setPrefWidth(550);

        primaryStage.setScene(new Scene(mainBox));
        primaryStage.getScene().getStylesheets().add(((Object)this).getClass().getResource(
                "/resources/css/modal-dialog.css").toExternalForm());
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

    private void toggleConnection() {
        if(WebSocketService.getInstance() == null) {
            WebSocketService webSocketService = new WebSocketService(ContextVS.getInstance().
                    getVotingSystemSSLCerts(), ContextVS.getInstance().getVicketServer());
            webSocketService.addListener(VotingSystemApp.this);
        }
        WebSocketService.getInstance().setConnectionEnabled(!wsConnected.get());
    }

    private void setVicketServerAvailable(final boolean available) {
        PlatformImpl.runLater(new Runnable(){
            @Override public void run() {
                if(available) {
                    mainBox.getChildren().add((mainBox.getChildren().size() - 1), vicketOptionsBox);
                    mainBox.getChildren().add(0, headerButtonsPane);

                } else {
                    if(mainBox.getChildren().contains(vicketOptionsBox)) {
                        mainBox.getChildren().remove(vicketOptionsBox);
                    }
                    if(mainBox.getChildren().contains(headerButtonsPane)) {
                        mainBox.getChildren().remove(headerButtonsPane);
                    }
                }
                primaryStage.sizeToScene();
            }
        });
    }


    private void setVotingSystemAvailable(final boolean available) {
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

    private void openVotingSystemProceduresPage() {
        log.debug("openVotingSystemProceduresPage");
        if(ContextVS.getInstance().getAccessControl() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"));
            return;
        }
        Platform.runLater(new Runnable() {
            @Override public void run() {
                new BrowserVS().loadURL(ContextVS.getInstance().getAccessControl().getProceduresPageURL(),
                        ContextVS.getMessage("votingSystemProceduresLbl"));
            }
        });
    }

    private void openVotingPage() {
        log.debug("openVotingPage");
        if(ContextVS.getInstance().getAccessControl() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"));
            return;
        }
        Platform.runLater(new Runnable() {
            @Override public void run() {
                browserVS.loadURL(ContextVS.getInstance().getAccessControl().getVotingPageURL(),
                        ContextVS.getMessage("voteButtonLbl"));
            }
        });
    }

    private void openSelectRepresentativePage() {
        log.debug("openSelectRepresentativePage");
        if(ContextVS.getInstance().getAccessControl() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"));
            return;
        }
        Platform.runLater(new Runnable() {
            @Override public void run() {
                browserVS.loadURL(ContextVS.getInstance().getAccessControl().getSelectRepresentativePageURL(),
                        ContextVS.getMessage("selectRepresentativeButtonLbl"));
            }
        });
    }

    private void openVicketUserProcedures() {
        log.debug("openVicketUserProcedures");
        if(ContextVS.getInstance().getVicketServer() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"));
            return;
        }
        Platform.runLater(new Runnable() {
            @Override public void run() {
                browserVS.loadURL(ContextVS.getInstance().getVicketServer().getUserProceduresPageURL(),
                        ContextVS.getMessage("vicketUsersLbl"));
            }
        });
    }

    private void openVicketAdminProcedures() {
        log.debug("openVicketAdminProcedures");

        if(ContextVS.getInstance().getVicketServer() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"));
            return;
        }

        Platform.runLater(new Runnable() {
            @Override public void run() {
                new BrowserVS().loadURL(ContextVS.getInstance().getVicketServer().getAdminProceduresPageURL(),
                        ContextVS.getMessage("vicketAdminLbl"));

            }
        });
    }

    private void openSettings() {
        log.debug("openSettings");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                if (settingsDialog == null) settingsDialog = new SettingsDialog();
                settingsDialog.show();
            }
        });
    }

    MessageDialog messageDialog;
    public void showMessage(final String message) {
        PlatformImpl.runLater(new Runnable() {
            @Override
            public void run() {
                if (messageDialog == null) messageDialog = new MessageDialog();
                messageDialog.showMessage(message);
            }
        });
    }

    @Override public void consumeWebSocketMessage(JSONObject messageJSON) {
        TypeVS operation = TypeVS.valueOf(messageJSON.getString("operation"));
        switch(operation) {
            case INIT_VALIDATED_SESSION:
                wsConnected.set(true);
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                        messageText.setText(WebSocketService.getInstance().getSessionUser().getName());
                        connectButton.setGraphic(Utils.getImage(FontAwesome.Glyph.FLASH));
                        connectButton.setText(ContextVS.getMessage("disConnectLbl"));
                        connectButton.setDisable(false);
                    }
                });
                break;
        }
    }

    @Override public void setConnectionStatus(ConnectionStatus status) {
        log.debug("setConnectionStatus - status: " + status.toString());
        switch (status) {
            case CLOSED:
                wsConnected.set(false);
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                        messageText.setText("");
                        connectButton.setGraphic(Utils.getImage(FontAwesome.Glyph.SQUARE, Utils.COLOR_BUTTON_ERROR));
                        connectButton.setText(ContextVS.getMessage("connectLbl"));
                        connectButton.setDisable(false);
                    }
                });
                break;
            case OPEN:
                break;
        }

    }

    /*private void clickShow(ActionEvent event) {
        Stage stage = new Stage();
        Parent root = FXMLLoader.load(YourClassController.class.getResource("YourClass.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("My modal window");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(((Node)event.getSource()).getScene().getWindow() );
        stage.show();
    }*/

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

        launch(args);
    }

}
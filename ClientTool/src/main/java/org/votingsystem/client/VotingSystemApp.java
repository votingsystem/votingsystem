package org.votingsystem.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.log4j.Logger;
import org.votingsystem.client.dialog.DecompressFileDialog;
import org.votingsystem.client.dialog.FXSettingsDialog;
import org.votingsystem.client.dialog.SignedDocumentsBrowser;
import org.votingsystem.client.util.BrowserVS;
import org.votingsystem.client.util.FXUtils;
import org.votingsystem.model.AppHostVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class VotingSystemApp extends Application implements DecompressFileDialog.Listener, AppHostVS {

    private static Logger logger = Logger.getLogger(VotingSystemApp.class);

    private BrowserVS browserVS;
    private FXSettingsDialog settingsDialog;
    public static String locale = "es";

    @Override public void stop() {
        logger.debug("stop");
        System.exit(0);
    }

    @Override public void start(final Stage primaryStage) throws Exception {
        ContextVS.initSignatureClient(this, "log4jClientTool.properties",
                "clientToolMessages.properties", locale);

        VBox verticalBox = new VBox(10);
        Button voteButton = new Button(ContextVS.getMessage("voteButtonLbl"));
        voteButton.setGraphic(new ImageView(FXUtils.getImage(this, "fa-envelope")));
        voteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openVotingPage();
            }});
        voteButton.setPrefWidth(300);

        Button selectRepresentativeButton = new Button(ContextVS.getMessage("selectRepresentativeButtonLbl"));
        selectRepresentativeButton.setGraphic(new ImageView(FXUtils.getImage(this, "fa-hand-o-right")));
        selectRepresentativeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openSelectRepresentativePage();
            }});
        selectRepresentativeButton.setPrefWidth(300);

        Button openSignedFileButton = new Button(ContextVS.getMessage("openSignedFileButtonLbl"));
        openSignedFileButton.setGraphic(new ImageView(FXUtils.getImage(this, "application-certificate")));
        openSignedFileButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openSignedFile();
            }});
        openSignedFileButton.setPrefWidth(300);

        final Button openBackupButton = new Button(ContextVS.getMessage("openBackupButtonLbl"));
        openBackupButton.setGraphic(new ImageView(FXUtils.getImage(this, "fa-archive")));
        openBackupButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                openBackup();
            }
        });
        openBackupButton.setPrefWidth(300);

        Button vicketAdminButton = new Button(ContextVS.getMessage("vicketAdminLbl"));
        vicketAdminButton.setGraphic(new ImageView(FXUtils.getImage(this, "fa-money")));
        vicketAdminButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openGroupAdmin();
            }});
        vicketAdminButton.setPrefWidth(300);

        Button settingsButton = new Button(ContextVS.getMessage("settingsLbl"));
        settingsButton.setGraphic(new ImageView(FXUtils.getImage(this, "fa-wrench")));
        settingsButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openSettings();
            }});

        HBox footerButtonsBox = new HBox(10);

        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setGraphic(new ImageView(FXUtils.getImage(this, "cancel_16")));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                primaryStage.hide();
            }});

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footerButtonsBox.getChildren().addAll(settingsButton, spacer, cancelButton);
        VBox.setMargin(footerButtonsBox, new Insets(20, 0, 0, 0));

        verticalBox.getChildren().addAll(voteButton, selectRepresentativeButton, openSignedFileButton, openBackupButton,  vicketAdminButton,
                footerButtonsBox);
        verticalBox.getStyleClass().add("modal-dialog");

        primaryStage.setScene(new Scene(verticalBox, Color.TRANSPARENT));
        primaryStage.getScene().getStylesheets().add(((Object)this).getClass().getResource(
                "/resources/css/modal-dialog.css").toExternalForm());
        primaryStage.initStyle(StageStyle.UNDECORATED);

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

    private void openSignedFile() {
        logger.debug("openSignedFile");
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                    SignedDocumentsBrowser documentBrowser =  new SignedDocumentsBrowser(new JFrame(), false);
                    documentBrowser.openSignedFile();
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }

    private void openBackup() {
        logger.debug("openBackup");
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                    final JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter(new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            return f.getName().toLowerCase().endsWith(".zip") || f.isDirectory();
                        }

                        public String getDescription() {
                            return "ZIP Files";
                        }
                    });
                    int returnVal = chooser.showOpenDialog(new JFrame());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File result = chooser.getSelectedFile();
                        String outputFolder = ContextVS.APPTEMPDIR + File.separator + UUID.randomUUID();
                        DecompressFileDialog dialog = new DecompressFileDialog(new JFrame(), true);
                        dialog.unZipBackup(VotingSystemApp.this, result.getAbsolutePath(), outputFolder);
                    }
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });

    }

    private void openGroupAdmin() {
        logger.debug("openGroupAdmin");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                if(browserVS == null) browserVS = new BrowserVS();
                browserVS.loadURL("http://vickets/Vickets/app/admin?menu=admin");
            }
        });
    }

    private void openVotingPage() {
        logger.debug("openVotingPage");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                if(browserVS == null) browserVS = new BrowserVS();
                browserVS.loadURL("http://sistemavotacion.org/AccessControl/");
            }
        });
    }

    private void openSelectRepresentativePage() {
        logger.debug("openSelectRepresentativePage");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                if(browserVS == null) browserVS = new BrowserVS();
                browserVS.loadURL("http://sistemavotacion.org/AccessControl/representative/main?menu=user");
            }
        });
    }


    private void openSettings() {
        logger.debug("openSettings");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                if (settingsDialog == null) settingsDialog = new FXSettingsDialog();
                settingsDialog.show();
            }
        });
    }

    @Override public void sendMessageToHost(OperationVS operation) {
        logger.debug("### sendMessageToHost");
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
        logger.debug("processDecompressedFile - statusCode:" + response.getStatusCode());
        if(ResponseVS.SC_OK == response.getStatusCode()) {
            SignedDocumentsBrowser documentBrowser = new SignedDocumentsBrowser(new JFrame(), false);
            documentBrowser.setVisible((String) response.getData());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
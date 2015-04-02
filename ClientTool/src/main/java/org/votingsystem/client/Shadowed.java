package org.votingsystem.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.votingsystem.client.dialog.PublishElectionDialog;
import org.votingsystem.client.pane.DecoratedPane;
import org.votingsystem.client.pane.WalletPane;
import org.votingsystem.client.util.ResizeHelper;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.Wallet;

import java.util.Set;
import java.util.logging.*;

public class Shadowed extends Application {

    private static final Logger log = Logger.getLogger(Shadowed.class.getName());

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) throws Exception {
        PublishElectionDialog.show(null, stage);
    }

    private void testLog() {
        ContextVS.initSignatureClient("clientToolMessages.properties", "es");
        log.setLevel(Level.FINEST);
        Handler conHdlr = new ConsoleHandler();
        conHdlr.setFormatter(new Formatter() {
            public String format(LogRecord record) {
                return record.getLevel() + " - " + record.getSourceClassName() + " - " + record.getSourceMethodName() +
                        " - " + record.getMessage() + "\n";
            }
        });
        conHdlr.setLevel(Level.FINEST);
        log.setUseParentHandlers(false);
        log.addHandler(conHdlr);
        log.info("info");
        log.fine("fine");
    }

    private void testWallet(Stage stage) throws Exception {
        Set<Currency> wallet = Wallet.getWallet("ABCDE");
        WalletPane walletPane = new WalletPane(wallet);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(new DecoratedPane(ContextVS.getMessage("walletLbl"), new MenuButton(), walletPane, stage)));
        //stage.setScene(new Scene(walletPane));
        stage.getScene().setFill(null);
        //Utils.addMouseDragSupport(stage);
        ResizeHelper.addResizeListener(stage);
        stage.show();
    }
}
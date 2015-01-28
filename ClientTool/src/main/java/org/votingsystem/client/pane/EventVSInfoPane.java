package org.votingsystem.client.pane;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.model.MetaInf;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import java.io.File;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSInfoPane extends VBox {

    private static Logger log = Logger.getLogger(EventVSInfoPane.class);


    private MetaInf metaInf = null;


    public EventVSInfoPane(final MetaInf metaInf) throws Exception {
        this.metaInf = metaInf;
        Label subjectLabel = new Label(ContextVS.getMessage("subjectLbl") + ": ");
        subjectLabel.setStyle("-fx-font-weight: bold;");
        Label subjectValueLabel = new Label(metaInf.getSubject());
        HBox subjectHBox = new HBox();
        subjectHBox.getChildren().addAll(subjectLabel, subjectValueLabel);
        VBox.setMargin(subjectHBox, new Insets(10, 15, 10, 15));

        HBox dateBeginHBox = new HBox();
        Label dateBeginLabel = new Label(ContextVS.getMessage("dateBeginLbl") + ": ");
        dateBeginLabel.setStyle("-fx-font-weight: bold;");
        Label dateBeginValueLabel = new Label(DateUtils.getDateStr(metaInf.getDateBegin(), "yyyy/MM/dd"));
        dateBeginHBox.getChildren().addAll(dateBeginLabel, dateBeginValueLabel);
        VBox.setMargin(dateBeginHBox, new Insets(10, 15, 10, 15));

        WebView webView = new WebView();
        webView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        webView.getEngine().loadContent(metaInf.getFormattedInfo());
        Utils.browserVSLinkListener(webView);

        if(metaInf.getType() == TypeVS.VOTING_EVENT) {
            Button representativesButton = new Button(ContextVS.getMessage("representativesDetailsLbl"));
            representativesButton.setOnAction(actionEvent ->  {
                HTMLMessageDialog messageDialog = new HTMLMessageDialog();
                messageDialog.showMessage(metaInf.getRepresentativesHTML(),
                        ContextVS.getMessage("representativesDetailsLbl"));
            });
            representativesButton.setGraphic(Utils.getImage(FontAwesome.Glyph.GROUP));
            dateBeginHBox.getChildren().addAll(Utils.getSpacer(), representativesButton);
        }
        this.getChildren().addAll(subjectHBox, dateBeginHBox, webView);
        HBox.setHgrow(this, Priority.ALWAYS);
        VBox.setVgrow(webView, Priority.ALWAYS);
    }

}

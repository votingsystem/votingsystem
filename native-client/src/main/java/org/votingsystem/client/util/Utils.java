package org.votingsystem.client.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import org.votingsystem.client.MainApp;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;

import java.io.InputStream;
import java.util.logging.Logger;



/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    private static Logger log = Logger.getLogger(Utils.class.getName());

    public static final String APPLICATION_ICON = "mail-mark-unread.png";

    public static final String COLOR_BUTTON_OK = "#888";
    public static final String COLOR_RESULT_OK = "#388746";
    public static final String COLOR_RED = "#ba0011";
    public static final String COLOR_RED_DARK = "#6c0404";
    public static final String COLOR_YELLOW_ALERT = "#fa1";


    public static final String EVENT_TYPE_CLICK = "click";
    public static final String EVENT_TYPE_MOUSEOVER = "mouseover";
    public static final String EVENT_TYPE_MOUSEOUT = "mouseclick";

    private static GlyphFont fontAwesome;
    static {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("fontawesome-webfont.ttf");
        fontAwesome = new FontAwesome(is);
        GlyphFontRegistry.register(fontAwesome);
    }

    public static Glyph getIcon(FontAwesome.Glyph glyph) {
        return fontAwesome.create(glyph);
    }

    public static Glyph getIcon(FontAwesome.Glyph glyph, String color, double size) {
        return fontAwesome.create(glyph).color(Color.web(color)).size(size);
    }

    public static Glyph getIcon(FontAwesome.Glyph glyph, double size) {
        return fontAwesome.create(glyph).size(size);
    }

    public static Glyph getIcon(FontAwesome.Glyph glyph, String colorStr) {
        return Glyph.create( "FontAwesome|" + glyph.name()).color(Color.web(colorStr));
    }

    public static Image getIconFromResources(String imageFilename) {
        return new Image(getResource("/images/" + imageFilename));
    }

    public static String getResource(String path) {
        return MainApp.class.getResource(path).toExternalForm();
    }

    public static Region getSpacer(){
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    // allow the dialog to be dragged around.
    public static void addMouseDragSupport(Stage stage) {
        final Node root = stage.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(mouseEvent -> {  // record a delta distance for the drag and drop operation.
            dragDelta.x = stage.getX() - mouseEvent.getScreenX();
            dragDelta.y = stage.getY() - mouseEvent.getScreenY();
        });
        root.setOnMouseDragged(mouseEvent -> {
            stage.setX(mouseEvent.getScreenX() + dragDelta.x);
            stage.setY(mouseEvent.getScreenY() + dragDelta.y);
        });
    }

    public static void addTextLimiter(final TextInputControl tf, final int maxLength) {
        tf.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(final ObservableValue<? extends String> ov, final String oldValue, final String newValue) {
                if (tf.getText().length() > maxLength) {
                    String s = tf.getText().substring(0, maxLength);
                    tf.setText(s);
                }
            }
        });
    }

    public static void browserLinkListener(WebView webView) {
        webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue ov, Worker.State oldState, Worker.State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                    org.w3c.dom.events.EventListener listener = evt -> {
                        String domEventType = evt.getType();
                        if (domEventType.equals(EVENT_TYPE_CLICK)) {
                            webView.getEngine().getLoadWorker().cancel();
                            String href = ((Element) evt.getTarget()).getAttribute("href");
                            evt.preventDefault();
                            log.info("href: " + href);
                        }
                    };
                    Document doc = webView.getEngine().getDocument();
                    NodeList nodeList = doc.getElementsByTagName("a");
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        ((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
                        //((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_MOUSEOVER, listener, false);
                        //((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_MOUSEOVER, listener, false);
                    }
                }
            }
        });
    }

    static class Delta { double x, y; }

}

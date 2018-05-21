package org.votingsystem.testlib.util;

import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class XMLUtils {

    private static final Logger log = Logger.getLogger(XMLUtils.class.getName());

    public static byte[] serialize(Document doc) throws IOException {
        return serialize(doc, false);
    }

    public static String dummyString() {
        return "<dummy></dummy>";
    }

    public static String prepareRequestToSign(byte[] bytesToSign) throws IOException, XmlPullParserException {
        Document document = XMLUtils.parse(bytesToSign);
        Element elementWithCanonicalizedAttributes = canonicalizeAttributes(document.getRootElement());
        Document doc = new Document();
        doc.addChild(Node.ELEMENT, elementWithCanonicalizedAttributes);
        String result = new String(XMLUtils.serialize(doc), "UTF-8");
        return result.replaceAll("<\\?xml(.+?)\\?>", "").trim();
    }


    public static Element canonicalizeAttributes(Element mainElement) {
        Element elementCanonicalized = new Element();
        elementCanonicalized.setName(mainElement.getName());

        List<String> attributeNameList = new ArrayList<>();
        for(int i = 0 ; i < mainElement.getAttributeCount(); i++) {
            attributeNameList.add(mainElement.getAttributeName(i));
        }
        Collections.sort(attributeNameList);
        for(String attributeName : attributeNameList) {
            elementCanonicalized.setAttribute(null, attributeName, mainElement.getAttributeValue(null, attributeName));
        }
        for(int i = 0; i < mainElement.getChildCount(); i ++) {
            if(mainElement.getChild(i) instanceof Element) {
                Element iteratedElement = (Element) mainElement.getChild(i);
                try {
                    Element orderedChild = canonicalizeAttributes(iteratedElement);
                    elementCanonicalized.addChild(i, Node.ELEMENT, orderedChild);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            } else elementCanonicalized.addChild(i, Node.TEXT, mainElement.getChild(i));
        }
        return elementCanonicalized;
    }

    public static byte[] serialize(Document doc, boolean indent) throws IOException {
        KXmlSerializer serializer = new KXmlSerializer();
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", indent);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.setOutput(baos, "UTF-8");
        doc.write(serializer);
        serializer.flush();
        byte[] result = baos.toByteArray();
        baos.close();
        return result;
    }

    public static byte[] serialize(Element element, boolean indent) throws IOException {
        return serialize(element, null, null, indent);
    }

    public static byte[] serialize(Element element, String prefix, String namespace, boolean indent)
            throws IOException {
        KXmlSerializer serializer = new KXmlSerializer();
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", indent);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.setOutput(baos, "UTF-8");
        serializer.setPrefix(prefix, namespace);
        element.write(serializer);
        serializer.flush();
        byte[] result = baos.toByteArray();
        baos.close();
        return result;
    }

    public static Document parse(byte[] documentBytes) throws IOException, XmlPullParserException {
        KXmlParser parser = new KXmlParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new ByteArrayInputStream(documentBytes), "UTF-8");
        Document doc = new Document();
        doc.parse(parser);
        return doc;
    }

    public static Element getElement(Element element, String elementName) {
        try {
            return element.getElement(null, elementName);
        } catch (Exception ex) {
            log.severe("Element not found: " + elementName);
        }
        return null;
    }

    public static String getTextChild(Element element, String tagName) {
        String result = null;
        if (element != null) {
            try {
                if (element.getElement(null, tagName) != null &&
                        element.getElement(null, tagName).getChildCount() > 0) {
                    result = (String) element.getElement(null, tagName).getChild(0);
                }
            } catch (Exception ex) {
                log.severe("not found tagName: " + tagName);
            }
        }
        return result;
    }

    public static String getHTMLContent(Element element, String tagName) {
        String result = null;
        if (element != null) {
            try {
                if (element.getElement(null, tagName) != null &&
                        element.getElement(null, tagName).getChildCount() > 0) {
                    result = "";
                    for(int i = 0; i < element.getElement(null, tagName).getChildCount(); i++) {
                        result = result + (String) element.getElement(null, tagName).getChild(i);
                    }
                }
            } catch (Exception ex) {
                log.severe("tagName: " + tagName);
            }
        }
        return result;
    }
}
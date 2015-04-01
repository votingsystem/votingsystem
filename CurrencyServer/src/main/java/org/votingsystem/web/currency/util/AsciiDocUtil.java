package org.votingsystem.web.currency.util;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.DocumentHeader;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AsciiDocUtil {

    private static Logger log = Logger.getLogger(AsciiDocUtil.class.getSimpleName());


    public static String getMetaInfVS(String asciiDocStr) {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        //StructuredDocument document = asciidoctor.readDocumentStructure(asciiDocStr, new HashMap<String, Object>());
        DocumentHeader header = asciidoctor.readDocumentHeader(asciiDocStr);
        String metaInf = (String) header.getAttributes().get("metaInfVS");
        return metaInf;
    }

    public static String getHTML(String asciiDocStr) {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        return asciidoctor.convert(asciiDocStr, new HashMap<String, Object>());
    }
}

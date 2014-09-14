package org.votingsystem.vicket.util;

import org.apache.log4j.Logger;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.DocumentHeader;

import java.util.HashMap;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AsciiDocUtil {
    private static Logger logger = Logger.getLogger(AsciiDocUtil.class);

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

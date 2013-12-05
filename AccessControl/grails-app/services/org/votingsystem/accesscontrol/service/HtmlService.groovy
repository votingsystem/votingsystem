package org.votingsystem.accesscontrol.service

import org.apache.xerces.parsers.DOMParser
import org.apache.xerces.xni.parser.XMLDocumentFilter
import org.cyberneko.html.HTMLConfiguration
import org.cyberneko.html.filters.ElementRemover

import javax.xml.transform.OutputKeys
import javax.xml.transform.Source
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class HtmlService {

	/*
	 * Clean HTML to render as PDF
	 * Parser settings -> http://nekohtml.sourceforge.net/settings.html
	 */
	String prepareHTMLToPDF(byte[] htmlBytes) {
		final InputStream inputStream = new ByteArrayInputStream(htmlBytes);
		List<XMLDocumentFilter> lista = new ArrayList<XMLDocumentFilter>();
	
		ElementRemover remover = new ElementRemover();
		remover.acceptElement("b", null);
        remover.acceptElement("strong", null);
        remover.acceptElement("em", null);
		remover.acceptElement("i", null);
		remover.acceptElement("u", null);
		remover.acceptElement("br", null);
		remover.acceptElement("ol", null);
        remover.acceptElement("ul", null);
		remover.acceptElement("li", null);
		remover.acceptElement("strike", null);
		remover.acceptElement("img", (String[])["src"]);
		remover.acceptElement("sub", (String[])["style"]);
		remover.acceptElement("span", (String[])["style"]);
		remover.acceptElement("font", (String[])["face", "color"]);
		remover.acceptElement("p", (String[])["style"]);
		remover.acceptElement("a", (String[])["href"]);
		remover.removeElement("script");
		remover.removeElement("INPUT");
		lista.add(remover)
		//lista.add(new Purifier())
		
		XMLDocumentFilter[] filters = lista.toArray() ;
		HTMLConfiguration config = new HTMLConfiguration();
		config.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
		DOMParser dp = new DOMParser(config);
		dp.setFeature("http://apache.org/xml/features/dom/include-ignorable-whitespace",false);
		dp.setFeature("http://cyberneko.org/html/features/balance-tags", true);
		dp.setProperty("http://cyberneko.org/html/properties/default-encoding", "UTF-8")
		
		config.setProperty("http://cyberneko.org/html/properties/filters", filters);
		dp.parse(new org.xml.sax.InputSource(inputStream));
		org.w3c.dom.Document doc = dp.getDocument();
		
		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer = transFactory.newTransformer();
		StringWriter buffer = new StringWriter();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		
		Source source = new DOMSource(doc);
		
		transformer.transform(source, new StreamResult(buffer));
		String result = buffer.toString();
		log.debug "result: ${result}"
		return result
	}
	
}


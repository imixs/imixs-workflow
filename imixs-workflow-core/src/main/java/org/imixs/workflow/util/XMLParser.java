package org.imixs.workflow.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XMLParser provides helper methods to parse xml strings
 * 
 * @author rsoika
 *
 */
public class XMLParser {

	private static Logger logger = Logger.getLogger(XMLParser.class.getName());

	/**
	 * This method parses a xml tag for attributes. The method returns a Map with
	 * all attributes found in the content string
	 * 
	 * e.g. <item data="abc" value="def" />
	 * 
	 * returns Map: {field=a, number=1}
	 * 
	 * @param content
	 * @return
	 */
	public static Map<String, String> findAttributes(String content) {
		Map<String, String> result = new HashMap<String, String>();
		 Pattern p=null;
		// short version of [A-Za-z0-9\-]
		//Pattern p = Pattern.compile("([\\w\\-]+)=\"*((?<=\")[^\"]+(?=\")|([^\\s]+))\"*");
		//  Pattern p = Pattern.compile("([\\w\\-]+)=\"*((?<=\")[^\"]+(?=\")|([^\\s]+))\"*");
		  
		  
		 // Pattern p = Pattern.compile("(\\S+)=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?");
		  
		   p = Pattern.compile("(\\S+)\\s*=\\s*[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))?[^\"']*)[\"']?");

		  
		  
		Matcher m = p.matcher(content);
		while (m.find()) {
			result.put(m.group(1), m.group(2));
		}
		return result;
	}
//[\"']   [\"']
	//   /^\s?([^=]+)\s?=\s?("([^"]+)"|\'([^\']+)\')\s?/
	
	/**
	 * This method parses a xml tag for a singel named attribute. The method returns
	 * the value of the attribute found in the content string
	 * 
	 * e.g. <item data="abc" />
	 * 
	 * returns "abc"
	 * 
	 * @param content
	 * @return
	 */
	public static String findAttribute(String content, String name) {
		Map<String, String> attriubtes = findAttributes(content);
		return attriubtes.get(name);
	}

	/**
	 * This method find specific tags inside a string and returns a list with all
	 * tags.
	 * 
	 * e.g. <date field="abc" />
	 * 
	 * <date field="abc">def</date>
	 * 
	 * @param content
	 * @return
	 */
	public static List<String> findTags(String content, String tag) {
		List<String> result = new ArrayList<String>();

		String regex = "<(?i)(" + tag + ")" + // matches the tag itself
				"([^<]+)" + // then anything in between the opening and closing
							// of the tag
				"(</" + tag + ">|/>)"; // and finally the end tag corresponding
										// to what we matched as the first group
										// (Exony_Credit_Card_ID, tag1 or tag2)

		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(content);
		while (m.find()) {
			result.add(m.group());
		}
		return result;
	}

	/**
	 * This method returns the tag values of a specific xml tag
	 * 
	 * e.g. <date field="abc">2016-12-31</date>
	 * 
	 * returns 2016-12-31
	 * 
	 * @param content
	 * @return
	 */
	public static List<String> findTagValues(String content, String tag) {
		List<String> result = new ArrayList<String>();
		List<String> tags = findTags(content, tag);
		// opening tag can contain optional attributes
		String regex = "(<" + tag + ".+?>|<" + tag + ">)(.+?)(</" + tag + ")";
		for (String singleTag : tags) {
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(singleTag);
			while (m.find()) {
				result.add(m.group(2));
			}
		}
		return result;
	}

	/**
	 * This method returns the tag value of a single tag. The method returns the
	 * first match! Use findTagValues to parse multiple tags in one string. e.g.
	 * <date field="abc">2016-12-31</date>
	 * 
	 * returns 2016-12-31
	 * 
	 * @param content
	 * @return
	 */
	public static String findTagValue(String content, String tag) {
		// opening tag can contain optional attributes
		List<String> tags = findTags(content, tag);
		if (tags.size() > 0) {
			// only first tag...
			content = tags.get(0);
		}
		String regex = "(<" + tag + ".+?>|<" + tag + ">)(.+?)(</" + tag + ")";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(content);
		if (m.find()) {
			return m.group(2);
		}
		// no result
		return "";
	}

	/**
	 * This method parses the xml content of a item element and returns a new
	 * ItemCollection containing all item values. Each tag is evaluated as the item
	 * name.
	 * 
	 * MultiValues are currently not supported.
	 * 
	 * Example:
	 * 
	 * <code>	  
				<modelversion>1.0.0</modelversion>
				<processid>1000</processid>
				<activityid>10</activityid>
				<items>namTeam</items>	  
	 * </code>
	 * 
	 * @param evalItemCollection
	 * @throws PluginException
	 */
	public static ItemCollection parseItemStructure(String xmlContent) throws PluginException {
		logger.finest("......parseItemStructure...");
		ItemCollection result = new ItemCollection();
		if (xmlContent.length() > 0) {
			// surround with a root element
			xmlContent = "<item>" + xmlContent.trim() + "</item>";
			try {

				// parse item list...
				DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = documentBuilder.parse(new InputSource(new StringReader(xmlContent)));
				Node node = doc.importNode(doc.getDocumentElement(), true);
				DocumentFragment docfrag = doc.createDocumentFragment();
				while (node.hasChildNodes()) {
					docfrag.appendChild(node.removeChild(node.getFirstChild()));
				}

				// append all items into the evalItemCollection...
				NodeList childs = docfrag.getChildNodes();
				int itemCount = childs.getLength();
				for (int i = 0; i < itemCount; i++) {
					Node childNode = childs.item(i);
					if (childNode instanceof Element && childNode.getFirstChild() != null) {
						String name = childNode.getNodeName();
						// String value =
						// childNode.getFirstChild().getNodeValue();
						String value = innerXml(childNode);

						result.replaceItemValue(name, value);
						logger.finest("......parsing item '" + name + "' value=" + value);
					}
				}

			} catch (ParserConfigurationException | TransformerFactoryConfigurationError | TransformerException
					| SAXException | IOException e) {
				throw new PluginException(XMLParser.class.getName(), "INVALID_FORMAT",
						"Parsing item content failed: " + e.getMessage());

			}
		}
		return result;
	}

	/**
	 * This method extracts the content of a XML node and prevents inner XML tags
	 * 
	 * @param node
	 * @return
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	private static String innerXml(Node node) throws TransformerFactoryConfigurationError, TransformerException {
		StringWriter writer = new StringWriter();
		String xml = null;
		Transformer transformer;
		transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(node), new StreamResult(writer));
		// now we remove the outer tag....
		xml = writer.toString();
		xml = xml.substring(xml.indexOf(">") + 1, xml.lastIndexOf("</"));
		return xml;
	}

}

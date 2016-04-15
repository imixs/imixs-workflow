package org.imixs.workflow.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XMLParser provides helper methods to parse xml strings
 * 
 * @author rsoika
 *
 */
public class XMLParser {

	/**
	 * This method parses a xml tag for attributes. The method returns a Map
	 * with all attributes found in the content string
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
		Pattern p = Pattern.compile("(\\w+)=\"*((?<=\")[^\"]+(?=\")|([^\\s]+))\"*");
		Matcher m = p.matcher(content);
		while (m.find()) {
			result.put(m.group(1), m.group(2));
		}
		return result;
	}

	/**
	 * This method parses a xml tag for a singel named attribute. The method
	 * returns the value of the attribute found in the content string
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
	 * This method find specific tags inside a string and returns a list with
	 * all tags.
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
	 * This method returns the tag values of a  specific xml tag
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
		List<String> tags = findTags(content,tag);
		// opening tag can contain optional attributes
		String regex="(<" + tag + ".+?>|<" + tag + ">)(.+?)(</"+tag+")";
		for (String singleTag:tags) {
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(singleTag);
			while (m.find()) {
				result.add(m.group(2));	
			}
		}
		return result;
	}
}

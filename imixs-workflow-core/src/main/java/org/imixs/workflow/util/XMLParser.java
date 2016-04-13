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
}

package org.imixs.workflow.util;

import java.util.HashMap;
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
	 * This method parses a xml tag for attributes. The method returns
	 * a Map with all attributes found in the content string
	 * 
	 * e.g. <item data="abc" value="def" />
	 * 
	 * returns Map: {field=a, number=1}
	 * 
	 * @param content
	 * @return
	 */
	public static Map<String,String> parseAttributes(String content) {
		Map<String,String> result=new HashMap<String,String>();
		Pattern p = Pattern.compile("(\\w+)=\"*((?<=\")[^\"]+(?=\")|([^\\s]+))\"*");
		Matcher m = p.matcher(content);
		while(m.find()){
			result.put(m.group(1), m.group(2));
		}
		return result;
	}
}

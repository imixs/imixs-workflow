package org.imixs.workflow.xml;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Wraps the Map<String,Integer> interface returned by the getIndexList method
 * of EntityService into a jax-rs root element
 * 
 * @author rsoika
 *
 */
@XmlRootElement
public class XMLIndexList implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private Map<String, Integer> map;

	public XMLIndexList() {

	}

	public XMLIndexList(Map<String, Integer> amap) {
		super();
		setMap(amap);
	}

	public void setMap(Map<String, Integer> map) {
		this.map = map;
	}

	public Map<String, Integer> getMap() {
		return map;
	}
}

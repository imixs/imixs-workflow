package org.imixs.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.exceptions.ModelException;
import org.xml.sax.SAXException;

/**
 * Static mokup model
 * 
 * 100.10 - save -> 100
 * <p>
 * 100.11 -foollow up -> 100
 * <p>
 * 100.20 - forward -> 200
 * <p>
 * 
 * 
 * entities is a mapobject storing the process and activity entities. the key is
 * a string (e.g. '100', '100.10)
 * 
 * @author rsoika
 *  
 */
public class MokModelManager implements ModelManager {

	protected Model model=null;

	/**
	 *  
	 * prepare a mok model
	 **/
	public MokModelManager() {
		model=new MokModel();
	}

	

	/** 
	 * Load a test model by filename
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws ParseException 
	 * @throws ModelException 
	 **/
	public MokModelManager(String sModelPath) throws ParseException, ParserConfigurationException, SAXException, IOException, ModelException {
		InputStream inputStream = getClass().getResourceAsStream(sModelPath);
		model = BPMNParser.parseModel(inputStream, "UTF-8");
	}
	
	
	@Override
	public Model getModel(String version) throws ModelException {
		
		if (!version.equals( model.getDefinition().getModelVersion())) {
			throw new ModelException (ModelException.INVALID_MODEL,
					"ModelVersion " + version + " undefined");
		}
		
		return model;
	}

	@Override
	public void addModel(Model amodel) {
		model=amodel;
		
	}

	@Override
	public void removeModel(String version) {
		model=null;
		
	}


	@Override
	public Model getModelByWorkitem(ItemCollection workitem) throws ModelException {
		return getModel(workitem.getModelVersion());
	}

}

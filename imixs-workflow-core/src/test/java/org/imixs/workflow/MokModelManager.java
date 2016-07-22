package org.imixs.workflow;

import org.imixs.workflow.exceptions.ModelException;

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

	Model model=null;

	/**
	 *  
	 * prepare a mok model
	 **/
	public MokModelManager() {
		model=new MokModel();
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

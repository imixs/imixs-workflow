/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.plugins;
import java.util.List;
import java.util.Vector;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Pluginmodul is a draft implementation of a statistic plugin 
 * @author Ralph Soika
 * @version 1.0  
 * @see    org.imixs.workflow.WorkflowManager 
 */

public class StatisticPlugin implements Plugin {
	int iCurrentProcessID,iNextProcessID;

	public void init(WorkflowContext actx) throws PluginException {
		// no operation
	}

	/**
	 * changes the namworkflowreadaccess and namworkflowwriteaccess attribues depending to the activityentity 
	 */
	@SuppressWarnings("unchecked")
	public int run(ItemCollection adocumentContext, ItemCollection adocumentActivity)
		throws PluginException {
		try {
			/* Das Plugin misst die Verweildauer eines Workitems in einem Prozesstatus
			 * Dabei wird exakt nur die Zeit zwischen Eintritt und Austritt gemessen
			 * Dazu wird numNextProcessID mit numProcessID der Activity verglichen
			 * 
			 * Wenn  numNextProcessID != numProcessID -> dann handelt es sich um
			 * einen Austritt aus der Prozessstufe, und es wird zum einen die aktuelle Verweildauer 
			 * in der Liste numDurationProcessnnnn gespeichert (anhand der L�nge der Liste kann man auch 
			 * erkennen wie oft bzw. wie durchschnittlich lange er drinn war) 
			 * und gleichzeitig der Eintritt in den neuen Prozessstatus durch anh�ngen von 0 in die
			 * neue numDurationProzessnnnn Liste
			 * 
			 * Felder:
			 * numProcessTime 
			 *    Liste mit zwei Eintr�gen f�r Start- und Endzeit
			 * 
			 */ 
			 
			double dNow=System.currentTimeMillis();

			//System.out.println("Statistic PLUGIN running...");
			
			// numProcessTime aktualisieren			 
			List processTime= adocumentContext.getItemValue("numProcessTime");
			// Startzeit pruefen
			if (processTime.size()==0) {
				System.out.println("es ist keine Prozessteim da - ich erzeuge neue");
				processTime.add(new Double(dNow));
				processTime.add(new Double(dNow));
			}
			 
			//double iProzessStart=((Double)processTime.elementAt(0)).doubleValue();
			double iProzessEnde=((Double)processTime.get(1)).doubleValue();

			iCurrentProcessID=adocumentActivity.getItemValueInteger("numProcessID");
			iNextProcessID=adocumentActivity.getItemValueInteger("numNextProcessID");
			
			// current Prozess Dauer
			List currentDuration= adocumentContext.getItemValue("numDurationProcess"+iCurrentProcessID);
			List nextDuration= adocumentContext.getItemValue("numDurationProcess"+iNextProcessID);
			if (currentDuration.size()==0) 			
				currentDuration.add(new Double(0));
			// Prozessaus- oder eintritt? Prozessdauer ermitteln
			if (iCurrentProcessID!=iNextProcessID) {
				// current Prozess Dauer
				double i=((Double)currentDuration.get(0)).doubleValue();
				double duration=dNow-iProzessEnde;

				currentDuration.set(currentDuration.size()-1,new Double(i+duration));

				// next Prozess Dauer
				nextDuration.add(new Double(0));
			}
			 
			 
			// Endzeit aktualisieren
			processTime.set(1,new Double(dNow));
			adocumentContext.replaceItemValue("numDurationProcess"+iCurrentProcessID,currentDuration);
			if (nextDuration.size()>0)
				adocumentContext.replaceItemValue("numDurationProcess"+iNextProcessID,nextDuration);
			adocumentContext.replaceItemValue("numProcessTime",processTime);
			

		} catch (Exception e) {
			System.out.println("[StatisticPlugin] Error run()" + e.toString());
			return Plugin.PLUGIN_ERROR;
		}

		return Plugin.PLUGIN_OK;
	}

	public void close(int status) throws PluginException {
		
	}


}

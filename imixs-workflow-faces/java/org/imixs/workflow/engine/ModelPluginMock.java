/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.engine;

import java.util.List;
import java.util.Vector;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Model;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.exceptions.ModelException;

/**
 * This class mocks a BPMNModel so that a custom list of plugins can be
 * registered independent form the plugins listed in the parsed bpmn model file.
 * This is useful for JUnit tests, where only some specific Plugins should run
 * the test.
 * 
 * 
 * <pre>
 * {@code
 *   ...
 *   MockitoAnnotations.initMocks(this);
 *   super.setModelPath(MODEL_PATH);
 *   super.setup();
 *   this.modelService.addModel(new ModelPluginMock(model, 
 *                  "org.imixs.marty.plugins.AppoverPlugin",
 *                  "org.imixs.workflow.plugins.RulePlugin"));
 *   ....
 * }
 * </pre>
 * 
 */
public class ModelPluginMock extends BPMNModel {
	protected BPMNModel internalModel = null;
	protected ItemCollection internalDefinition = null;
	protected Vector<String> plugins = null;

	/**
	 * this constructor changes the registered plugins
	 * 
	 */
	public ModelPluginMock(Model aModel, String... pluginList) {
		super();
		internalModel = (BPMNModel) aModel;
		// define custom plugin set....
		plugins = new Vector<String>();
		for (String sPlugin : pluginList) {
			plugins.add(sPlugin);
		}
		internalDefinition = internalModel.getDefinition();
		internalDefinition.replaceItemValue("txtplugins", plugins);
	}

	@Override
	public byte[] getRawData() {
		return internalModel.getRawData();
	}

	@Override
	public String getVersion() {
		return internalModel.getVersion();
	}

	@Override
	public ItemCollection getDefinition() {
		return internalDefinition;
	}

	@Override
	public ItemCollection getTask(int processid) throws ModelException {
		return internalModel.getTask(processid);
	}

	@Override
	public ItemCollection getEvent(int processid, int activityid) throws ModelException {
		return internalModel.getEvent(processid, activityid);
	}

	@Override
	public List<String> getGroups() {
		return internalModel.getGroups();
	}

	@Override
	public List<ItemCollection> findAllTasks() {
		return internalModel.findAllTasks();
	}

	@Override
	public List<ItemCollection> findAllEventsByTask(int processid) {
		return super.findAllEventsByTask(processid);
	}

	@Override
	public List<ItemCollection> findTasksByGroup(String group) {
		return internalModel.findTasksByGroup(group);
	}

}
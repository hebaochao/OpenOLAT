/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/
package org.olat.core.gui.components.form.flexible.impl.elements.table;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentCollection;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.impl.FormBaseComponentImpl;
import org.olat.core.gui.translator.Translator;

/**
 * @author Christian Guretzki
 */
class FlexiTableComponent extends FormBaseComponentImpl implements ComponentCollection {

	private ComponentRenderer RENDERER = new FlexiTableRenderer();
	private FlexiTableElementImpl element;
	

	public FlexiTableComponent(FlexiTableElementImpl element) {
		super(element.getName());
		this.element = element;
	}
	
	public FlexiTableComponent(FlexiTableElementImpl element, Translator translator) {
		super(element.getName(), translator);
		this.element = element;
	}
	
	FlexiTableElementImpl getFlexiTableElement() {
		return element;
	}

	@Override
	public Component getComponent(String name) {
		FormItem item = element.getFormComponent(name);
		if(item != null) {
			return item.getComponent();
		}
		return null;
	}

	@Override
	public Iterable<Component> getComponents() {
		List<Component> cmp = new ArrayList<Component>();
		for(FormItem item:element.getFormItems()) {
			cmp.add(item.getComponent());
		}
		return cmp;
	}

	/**
	 * @see org.olat.core.gui.components.Component#getHTMLRendererSingleton()
	 */
	@Override
	public ComponentRenderer getHTMLRendererSingleton() {
		return RENDERER;
	}

}

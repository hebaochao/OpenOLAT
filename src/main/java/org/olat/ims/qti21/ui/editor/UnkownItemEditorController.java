/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.ims.qti21.ui.editor;

import java.util.List;

import javax.xml.transform.stream.StreamResult;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.util.Util;
import org.olat.ims.qti21.QTI21Service;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.ed.ph.jqtiplus.node.content.basic.Block;
import uk.ac.ed.ph.jqtiplus.node.item.AssessmentItem;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.Interaction;
import uk.ac.ed.ph.jqtiplus.serialization.QtiSerializer;

/**
 * 
 * Initial date: 26.05.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class UnkownItemEditorController extends FormBasicController {
	
	private final AssessmentItem item;
	private final QtiSerializer qtiSerializer;
	
	@Autowired
	private QTI21Service qtiService;

	public UnkownItemEditorController(UserRequest ureq, WindowControl wControl, AssessmentItem item) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(AssessmentTestEditorController.class, getLocale()));
		this.item = item;
		qtiSerializer = qtiService.qtiSerializer();
		
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormWarning("warning.alien.assessment.item");
		
		String title = item.getTitle();
		uifactory.addStaticTextElement("title", "form.imd.title", title, formLayout);

		//question
		StringOutput sb = new StringOutput();
		List<Block> blocks = item.getItemBody().getBlocks();
		for(Block block:blocks) {
			if(block instanceof Interaction) {
				break;
			} else if(block != null) {
				qtiSerializer.serializeJqtiObject(block, new StreamResult(sb));
			}
		}
		
		if(sb.length() > 0) {
			uifactory.addStaticTextElement("desc", "form.imd.descr", sb.toString(), formLayout);
		}
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}
}
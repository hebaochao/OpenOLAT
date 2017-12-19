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
package org.olat.modules.qpool.ui.datasource;

import java.util.List;

import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.ims.qti.QTIConstants;
import org.olat.modules.qpool.QuestionItem;
import org.olat.modules.qpool.QuestionItemShort;
import org.olat.modules.qpool.model.QuestionItemImpl;
import org.olat.modules.taxonomy.TaxonomyLevel;

/**
 * 
 * Initial date: 17.12.2017<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public abstract class TaxonomyLevelItemsSource extends DefaultItemsSource {

	private final TaxonomyLevel taxonomyLevel;
	
	public TaxonomyLevelItemsSource(Identity me, Roles roles, TaxonomyLevel taxonomyLevel) {
		super(me, roles, taxonomyLevel.getDisplayName());
		this.taxonomyLevel = taxonomyLevel;
		getDefaultParams().setLikeTaxonomyLevel(taxonomyLevel);
		getDefaultParams().setExcludeFormat(QTIConstants.QTI_12_FORMAT);
	}

	public TaxonomyLevel getTaxonomyLevel() {
		return taxonomyLevel;
	}

	@Override
	public int postImport(List<QuestionItem> items, boolean editable) {
		if(items == null || items.isEmpty()) return 0;
		for(QuestionItemShort item : items) {
			if(item instanceof QuestionItemImpl) {
				QuestionItemImpl itemImpl = (QuestionItemImpl) item;
				itemImpl.setTaxonomyLevel(taxonomyLevel);
			}
		}
		qpoolService.index(items);
		return items.size();
	}
	
}
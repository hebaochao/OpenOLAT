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
package org.olat.modules.portfolio.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.olat.core.commons.persistence.SortKey;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableSortOptions;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel.Align;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.StringHelper;
import org.olat.modules.portfolio.AssessmentSection;
import org.olat.modules.portfolio.Assignment;
import org.olat.modules.portfolio.Binder;
import org.olat.modules.portfolio.BinderConfiguration;
import org.olat.modules.portfolio.BinderSecurityCallback;
import org.olat.modules.portfolio.Category;
import org.olat.modules.portfolio.CategoryToElement;
import org.olat.modules.portfolio.Page;
import org.olat.modules.portfolio.PortfolioRoles;
import org.olat.modules.portfolio.Section;
import org.olat.modules.portfolio.SectionStatus;
import org.olat.modules.portfolio.ui.PageListDataModel.PageCols;
import org.olat.modules.portfolio.ui.component.TimelinePoint;
import org.olat.modules.portfolio.ui.model.PageRow;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 07.06.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class BinderPageListController extends AbstractPageListController  {
	
	private Link newEntryLink;
	
	private CloseableModalController cmc;
	private PageMetadataEditController newPageCtrl;
	private AssignmentEditController newAssignmentCtrl;

	private final Binder binder;
	private final List<Identity> owners;
	
	@Autowired
	private UserManager userManager;
	
	public BinderPageListController(UserRequest ureq, WindowControl wControl, TooledStackedPanel stackPanel,
			BinderSecurityCallback secCallback, Binder binder, BinderConfiguration config) {
		super(ureq, wControl, stackPanel, secCallback, config, "binder_pages", true);
		this.binder = binder;
		owners = portfolioService.getMembers(binder, PortfolioRoles.owner.name());
		
		initForm(ureq);
		loadModel(null);
	}

	@Override
	public void initTools() {
		if(secCallback.canAddPage()) {
			newEntryLink = LinkFactory.createToolLink("new.page", translate("create.new.page"), this);
			newEntryLink.setIconLeftCSS("o_icon o_icon-lg o_icon_new_portfolio");
			stackPanel.addTool(newEntryLink, Align.right);
		}
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		super.initForm(formLayout, listener, ureq);
		
		if(formLayout instanceof FormLayoutContainer) {
			FormLayoutContainer layoutCont = (FormLayoutContainer)formLayout;
			StringBuilder ownerSb = new StringBuilder();
			for(Identity owner:owners) {
				if(ownerSb.length() > 0) ownerSb.append(", ");
				ownerSb.append(userManager.getUserDisplayName(owner));
			}
			layoutCont.contextPut("owners", ownerSb.toString());
			layoutCont.contextPut("binderTitle", StringHelper.escapeHtml(binder.getTitle()));
		}

		FlexiTableSortOptions options = new FlexiTableSortOptions();
		options.setFromColumnModel(false);
		options.setDefaultOrderBy(new SortKey(PageCols.date.name(), false));
		tableEl.setSortSettings(options);
	}

	@Override
	protected void loadModel(String searchString) {
		List<Section> sections = portfolioService.getSections(binder);
		
		List<CategoryToElement> categorizedElements = portfolioService.getCategorizedSectionsAndPages(binder);
		Map<OLATResourceable,List<Category>> categorizedElementMap = new HashMap<>();
		Map<Section,Set<String>> sectionAggregatedCategoriesMap = new HashMap<>();
		for(CategoryToElement categorizedElement:categorizedElements) {
			List<Category> categories = categorizedElementMap.get(categorizedElement.getCategorizedResource());
			if(categories == null) {
				categories = new ArrayList<>();
				categorizedElementMap.put(categorizedElement.getCategorizedResource(), categories);
			}
			categories.add(categorizedElement.getCategory());
		}
		
		//comments
		Map<Long,Long> numberOfCommentsMap = portfolioService.getNumberOfComments(binder);
		
		//assessment sections
		List<AssessmentSection> assessmentSections = portfolioService.getAssessmentSections(binder, getIdentity());
		Map<Section,AssessmentSection> sectionToAssessmentSectionMap = assessmentSections.stream()
				.collect(Collectors.toMap(as -> as.getSection(), as -> as));
		
		//assignments
		List<Assignment> assignments = portfolioService.getAssignments(binder);
		Map<Section,List<Assignment>> sectionToAssignmentMap = new HashMap<>();
		for(Assignment assignment:assignments) {
			List<Assignment> assignmentList;
			if(sectionToAssignmentMap.containsKey(assignment.getSection())) {
				assignmentList = sectionToAssignmentMap.get(assignment.getSection());
			} else {
				assignmentList = new ArrayList<>();
				sectionToAssignmentMap.put(assignment.getSection(), assignmentList);
			}
			assignmentList.add(assignment);
		}

		List<Page> pages = portfolioService.getPages(binder, searchString);
		List<PageRow> rows = new ArrayList<>(pages.size());
		List<TimelinePoint> points = new ArrayList<>(pages.size());
		for (Page page : pages) {
			boolean first = false;
			Section section = page.getSection();
			if (sections.remove(section)) {
				first = true;
			}
			PageRow pageRow = forgeRow(page, sectionToAssessmentSectionMap.get(section), sectionToAssignmentMap.get(section),
					first, categorizedElementMap, numberOfCommentsMap);
			rows.add(pageRow);
			if(secCallback.canAddPage() && section != null
					&& section.getSectionStatus() != SectionStatus.closed
					&& section.getSectionStatus() != SectionStatus.submitted) {
				FormLink newEntryButton = uifactory.addFormLink("new.entry." + (++counter), "new.entry", "create.new.page", null, flc, Link.BUTTON);
				newEntryButton.setCustomEnabledLinkCSS("btn btn-primary");
				newEntryButton.setUserObject(pageRow);
				pageRow.setNewEntryLink(newEntryButton);
			}
			
			if(secCallback.canAddAssignment() && section != null) {
				FormLink newAssignmentButton = uifactory.addFormLink("new.assignment." + (++counter), "new.assignment", "create.new.assignment", null, flc, Link.BUTTON);
				newAssignmentButton.setCustomEnabledLinkCSS("btn btn-primary");
				newAssignmentButton.setUserObject(pageRow);
				pageRow.setNewAssignmentLink(newAssignmentButton);
			}
			
			if(section != null) {
				Set<String> categories = sectionAggregatedCategoriesMap.get(section);
				if(categories == null) {
					categories = new HashSet<>();
					sectionAggregatedCategoriesMap.put(section, categories);
				}
				if(pageRow.hasPageCategories()) {
					categories.addAll(pageRow.getPageCategories());
				}
				
				pageRow.setSectionCategories(categories);
			}
			

			String s = page.getPageStatus() == null ? "draft" : page.getPageStatus().name();
			points.add(new TimelinePoint(page.getKey().toString(), page.getTitle(), page.getCreationDate(), s));
		}
		
		//sections without pages
		if(!StringHelper.containsNonWhitespace(searchString)) {
			for(Section section:sections) {
				PageRow pageRow = forgeRow(section, sectionToAssessmentSectionMap.get(section), sectionToAssignmentMap.get(section),
						true, categorizedElementMap);
				rows.add(pageRow);
				if(secCallback.canAddPage() && section != null
						&& section.getSectionStatus() != SectionStatus.closed
						&& section.getSectionStatus() != SectionStatus.submitted) {
					FormLink newEntryButton = uifactory.addFormLink("new.entry." + (++counter), "new.entry", "create.new.page", null, flc, Link.BUTTON);
					newEntryButton.setCustomEnabledLinkCSS("btn btn-primary");
					newEntryButton.setUserObject(pageRow);
					pageRow.setNewEntryLink(newEntryButton);
				}
				
				if(secCallback.canAddAssignment() && section != null) {
					FormLink newAssignmentButton = uifactory.addFormLink("new.assignment." + (++counter), "new.assignment", "create.new.assignment", null, flc, Link.BUTTON);
					newAssignmentButton.setCustomEnabledLinkCSS("btn btn-primary");
					newAssignmentButton.setUserObject(pageRow);
					pageRow.setNewAssignmentLink(newAssignmentButton);
				}
			}
		}

		timelineEl.setPoints(points);
		model.setObjects(rows);
		tableEl.reloadData();
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source instanceof FormLink) {
			FormLink link = (FormLink)source;
			String cmd = link.getCmd();
			if("new.entry".equals(cmd)) {
				PageRow row = (PageRow)link.getUserObject();
				doCreateNewPage(ureq, row.getSection());
			} else if("new.assignment".equals(cmd)) {
				PageRow row = (PageRow)link.getUserObject();
				doCreateNewAssignment(ureq, row.getSection());
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if(newEntryLink == source) {
			doCreateNewPage(ureq, null);
		}
		super.event(ureq, source, event);
	}
	
	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if(newPageCtrl == source || newAssignmentCtrl == source) {
			if(event == Event.DONE_EVENT) {
				loadModel(null);
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
			cmc.deactivate();
			cleanUp();
		} else if(cmc == source) {
			cleanUp();
		}
		super.event(ureq, source, event);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(newAssignmentCtrl);
		removeAsListenerAndDispose(newPageCtrl);
		removeAsListenerAndDispose(cmc);
		newAssignmentCtrl = null;
		newPageCtrl = null;
		cmc = null;
	}
	
	private void doCreateNewPage(UserRequest ureq, Section preSelectedSection) {
		if(newPageCtrl != null) return;
		
		newPageCtrl = new PageMetadataEditController(ureq, getWindowControl(), binder, false, preSelectedSection, true);
		listenTo(newPageCtrl);
		
		String title = translate("create.new.page");
		cmc = new CloseableModalController(getWindowControl(), null, newPageCtrl.getInitialComponent(), true, title, true);
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doCreateNewAssignment(UserRequest ureq, Section section) {
		if(newAssignmentCtrl != null) return;
		
		newAssignmentCtrl = new AssignmentEditController(ureq, getWindowControl(), section);
		listenTo(newAssignmentCtrl);
		
		String title = translate("create.new.assignment");
		cmc = new CloseableModalController(getWindowControl(), null, newAssignmentCtrl.getInitialComponent(), true, title, true);
		listenTo(cmc);
		cmc.activate();
	}
}
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
package org.olat.modules.lecture.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.basesecurity.BaseSecurityModule;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.components.form.flexible.impl.elements.table.BooleanCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.StaticFlexiCellRenderer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.logging.activity.CoreLoggingResourceable;
import org.olat.core.logging.activity.LearningResourceLoggingAction;
import org.olat.core.logging.activity.OlatResourceableType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.modules.lecture.LectureBlock;
import org.olat.modules.lecture.LectureBlockAuditLog;
import org.olat.modules.lecture.LectureBlockRollCall;
import org.olat.modules.lecture.LectureBlockStatus;
import org.olat.modules.lecture.LectureModule;
import org.olat.modules.lecture.LectureRollCallStatus;
import org.olat.modules.lecture.LectureService;
import org.olat.modules.lecture.RollCallSecurityCallback;
import org.olat.modules.lecture.ui.TeacherRollCallDataModel.RollCols;
import org.olat.modules.lecture.ui.component.LectureBlockRollCallStatusItem;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 27 mars 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class TeacherRollCallController extends FormBasicController {
	
	protected static final String USER_PROPS_ID = TeacherRollCallController.class.getCanonicalName();
	
	public static final int USER_PROPS_OFFSET = 500;
	public static final int CHECKBOX_OFFSET = 1000;
	
	private static final String[] onKeys = new String[]{ "on" };
	private static final String[] onValues = new String[]{ "" };
	
	private FlexiTableElement tableEl;
	private TeacherRollCallDataModel tableModel;
	private FormSubmit quickSaveButton;
	private FormLink reopenButton, cancelLectureBlockButton, closeLectureBlocksButton;
	
	private ReasonController reasonCtrl;
	private CloseableModalController cmc;
	private CloseableCalloutWindowController reasonCalloutCtrl;
	private CloseRollCallConfirmationController closeRollCallCtrl;
	private CancelRollCallConfirmationController cancelRollCallCtrl;
	
	private int counter = 0;
	private LectureBlock lectureBlock;
	private final boolean isAdministrativeUser;
	private List<UserPropertyHandler> userPropertyHandlers;
	private RollCallSecurityCallback secCallback;
	private final boolean authorizedAbsenceEnabled;
	private final boolean absenceDefaultAuthorized;
	
	private int numOfLectures;
	private List<Identity> participants;
	
	@Autowired
	private UserManager userManager;
	@Autowired
	private LectureModule lectureModule;
	@Autowired
	private LectureService lectureService;
	@Autowired
	private BaseSecurityModule securityModule;
	
	public TeacherRollCallController(UserRequest ureq, WindowControl wControl,
			LectureBlock block, List<Identity> participants, RollCallSecurityCallback secCallback) {
		super(ureq, wControl, "rollcall");
		
		this.lectureBlock = block;
		this.secCallback = secCallback;
		this.participants = participants;
		setTranslator(userManager.getPropertyHandlerTranslator(getTranslator()));
		
		numOfLectures = lectureBlock.getEffectiveLecturesNumber();
		if(numOfLectures <= 0 && lectureBlock.getStatus() != LectureBlockStatus.cancelled) {
			numOfLectures = lectureBlock.getPlannedLecturesNumber();
		}
		
		Roles roles = ureq.getUserSession().getRoles();
		isAdministrativeUser = securityModule.isUserAllowedAdminProps(roles);
		userPropertyHandlers = userManager.getUserPropertyHandlersFor(USER_PROPS_ID, isAdministrativeUser);

		authorizedAbsenceEnabled = lectureModule.isAuthorizedAbsenceEnabled();
		absenceDefaultAuthorized = lectureModule.isAbsenceDefaultAuthorized();
		
		initForm(ureq);
		loadModel();
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if(formLayout instanceof FormLayoutContainer) {
			FormLayoutContainer layoutCont = (FormLayoutContainer)formLayout;
			
			StringBuilder sb = new StringBuilder();
			List<Identity> teachers = lectureService.getTeachers(lectureBlock);
			for(Identity teacher:teachers) {
				if(sb.length() > 0) sb.append(", ");
				sb.append(StringHelper.escapeJavaScript(userManager.getUserDisplayName(teacher)));
			}
			
			Formatter formatter = Formatter.getInstance(getLocale());
			String date = formatter.formatDate(lectureBlock.getStartDate());
			String startTime = formatter.formatTimeShort(lectureBlock.getStartDate());
			String endTime = formatter.formatTimeShort(lectureBlock.getEndDate());
			
			String[] args = new String[] {
					lectureBlock.getTitle(),	// {0}	
					sb.toString(),				// {1}
					date,						// {2}
					startTime,					// {3}
					endTime						// {4}
			};
		
			layoutCont.contextPut("date", date);
			layoutCont.contextPut("startTime", startTime);
			layoutCont.contextPut("endTime", endTime);
			layoutCont.contextPut("dateAndTime", translate("lecture.block.dateAndTime", args));
			layoutCont.contextPut("teachers", sb.toString());
			layoutCont.contextPut("lectureBlockTitle", StringHelper.escapeHtml(lectureBlock.getTitle()));
			layoutCont.contextPut("lectureBlockExternalId", StringHelper.escapeHtml(lectureBlock.getExternalId()));
			StringBuilder description = Formatter.stripTabsAndReturns(Formatter.formatURLsAsLinks(lectureBlock.getDescription()));
			layoutCont.contextPut("lectureBlockDescription", StringHelper.xssScan(description));
			StringBuilder preparation = Formatter.stripTabsAndReturns(Formatter.formatURLsAsLinks(lectureBlock.getPreparation()));
			layoutCont.contextPut("lectureBlockPreparation", StringHelper.xssScan(preparation));
			layoutCont.contextPut("lectureBlockLocation", StringHelper.escapeHtml(lectureBlock.getLocation()));
			layoutCont.contextPut("lectureBlock",lectureBlock);
			layoutCont.contextPut("lectureBlockOptional", !lectureBlock.isCompulsory());
			layoutCont.setFormTitle(translate("lecture.block", args));
			layoutCont.setFormDescription(StringHelper.escapeJavaScript(lectureBlock.getDescription()));
		}
		
		// table
		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		if(isAdministrativeUser) {
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(RollCols.username));
		}
		
		int colPos = USER_PROPS_OFFSET;
		for (UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
			if (userPropertyHandler == null) continue;

			String propName = userPropertyHandler.getName();
			boolean visible = userManager.isMandatoryUserProperty(USER_PROPS_ID , userPropertyHandler);

			FlexiColumnModel col = new DefaultFlexiColumnModel(visible, userPropertyHandler.i18nColumnDescriptorLabelKey(), colPos, true, propName);
			columnsModel.addFlexiColumnModel(col);
			colPos++;
		}
		
		if(lectureBlock.isCompulsory()) {
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(RollCols.status));
			
			for(int i=0; i<numOfLectures; i++) {
				FlexiColumnModel col = new DefaultFlexiColumnModel("table.header.lecture." + (i+1), i + CHECKBOX_OFFSET, true, "lecture." + (i+1));
				columnsModel.addFlexiColumnModel(col);
			}
			
			//all button
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel("all",
					RollCols.all.ordinal(), "all",
					new BooleanCellRenderer(new StaticFlexiCellRenderer(translate("all"), "all", null, null, translate("all.desc")), null)));
			if(secCallback.canViewAuthorizedAbsences() || secCallback.canEditAuthorizedAbsences()) {
				columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(RollCols.authorizedAbsence));
			}
		}
		
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(RollCols.comment));

		tableModel = new TeacherRollCallDataModel(columnsModel, secCallback, getLocale()); 
		tableEl = uifactory.addTableElement(getWindowControl(), "table", tableModel, 20, false, getTranslator(), formLayout);
		tableEl.setCustomizeColumns(true);
		
		//buttons
		uifactory.addFormCancelButton("cancel", formLayout, ureq, getWindowControl());
		quickSaveButton = uifactory.addFormSubmitButton("save", "save.temporary", formLayout);
		quickSaveButton.setElementCssClass("o_sel_lecture_quick_save");
		closeLectureBlocksButton = uifactory.addFormLink("close.lecture.blocks", formLayout, Link.BUTTON);
		closeLectureBlocksButton.setElementCssClass("o_sel_lecture_close");
		if(lectureModule.isStatusCancelledEnabled()) {
			cancelLectureBlockButton = uifactory.addFormLink("cancel.lecture.blocks", formLayout, Link.BUTTON);
		}
		reopenButton = uifactory.addFormLink("reopen.lecture.blocks", formLayout, Link.BUTTON);
		reopenButton.setElementCssClass("o_sel_lecture_reopen");
		updateUI();
	}
	
	private void updateUI() {
		quickSaveButton.setVisible(secCallback.canEdit());
		closeLectureBlocksButton.setVisible(secCallback.canEdit());
		if(cancelLectureBlockButton != null) {
			cancelLectureBlockButton.setVisible(secCallback.canEdit());
		}
		reopenButton.setVisible(secCallback.canReopen());
		
		List<TeacherRollCallRow> rows = tableModel.getObjects();
		if(rows != null) {
			for(TeacherRollCallRow row:rows) {
				MultipleSelectionElement[] checks = row.getChecks();
				if(checks != null) {
					for(MultipleSelectionElement check:checks) {
						check.setEnabled(secCallback.canEditAbsences());
						check.getComponent().setDirty(true);
					}
				}
				row.getCommentEl().setEnabled(secCallback.canEdit());
			}
		}
		
		tableEl.reset(false, false, true);
	}

	private void loadModel() {
		List<LectureBlockRollCall> rollCalls = lectureService.getRollCalls(lectureBlock);
		Map<Identity,LectureBlockRollCall> rollCallMap = new HashMap<>();
		for(LectureBlockRollCall rollCall:rollCalls) {
			rollCallMap.put(rollCall.getIdentity(), rollCall);
		}
		
		List<TeacherRollCallRow> rows = new ArrayList<>(participants.size());
		for(Identity participant:participants) {
			LectureBlockRollCall rollCall = rollCallMap.get(participant);
			rows.add(forgeRow(participant, rollCall));
		}
		tableModel.setObjects(rows);
	}
	
	private TeacherRollCallRow forgeRow(Identity participant, LectureBlockRollCall rollCall) {
		TeacherRollCallRow row = new TeacherRollCallRow(rollCall, participant, userPropertyHandlers, getLocale());
		
		int numOfChecks = lectureBlock.isCompulsory() ? numOfLectures : 0;
		MultipleSelectionElement[] checks = new MultipleSelectionElement[numOfChecks];
		List<Integer> absences = rollCall == null ? Collections.emptyList() : rollCall.getLecturesAbsentList();
		
		for(int i=0; i<numOfChecks; i++) {
			String checkId = "check_".concat(Integer.toString(++counter));
			MultipleSelectionElement check = uifactory.addCheckboxesHorizontal(checkId, null, flc, onKeys, onValues);
			check.setDomReplacementWrapperRequired(false);
			check.addActionListener(FormEvent.ONCHANGE);
			check.setEnabled(secCallback.canEditAbsences());
			check.setUserObject(row);
			check.setAjaxOnly(true);
			if(absences.contains(i)) {
				check.select(onKeys[0], true);
			}
			checks[i] = check;
			flc.add(check);
		}
		row.setChecks(checks);
		
		LectureBlockRollCallStatusItem statusEl = new LectureBlockRollCallStatusItem("status_".concat(Integer.toString(++counter)),
				row, authorizedAbsenceEnabled, absenceDefaultAuthorized, getTranslator());
		row.setRollCallStatusEl(statusEl);
		
		if(secCallback.canEditAuthorizedAbsences() || secCallback.canViewAuthorizedAbsences()) {
			String page = velocity_root + "/authorized_absence_cell.html";
			FormLayoutContainer absenceCont = FormLayoutContainer.createCustomFormLayout("auth_cont_".concat(Integer.toString(++counter)), getTranslator(), page);
			absenceCont.setRootForm(mainForm);
			flc.add(absenceCont);
			
			String authorizedAbsencedId = "auth_abs_".concat(Integer.toString(++counter));
			MultipleSelectionElement authorizedAbsencedEl = uifactory.addCheckboxesHorizontal(authorizedAbsencedId, null, absenceCont, onKeys, onValues);
			authorizedAbsencedEl.setDomReplacementWrapperRequired(false);
			authorizedAbsencedEl.addActionListener(FormEvent.ONCHANGE);
			authorizedAbsencedEl.setUserObject(row);
			authorizedAbsencedEl.setAjaxOnly(true);
			authorizedAbsencedEl.setEnabled(secCallback.canEdit() && secCallback.canEditAuthorizedAbsences());
			
			boolean hasAuthorization = rollCall != null && rollCall.getAbsenceAuthorized() != null
					&& rollCall.getAbsenceAuthorized().booleanValue();
			if(hasAuthorization) {
				authorizedAbsencedEl.select(onKeys[0], true);
			}
			row.setAuthorizedAbsence(authorizedAbsencedEl);
			flc.add(authorizedAbsencedEl);

			String reasonId = "abs_reason_".concat(Integer.toString(++counter));
			FormLink reasonLink = uifactory.addFormLink(reasonId, "", null, absenceCont, Link.BUTTON_XSMALL | Link.NONTRANSLATED);
			reasonLink.setTitle(translate("reason"));
			reasonLink.setDomReplacementWrapperRequired(false);
			reasonLink.setIconLeftCSS("o_icon o_icon_notes");
			reasonLink.setVisible(hasAuthorization);
			reasonLink.setUserObject(row);
			row.setReasonLink(reasonLink);
			
			row.setAuthorizedAbsenceCont(absenceCont);
			absenceCont.contextPut("row", row);
		}

		String comment = rollCall == null ? "" : rollCall.getComment();
		String commentId = "comment_".concat(Integer.toString(++counter));
		TextElement commentEl = uifactory.addTextElement(commentId, commentId, null, 128, comment, flc);
		commentEl.setDomReplacementWrapperRequired(false);
		commentEl.setEnabled(secCallback.canEdit());
		row.setCommentEl(commentEl);
		flc.add(commentEl);
		return row;
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void propagateDirtinessToContainer(FormItem source, FormEvent event) {
		if(!(source instanceof MultipleSelectionElement)) {
			super.propagateDirtinessToContainer(source, event);
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(reasonCtrl == source) {
			if(event == Event.DONE_EVENT) {
				doReason(reasonCtrl.getTeacherRollCallRow(), reasonCtrl.getReason());
			}
			reasonCalloutCtrl.deactivate();
			cleanUp();
		} else if(reasonCalloutCtrl == source) {
			cleanUp();
		} else if(closeRollCallCtrl == source) {
			lectureBlock = closeRollCallCtrl.getLectureBLock();
			cmc.deactivate();
			cleanUp();
			if(event == Event.DONE_EVENT) {
				fireEvent(ureq, Event.DONE_EVENT);
			}
		} else if(cancelRollCallCtrl == source) {
			lectureBlock = cancelRollCallCtrl.getLectureBlock();
			cmc.deactivate();
			cleanUp();
			if(event == Event.DONE_EVENT) {
				fireEvent(ureq, Event.DONE_EVENT);
			}
		} else if(cmc == source) {
			cleanUp();
		}
		super.event(ureq, source, event);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(cancelRollCallCtrl);
		removeAsListenerAndDispose(reasonCalloutCtrl);
		removeAsListenerAndDispose(closeRollCallCtrl);
		removeAsListenerAndDispose(reasonCtrl);
		removeAsListenerAndDispose(cmc);
		cancelRollCallCtrl = null;
		reasonCalloutCtrl = null;
		closeRollCallCtrl = null;
		reasonCtrl = null;
		cmc = null;
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;

		for(int i=tableModel.getRowCount(); i-->0; ) {
			TeacherRollCallRow row = tableModel.getObject(i);
			row.getAuthorizedAbsence().clearError();
			
			if(row.getRollCall() == null) {
				//??? stop?
			} else if(!absenceDefaultAuthorized) {
				String reason = row.getRollCall().getAbsenceReason();
				if(row.getAuthorizedAbsence().isAtLeastSelected(1) && !StringHelper.containsNonWhitespace(reason)) {
					row.getAuthorizedAbsence().setErrorKey("error.reason.mandatory", null);
					allOk &= false;
				}
			}
		}

		return allOk & super.validateFormLogic(ureq);
	}
	
	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source == tableEl) {
			if(event instanceof SelectionEvent) {
				SelectionEvent se = (SelectionEvent)event;
				String cmd = se.getCommand();
				TeacherRollCallRow row = tableModel.getObject(se.getIndex());
				if("all".equals(cmd)) {
					doCheckAllRow(row);
				}
			}
		} else if(source instanceof MultipleSelectionElement) {
			MultipleSelectionElement check = (MultipleSelectionElement)source;
			TeacherRollCallRow row = (TeacherRollCallRow)check.getUserObject();
			if(row.getAuthorizedAbsence() == check) {
				doAuthorizedAbsence(row, check);
				if(check.isAtLeastSelected(1)) {
					doCalloutReasonAbsence(ureq, check.getFormDispatchId() + "_C_0", row);
				}
			} else {
				doCheckRow(row, check);
			}
		} else if(reopenButton == source) {
			doReopen(ureq);
		} else if(closeLectureBlocksButton == source) {
			if(validateFormLogic(ureq)) {
				saveLectureBlocks();
				doConfirmCloseLectureBlock(ureq);
			}
		} else if(cancelLectureBlockButton == source) {
			saveLectureBlocks();
			doConfirmCancelLectureBlock(ureq);
		} else if(source instanceof FormLink) {
			FormLink link = (FormLink)source;
			String cmd = link.getCmd();
			if(cmd != null && cmd.startsWith("abs_reason_")) {
				TeacherRollCallRow row = (TeacherRollCallRow)link.getUserObject();
				doCalloutReasonAbsence(ureq, link.getFormDispatchId(), row);
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		saveLectureBlocks();
		fireEvent(ureq, Event.CHANGED_EVENT);
	}
	
	private void saveLectureBlocks() {
		String before = lectureService.toAuditXml(lectureBlock);
		
		for(int i=tableModel.getRowCount(); i-->0; ) {
			TeacherRollCallRow row = tableModel.getObject(i);
			
			int numOfChecks = row.getChecks().length;
			List<Integer> absenceList = new ArrayList<>(numOfChecks);
			for(int j=0; j<numOfChecks; j++) {
				if(row.getCheck(j).isAtLeastSelected(1)) {
					absenceList.add(j);
				}
			}
			
			String comment = row.getCommentEl().getValue();
			LectureBlockRollCall rollCall = lectureService.addRollCall(row.getIdentity(), lectureBlock, row.getRollCall(),
					comment, absenceList);
			row.setRollCall(rollCall);
		}

		lectureBlock = lectureService.getLectureBlock(lectureBlock);
		
		if(lectureBlock.getRollCallStatus() == null) {
			lectureBlock.setRollCallStatus(LectureRollCallStatus.open);
		}
		if(lectureBlock.getStatus() == null || lectureBlock.getStatus() == LectureBlockStatus.active) {
			lectureBlock.setStatus(LectureBlockStatus.active);
		}
		lectureBlock = lectureService.save(lectureBlock, null);
		lectureService.recalculateSummary(lectureBlock.getEntry());
		
		String after = lectureService.toAuditXml(lectureBlock);
		lectureService.auditLog(LectureBlockAuditLog.Action.saveLectureBlock, before, after, null, lectureBlock, null, lectureBlock.getEntry(), null, getIdentity());

	}
	
	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}
	
	private void doCheckAllRow(TeacherRollCallRow row) {
		List<Integer> allAbsences = new ArrayList<>(numOfLectures);
		for(int i=0; i<numOfLectures; i++) {
			allAbsences.add(i);
		}
		LectureBlockRollCall rollCall = lectureService.addRollCall(row.getIdentity(), lectureBlock, row.getRollCall(), null, allAbsences);
		for(MultipleSelectionElement check:row.getChecks()) {
			check.select(onKeys[0], true);
		}
		row.setRollCall(rollCall);
		row.getRollCallStatusEl().getComponent().setDirty(true);
		tableEl.reloadData();
		flc.setDirty(true);
	}
	
	private void doCheckRow(TeacherRollCallRow row, MultipleSelectionElement check) {
		int index = row.getIndexOfCheck(check);
		List<Integer> indexList = Collections.singletonList(index);
		
		LectureBlockRollCall rollCall;
		String before = lectureService.toAuditXml(row.getRollCall());
		if(check.isAtLeastSelected(1)) {
			rollCall = lectureService.addRollCall(row.getIdentity(), lectureBlock, row.getRollCall(), indexList);
			lectureService.auditLog(LectureBlockAuditLog.Action.addToRollCall, before, lectureService.toAuditXml(rollCall),
					Integer.toString(index), lectureBlock, rollCall, lectureBlock.getEntry(), row.getIdentity(), getIdentity());
		} else {
			rollCall = lectureService.removeRollCall(row.getIdentity(), lectureBlock, row.getRollCall(), indexList);
			lectureService.auditLog(LectureBlockAuditLog.Action.removeFromRollCall, before, lectureService.toAuditXml(rollCall),
					Integer.toString(index), lectureBlock, rollCall, lectureBlock.getEntry(), row.getIdentity(), getIdentity());
		}
		row.setRollCall(rollCall);
		if(authorizedAbsenceEnabled) {
			if(rollCall.getAbsenceAuthorized() != null && rollCall.getAbsenceAuthorized().booleanValue()) {
				row.getAuthorizedAbsence().select(onKeys[0], true);
			} else {
				row.getAuthorizedAbsence().uncheckAll();
			}
			row.getAuthorizedAbsenceCont().setDirty(true);
		}
		row.getRollCallStatusEl().getComponent().setDirty(true);
	}
	
	private void doAuthorizedAbsence(TeacherRollCallRow row, MultipleSelectionElement check) {
		LectureBlockRollCall rollCall = row.getRollCall();
		boolean authorized = check.isAtLeastSelected(1);
		if(rollCall == null) {
			rollCall = lectureService.getOrCreateRollCall(row.getIdentity(), lectureBlock, authorized, null);
			lectureService.auditLog(LectureBlockAuditLog.Action.createRollCall, null, lectureService.toAuditXml(rollCall),
					authorized ? "true" : "false", lectureBlock, rollCall, lectureBlock.getEntry(), row.getIdentity(), getIdentity());
		} else {
			String before = lectureService.toAuditXml(rollCall);
			rollCall.setAbsenceAuthorized(authorized);
			rollCall = lectureService.updateRollCall(rollCall);
			lectureService.auditLog(LectureBlockAuditLog.Action.updateAuthorizedAbsence, before, lectureService.toAuditXml(rollCall),
					authorized ? "true" : "false", lectureBlock, rollCall, lectureBlock.getEntry(), row.getIdentity(), getIdentity());
		}

		row.getReasonLink().setVisible(authorized);
		row.getAuthorizedAbsenceCont().setDirty(true);
		row.getAuthorizedAbsence().clearError();
		row.setRollCall(rollCall);
		row.getRollCallStatusEl().getComponent().setDirty(true);
	}
	
	private void doCalloutReasonAbsence(UserRequest ureq, String elementId, TeacherRollCallRow row) {
		boolean canEdit = secCallback.canEdit() && secCallback.canEditAuthorizedAbsences();
		reasonCtrl = new ReasonController(ureq, getWindowControl(), row, canEdit);
		listenTo(reasonCtrl);

		reasonCalloutCtrl = new CloseableCalloutWindowController(ureq, getWindowControl(),
				reasonCtrl.getInitialComponent(), elementId, "", true, "");
		listenTo(reasonCalloutCtrl);
		reasonCalloutCtrl.activate();
	}
	
	private void doReason(TeacherRollCallRow row, String reason) {
		LectureBlockRollCall rollCall = row.getRollCall();
		String before = lectureService.toAuditXml(rollCall);
		if(rollCall == null) {
			row.getAuthorizedAbsence().select(onKeys[0], true);
			rollCall = lectureService.getOrCreateRollCall(row.getIdentity(), lectureBlock, true, reason);
			lectureService.auditLog(LectureBlockAuditLog.Action.createRollCall, before, lectureService.toAuditXml(rollCall),
					reason, lectureBlock, rollCall, lectureBlock.getEntry(), row.getIdentity(), getIdentity());
		} else {
			rollCall.setAbsenceReason(reason);
			rollCall = lectureService.updateRollCall(rollCall);
			lectureService.auditLog(LectureBlockAuditLog.Action.updateRollCall, before, lectureService.toAuditXml(rollCall),
					reason, lectureBlock, rollCall, lectureBlock.getEntry(), row.getIdentity(), getIdentity());
		}

		row.setRollCall(rollCall);
	}
	
	private void doConfirmCloseLectureBlock(UserRequest ureq) {
		if(closeRollCallCtrl != null) return;
		
		closeRollCallCtrl = new CloseRollCallConfirmationController(ureq, getWindowControl(), lectureBlock, secCallback);
		listenTo(closeRollCallCtrl);
		
		cmc = new CloseableModalController(getWindowControl(), "close", closeRollCallCtrl.getInitialComponent(), true, translate("close.lecture.blocks"));
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doConfirmCancelLectureBlock(UserRequest ureq) {
		if(closeRollCallCtrl != null) return;
		
		cancelRollCallCtrl = new CancelRollCallConfirmationController(ureq, getWindowControl(), lectureBlock, secCallback);
		listenTo(cancelRollCallCtrl);
		
		cmc = new CloseableModalController(getWindowControl(), "close", cancelRollCallCtrl.getInitialComponent(), true, translate("cancel.lecture.blocks"));
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doReopen(UserRequest ureq) {
		String before = lectureService.toAuditXml(lectureBlock);
		lectureBlock.setRollCallStatus(LectureRollCallStatus.reopen);
		lectureBlock = lectureService.save(lectureBlock, null);
		secCallback.updateLectureBlock(lectureBlock);
		updateUI();
		fireEvent(ureq, Event.CHANGED_EVENT);
		
		String after = lectureService.toAuditXml(lectureBlock);
		lectureService.auditLog(LectureBlockAuditLog.Action.reopenLectureBlock, before, after, null, lectureBlock, null, lectureBlock.getEntry(), null, getIdentity());
		ThreadLocalUserActivityLogger.log(LearningResourceLoggingAction.LECTURE_BLOCK_ROLL_CALL_REOPENED, getClass(),
				CoreLoggingResourceable.wrap(lectureBlock, OlatResourceableType.lectureBlock, lectureBlock.getTitle()));
	}
}

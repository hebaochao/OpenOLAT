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
package org.olat.modules.fo.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.olat.basesecurity.BaseSecurityModule;
import org.olat.core.commons.modules.bc.meta.MetaInfo;
import org.olat.core.commons.modules.bc.meta.tagged.MetaTagged;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.services.mark.Mark;
import org.olat.core.commons.services.mark.MarkResourceStat;
import org.olat.core.commons.services.mark.MarkingService;
import org.olat.core.dispatcher.mapper.Mapper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.link.LinkPopupSettings;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.media.NotFoundMediaResource;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.ConsumableBoolean;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSMediaResource;
import org.olat.core.util.vfs.filters.VFSItemExcludePrefixFilter;
import org.olat.modules.fo.Forum;
import org.olat.modules.fo.ForumCallback;
import org.olat.modules.fo.ForumChangedEvent;
import org.olat.modules.fo.ForumLoggingAction;
import org.olat.modules.fo.Message;
import org.olat.modules.fo.MessageLight;
import org.olat.modules.fo.MessageRef;
import org.olat.modules.fo.Status;
import org.olat.modules.fo.archiver.formatters.ForumDownloadResource;
import org.olat.modules.fo.manager.ForumManager;
import org.olat.modules.fo.ui.MessageEditController.EditMode;
import org.olat.modules.fo.ui.events.SelectMessageEvent;
import org.olat.portfolio.EPUIFactory;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.user.DisplayPortraitController;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;
import org.olat.util.logging.activity.LoggingResourceable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * The list of messages in a thread.
 * 
 * Initial date: 10.11.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class MessageListController extends BasicController implements GenericEventListener {

	protected static final String USER_PROPS_ID = ForumUserListController.class.getCanonicalName();
	
	private final VelocityContainer mainVC;

	private Link backLink, archiveThreadButton,
			stickyButton, removeStickyButton,
			closeThreadButton, openThreadButton,
			hideThreadButton, showThreadButton,
			allButton, allFlatButton, oneButton, markedButton, newButton;
	
	private CloseableModalController cmc;
	private MessageEditController editMessageCtrl, replyMessageCtrl;
	private DialogBoxController confirmDeleteCtrl, confirmSplitCtrl;
	private ForumMessageListController moveCtrl, messageTableCtrl;
	
	private Message thread;
	private boolean reloadList;
	
	private final Forum forum;
	private final boolean guestOnly;
	private final Formatter formatter;
	private final String thumbnailMapper;
	private final ForumCallback foCallback;
	private final OLATResourceable forumOres;
	private final boolean isAdministrativeUser;
	private final List<UserPropertyHandler> userPropertyHandlers;
	
	private LoadMode loadMode;
	private List<MessageView> backupViews;

	@Autowired
	private UserManager userManager;
	@Autowired
	private ForumManager forumManager;
	@Autowired
	private MarkingService markingService;
	@Autowired
	private BaseSecurityModule securityModule;
	@Autowired
	private EPFrontendManager epMgr;
	
	public MessageListController(UserRequest ureq, WindowControl wControl,
			Forum forum, ForumCallback foCallback) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(Forum.class, getLocale(), getTranslator()));

		this.forum = forum;
		this.foCallback = foCallback;
		formatter = Formatter.getInstance(getLocale());
		forumOres = OresHelper.createOLATResourceableInstance("Forum", forum.getKey());
		guestOnly = ureq.getUserSession().getRoles().isGuestOnly();
		isAdministrativeUser = securityModule.isUserAllowedAdminProps(ureq.getUserSession().getRoles());
		userPropertyHandlers = userManager.getUserPropertyHandlersFor(USER_PROPS_ID, isAdministrativeUser);
		
		thumbnailMapper = registerCacheableMapper(ureq, "fo_att_" + forum.getKey(), new AttachmentsMapper());

		mainVC = createVelocityContainer("threadview");
		mainVC.contextPut("threadMode", Boolean.TRUE);
		mainVC.contextPut("thumbMapper", thumbnailMapper);
		mainVC.contextPut("guestOnly", new Boolean(guestOnly));
		
		messageTableCtrl = new ForumMessageListController(ureq, getWindowControl(), forum, false);
		listenTo(messageTableCtrl);
		mainVC.put("singleThreadTable", messageTableCtrl.getInitialComponent());
		
		putInitialPanel(mainVC);
		initButtons();
		
		// Register for forum events
		CoordinatorManager.getInstance().getCoordinator().getEventBus().registerFor(this, getIdentity(), forum);
	}
	
	@Override
	protected void doDispose() {
		CoordinatorManager.getInstance().getCoordinator().getEventBus().deregisterFor(this, forum);
	}
	
	private void initButtons() {
		backLink = LinkFactory.createCustomLink("backLinkLT", "back", "listalltitles", Link.LINK_BACK, mainVC, this);

		archiveThreadButton = LinkFactory.createButtonSmall("archive.thread", mainVC, this);
		archiveThreadButton.setIconLeftCSS("o_icon o_icon-fw o_icon_archive_tool");
		
		stickyButton = LinkFactory.createLink("msg.sticky", mainVC, this);
		stickyButton.setIconLeftCSS("o_icon o_icon-fw o_forum_status_sticky_icon");
		removeStickyButton = LinkFactory.createLink("remove.sticky", mainVC, this);
		removeStickyButton.setIconLeftCSS("o_icon o_icon-fw o_forum_status_thread_icon");

		closeThreadButton = LinkFactory.createLink("close.thread", mainVC, this);
		closeThreadButton.setIconLeftCSS("o_icon o_icon-fw o_forum_status_closed_icon");
		openThreadButton = LinkFactory.createLink("open.thread", mainVC, this);
		openThreadButton.setIconLeftCSS("o_icon o_icon-fw o_forum_status_opened_icon");	

		hideThreadButton = LinkFactory.createLink("hide.thread", mainVC, this);
		hideThreadButton.setIconLeftCSS("o_icon o_icon-fw o_forum_status_hidden_icon");
		showThreadButton = LinkFactory.createLink("show.thread", mainVC, this);
		showThreadButton.setIconLeftCSS("o_icon o_icon-fw o_forum_status_visible_icon");

		allButton = LinkFactory.createButtonSmall("viewswitch.threadview", mainVC, this);
		allButton.setIconLeftCSS("o_icon o_icon-fw o_icon-flip-vertical o_forum_all_icon");
		allButton.setElementCssClass("o_forum_all_messages");
		allFlatButton = LinkFactory.createButtonSmall("viewswitch.flatview", mainVC, this);
		allFlatButton.setIconLeftCSS("o_icon o_icon-fw o_forum_all_flat_icon");
		allFlatButton.setElementCssClass("o_forum_all_flat_messages");
		oneButton = LinkFactory.createButtonSmall("viewswitch.messageview", mainVC, this);
		oneButton.setIconLeftCSS("o_icon o_icon-fw o_forum_one_icon");
		oneButton.setElementCssClass("o_forum_one_message");
		markedButton = LinkFactory.createButtonSmall("viewswitch.marked", mainVC, this);
		markedButton.setIconLeftCSS("o_icon o_icon-fw o_forum_marked_icon");
		markedButton.setElementCssClass("o_forum_marked_messages");
		newButton = LinkFactory.createButtonSmall("viewswitch.new", mainVC, this);
		newButton.setIconLeftCSS("o_icon o_icon-fw o_forum_new_icon");
		newButton.setElementCssClass("o_forum_new_messages");
	}
	
	private void updateButtons(Link activeLink) {
		allButton.setCustomEnabledLinkCSS(activeButton(allButton, activeLink));
		allFlatButton.setCustomEnabledLinkCSS(activeButton(allFlatButton, activeLink));
		oneButton.setCustomEnabledLinkCSS(activeButton(oneButton, activeLink));
		markedButton.setCustomEnabledLinkCSS(activeButton(markedButton, activeLink));
		newButton.setCustomEnabledLinkCSS(activeButton(newButton, activeLink));
		
		archiveThreadButton.setVisible(thread != null && foCallback.mayArchiveForum());
		if(thread == null || guestOnly || !foCallback.mayEditMessageAsModerator()) {
			closeThreadButton.setVisible(false);
			openThreadButton.setVisible(false);
			hideThreadButton.setVisible(false);
			showThreadButton.setVisible(false);
			stickyButton.setVisible(false);
			removeStickyButton.setVisible(false);
		} else {
			Status status = Status.getStatus(thread.getStatusCode());
			boolean isClosed = status.isClosed();
			boolean isHidden = status.isHidden();
			boolean isSticky = status.isSticky();
			closeThreadButton.setVisible(!isClosed);
			openThreadButton.setVisible(isClosed);
			hideThreadButton.setVisible(!isHidden);
			showThreadButton.setVisible(isHidden);
			stickyButton.setVisible(foCallback.mayEditMessageAsModerator() && thread != null && !isSticky);
			removeStickyButton.setVisible(foCallback.mayEditMessageAsModerator() && thread != null && isSticky);
		}
	}
	
	private String activeButton(Link link, Link activeLink) {
		return "btn btn-sm btn-default o_forum_tool " + (link == activeLink ? "active" : "");
	}
	
	private void reloadModel(UserRequest ureq, Message message) {
		reloadList = false;
		if(loadMode == LoadMode.thread) {
			loadThread(ureq, thread);
			scrollTo(message);
		} else if(message != null) {
			MessageView view = loadView(ureq, message);
			backupViews.add(view);
			
			mainVC.contextPut("messages", backupViews);
			messageTableCtrl.loadMessages(new ArrayList<>(0));

			updateButtons(allFlatButton);
			mainVC.contextPut("threadMode", Boolean.FALSE);
			scrollTo(message);
		}
	}
	
	/**
	 * The method doesn't scroll has the delete poped a blue box
	 * @param ureq
	 * @param message
	 */
	private void reloadModelAfterDelete(UserRequest ureq, MessageView message) {
		if(loadMode == LoadMode.thread) {
			loadThread(ureq, thread);
		} else if(message != null) {
			for(MessageView msg:backupViews) {
				if(msg.getKey().equals(message.getKey())) {
					backupViews.remove(msg);
					break;
				}
			}
			
			mainVC.contextPut("messages", backupViews);
			messageTableCtrl.loadMessages(new ArrayList<>(0));

			updateButtons(allFlatButton);
			mainVC.contextPut("threadMode", Boolean.FALSE);
		}
	}
	
	public void scrollTo(MessageRef ref) {
		if(ref != null && (thread == null || !thread.getKey().equals(ref.getKey()))) {
			mainVC.contextPut("goToMessage", new ConsumableBoolean(true));
			mainVC.contextPut("goToMessageId", ref.getKey());
		}
	}
	
	public void loadUserMessages(UserRequest ureq, Identity user) {
		loadMode = LoadMode.userMessages;
		List<MessageLight> messages = forumManager.getLightMessagesByUser(forum, user);
		backupViews = loadThread(ureq, messages, false);
		messageTableCtrl.loadMessages(new ArrayList<>(0));
		
		allButton.setVisible(false);
		updateButtons(allFlatButton);
		mainVC.contextPut("threadMode", Boolean.FALSE);
		mainVC.contextPut("filteredForFirstName", user.getUser().getProperty(UserConstants.FIRSTNAME, getLocale()));
		mainVC.contextPut("filteredForLastName", user.getUser().getProperty(UserConstants.LASTNAME, getLocale()));
	}
	
	public void loadUserMessagesUnderPseudo(UserRequest ureq, Identity user, String pseudonym) {
		loadMode = LoadMode.userMessagesUnderPseudo;
		List<MessageLight> messages = forumManager.getLightMessagesByUserUnderPseudo(forum, user, pseudonym);
		backupViews = loadThread(ureq, messages, false);
		messageTableCtrl.loadMessages(new ArrayList<>(0));
		
		allButton.setVisible(false);
		updateButtons(allFlatButton);
		mainVC.contextPut("threadMode", Boolean.FALSE);
		mainVC.contextRemove("filteredForFirstName");
		mainVC.contextPut("filteredForLastName", pseudonym);
	}
	
	public void loadGuestMessages(UserRequest ureq) {
		loadMode = LoadMode.guestMessages;
		List<MessageLight> messages = forumManager.getLightMessagesOfGuests(forum);
		backupViews = loadThread(ureq, messages, false);
		messageTableCtrl.loadMessages(new ArrayList<>(0));
		
		allButton.setVisible(false);
		updateButtons(allFlatButton);
		mainVC.contextPut("threadMode", Boolean.FALSE);
		mainVC.contextRemove("filteredForFirstName");
		mainVC.contextPut("filteredForLastName", translate("guest"));
	}
	
	public void loadThread(UserRequest ureq, Message threadMessage) {
		loadMode = LoadMode.thread;
		thread = threadMessage;

		List<MessageLight> messages = forumManager.getLightMessagesByThread(forum, thread);
		messages.add(0, thread);
		backupViews = loadThread(ureq, messages, true);
		messageTableCtrl.loadMessages(new ArrayList<>(0));
		
		allButton.setVisible(true);
		updateButtons(allButton);
		mainVC.contextPut("threadMode", Boolean.TRUE);
		mainVC.contextRemove("filteredForFirstName");
		mainVC.contextRemove("filteredForLastName");
	}
	
	private MessageView loadView(UserRequest ureq, MessageLight message) {
		Set<Long> rms =  null;
		Map<String,Mark> marks = Collections.emptyMap();
		Map<String,Long> artefactStats = Collections.emptyMap();
		List<String> subPaths = Collections.singletonList(message.getKey().toString());
		if(!guestOnly) {
			String businessPath = BusinessControlFactory.getInstance().getAsString(getWindowControl().getBusinessControl()) + "[Message:" + message.getKey() + "]";
			artefactStats = epMgr.getNumOfArtefactsByStartingBusinessPath(businessPath, getIdentity());

			marks = new HashMap<>();
			List<Mark> markList = markingService.getMarkManager().getMarks(forumOres, getIdentity(), subPaths);
			for(Mark mark:markList) {
				marks.put(mark.getResSubPath(), mark);
			}
		}
		
		List<MarkResourceStat> statList = markingService.getMarkManager().getStats(forumOres, subPaths, getIdentity());
		Map<String,MarkResourceStat> stats = new HashMap<String,MarkResourceStat>(statList.size() * 2 + 1);
		for(MarkResourceStat stat:statList) {
			stats.put(stat.getSubPath(), stat);
		}

		MessageView view = new MessageView(message, userPropertyHandlers, getLocale());
		view.setNumOfChildren(0);
		addMessageToCurrentMessagesAndVC(ureq, message, view, backupViews.size(), marks, stats, artefactStats, rms);
		return view;
	}
	
	private List<MessageView> loadThread(UserRequest ureq, List<MessageLight> messages, boolean reorder) {
		Set<Long> rms =  null;
		Map<String,Mark> marks = Collections.emptyMap();
		Map<String,Long> artefactStats = Collections.emptyMap();
		if(!guestOnly) {
			rms = forumManager.getReadSet(getIdentity(), forum);

			String businessPath = BusinessControlFactory.getInstance().getAsString(getWindowControl().getBusinessControl()) + "[Message:";
			artefactStats = epMgr.getNumOfArtefactsByStartingBusinessPath(businessPath, getIdentity());

			marks = new HashMap<>(marks.size() * 2 + 1);
			List<Mark> markList = markingService.getMarkManager().getMarks(forumOres, getIdentity(), null);
			for(Mark mark:markList) {
				marks.put(mark.getResSubPath(), mark);
			}
		}
		
		List<MarkResourceStat> statList = markingService.getMarkManager().getStats(forumOres, null, getIdentity());
		Map<String,MarkResourceStat> stats = new HashMap<String,MarkResourceStat>(statList.size() * 2 + 1);
		for(MarkResourceStat stat:statList) {
			stats.put(stat.getSubPath(), stat);
		}

		if(reorder) {
			List<MessageLight> orderedMessages = new ArrayList<MessageLight>();
			orderMessagesThreaded(messages, orderedMessages, thread);
			messages = orderedMessages;
		}

		List<MessageView> views = new ArrayList<>(messages.size());
		Map<Long,MessageView> keyToViews = new HashMap<>();
		for(MessageLight msg:messages) {
			MessageView view = new MessageView(msg, userPropertyHandlers, getLocale());
			view.setNumOfChildren(0);
			views.add(view);
			keyToViews.put(msg.getKey(), view);
		}

		//calculate depth and number of children
		for(MessageView view:views) {
			if(view.getParentKey() == null) {
				view.setDepth(0);
			} else {
				view.setDepth(1);
				for(MessageView parent = keyToViews.get(view.getParentKey()); parent != null; parent = keyToViews.get(parent.getParentKey())) {
					parent.setNumOfChildren(parent.getNumOfChildren() + 1);
					view.setDepth(view.getDepth() + 1);
				}
			}
		}
		
		int msgNum = 0;
		//append ui things
		for (MessageLight msg: messages) {
			addMessageToCurrentMessagesAndVC(ureq, msg, keyToViews.get(msg.getKey()), msgNum++, marks, stats, artefactStats, rms);
		}
		
		mainVC.contextPut("messages", views);
		return views;
	}
	
	/**
	 * Orders the messages in the logical instead of chronological order.
	 * @param messages
	 * @param orderedList
	 * @param startMessage
	 */	
	private void orderMessagesThreaded(List<MessageLight> messages, List<MessageLight> orderedList, MessageRef startMessage) {
		if (messages == null || orderedList == null || startMessage == null) return;
		Iterator<MessageLight> iterMsg = messages.iterator();
		while (iterMsg.hasNext()) {
			MessageLight msg = iterMsg.next();
			if (msg.getParentKey() == null) {
				orderedList.add(msg);
				List<MessageLight> copiedMessages = new ArrayList<>(messages);
				copiedMessages.remove(msg);
				messages = copiedMessages;
				continue;
			}
			if ((msg.getParentKey() != null) && (msg.getParentKey().equals(startMessage.getKey()))) {
				orderedList.add(msg);
				orderMessagesThreaded(messages, orderedList, msg);
			}
		}
	}
	
	private void markRead(MessageLight message) {
		if(!guestOnly) {
			forumManager.markAsRead(getIdentity(), forum, message);
		}
	}
	
	private void addMessageToCurrentMessagesAndVC(UserRequest ureq, MessageLight m, MessageView messageView, int msgCount,
			Map<String,Mark> marks, Map<String,MarkResourceStat> stats, Map<String,Long> artefactStats,
			Set<Long> readSet) {
		
		// all values belonging to a message are stored in this map
		// these values can be accessed in velocity. make sure you clean up
		// everything
		// you create here in disposeCurrentMessages()!
		String keyString = m.getKey().toString();
		if (readSet == null || readSet.contains(m.getKey())) {
			messageView.setNewMessage(false);
		} else {// mark now as read
			markRead(m);
			messageView.setNewMessage(true);
		}
		// add some data now
		messageView.setFormattedCreationDate(formatter.formatDateAndTime(m.getCreationDate()));
		messageView.setFormattedLastModified(formatter.formatDateAndTime(m.getLastModified()));
		
		Identity modifier = m.getModifier();
		if (modifier != null) {
			messageView.setModified(true);
			messageView.setModifierFirstName(modifier.getUser().getProperty(UserConstants.FIRSTNAME, getLocale()));
			messageView.setModifierLastName(modifier.getUser().getProperty(UserConstants.LASTNAME, getLocale()));
		} else {
			messageView.setModified(false);
		}
		
		Identity creator = m.getCreator();
		boolean userIsMsgCreator = false;
		//keeps the first 15 chars
		if(creator != null) {
			userIsMsgCreator = getIdentity().equals(creator);
			if(!StringHelper.containsNonWhitespace(m.getPseudonym())) {
				messageView.setCreatorFirstname(Formatter.truncate(creator.getUser().getProperty(UserConstants.FIRSTNAME, getLocale()), 18));
				messageView.setCreatorLastname(Formatter.truncate(creator.getUser().getProperty(UserConstants.LASTNAME, getLocale()), 18));
			}
		}
		
		// message attachments
		VFSContainer msgContainer = forumManager.getMessageContainer(forum.getKey(), m.getKey());
		messageView.setMessageContainer(msgContainer);
		List<VFSItem> attachments = new ArrayList<VFSItem>(msgContainer.getItems(new VFSItemExcludePrefixFilter(MessageEditController.ATTACHMENT_EXCLUDE_PREFIXES)));				
		messageView.setAttachments(attachments);

		// number of children and modify/delete permissions
		int numOfChildren = messageView.getNumOfChildren();
		
		messageView.setAuthor(userIsMsgCreator);
		boolean threadTop = m.getThreadtop() == null;
		messageView.setThreadTop(threadTop);
		boolean isThreadClosed;
		if(threadTop) {
			isThreadClosed = Status.getStatus(m.getStatusCode()).isClosed();
		} else {
			if(thread == null) {
				isThreadClosed = Status.getStatus(m.getThreadtop().getStatusCode()).isClosed();
			} else {
				isThreadClosed = Status.getStatus(thread.getStatusCode()).isClosed();
			}
		}
		messageView.setClosed(isThreadClosed);
		
		if(!guestOnly && !m.isGuest() && !StringHelper.containsNonWhitespace(m.getPseudonym())) {
			// add portrait to map for later disposal and key for rendering in velocity
			DisplayPortraitController portrait = new DisplayPortraitController(ureq, getWindowControl(), m.getCreator(), true, true, false, true);
			messageView.setPortrait(portrait);
			mainVC.put("portrait_".concat(keyString), portrait.getInitialComponent());
		  
			// Add link with username that is clickable
			String creatorFullName = StringHelper.escapeHtml(UserManager.getInstance().getUserDisplayName(creator));
			Link visitingCardLink = LinkFactory.createCustomLink("vc_"+msgCount, "vc_"+msgCount, creatorFullName, Link.LINK_CUSTOM_CSS + Link.NONTRANSLATED, mainVC, this);
			visitingCardLink.setUserObject(messageView);
			
			LinkPopupSettings settings = new LinkPopupSettings(800, 600, "_blank");
			visitingCardLink.setPopup(settings);
		}

		if(!isThreadClosed) {
			if((numOfChildren == 0 && userIsMsgCreator) || foCallback.mayDeleteMessageAsModerator()) {
				Link deleteLink = LinkFactory.createCustomLink("dl_"+msgCount, "dl_"+msgCount, "msg.delete", Link.BUTTON_SMALL, mainVC, this);
				deleteLink.setIconLeftCSS("o_icon o_icon-fw o_icon_delete_item");
				deleteLink.setUserObject(messageView);
			}
			
			if((numOfChildren == 0 && userIsMsgCreator) || foCallback.mayEditMessageAsModerator()) {
				Link editLink = LinkFactory.createCustomLink("ed_"+msgCount, "ed_"+msgCount, "msg.update", Link.BUTTON_SMALL, mainVC, this);
				editLink.setIconLeftCSS("o_icon o_icon-fw o_icon_edit");
				editLink.setUserObject(messageView);
			}
			
			if(foCallback.mayReplyMessage()) {
				Link quoteLink = LinkFactory.createCustomLink("qt_"+msgCount, "qt_"+msgCount, "msg.quote", Link.BUTTON_SMALL, mainVC, this);
				quoteLink.setElementCssClass("o_sel_forum_reply_quoted");
				quoteLink.setIconLeftCSS("o_icon o_icon-fw o_icon_reply_with_quote");
				quoteLink.setUserObject(messageView);
				
				Link replyLink = LinkFactory.createCustomLink("rp_"+msgCount, "rp_"+msgCount, "msg.reply", Link.BUTTON_SMALL, mainVC, this);
				replyLink.setElementCssClass("o_sel_forum_reply");
				replyLink.setIconLeftCSS("o_icon o_icon-fw o_icon_reply");
				replyLink.setUserObject(messageView);
			}
			
			if(foCallback.mayEditMessageAsModerator() && !threadTop) {
				Link splitLink = LinkFactory.createCustomLink("split_"+msgCount, "split_"+msgCount, "msg.split", Link.LINK, mainVC, this);
				splitLink.setIconLeftCSS("o_icon o_icon-fw o_icon_split");
				splitLink.setUserObject(messageView);
				
				Link moveLink = LinkFactory.createCustomLink("move_"+msgCount, "move_"+msgCount, "msg.move", Link.LINK, mainVC, this);
				moveLink.setIconLeftCSS("o_icon o_icon-fw o_icon_move");
				moveLink.setUserObject(messageView);
			}
		}
		
		Mark currentMark = marks.get(keyString);
		MarkResourceStat stat = stats.get(keyString);
		if(!guestOnly) {
			String businessPath = currentMark == null ?
					getWindowControl().getBusinessControl().getAsString() + "[Message:" + m.getKey() + "]"
					: currentMark.getBusinessPath();
			Controller markCtrl = markingService.getMarkController(ureq, getWindowControl(), currentMark, stat, forumOres, keyString, businessPath);
			mainVC.put("mark_" + msgCount, markCtrl.getInitialComponent());
		}
		
		if(userIsMsgCreator) {
			OLATResourceable messageOres = OresHelper.createOLATResourceableInstance("Forum", m.getKey());
			String businessPath = BusinessControlFactory.getInstance().getAsString(getWindowControl().getBusinessControl())
					+ "[Message:" + m.getKey() + "]";
			Long artefact = artefactStats.get(businessPath);
			int numOfArtefact = artefact == null ? 0 : artefact.intValue();
			Controller ePFCollCtrl = EPUIFactory
					.createArtefactCollectWizzardController(ureq, getWindowControl(), numOfArtefact, messageOres, businessPath);
			if (ePFCollCtrl != null) {
				messageView.setArtefact(ePFCollCtrl);
				mainVC.put("eportfolio_" + keyString, ePFCollCtrl.getInitialComponent());
			}
		}
	}

	@Override
	public void event(Event event) {
		if(event instanceof ForumChangedEvent) {
			ForumChangedEvent fce = (ForumChangedEvent)event;
			if(ForumChangedEvent.CHANGED_MESSAGE.equals(fce.getCommand()) || ForumChangedEvent.NEW_MESSAGE.equals(fce.getCommand())) {
				Long threadtopKey = fce.getThreadtopKey();
				Long senderId = fce.getSendByIdentityKey();
				if(thread != null && threadtopKey != null && thread.getKey().equals(threadtopKey)
						&& (senderId == null || !senderId.equals(getIdentity().getKey()))) {
					reloadList = true;
				}
			}
		}
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(backLink == source) {
			fireEvent(ureq, Event.BACK_EVENT);
		} else if(archiveThreadButton == source) {
			doArchiveThread(ureq, thread);
		} else if (closeThreadButton == source) {
			doCloseThread();
		} else if (openThreadButton == source) {
			doOpenThread();
		} else if (hideThreadButton == source) {
			doHideThread();
		} else if (showThreadButton == source) {
			doShowThread();
		} else if (allButton == source) {
			doShowAll(ureq);
		} else if (allFlatButton == source) {
			doShowAllFlat(ureq);
		}  else if (oneButton == source) {
			doShowOne(ureq);
		}  else if (markedButton == source) {
			doShowMarked();		
		}  else if (newButton == source) {
			doShowNew();		
		} else if(stickyButton == source || removeStickyButton == source) {
			doToogleSticky();
		} else if (source instanceof Link) {
			Link link = (Link)source;
			String command = link.getCommand();
			Object uobject = link.getUserObject();

			if (command.startsWith("qt_")) {
				doReply(ureq, (MessageView)uobject, true);
			} else if (command.startsWith("rp_")) {
				doReply(ureq, (MessageView)uobject, false);
			} else if (command.startsWith("dl_")) {
				doConfirmDeleteMessage(ureq, (MessageView)uobject);
			} else if (command.startsWith("ed_")) {
				doEditMessage(ureq, (MessageView)uobject);
			}	else if (command.startsWith("split_")) {
				doConfirmSplit(ureq, (MessageView)uobject);
			} else if (command.startsWith("move_")) {
				doMoveMessage(ureq, (MessageView)uobject);
			}
		} else if(mainVC == source) {
			String cmd = event.getCommand();
			if (cmd.startsWith("attachment_")) {
				doDeliverAttachment(ureq, cmd);
			}
		}
	}

	private void doDeliverAttachment(UserRequest ureq, String cmd) {
		MediaResource res = null;
		try {
			int index = cmd.lastIndexOf("_");
			String attachmentPosition = cmd.substring(cmd.indexOf("_") + 1, index);
			String messageKey = cmd.substring(index + 1);
			
			int position = Integer.parseInt(attachmentPosition);
			Long key = new Long(messageKey);
			for(MessageView view:backupViews) {
				if(view.getKey().equals(key)) {
					List<VFSItem> attachments = view.getAttachments();
					VFSLeaf attachment = (VFSLeaf)attachments.get(position - 1);//velocity counter start with 1
					VFSMediaResource fileResource = new VFSMediaResource(attachment);
					fileResource.setDownloadable(true); // prevent XSS attack
					res = fileResource;
				}
			}
		} catch (Exception e) {
			logError("Cannot deliver message attachment", e);
		}
		if(res == null) {
			res = new NotFoundMediaResource(cmd);
		}
		ureq.getDispatchResult().setResultingMediaResource(res);
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == confirmDeleteCtrl) {
			if (DialogBoxUIFactory.isYesEvent(event) || DialogBoxUIFactory.isOkEvent(event)) {
				MessageView deletedMessage = (MessageView)confirmDeleteCtrl.getUserObject();
				doDeleteMessage(deletedMessage);
				reloadModelAfterDelete(ureq, deletedMessage);
			}
		} else if(editMessageCtrl == source) {
			// edit done -> save 
			Message message = editMessageCtrl.getMessage();
			if(message != null) {
				reloadModel(ureq, message);
			} else {
				showInfo("header.cannoteditmessage");
			}
			cmc.deactivate();
		} else if(replyMessageCtrl == source) {
			Message reply = replyMessageCtrl.getMessage();
			if(reply != null) {	
				reloadModel(ureq, reply);
			} else {
			  	showInfo("header.cannotsavemessage");
			}
			cmc.deactivate();
		} else if(messageTableCtrl == source) {
			if(event instanceof SelectMessageEvent) {
				SelectMessageEvent sme = (SelectMessageEvent)event;
				doSelectTheOne(sme.getMessageKey());
			}
		} else if(moveCtrl == source) {
			if(event instanceof SelectMessageEvent) {
				SelectMessageEvent sme = (SelectMessageEvent)event;
				doFinalizeMove(ureq, moveCtrl.getSelectView(), sme.getMessageKey());
				cmc.deactivate();
			}
		} else if(confirmSplitCtrl == source) {
			if (DialogBoxUIFactory.isYesEvent(event) || DialogBoxUIFactory.isOkEvent(event)) {
				MessageView splitedMessage = (MessageView)confirmSplitCtrl.getUserObject();
				doSplitThread(ureq, splitedMessage);
			}
		} else if(source == cmc) {
			cleanUp();
		}
		super.event(ureq, source, event);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(replyMessageCtrl);
		removeAsListenerAndDispose(editMessageCtrl);
		removeAsListenerAndDispose(cmc);
		replyMessageCtrl = null;
		editMessageCtrl = null;
		cmc = null;
	}

	private void doReply(UserRequest ureq, MessageView parent, boolean quote) {
		// user has clicked on button 'reply'
		if (foCallback.mayReplyMessage()) {
			Message newMessage = forumManager.createMessage(forum, getIdentity(), guestOnly);
			Message parentMessage = forumManager.getMessageById(parent.getKey());
			
			String reString = "";
			if(parent != null && parent.isThreadTop()) {
				//add reString only for the first answer
				reString = translate("msg.title.re");
			}			
			newMessage.setTitle(reString + parentMessage.getTitle());
			if (quote) {
				// load message to form as quotation				
				StringBuilder quoteSb = new StringBuilder();
				quoteSb.append("<p></p><div class=\"o_quote_wrapper\"><div class=\"o_quote_author mceNonEditable\">");
				String date = formatter.formatDateAndTime(parentMessage.getCreationDate());
				String creatorName;
				if(StringHelper.containsNonWhitespace(parentMessage.getPseudonym())) {
					creatorName = parentMessage.getPseudonym();
				} else if(parentMessage.isGuest()) {
					creatorName = translate("guest");
				} else {
					User creator = parentMessage.getCreator().getUser();
					creatorName = creator.getProperty(UserConstants.FIRSTNAME, getLocale()) + " " + creator.getProperty(UserConstants.LASTNAME, getLocale());
				}
				
				quoteSb.append(translate("msg.quote.intro", new String[]{ date, creatorName}))
				     .append("</div><blockquote class=\"o_quote\">")
				     .append(parentMessage.getBody())
				     .append("</blockquote></div>")
				     .append("<p></p>");
				newMessage.setBody(quoteSb.toString());
			}

			replyMessageCtrl = new MessageEditController(ureq, getWindowControl(), forum, foCallback, newMessage, parentMessage, EditMode.reply);
			listenTo(replyMessageCtrl);
			
			String title = quote ? translate("msg.quote") : translate("msg.reply");
			cmc = new CloseableModalController(getWindowControl(), "close", replyMessageCtrl.getInitialComponent(), true, title);
			listenTo(cmc);
			cmc.activate();
		} else {
			showInfo("may.not.reply.msg");
		}
	}
	
	private void doConfirmDeleteMessage(UserRequest ureq, MessageView message) {
		// user has clicked on button 'delete'
		// -> display modal dialog 'Do you really want to delete this message?'
		// 'yes': back to allThreadTable, 'no' back to messageDetails
		
		int numOfChildren = forumManager.countMessageChildren(message.getKey());
		boolean children = numOfChildren > 0;
		boolean userIsMsgCreator = message.isAuthor() ;
		String currentMsgTitle = StringHelper.escapeHtml(message.getTitle());
		
		if (foCallback.mayDeleteMessageAsModerator()) {
			// user is forum-moderator -> may delete every message on every level
			if (numOfChildren == 0) {
				confirmDeleteCtrl = activateYesNoDialog(ureq, null, translate("reallydeleteleaf", currentMsgTitle), confirmDeleteCtrl);
				confirmDeleteCtrl.setUserObject(message);
			} else if (numOfChildren == 1) {
				confirmDeleteCtrl = activateYesNoDialog(ureq, null, translate("reallydeletenode1", currentMsgTitle), confirmDeleteCtrl);
				confirmDeleteCtrl.setUserObject(message);
			} else {
				confirmDeleteCtrl = activateYesNoDialog(ureq, null, getTranslator().translate("reallydeletenodeN", new String[] { currentMsgTitle, Integer.toString(numOfChildren) }), confirmDeleteCtrl);
				confirmDeleteCtrl.setUserObject(message);
			}
		} else if (userIsMsgCreator && !children ) {
			// user may delete his own message if it has no children
			confirmDeleteCtrl = activateYesNoDialog(ureq, null, translate("reallydeleteleaf", currentMsgTitle), confirmDeleteCtrl);
			confirmDeleteCtrl.setUserObject(message);
		} else if (userIsMsgCreator && children) {
			// user may not delete his own message because it has at least one child
			showWarning("may.not.delete.msg.as.author");
		} else {
			// user isn't author of the current message
			showInfo("may.not.delete.msg");
		}
	}
	
	private void doDeleteMessage(MessageView message) { 
		boolean userIsMsgCreator = message.isAuthor();
		if (foCallback.mayDeleteMessageAsModerator()
				|| (userIsMsgCreator && forumManager.countMessageChildren(message.getKey()) == 0)) {
			Message reloadedMessage = forumManager.getMessageById(message.getKey());
			if(reloadedMessage != null) {
				boolean hasParent = reloadedMessage.getParent() != null;
				forumManager.deleteMessageTree(forum.getKey(), reloadedMessage);
				showInfo("deleteok");
				// do logging
				if(hasParent) {
					ThreadLocalUserActivityLogger.log(ForumLoggingAction.FORUM_MESSAGE_DELETE, getClass(),
							LoggingResourceable.wrap(reloadedMessage));
				} else {
					ThreadLocalUserActivityLogger.log(ForumLoggingAction.FORUM_THREAD_DELETE, getClass(),
							LoggingResourceable.wrap(reloadedMessage));
				}
			}
		} else {
			showWarning("may.not.delete.msg.as.author");
		}
	}
	
	private void doEditMessage(UserRequest ureq, MessageView message) {
		// user has clicked on button 'edit'
		boolean userIsMsgCreator = message.isAuthor();
		boolean children = forumManager.countMessageChildren(message.getKey()) > 0;
		if (foCallback.mayEditMessageAsModerator() || (userIsMsgCreator && !children)) {
			Message reloadedMessage = forumManager.loadMessage(message.getKey());
			editMessageCtrl = new MessageEditController(ureq, getWindowControl(), forum, foCallback, reloadedMessage, null, EditMode.edit);
			listenTo(editMessageCtrl);
			
			String title = translate("msg.update");
			cmc = new CloseableModalController(getWindowControl(), "close", editMessageCtrl.getInitialComponent(), true, title);
			listenTo(editMessageCtrl);
			cmc.activate();
		} else if ((userIsMsgCreator) && (children == true)) {
			// user is author of the current message but it has already at least
			// one child
			showWarning("may.not.save.msg.as.author");
		} else {
			// user isn't author of the current message
			showInfo("may.not.edit.msg");
		}
	}
	
	private void doConfirmSplit(UserRequest ureq, MessageView message) {		
		if (foCallback.mayEditMessageAsModerator()) {
			// user is forum-moderator -> may delete every message on every level
			int numOfChildren = forumManager.countMessageChildren(message.getKey());
			// provide yesNoSplit as argument, this ensures that dc is disposed before newly created
			String text =  translate("reallysplitthread", new String[] { message.getTitle(), Integer.toString(numOfChildren) });
			confirmSplitCtrl = activateYesNoDialog(ureq, null, text, confirmSplitCtrl);
			confirmSplitCtrl.setUserObject(message);
		}
	}
	
	private void doSplitThread(UserRequest ureq, MessageView message) {
		if (foCallback.mayEditMessageAsModerator()) {
			Message reloadedMessage = forumManager.getMessageById(message.getKey());
			Message newTopMessage = forumManager.splitThread(reloadedMessage);
			//do logging
			ThreadLocalUserActivityLogger.log(ForumLoggingAction.FORUM_THREAD_SPLIT, getClass(), LoggingResourceable.wrap(newTopMessage));
			//open the new thread
			fireEvent(ureq, new SelectMessageEvent(SelectMessageEvent.SELECT_THREAD, newTopMessage.getKey()));
		} else {
			showWarning("may.not.split.thread");
		}
	}
	
	private void doArchiveThread(UserRequest ureq, Message currMsg) {
		Message m = currMsg.getThreadtop();
		Long topMessageId = (m == null) ? currMsg.getKey() : m.getKey();
		
		VFSContainer forumContainer = forumManager.getForumContainer(forum.getKey());
		ForumDownloadResource download = new ForumDownloadResource("Forum", forum, foCallback, topMessageId, forumContainer, getLocale());
		ureq.getDispatchResult().setResultingMediaResource(download);
	}
	
	private void doToogleSticky() {
		Status status = Status.getStatus(thread.getStatusCode());
		status.setSticky(!status.isSticky());
		thread.setStatusCode(Status.getStatusCode(status));
		thread = forumManager.updateMessage(thread, false);
		DBFactory.getInstance().commit();
		
		stickyButton.setVisible(!status.isSticky() && foCallback.mayEditMessageAsModerator());
		removeStickyButton.setVisible(status.isSticky() && foCallback.mayEditMessageAsModerator());
		mainVC.setDirty(true);
		
		ForumChangedEvent event = new ForumChangedEvent(ForumChangedEvent.STICKY, thread.getKey(), null, getIdentity());
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(event, forumOres);	
		ThreadLocalUserActivityLogger.log(ForumLoggingAction.FORUM_MESSAGE_EDIT, getClass(), LoggingResourceable.wrap(thread));
	}
	
	/**
	 * Sets the closed status to the thread message.
	 * @param ureq
	 * @param msg
	 * @param closed
	 */
	private void doCloseThread() {	
		if (thread != null) {
			thread = forumManager.getMessageById(thread.getKey());
			Status status = Status.getStatus(thread.getStatusCode());
			status.setClosed(true);
			thread.setStatusCode(Status.getStatusCode(status));
			thread = forumManager.updateMessage(thread, false);
			DBFactory.getInstance().commit();// before sending async event
			
			closeThreadButton.setVisible(false);
			openThreadButton.setVisible(true && !guestOnly);
			mainVC.setDirty(true);
			
			ForumChangedEvent event = new ForumChangedEvent(ForumChangedEvent.CLOSE, thread.getKey(), null, getIdentity());
			CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(event, forumOres);	
			ThreadLocalUserActivityLogger.log(ForumLoggingAction.FORUM_THREAD_CLOSE, getClass(), LoggingResourceable.wrap(thread));
		}
	}
	
	private void doOpenThread() {	
		if (thread != null) {
			thread = forumManager.getMessageById(thread.getKey());
			Status status = Status.getStatus(thread.getStatusCode());
			status.setClosed(false);
			thread.setStatusCode(Status.getStatusCode(status));
			thread = forumManager.updateMessage(thread, true);
			DBFactory.getInstance().commit();// before sending async event
			
			closeThreadButton.setVisible(true && !guestOnly);
			openThreadButton.setVisible(false);
			mainVC.setDirty(true);

			ForumChangedEvent event = new ForumChangedEvent(ForumChangedEvent.OPEN, thread.getKey(), null, getIdentity());
			CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(event, forumOres);	
			ThreadLocalUserActivityLogger.log(ForumLoggingAction.FORUM_THREAD_REOPEN, getClass(), LoggingResourceable.wrap(thread));
		}
	}
	
	/**
	 * Sets the hidden status to the thread message.
	 * @param ureq
	 * @param msg
	 * @param hidden
	 */
	private void doHideThread() {
		if (thread != null) {
			thread = forumManager.getMessageById(thread.getKey());
			Status status = Status.getStatus(thread.getStatusCode());
			status.setHidden(true);
			thread.setStatusCode(Status.getStatusCode(status));
			thread = forumManager.updateMessage(thread, false);
			DBFactory.getInstance().commit();// before sending async event
			
			hideThreadButton.setVisible(false);
			showThreadButton.setVisible(true && !guestOnly);
			mainVC.setDirty(true);

			ForumChangedEvent event = new ForumChangedEvent(ForumChangedEvent.HIDE, thread.getKey(), null, getIdentity());
			CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(event, forumOres);	
			ThreadLocalUserActivityLogger.log(ForumLoggingAction.FORUM_THREAD_HIDE, getClass(), LoggingResourceable.wrap(thread));
		}
	}
	
	/**
	 * Sets the hidden status to the threadtop message.
	 * @param ureq
	 * @param msg
	 * @param hidden
	 */
	private void doShowThread() {
		if (thread != null) {
			thread = forumManager.getMessageById(thread.getKey());
			Status status = Status.getStatus(thread.getStatusCode());
			status.setHidden(false);
			thread.setStatusCode(Status.getStatusCode(status));
			thread = forumManager.updateMessage(thread, true);
			DBFactory.getInstance().commit();// before sending async event
			
			hideThreadButton.setVisible(true && !guestOnly);
			showThreadButton.setVisible(false);
			mainVC.setDirty(true);

			ForumChangedEvent event = new ForumChangedEvent(ForumChangedEvent.SHOW, thread.getKey(), null, getIdentity());
			CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(event, forumOres);	
			ThreadLocalUserActivityLogger.log(ForumLoggingAction.FORUM_THREAD_SHOW, getClass(), LoggingResourceable.wrap(thread));
		}
	}
	
	private void doShowAll(UserRequest ureq) {
		if(reloadList) {
			reloadModel(ureq, null);
		}
		updateButtons(allButton);
		mainVC.contextPut("threadMode", Boolean.TRUE);
		mainVC.contextPut("messages", backupViews);
		mainVC.contextRemove("mode");
	}
	
	private void doShowAllFlat(UserRequest ureq) {
		if(reloadList) {
			reloadModel(ureq, null);
		}
		updateButtons(allFlatButton);
		mainVC.contextPut("threadMode", Boolean.FALSE);
		mainVC.contextPut("messages", backupViews);
		mainVC.contextRemove("mode");
	}
	
	private void doShowOne(UserRequest ureq) {
		if(reloadList) {
			reloadModel(ureq, null);
		}
		updateButtons(oneButton);
		mainVC.contextPut("mode", "one");
		mainVC.contextPut("threadMode", Boolean.FALSE);
		
		if(backupViews != null && backupViews.size() > 0) {
			List<MessageView> oneView = new ArrayList<>(1);
			oneView.add(backupViews.get(0));
			mainVC.contextPut("messages", oneView);
			messageTableCtrl.setSelectView(oneView.get(0));
			messageTableCtrl.loadMessages(new ArrayList<>(backupViews));
		}
	}
	
	private void doSelectTheOne(Long messageKey) {
		updateButtons(oneButton);
		mainVC.contextPut("mode", "one");
		mainVC.contextPut("threadMode", Boolean.FALSE);
		
		if(backupViews != null && backupViews.size() > 0) {
			List<MessageView> oneView = new ArrayList<>(1);
			for(MessageView message:backupViews) {
				if(message.getKey().equals(messageKey)) {
					oneView.add(message);
				}
			}
			mainVC.contextPut("messages", oneView);
			messageTableCtrl.setSelectView(oneView.get(0));
			messageTableCtrl.loadMessages(new ArrayList<>(backupViews));
		}
	}
	
	protected void doShowMarked() {
		updateButtons(markedButton);
		mainVC.contextPut("threadMode", Boolean.FALSE);
		mainVC.contextPut("mode", "marked");
		
		List<Mark> markList = markingService.getMarkManager().getMarks(forumOres, getIdentity(), null);
		Set<String> marks = new HashSet<>(markList.size() * 2 + 1);
		for(Mark mark:markList) {
			marks.add(mark.getResSubPath());
		}

		List<MessageView> views = new ArrayList<>();
		for(MessageView view:backupViews) {
			if(marks.contains(view.getKey().toString())) {
				views.add(view);
			}
		}
		
		mainVC.contextPut("messages", views);
	}
	
	protected void doShowNew() {
		updateButtons(newButton);
		mainVC.contextPut("threadMode", Boolean.FALSE);
		mainVC.contextPut("mode", "new");
		
		Set<Long> rms = forumManager.getReadSet(getIdentity(), forum);
		List<MessageView> views = new ArrayList<>();
		for(MessageView view:backupViews) {
			if(!rms.contains(view.getKey())) {
				views.add(view);
			}
		}
		mainVC.contextPut("messages", views);
	}
	
	private void doMoveMessage(UserRequest ureq, MessageView message) {
		removeAsListenerAndDispose(moveCtrl);
		removeAsListenerAndDispose(cmc);
		
		if (foCallback.mayEditMessageAsModerator()) {
			moveCtrl = new ForumMessageListController(ureq, getWindowControl(), forum, true);
			moveCtrl.loadAllMessages();
			moveCtrl.setSelectView(message);
			listenTo(moveCtrl);

			//push the modal dialog with the table as content
			String title = "";
			cmc = new CloseableModalController(getWindowControl(), "close", moveCtrl.getInitialComponent(), true, title);
			listenTo(cmc);
			
			cmc.activate();
		}
	}
	
	private void doFinalizeMove(UserRequest ureq, MessageView messageToMove, Long parentMessageKey) {
		if (foCallback.mayEditMessageAsModerator()) {
			Message message = forumManager.getMessageById(messageToMove.getKey());
			Message parentMessage = forumManager.getMessageById(parentMessageKey);
			message = forumManager.moveMessage(message, parentMessage);
			markRead(message);
			
			ThreadLocalUserActivityLogger.log(ForumLoggingAction.FORUM_MESSAGE_MOVE, getClass(), LoggingResourceable.wrap(message));
			Long threadKey = parentMessage.getThreadtop() == null ? parentMessage.getKey() : parentMessage.getThreadtop().getKey();
			fireEvent(ureq, new SelectMessageEvent(SelectMessageEvent.SELECT_THREAD, threadKey, message.getKey()));
		} else {
			showWarning("may.not.move.message");
		}
	}
	
	public enum LoadMode {
		thread,
		userMessages,
		userMessagesUnderPseudo,
		guestMessages,
	}
	
	private class AttachmentsMapper implements Mapper {
		
		@Override
		public MediaResource handle(String relPath, HttpServletRequest request) {
			String[] query = relPath.split("/"); // expected path looks like this /messageId/attachmentUUID/filename
			if (query.length == 4) {
				try {
					Long mId = Long.valueOf(Long.parseLong(query[1]));
					MessageView view = null;
					for (MessageView m : backupViews) {
						// search for message in current message map
						if (m.getKey().equals(mId)) {
							view = m;
							break;
						}
					}
					if (view != null) {
						List<VFSItem> attachments = view.getAttachments();
						for (VFSItem vfsItem : attachments) {
							MetaInfo meta = ((MetaTagged)vfsItem).getMetaInfo();
							if (meta.getUUID().equals(query[2])) {
								if (meta.isThumbnailAvailable()) {
									VFSLeaf thumb = meta.getThumbnail(200, 200, false);
									if(thumb != null) {
										// Positive lookup, send as response
										return new VFSMediaResource(thumb);
									}
								}
								break;
							}
						}
					}
				} catch (NumberFormatException e) {
					//
				}
			}
			// In any error case, send not found
			return new NotFoundMediaResource(request.getRequestURI());
		}
	}
}
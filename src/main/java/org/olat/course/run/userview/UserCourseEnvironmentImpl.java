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
*/

package org.olat.course.run.userview;

import java.util.Collections;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.PersistenceHelper;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.IdentityEnvironment;
import org.olat.course.assessment.manager.EfficiencyStatementManager;
import org.olat.course.certificate.CertificatesManager;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.scoring.ScoreAccounting;
import org.olat.group.BusinessGroup;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.model.RepositoryEntryLifecycle;

/**
 * Initial Date:  Feb 6, 2004
 * @author Felix Jost
 *
 */
public class UserCourseEnvironmentImpl implements UserCourseEnvironment {
	private final IdentityEnvironment identityEnvironment;
	private final CourseEnvironment courseEnvironment;
	private ConditionInterpreter conditionInterpreter;
	private ScoreAccounting scoreAccounting;
	private RepositoryEntryLifecycle lifecycle;
	private RepositoryEntry courseRepoEntry;
	private List<BusinessGroup> coachedGroups;
	private List<BusinessGroup> participatingGroups;
	private List<BusinessGroup> waitingLists;
	
	private final WindowControl windowControl;
	
	private Boolean admin, coach, participant;
	private Boolean adminAnyCourse, coachAnyCourse, participantAnyCourse;
	
	private Boolean certification;
	private Boolean courseReadOnly;
	
	public UserCourseEnvironmentImpl(IdentityEnvironment identityEnvironment, CourseEnvironment courseEnvironment) {
		this(identityEnvironment, courseEnvironment, null, null, null, null, null, null, null, null);
		if(courseEnvironment != null) {
			courseReadOnly = courseEnvironment.getCourseGroupManager().getCourseEntry().getRepositoryEntryStatus().isClosed();
		}
	}
	
	public UserCourseEnvironmentImpl(IdentityEnvironment identityEnvironment, CourseEnvironment courseEnvironment, Boolean courseReadOnly) {
		this(identityEnvironment, courseEnvironment, null, null, null, null, null, null, null, courseReadOnly);
	}
	
	public UserCourseEnvironmentImpl(IdentityEnvironment identityEnvironment, CourseEnvironment courseEnvironment, WindowControl windowControl,
			List<BusinessGroup> coachedGroups, List<BusinessGroup> participatingGroups, List<BusinessGroup> waitingLists,
			Boolean coach, Boolean admin, Boolean participant, Boolean courseReadOnly) {
		this.courseEnvironment = courseEnvironment;
		this.identityEnvironment = identityEnvironment;
		this.scoreAccounting = new ScoreAccounting(this);
		this.conditionInterpreter = new ConditionInterpreter(this);
		this.coachedGroups = coachedGroups;
		this.participatingGroups = participatingGroups;
		this.waitingLists = waitingLists;
		this.coach = coach;
		this.admin = admin;
		this.participant = participant;
		this.windowControl = windowControl;
		this.courseReadOnly = courseReadOnly;
	}

	/**
	 * @return Returns the courseEnvironment.
	 */
	@Override
	public CourseEnvironment getCourseEnvironment() {
		return courseEnvironment;
	}

	@Override
	public IdentityEnvironment getIdentityEnvironment() {
		return identityEnvironment;
	}

	@Override
	public WindowControl getWindowControl() {
		return windowControl;
	}

	@Override
	public ConditionInterpreter getConditionInterpreter() {
		return conditionInterpreter;
	}

	@Override
	public ScoreAccounting getScoreAccounting() {
		return scoreAccounting;
	}

	@Override
	public CourseEditorEnv getCourseEditorEnv() {
		// return null signalling this is real user environment
		return null;
	}
	
	@Override
	public boolean isIdentityInCourseGroup(Long groupKey) {
		if(coachedGroups != null && participatingGroups != null) {
			return PersistenceHelper.listContainsObjectByKey(participatingGroups, groupKey)
					|| PersistenceHelper.listContainsObjectByKey(coachedGroups, groupKey);
		}
		CourseGroupManager cgm = courseEnvironment.getCourseGroupManager();
		return cgm.isIdentityInGroup(identityEnvironment.getIdentity(), groupKey);
	}

	@Override
	public boolean isCoach() {
		if(coach != null) {
			return coach.booleanValue();
		}
		//lazy loading
		CourseGroupManager cgm = courseEnvironment.getCourseGroupManager();
		boolean coachLazy = cgm.isIdentityCourseCoach(identityEnvironment.getIdentity());
		coach = new Boolean(coachLazy);
		return coachLazy;
	}

	@Override
	public boolean isAdmin() {
		if(admin != null) {
			return admin.booleanValue();
		}
		//lazy loading
		CourseGroupManager cgm = courseEnvironment.getCourseGroupManager();
		boolean admiLazy = cgm.isIdentityCourseAdministrator(identityEnvironment.getIdentity());
		admin = new Boolean(admiLazy);
		return admiLazy;
	}

	@Override
	public boolean isParticipant() {
		if(participant != null) {
			return participant.booleanValue();
		}
		//lazy loading
		CourseGroupManager cgm = courseEnvironment.getCourseGroupManager();
		boolean partLazy = cgm.isIdentityCourseParticipant(identityEnvironment.getIdentity());
		participant = new Boolean(partLazy);
		return partLazy;
	}

	@Override
	public boolean isAdminOfAnyCourse() {
		if(adminAnyCourse != null) {
			return adminAnyCourse.booleanValue();
		}

		CourseGroupManager cgm = courseEnvironment.getCourseGroupManager();
		boolean adminLazy = identityEnvironment.getRoles().isOLATAdmin()
				|| identityEnvironment.getRoles().isInstitutionalResourceManager()
				|| cgm.isIdentityAnyCourseAdministrator(identityEnvironment.getIdentity());
		adminAnyCourse = new Boolean(adminLazy);
		return adminLazy;
	}

	@Override
	public boolean isCoachOfAnyCourse() {
		if(coachAnyCourse != null) {
			return coachAnyCourse.booleanValue();
		}

		CourseGroupManager cgm = courseEnvironment.getCourseGroupManager();
		boolean coachLazy = cgm.isIdentityAnyCourseCoach(identityEnvironment.getIdentity());
		coachAnyCourse = new Boolean(coachLazy);
		return coachLazy;
	}

	@Override
	public boolean isParticipantOfAnyCourse() {
		if(participantAnyCourse != null) {
			return participantAnyCourse.booleanValue();
		}

		CourseGroupManager cgm = courseEnvironment.getCourseGroupManager();
		boolean participantLazy = cgm.isIdentityAnyCourseParticipant(identityEnvironment.getIdentity());
		participantAnyCourse = new Boolean(participantLazy);
		return participantLazy;
	}

	@Override
	public RepositoryEntryLifecycle getLifecycle() {
		if(lifecycle == null) {
			RepositoryEntry re = getCourseRepositoryEntry();
			if(re != null) {
				lifecycle = re.getLifecycle();
			}
		}
		return lifecycle;
	}

	public RepositoryEntry getCourseRepositoryEntry() {
		if(courseRepoEntry == null) {
			courseRepoEntry = courseEnvironment.getCourseGroupManager().getCourseEntry();
		}
		return courseRepoEntry;
	}
	
	public int sizeCoachedGroups() {
		return coachedGroups == null ? 0 : coachedGroups.size();
	}

	public List<BusinessGroup> getCoachedGroups() {
		if(coachedGroups == null) {
			return Collections.emptyList();
		}
		return coachedGroups;
	}

	public List<BusinessGroup> getParticipatingGroups() {
		if(participatingGroups == null) {
			return Collections.emptyList();
		}
		return participatingGroups;
	}

	public List<BusinessGroup> getWaitingLists() {
		if(waitingLists == null) {
			return Collections.emptyList();
		}
		return waitingLists;
	}
	
	@Override
	public boolean isCourseReadOnly() {
		return courseReadOnly == null ? false : courseReadOnly.booleanValue();
	}
	
	public void setCourseReadOnly(Boolean courseReadOnly) {
		this.courseReadOnly = courseReadOnly;
	}
	
	@Override
	public boolean hasEfficiencyStatementOrCertificate(boolean update) {
		if(certification == null || update) {
			EfficiencyStatementManager efficiencyStatementManager = CoreSpringFactory.getImpl(EfficiencyStatementManager.class);
			boolean hasStatement = efficiencyStatementManager
					.hasUserEfficiencyStatement(getCourseRepositoryEntry().getKey(), identityEnvironment.getIdentity());
			if(hasStatement) {
				certification = Boolean.TRUE;
			} else {
				CertificatesManager certificatesManager = CoreSpringFactory.getImpl(CertificatesManager.class);
				certification = certificatesManager
						.hasCertificate(identityEnvironment.getIdentity(), getCourseRepositoryEntry().getOlatResource().getKey());
			} 
		}
		return certification.booleanValue();
	}

	public void setGroupMemberships(List<BusinessGroup> coachedGroups,
			List<BusinessGroup> participatingGroups, List<BusinessGroup> waitingLists) {
		this.coachedGroups = coachedGroups;
		this.participatingGroups = participatingGroups;
		this.waitingLists = waitingLists;
	}
	
	public void setUserRoles(boolean admin, boolean coach, boolean participant) {
		this.admin = new Boolean(admin);
		this.coach = new Boolean(coach);
		this.participant = new Boolean(participant);
	}
}

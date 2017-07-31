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
package org.olat.shibboleth.manager;

import java.util.Map;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.CoreSpringFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.shibboleth.ShibbolethDispatcher;
import org.olat.shibboleth.ShibbolethManager;
import org.olat.shibboleth.ShibbolethModule;
import org.olat.shibboleth.handler.ShibbolethAttributeHandler;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * Initial date: 19.07.2017<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
@Service
public class ShibbolethManagerImpl implements ShibbolethManager {

	private static final OLog log = Tracing.createLoggerFor(ShibbolethManagerImpl.class);

	private BaseSecurity securityManager;

	@Autowired
	private ShibbolethModule shibbolethModule;
	@Autowired
	private UserManager userManager;

	public ShibbolethManagerImpl() {
		securityManager = BaseSecurityManager.getInstance();
	}

	@Override
	public Identity createAndPersistUser(String username, String shibbolethUniqueID, String language, Map<String, String> shibbolethAttributes) {
		if (shibbolethAttributes == null) return null;

		User user = userManager.createUser(null, null, null);
		user = syncAttributes(user, shibbolethAttributes);
		user.getPreferences().setLanguage(language);
		Identity identity = securityManager.createAndPersistIdentityAndUser(username, null, user,
				ShibbolethDispatcher.PROVIDER_SHIB, shibbolethUniqueID);

		SecurityGroup olatUserGroup = securityManager.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
		securityManager.addIdentityToSecurityGroup(identity, olatUserGroup);

		return identity;
	}

	@Override
	public void syncUser(Identity identity, Map<String, String> shibbolethAttributes) {
		if (identity == null || shibbolethAttributes == null) {
			return;
		}

		User user = identity.getUser();
		user = syncAttributes(user, shibbolethAttributes);
		userManager.updateUser(user);
	}

	@Override
	public User syncAttributes(User user, Map<String, String> shibbolethAttributes) {
		if (user == null) return null;
		if (shibbolethAttributes == null) return user;

		for (Map.Entry<String, String> mapping : shibbolethModule.getUserMapping().entrySet()) {
			String attributeName = mapping.getKey();
			String attributeValue = shibbolethAttributes.get(attributeName);
			attributeValue = parseShibbolethAttribute(attributeName, attributeValue);
			user.setProperty(mapping.getValue(), attributeValue);
		}

		return user;
	}

	@Override
	public String parseShibbolethAttribute(String attributeName, String attributeValue) {
		String parsedArrtibuteValue = attributeValue;

		String shibbolethAttributeHandlerName =
				shibbolethModule.getShibbolethAttributeHandlerName(attributeName);
		try {
			ShibbolethAttributeHandler shibbolethAttributeHandler =
					(ShibbolethAttributeHandler) CoreSpringFactory.getBean(shibbolethAttributeHandlerName);
			 parsedArrtibuteValue = shibbolethAttributeHandler.parse(attributeValue);
		} catch (Exception e) {
			log.error("Not able to parse the Shibboleth attribute. ShibbolethAttributeHandler '"
					+ shibbolethAttributeHandlerName + "' does not exist.");
		}

		return parsedArrtibuteValue;
	}

}
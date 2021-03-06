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
package org.olat.core.commons.modules.bc.meta;

import java.io.File;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.FolderLicenseHandler;
import org.olat.core.commons.services.license.License;
import org.olat.core.commons.services.license.LicenseHandler;
import org.olat.core.commons.services.license.LicenseService;
import org.olat.core.commons.services.license.LicenseType;
import org.olat.core.commons.services.thumbnail.ThumbnailService;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.OlatRelPathImpl;


public class MetaInfoFactory {
	
	private static final OLog log = Tracing.createLoggerFor(MetaInfoFactory.class);
	
	private ThumbnailService thumbnailService;
	
	/**
	 * [spring]
	 * @param thumbnailService
	 */
	public void setThumbnailService(ThumbnailService thumbnailService) {
		this.thumbnailService = thumbnailService;
	}

	public MetaInfo createMetaInfoFor(OlatRelPathImpl path) {
		File originFile = getOriginFile(path);
		if(originFile == null) {
			return null;
		}
		String canonicalMetaPath = getCanonicalMetaPath(originFile, path);
		if (canonicalMetaPath == null) {
			return null;
		}
		
		File metaFile = new File(canonicalMetaPath);
		MetaInfoFileImpl meta = new MetaInfoFileImpl(canonicalMetaPath, metaFile, originFile);
		meta.setThumbnailService(thumbnailService);
		return meta;
	}
	
	public void resetThumbnails(File metafile) {
		try {
			new MetaInfoFileImpl(metafile).clearThumbnails();
		} catch (Exception e) {
			log.error("", e);
		}
	}
	
	protected static String getCanonicalMetaPath(OlatRelPathImpl olatRelPathImpl) {
		File f = getOriginFile(olatRelPathImpl);
		return getCanonicalMetaPath(f, olatRelPathImpl);
	}
	
	private static String getCanonicalMetaPath(File originFile, OlatRelPathImpl olatRelPathImpl) {
		String canonicalMetaPath;
		if (originFile == null || !originFile.exists()) {
			canonicalMetaPath = null;
		} else if (originFile.isDirectory()) {
			canonicalMetaPath = FolderConfig.getCanonicalMetaRoot() + olatRelPathImpl.getRelPath() + "/.xml";
		} else {
			canonicalMetaPath = FolderConfig.getCanonicalMetaRoot() + olatRelPathImpl.getRelPath() + ".xml";
		}
		return canonicalMetaPath;
	}
	
	protected static File getOriginFile(OlatRelPathImpl olatRelPathImpl) {
		return new File(FolderConfig.getCanonicalRoot() + olatRelPathImpl.getRelPath());
	}
	
	/**
	 * Get the license of the MetaInfo
	 *
	 * @param meta
	 * @return the license or null if no license is stored in the MetaInfo
	 */
	public License getLicense(MetaInfo meta) {
		LicenseService licenseService = CoreSpringFactory.getImpl(LicenseService.class);
		License license = null;
		boolean hasLicense = meta != null && StringHelper.containsNonWhitespace(meta.getLicenseTypeName());
		if (hasLicense) { 
			String licenseTypeName = meta.getLicenseTypeName();
			LicenseType licenseType = licenseService.loadLicenseTypeByName(licenseTypeName);
			if (licenseType == null) {
				licenseType = licenseService.createLicenseType(licenseTypeName);
				licenseType.setText(meta.getLicenseText());
				licenseService.saveLicenseType(licenseType);
			}
			license = licenseService.createLicense(licenseType);
			license.setLicensor(meta.getLicensor());
			if (licenseService.isFreetext(licenseType)) {
				license.setFreetext(meta.getLicenseText());
			}
		}
		return license;
	}
	
	/**
	 * Get the license of the MetaInfo or create a new default license:
	 *
	 * @param meta
	 * @param itentity the current user
	 * @return
	 */
	public License getOrCreateLicense(MetaInfo meta, Identity itentity) {
		LicenseHandler licenseHandler = CoreSpringFactory.getImpl(FolderLicenseHandler.class);
		LicenseService licenseService = CoreSpringFactory.getImpl(LicenseService.class);
		License license = getLicense(meta);
		if (license == null) {
			license = licenseService.createDefaultLicense(licenseHandler, itentity);
		}
		return license;
	}
}
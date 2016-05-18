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
package org.olat.modules.video.manager;

import java.util.Date;
import java.util.List;

import org.olat.core.commons.persistence.DB;
import org.olat.modules.video.VideoTranscoding;
import org.olat.modules.video.model.VideoTranscodingImpl;
import org.olat.resource.OLATResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * DAO implementation for manipulating VideoTranscoding objects
 * 
 * Initial date: 05.05.2016<br>
 * 
 * @author gnaegi, gnaegi@frentix.com, http://www.frentix.com
 *
 */
@Service("videoTranscodingDao")
public class VideoTranscodingDAO {

	@Autowired
	private DB dbInstance;

	/**
	 * Factory method to create and persist new video transcoding objects for a
	 * given video resource
	 * 
	 * @param videoResource
	 * @param resolution
	 * @param format
	 * @return
	 */
	VideoTranscoding createVideoTranscoding(OLATResource videoResource, int resolution, String format) {
		VideoTranscodingImpl videoTranscoding = new VideoTranscodingImpl();
		videoTranscoding.setCreationDate(new Date());
		videoTranscoding.setLastModified(videoTranscoding.getCreationDate());
		videoTranscoding.setVideoResource(videoResource);
		videoTranscoding.setResolution(resolution);
		videoTranscoding.setFormat(format);
		videoTranscoding.setStatus(VideoTranscoding.TRANSCODING_STATUS_WAITING);
		dbInstance.getCurrentEntityManager().persist(videoTranscoding);
		return videoTranscoding;
	}

	/**
	 * Merge updated video transcoding, persist on DB
	 * 
	 * @param videoTranscoding
	 * @return Updated transcoding object
	 */
	VideoTranscoding updateTranscoding(VideoTranscoding videoTranscoding) {
		((VideoTranscodingImpl) videoTranscoding).setLastModified(new Date());
		VideoTranscoding trans = dbInstance.getCurrentEntityManager().merge(videoTranscoding);
		return trans;
	}

	/**
	 * Delete all video transcoding objects for a given video resource
	 * 
	 * @param videoResource
	 * @return
	 */
	int deleteVideoTranscodings(OLATResource videoResource) {
		String deleteQuery = "delete from videotranscoding where fk_resource_id=:resourceKey";
		return dbInstance.getCurrentEntityManager().createQuery(deleteQuery)
				.setParameter("resourceKey", videoResource.getKey()).executeUpdate();
	}

	/**
	 * Delete a specifig video transcoding version
	 * 
	 * @param videoTranscoding
	 */
	void deleteVideoTranscoding(VideoTranscoding videoTranscoding) {
		dbInstance.getCurrentEntityManager().remove(videoTranscoding);
	}

	/**
	 * Get all video transcodings for a specific video resource, sorted by
	 * resolution, highes resolution first
	 * 
	 * @param videoResource
	 * @return
	 */
	List<VideoTranscoding> getVideoTranscodings(OLATResource videoResource) {
		StringBuilder sb = new StringBuilder();
		sb.append("select trans from videotranscoding as trans")
			.append(" inner join fetch trans.videoResource as res")
			.append(" where res.key=:resourceKey")
			.append(" order by trans.resolution desc");
		return dbInstance.getCurrentEntityManager().createQuery(sb.toString(), VideoTranscoding.class)
				.setParameter("resourceKey", videoResource.getKey()).getResultList();
	}

	/**
	 * Get all video transcodings which are waiting for transcoding or are
	 * currently in transcoding in FIFO ordering
	 * 
	 * @return
	 */
	List<VideoTranscoding> getVideoTranscodingsPendingAndInProgress() {
		StringBuilder sb = new StringBuilder();
			sb.append("select trans from videotranscoding as trans")
			.append(" inner join fetch trans.videoResource as res")
			.append(" where trans.status != 100")
			.append(" order by trans.creationDate asc, trans.id asc");
		return dbInstance.getCurrentEntityManager().createQuery(sb.toString(), VideoTranscoding.class).getResultList();
	}

}
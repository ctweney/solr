/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.sakaiproject.nakamura.solr;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.ActivityManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.solr.IndexingHandler;
import org.sakaiproject.nakamura.api.solr.RepositorySession;
import org.sakaiproject.nakamura.api.solr.ResourceIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true)
@Service(value = ResourceIndexingService.class)
@Properties( value={@Property(name="type", value="sparse-activity" )})
public class ActivityIndexingServiceImpl extends SparseIndexingServiceImpl {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActivityIndexingServiceImpl.class);

  @Override
  protected boolean ignore(String path) {
    return super.ignore(path) || !(path.startsWith("act:"));
  }

  @Override
  protected IndexingHandler getHandler(RepositorySession repositorySession, String path) {
    org.sakaiproject.nakamura.api.lite.Session sparseSession = repositorySession
        .adaptTo(org.sakaiproject.nakamura.api.lite.Session.class);

    while (path != null) {
      if (!ignoreCache.containsKey(path)) {
        try {
          if (sparseSession != null) {
            ActivityManager activityManager = sparseSession.getActivityManager();
            Content c = activityManager.get(path);
            LOGGER.debug("Checking Activity Content Item at {} got {} ", path, c);
            if (c != null) {
              if (c.hasProperty("sling:resourceType")) {
                String resourceType = (String) c.getProperty("sling:resourceType");
                IndexingHandler handler = indexers.get(resourceType);
                if (handler != null) {
                  LOGGER.debug("Handler of type {} found {} for {} from {} ", new Object[]{
                      resourceType, handler, path, indexers});
                  return handler;
                } else {
                  LOGGER.debug("Ignored {} no handler for {} ", path, resourceType);
                  ignoreCache.put(path, path);
                }
              } else {
                LOGGER.debug("Ignored {} no resource type ", path);
              }
            }
          }
        } catch (StorageClientException e) {
          LOGGER.debug(e.getMessage(), e);
        } catch (AccessDeniedException e) {
          LOGGER.debug(e.getMessage(), e);
        }
      }
      if (StorageClientUtils.isRoot(path)) {
        break;
      }
      path = Utils.getParentPath(path);
    }
    return defaultHandler;
  }

}

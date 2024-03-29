/*
 * Copyright 2010-2011 Jasha Joachimsthal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.blog.components;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.onehippo.forge.blog.beans.Blogpost;
import org.onehippo.forge.blog.hstextensions.ContentRewriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HST component that queries the repository for the newest {@link org.onehippo.forge.blog.beans.Blogpost} based on the date
 *  Needs HST parameter {@literal blogFolder} that contains the path to the blogposts relative from the site root.
 *
 * @author Jasha Joachimsthal
 */
public class LatestBlog extends BaseSiteComponent {

    private static final Logger log = LoggerFactory.getLogger(LatestBlog.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        HippoBean latestBlog = getLatestBlog(request);

        if (latestBlog == null) {
            log.debug("No blogposts");
            return;
        }
        request.setAttribute("document", latestBlog);
        request.setAttribute("contentrewriter", new ContentRewriterImpl());
    }

    private HippoBean getLatestBlog(HstRequest request) {
        String blogFolder = getParameter("blogFolder", request);
        if (StringUtils.isBlank(blogFolder)) {
            log.warn("No path configured for parameter 'blogFolder' in HST config. Will not execute query");
            return null;
        }

        HippoBean blogsFolderBean = getSiteContentBaseBean(request).getBean(blogFolder, HippoFolderBean.class);
        if(blogsFolderBean ==null){
            log.error("Blog folder bean was null");
            return null;
        }
        try {
            HstQuery hstQuery = getQueryManager().createQuery(blogsFolderBean, Blogpost.class);
            hstQuery.addOrderByDescending("blog:date");
            hstQuery.setLimit(1);

            HstQueryResult result = hstQuery.execute();

            if (result == null || result.getSize() == 0) {
                return null;
            }

            HippoBeanIterator beanIterator = result.getHippoBeans();
            while (beanIterator.hasNext()) {
                HippoBean bean = beanIterator.next();
                if (bean != null) {
                    return bean;
                }
            }
        } catch (QueryException e) {
            log.warn("Could not execute query to fetch latest blogpost", e);
        }
        return null;
    }
}

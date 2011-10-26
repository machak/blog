/*
 * Copyright 2010 Jasha Joachimsthal
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
package org.onehippo.forge.blog.beans;

import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * Annotated bean for {@link Node} of type {@link BeanConstants#DOCTYPE_COMMENT}
 * @author Jasha Joachimsthal
 *
 */
@Node(jcrType = BeanConstants.DOCTYPE_COMMENT)
public class CommentBean extends TextDocument {
    private static final String COMMENT = "comment";
    private String person;
    private String email;
    private String website;
    private String summary;

    @Override
    public String getType() {
        return COMMENT;
    }

    @Override
    public String getSummary() {
        return summary == null ? (String) getProperty(BeanConstants.PROP_SUMMARY) : summary;
    }

    @Override
    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPerson() {
        return person == null ? getHtmlEscapedProperty("blog:name") : person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public String getEmail() {
        return email == null ? getHtmlEscapedProperty("blog:email") : email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website == null ? getHtmlEscapedProperty("blog:website") : website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getBloggerIdRef() {
        return getProperty("blog:bloggeridref");
    }

    private String commentToUuidOfHandle;

    public void setCommentTo(String commentToUuidOfHandle) {
        this.commentToUuidOfHandle = commentToUuidOfHandle;
    }

    public BaseDocument getCommentTo() {
        HippoBean bean = getBean("blog:commentlink");
        if (!(bean instanceof CommentLinkBean)) {
            return null;
        }
        CommentLinkBean commentLinkBean = (CommentLinkBean) bean;
        HippoBean b = commentLinkBean.getReferencedBean();
        if (!(b instanceof BaseDocument)) {
            return null;
        }
        return (BaseDocument) b;
    }

    public boolean bind(Object content, javax.jcr.Node node) throws ContentNodeBindingException {
        super.bind(content, node);
        try {
            BaseDocument bean = (BaseDocument) content;
            node.setProperty(BeanConstants.PROP_DATE, bean.getCalendar());
            node.setProperty("blog:name", getPerson());
            node.setProperty("blog:email", getEmail());
            node.setProperty("blog:website", getWebsite());
            node.setProperty(BeanConstants.PROP_SUMMARY, getSummary());

            javax.jcr.Node commentLink;
            if (node.hasNode("blog:commentlink")) {
                commentLink = node.getNode("blog:commentlink");
            } else {
                commentLink = node.addNode("blog:commentlink", "blog:commentlink");
            }
            commentLink.setProperty("hippo:docbase", commentToUuidOfHandle);
            commentLink.setProperty("hippo:values", new String[0]);
            commentLink.setProperty("hippo:modes", new String[0]);
            commentLink.setProperty("hippo:facets", new String[0]);

        } catch (Exception e) {
            throw new ContentNodeBindingException(e);
        }
        return true;
    }

}

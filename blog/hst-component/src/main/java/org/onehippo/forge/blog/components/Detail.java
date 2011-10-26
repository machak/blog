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
package org.onehippo.forge.blog.components;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowCallbackHandler;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.utils.BeanUtils;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.onehippo.forge.blog.beans.BeanConstants;
import org.onehippo.forge.blog.beans.Blogpost;
import org.onehippo.forge.blog.beans.CommentBean;
import org.onehippo.forge.blog.hstextensions.ContentRewriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.akismet.Akismet;

/**
 * <p>HST Component for displaying Detail content. Handles adding user generated comments to {@link org.onehippo.forge.blog.beans.Blogpost}.</p>
 * <p>For mail notification of new {@link org.onehippo.forge.blog.beans.CommentBean} the following component parameters are expected:</p>
 * <dl>
 * <dt>mail.smtp.host</dt><dd>SMTP host, default is localhost</dd>
 * <dt>sender.email</dt><dd>Email address as sender of the notification mails. Default is noreply@example.com</dd>
 * <dt>receiver.email</dt><dd><strong>Required</strong> email address for the receiver of the notification mails.</dd>
 * </dl>
 * <p>For comment spam check service (Akismet), the following component parameters are expected:</p>
 * <dl>
 * <dt>spamfilter.apikey</dt><dd>API key for the spam check service</dd>
 * <dt>spamfilter.websiteurl</dt><dd>Full URL of the homepage, e.g. http://www.example.com/</dd>
 * </dl>
 *
 * @author Jasha Joachimsthal
 */
public class Detail extends BaseSiteComponent {

    protected static final String PARAM_SPAMFILTER_WEBSITEURL = "spamfilter.websiteurl";
    protected static final String PARAM_SPAMFILTER_APIKEY = "spamfilter.apikey";
    private static final String COMMENT_FOLDER = "/comment/";
    private static final String COMMENT_NODENAME_PREFIX = "comment-for-";
    protected static final String PARAM_WEBSITE = "website";
    protected static final String PARAM_EMAIL = "email";
    protected static final String PARAM_PERSON = "person";
    protected static final String PARAM_COMMENT = "comment";
    private static final String REFERER = "Referer";
    private static final String USER_AGENT = "User-Agent";
    private static final String DEFAULT_SENDER_MAIL = "noreply@example.com";
    private static final String LOCALHOST = "localhost";
    private static final String YYYY_MM_DD = "yyyy/MM/dd";
    private static final String HTML_BR = "<br />";
    private static final String LINE_END = "\r\n";
    private static final String REGEX_HTML_TAG = "\\<.*?\\>";
    protected static final String ENABLE_COMMENTS = "enableComments";
    private static final int MAX_TITLE_LENGTH = 30;
    public static final Logger log = LoggerFactory.getLogger(Detail.class);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(REGEX_HTML_TAG);
    private static final Pattern BR_TAG = Pattern.compile(LINE_END);

    /**
     * Gets {@link org.hippoecm.hst.content.beans.standard.HippoDocumentBean} for this request. If it's a {@link org.onehippo.forge.blog.beans.Blogpost}, also fetch comments
     */
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

        super.doBeforeRender(request, response);
        HippoBean documentBean = getContentBean(request);

        if (documentBean == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        request.setAttribute("document", documentBean);
        request.setAttribute("contentrewriter", new ContentRewriterImpl());

        if (documentBean instanceof Blogpost) {
            final HippoDocumentBean blogpost = (HippoDocumentBean) documentBean;
            findComments(request, blogpost);
            checkCommentsEnabled(request);
        }

    }

    /*
     * (non-Javadoc)
     * @see org.hippoecm.hst.core.component.GenericHstComponent#doAction(org.hippoecm.hst.core.component.HstRequest, org.hippoecm.hst.core.component.HstResponse)
     */
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        String type = request.getParameter("type");

        if ("add".equals(type)) {
            doAddComment(request, response);
        }
    }

    /**
     * Queries the repository for incoming beans for the current {@link org.onehippo.forge.blog.beans.Blogpost}.
     * Adds the comments to the {@link org.hippoecm.hst.core.component.HstRequest}
     *
     * @param request  {@link org.hippoecm.hst.core.component.HstRequest}
     * @param blogpost {@link org.onehippo.forge.blog.beans.Blogpost}
     */
    protected void findComments(HstRequest request, final HippoDocumentBean blogpost) {
        try {
            HstQuery commentQuery = BeanUtils.createIncomingBeansQuery(blogpost, this.getSiteContentBaseBean(request),
                    "blog:commentlink/@hippo:docbase", this, CommentBean.class, false);
            commentQuery.addOrderByDescending(BeanConstants.PROP_DATE);

            List<CommentBean> comments = BeanUtils.getIncomingBeans(commentQuery, CommentBean.class);

            request.setAttribute("comments", comments);
        } catch (QueryException e) {
            log.warn("Error finding blog comments", e);
        }
    }

    /**
     * Checks if comments are enabled and adds its result to the request
     *
     * @param request {@link org.hippoecm.hst.core.component.HstRequest}
     */
    protected void checkCommentsEnabled(HstRequest request) {
        boolean enableComments = Boolean.parseBoolean(getParameter(ENABLE_COMMENTS, request));
        request.setAttribute(ENABLE_COMMENTS, enableComments);
    }

    /**
     * Handles adding a user generated {@link org.onehippo.forge.blog.beans.CommentBean}
     *
     * @param request  {@link org.hippoecm.hst.core.component.HstRequest}
     * @param response {@link org.hippoecm.hst.core.component.HstResponse}
     */
    protected void doAddComment(HstRequest request, HstResponse response) {
        HippoBean commentTo = this.getContentBean(request);
        if (!(commentTo instanceof HippoDocumentBean)) {
            log.warn("Cannot comment on this type of bean");
            return;
        }

        String comment = request.getParameter(PARAM_COMMENT);
        String person = request.getParameter(PARAM_PERSON);

        if (StringUtils.isBlank(person) || StringUtils.isBlank(comment)) {
            return;
        }

        String email = request.getParameter(PARAM_EMAIL);
        String website = request.getParameter(PARAM_WEBSITE);

        if (isSpam(request, commentTo)) {
            handleSpam(response, comment, person, email, website);
            return;
        }

        String commentToUuidOfHandle = ((HippoDocumentBean) commentTo).getCanonicalHandleUUID();
        comment = HTML_TAG_PATTERN.matcher(comment).replaceAll(StringUtils.EMPTY);

        Session persistableSession = null;
        WorkflowPersistenceManager wpm = null;

        String commentsFolderPath = getCommentFolderPath(request);
        try {
            // retrieves writable session. NOTE: this session should be logged out manually!
            persistableSession = getPersistableSession(request);
            wpm = getWorkflowPersistenceManager(persistableSession);
            wpm.setWorkflowCallbackHandler(new WorkflowCallbackHandler<FullReviewedActionsWorkflow>() {
                public void processWorkflow(FullReviewedActionsWorkflow wf) throws RepositoryException,
                        WorkflowException, RemoteException {
                    wf.requestPublication();
                }
            });


            CommentBean commentBean = createCommentBean(commentTo, wpm, commentsFolderPath);
            // update content properties
            if (commentBean == null) {
                throw new HstComponentException("WorkflowPersitenceManager returned null for a CommentBean");
            }

            String title = comment.trim().length() > MAX_TITLE_LENGTH ? comment.trim().substring(0, MAX_TITLE_LENGTH)
                    : comment.trim();
            commentBean.setTitle(title);
            commentBean.setPerson(person);
            commentBean.setEmail(email);
            commentBean.setWebsite(website);
            commentBean.setSummary(BR_TAG.matcher(comment).replaceAll(HTML_BR));
            commentBean.setCalendar(Calendar.getInstance());
            commentBean.setCommentTo(commentToUuidOfHandle);

            sendNotificationMail(commentBean, request);

            // update now
            wpm.update(commentBean);

        } catch (ObjectBeanManagerException e) {
            log.warn("ObjectBeanManagerException occurred while handling comment: ", e);
            refreshWorkflowPersistenceManager(wpm);
        } catch (RepositoryException e) {
            log.warn("RepositoryException occurred while handling comment: ", e);
            refreshWorkflowPersistenceManager(wpm);
        } finally {
            if (persistableSession != null) {
                persistableSession.logout();
            }
        }

    }

    /**
     * Creates {@link org.onehippo.forge.blog.beans.CommentBean} through the workflow
     *
     * @param commentTo          {@link org.hippoecm.hst.content.beans.standard.HippoBean} that gets a comment
     * @param wpm                {@link org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager}
     * @param commentsFolderPath the path under which the comment is stored
     * @return path of the comment node
     * @throws org.hippoecm.hst.content.beans.ObjectBeanManagerException
     *          in case of a workflow error
     */
    private CommentBean createCommentBean(HippoBean commentTo, ObjectBeanPersistenceManager wpm, String commentsFolderPath)
            throws ObjectBeanManagerException {
        // comment node name is simply a concatenation of 'comment-' and current time millis.
        StringBuilder commentNodeName = new StringBuilder(COMMENT_NODENAME_PREFIX).append(commentTo.getName());
        commentNodeName.append('-').append(System.currentTimeMillis());

        // create comment node now
        wpm.createAndReturn(commentsFolderPath, BeanConstants.DOCTYPE_COMMENT, commentNodeName.toString(), true);

        // retrieve the comment content to manipulate
        return (CommentBean) wpm.getObject(commentsFolderPath + '/' + commentNodeName);
    }

    /**
     * Creates folder for the comment and returns its path
     *
     * @param request current {@link org.hippoecm.hst.core.component.HstRequest}
     * @return String with the folder path
     */
    private String getCommentFolderPath(HstRequest request) {
        // it is not important where we store comments. WE just use some (canonical) time path below our project content
        String siteCanonicalBasePath = this.getHstSite(request).getMount().getCanonicalContentPath();
        Calendar currentDate = Calendar.getInstance();

        DateFormat folderFormat = new SimpleDateFormat(YYYY_MM_DD);
        StringBuilder commentsFolderPath = new StringBuilder(siteCanonicalBasePath).append(COMMENT_FOLDER);
        commentsFolderPath.append(folderFormat.format(currentDate.getTime()));
        return commentsFolderPath.toString();
    }

    /**
     * Refreshes {@link org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager} (called from catch blocks)
     *
     * @param wpm {@link org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager}
     */
    private void refreshWorkflowPersistenceManager(ObjectBeanPersistenceManager wpm) {
        if (wpm == null) {
            return;
        }
        try {
            wpm.refresh();
        } catch (ObjectBeanPersistenceException obpe) {
            log.warn("Failed to refresh: ", obpe);
        }
    }

    /**
     * Checks external service (Akismet) if the comment is possible spam
     *
     * @param request {@link org.hippoecm.hst.core.component.HstRequest}
     * @param bean    of the current page to which the comment is added (needed for link rewriting)
     * @return {@literal true} if the external service thinks the comment is spam, {@literal false} if the external
     *         service responses the comment is safe OR if no external spam check service is configured
     */
    protected boolean isSpam(final HstRequest request, final HippoBean bean) {
        String apikey = getParameter(PARAM_SPAMFILTER_APIKEY, request);
        String websiteurl = getParameter(PARAM_SPAMFILTER_WEBSITEURL, request);
        if (StringUtils.isBlank(apikey) || StringUtils.isBlank(websiteurl)) {
            log.debug("No spamfilter is configured. All comments will be allowed.");
            return false;
        }
        Akismet akismet = new Akismet(apikey, websiteurl);

        String userAgent = request.getHeader(USER_AGENT);
        String referrer = request.getHeader(REFERER);
        String remoteAddr = request.getRemoteAddr();
        String comment = request.getParameter(PARAM_COMMENT);
        String person = request.getParameter(PARAM_PERSON);
        String email = request.getParameter(PARAM_EMAIL);
        String website = request.getParameter(PARAM_WEBSITE);
        final HstLink link = request.getRequestContext().getHstLinkCreator().create(bean, request.getRequestContext());
        String permalink = link.toUrlForm(request.getRequestContext(), true);
        return akismet.commentCheck(remoteAddr, userAgent, referrer, permalink, Akismet.COMMENT_TYPE_COMMENT, person,
                email, website, comment, null);
    }


    /**
     * Called in case the spam checker marks the new comment as spam
     *
     * @param response {@link org.hippoecm.hst.core.component.HstResponse}
     * @param comment  Text of the comment
     * @param person   name of the commenter
     * @param email    email address of the commenter
     * @param website  url of the commenter's website
     */
    protected void handleSpam(HstResponse response, String comment, String person, String email,
                              String website) {
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder("Received comment spam:");
            sb.append("\nFrom: ").append(person);
            sb.append("\nMail: ").append(email);
            sb.append("\nWebsite ").append(website);
            sb.append("\nComment\n").append(comment);
            log.info(sb.toString());
        }
        try {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (IOException e) {
            log.error("Could not send http response error to spammer", e);
        }
    }

    /**
     * <p>Sends mail notification for newly added comment.</p>
     * <p>If component configuration for <code>receiver.email</code> is missing, no mails will be sent.</p>
     *
     * @param commentBean {@link org.onehippo.forge.blog.beans.CommentBean}
     * @param request     {@link org.hippoecm.hst.core.component.HstRequest}
     */
    protected void sendNotificationMail(final CommentBean commentBean, final HstRequest request) {
        String mailhost = getParameter(Email.MAIL_HOST, request);
        String senderEmail = getParameter(Email.SENDER_EMAIL, request);
        String receiverEmail = getParameter(Email.RECEIVER_EMAIL, request);

        if (StringUtils.isBlank(mailhost)) {
            log.info("No value for mail.smtp.host in component configuration, trying localhost");
            mailhost = LOCALHOST;
        }
        if (StringUtils.isBlank(senderEmail)) {
            log.info("No value for sender.email in component configuration, trying noreply@example.com");
            senderEmail = DEFAULT_SENDER_MAIL;
        }
        if (StringUtils.isBlank(receiverEmail)) {
            log.warn("No value for receiver.email in component configuration. Will not try to send mail notification.");
            return;
        }

        SimpleEmail email = new SimpleEmail();
        StringBuilder subject = new StringBuilder("New comment: ");
        subject.append(commentBean.getTitle());

        StringBuilder msg = new StringBuilder("The following comment has been added:\n");
        msg.append(commentBean.getSummary());

        email.setHostName(mailhost);
        try {
            email.addTo(receiverEmail);
            email.setFrom(senderEmail);
            email.setSubject(subject.toString());
            email.setMsg(msg.toString());
            email.send();
        } catch (EmailException e) {
            log.error("Error sending notification for added comment", e);
        }
    }

}

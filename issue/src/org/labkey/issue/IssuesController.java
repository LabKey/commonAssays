/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

package org.labkey.issue;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.beehive.netui.pageflow.FormData;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.issues.IssuesSchema;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.*;
import org.labkey.api.security.*;
import org.labkey.api.util.MailHelper.ViewMessage;
import org.labkey.api.util.*;
import org.labkey.api.view.*;
import org.labkey.api.wiki.WikiRenderer;
import org.labkey.api.wiki.WikiRendererType;
import org.labkey.api.wiki.WikiService;
import org.labkey.issue.model.Issue;
import org.labkey.issue.model.IssueManager;
import org.labkey.issue.model.IssueManager.CustomColumnConfiguration;
import org.labkey.issue.query.IssuesQuerySchema;
import org.labkey.issue.query.IssuesQueryView;
import org.labkey.issue.query.IssuesTable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Jpf.Controller
public class IssuesController extends ViewController
{
    private static Logger _log = Logger.getLogger(IssuesController.class);

    // keywords enum
    public static final int ISSUE_NONE = 0;
    public static final int ISSUE_AREA = 1;
    public static final int ISSUE_TYPE = 2;
    public static final int ISSUE_MILESTONE = 3;
    public static final int ISSUE_STRING1 = 4;
    public static final int ISSUE_STRING2 = 5;
    public static final int ISSUE_PRIORITY = 6;
    public static final int ISSUE_RESOLUTION = 7;


    public IssuesController() throws Exception
    {
    }


    private Issue getIssue(Container c, int issueId) throws SQLException
    {
        return IssueManager.getIssue(openSession(), c, issueId);
    }


    int getIssueId() throws ServletException, IOException
    {
        try
        {
            String param = getRequest().getParameter("issueId");
            if (null == param)
                param = getRequest().getParameter("pk");
            return Integer.parseInt(param);
        }
        catch (Exception x)
        {
            HttpView.throwNotFound();
        }
        return 0;
    }


    /**
     * This method represents the point of entry into the pageflow
     */
    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    public Forward begin() throws Exception
    {
        ViewURLHelper url = new ViewURLHelper("Issues", "list", getContainer());
        url.addParameter("Issues.Status~neq","closed");
        return new ViewForward(url);
    }


    public static final String ISSUES_COLUMNS_LOOKUP = "IssuesColumns";

    private void setListColumnNames(String columnNames) throws SQLException, ServletException
    {
        PropertyManager.PropertyMap map = PropertyManager.getWritableProperties(0, getContainer().getId(), ISSUES_COLUMNS_LOOKUP, true);
        map.put("ListColumns", columnNames);  // NULL will remove this property
        PropertyManager.saveProperties(map);
    }


    private CustomColumnConfiguration getCustomColumnConfiguration() throws SQLException, ServletException
    {
        return IssueManager.getCustomColumnConfiguration(getContainer());
    }


    private Map<String, String> getColumnCaptions() throws SQLException, ServletException
    {
        return getCustomColumnConfiguration().getColumnCaptions();
    }


    // Lame "column picker" for grid... not hooked up to UI yet, but this action can be called to add/remove/reorder columns for this container

    @Jpf.Action @RequiresPermission(ACL.PERM_ADMIN)
    public Forward setListColumns() throws Exception
    {
        String columnNames = (String)getViewContext().get("columns");
        setListColumnNames(columnNames);

        return new ViewForward(getListUrl(getContainer()));
    }


    @Jpf.Action @RequiresPermission(ACL.PERM_ADMIN)
    public Forward setCustomColumnConfiguration() throws Exception
    {
        CustomColumnConfiguration ccc = new CustomColumnConfiguration(getViewContext());
        IssueManager.saveCustomColumnConfiguration(getContainer(), ccc);

        return adminForward();
    }


    @Jpf.Action @RequiresPermission(ACL.PERM_ADMIN)
    public Forward updateRequiredFields(IssuePreferenceForm form) throws Exception
    {
        final StringBuffer sb = new StringBuffer();
        if (form.getRequiredFields().length > 0)
        {
            String sep = "";
            for (String field : form.getRequiredFields())
            {
                sb.append(sep);
                sb.append(field);
                sep = ";";
            }
        }
        IssueManager.setRequiredIssueFields(sb.toString());
        return adminForward();
    }


    private static ViewURLHelper getListUrl(Container c)
    {
        ViewURLHelper url = new ViewURLHelper("Issues", "list", c);
        url.addParameter(DataRegion.LAST_FILTER_PARAM, "true");
        return url;
    }


    private static final String ISSUES_QUERY = "Issues";
    private HttpView getIssuesView(ListForm form) throws SQLException, ServletException
    {
        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), IssuesQuerySchema.SCHEMA_NAME);
        QuerySettings settings = new QuerySettings(getViewURLHelper(), ISSUES_QUERY);
        settings.setQueryName(ISSUES_QUERY);

        IssuesQueryView queryView = new IssuesQueryView(getViewContext(), schema, settings);

        // add the header for buttons and views
        QueryDefinition qd = schema.getQueryDefForTable(ISSUES_QUERY);
        Map<String, CustomView> views = qd.getCustomViews(getUser());

        form.setCustomizeURL(queryView.getCustomizeURL());
        form.setViews(views);
        VBox box = new VBox();

        if (form.getPrint())
            queryView.setButtonBarPosition(DataRegion.ButtonBarPosition.NONE);
        else
            box.addView(new JspView<ListForm>("/org/labkey/issue/list.jsp", form));

        box.addView(queryView);
        return box;
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    public Forward list(ListForm form) throws Exception
    {
        // convert AssignedTo/Email to AssignedTo/DisplayName: old bookmarks
        // reference Email, which is no longer displayed.
        ViewURLHelper url = cloneViewURLHelper();
        String[] emailFilters = url.getKeysByPrefix(ISSUES_QUERY + ".AssignedTo/Email");
        if (emailFilters != null && emailFilters.length > 0)
        {
            for (String emailFilter : emailFilters)
                url.deleteParameter(emailFilter);
            return new ViewForward(url);
        }

        HttpView view = getIssuesView(form);
        if (view != null)
        {
            // Use export=1, print=1 parameters on list() action so .lastFilter=true works in export & print cases
            if (form.getPrint())
            {
                includeView(new PrintTemplate(view, "Issues List"));
            }
            else
            {
                _includeFastView(view, "Issues List");
            }
        }
        return null;
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward exportTsv(QueryForm form) throws Exception
    {
        QueryView view = QueryView.create(form);

        final TSVGridWriter writer = view.getTsvWriter();
        writer.setColumnHeaderType(TSVGridWriter.ColumnHeaderType.caption);
        writer.write(getResponse());

        return null;
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    public Forward details(DetailsForm form) throws Exception
    {
        Integer issueId = null;
        Issue issue = null;

        try
        {
            issueId = Integer.parseInt(form.getIssueId());
        }
        catch(NumberFormatException e)
        {
            // Fall through
        }

        if (issueId == null || (issue = getIssue(getContainer(), issueId)) == null)
            HttpView.throwNotFound("Unable to find issue " + form.getIssueId());

        IssuePage page = (IssuePage) JspLoader.createPage(getRequest(), IssuesController.class, "detailView.jsp");
        JspView v = new JspView(page);

        page.setIssue(issue);
        page.setCustomColumnConfiguration(getCustomColumnConfiguration());
        //pass user's update perms to jsp page to determine whether to show notify list
        page.setUserHasUpdatePermissions(hasUpdatePermission(getUser(), issue));

        String title = "" + issue.getIssueId() + " : " + issue.getTitle();

        if (form.isPrint())
        {
            page.setPrint(true);
            return includeView(new PrintTemplate(v, title));
        }
        else
            return _renderInTemplate(v, title, null);
    }


    @Jpf.Action @RequiresPermission(ACL.PERM_INSERT)
    public Forward insert(InsertForm form) throws Exception
    {
        Issue issue = new Issue();

        if (form.getAssignedto() != null)
        {
            User user = UserManager.getUser(form.getAssignedto());
            if (user != null)
            {
                issue.setAssignedTo(user.getUserId());
            }
        }

        if (form.getTitle() != null && !form.getTitle().equals(""))
        {
            issue.setTitle(form.getTitle());
        }
        issue.Open(getContainer(), getUser());

        IssuePage page = (IssuePage) JspLoader.createPage(getRequest(), IssuesController.class, "updateView.jsp");
        JspView v = new JspView(page);

        CustomColumnConfiguration ccc = getCustomColumnConfiguration();

        page.setAction("insert");
        page.setIssue(issue);
        page.setCustomColumnConfiguration(ccc);
        page.setBody(form.getBody());
        page.setCallbackURL(form.getCallbackURL());
        page.setEditable(getEditableFields(page.getAction(), ccc));

        return _renderInTemplate(v, "Insert new issue", null);
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_UPDATEOWN)
    public Forward update() throws Exception
    {
        int issueId = getIssueId();
        Issue issue = getIssue(getContainer(), issueId);

        User user = getUser();
        requiresUpdatePermission(user, issue);

        IssuePage page = (IssuePage) JspLoader.createPage(getRequest(), IssuesController.class, "updateView.jsp");
        JspView v = new JspView(page);

        CustomColumnConfiguration ccc = getCustomColumnConfiguration();

        page.setAction("update");
        page.setIssue(issue);
        page.setCustomColumnConfiguration(ccc);
        page.setEditable(getEditableFields(page.getAction(), ccc));

        return _renderInTemplate(v, "(update) " + issue.getTitle(), null);
    }

    private Set<String> getEditableFields(String action, CustomColumnConfiguration ccc)
    {
        final Set<String> editable = new HashSet<String>(20);

        editable.add("title");
        editable.add("assignedTo");
        editable.add("type");
        editable.add("area");
        editable.add("priority");
        editable.add("milestone");
        editable.add("comments");

        for (String columnName : ccc.getColumnCaptions().keySet())
            editable.add(columnName);

        //if (!"insert".equals(action))
        editable.add("notifyList");

        if ("resolve".equals(action))
        {
            editable.add("resolution");
            editable.add("duplicate");
        }

        return editable;
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_UPDATEOWN)
    public Forward resolve() throws Exception
    {
        int issueId = getIssueId();
        Issue issue = getIssue(getContainer(), issueId);

        User user = getUser();
        requiresUpdatePermission(user, issue);

        issue.beforeResolve(user);

        IssuePage page = (IssuePage) JspLoader.createPage(getRequest(), IssuesController.class, "updateView.jsp");
        JspView v = new JspView(page);

        CustomColumnConfiguration ccc = getCustomColumnConfiguration();

        page.setAction("resolve");
        page.setIssue(issue);
        page.setCustomColumnConfiguration(ccc);
        page.setEditable(getEditableFields(page.getAction(), ccc));

        return _renderInTemplate(v, "(resolve) " + issue.getTitle(), null);
    }


    @Jpf.Action @RequiresPermission(ACL.PERM_UPDATEOWN)
    public Forward close() throws Exception
    {
        int issueId = getIssueId();
        Issue issue = getIssue(getContainer(), issueId);

        User user = getUser();
        requiresUpdatePermission(user, issue);

        issue.Close(user);

        IssuePage page = (IssuePage) JspLoader.createPage(getRequest(), IssuesController.class, "updateView.jsp");
        JspView v = new JspView(page);

        CustomColumnConfiguration ccc = getCustomColumnConfiguration();

        page.setAction("close");
        page.setIssue(issue);
        page.setCustomColumnConfiguration(ccc);
        page.setEditable(getEditableFields(page.getAction(), ccc));

        return _renderInTemplate(v, "(close) " + issue.getTitle(), null);
    }


    @Jpf.Action @RequiresPermission(ACL.PERM_UPDATEOWN)
    public Forward reopen() throws Exception
    {
        int issueId = getIssueId();
        Issue issue = getIssue(getContainer(), issueId);

        User user = getUser();
        requiresUpdatePermission(user, issue);

        issue.Open(getContainer(), user);

        IssuePage page = (IssuePage) JspLoader.createPage(getRequest(), IssuesController.class, "updateView.jsp");
        JspView v = new JspView(page);

        CustomColumnConfiguration ccc = getCustomColumnConfiguration();

        page.setAction("open");
        page.setIssue(issue);
        page.setCustomColumnConfiguration(ccc);
        page.setEditable(getEditableFields(page.getAction(), ccc));

        return _renderInTemplate(v, "(open) " + issue.getTitle(), null);
    }


    @Jpf.Action @RequiresPermission(ACL.PERM_INSERT)
    protected Forward doInsert(IssuesForm form) throws Exception
    {
        Container c = getContainer();
        User user = getUser();

        Issue issue = form.getBean();
        issue.Open(c, user);

        if (issue.getTitle() == null)
            return _renderInTemplate(getInsertErrorView(issue, "Error: Issue title cannot be null"), "Insert new issue", null);

        try
        {
            addComment(issue, (Issue)form.getOldValues(), user, form.getAction(), form.getComment(), getColumnCaptions());
            IssueManager.saveIssue(openSession(), user, c, issue);
        }
        catch (Exception x)
        {
            Throwable ex = x.getCause() == null ? x : x.getCause();
            String error = ex.getMessage();
            _log.debug("IssuesContoller.doInsert", x);
            issue.Open(c, user);

            return _renderInTemplate(getInsertErrorView(issue, error), "Insert new issue", null);
        }

        ViewURLHelper url = getDetailsForwardURL(issue);

        final String assignedTo = UserManager.getDisplayName(issue.getAssignedTo());
        if (assignedTo != null)
            sendUpdateEmail(issue, url, "opened and assigned to " + assignedTo, form.getAction());
        else
            sendUpdateEmail(issue, url, "opened", form.getAction());

        ViewURLHelper forwardURL = getForwardURL(issue);
        return new ViewForward(forwardURL);
    }

    private JspView getInsertErrorView(Issue issue, String error) throws ServletException, SQLException
    {
        IssuePage page = (IssuePage) JspLoader.createPage(getRequest(), IssuesController.class, "updateView.jsp");
        JspView v = new JspView(page);

        CustomColumnConfiguration ccc = getCustomColumnConfiguration();

        page.setAction("insert");
        page.setIssue(issue);
        page.setError(error);
        page.setCustomColumnConfiguration(ccc);
        page.setEditable(getEditableFields(page.getAction(), ccc));

        return v;
    }

    private ViewURLHelper getDetailsForwardURL(Issue issue)
    {
        ViewURLHelper url = cloneViewURLHelper();
        url.setAction("details");
        url.addParameter("issueId", "" + issue.getIssueId());
        return url;
    }

    private ViewURLHelper getForwardURL(Issue issue) throws URISyntaxException
    {
        ViewURLHelper url;
        String callbackURL = getRequest().getParameter("callbackURL");
        if (callbackURL != null)
        {
            url = new ViewURLHelper(callbackURL);
            url.addParameter("issueId", "" + issue.getIssueId());
            return url;
        }
        else
        {
            return getDetailsForwardURL(issue);
        }
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_UPDATEOWN)
    protected Forward doUpdate(IssuesForm form) throws Exception
    {
        Container c = getContainer();
        User user = getUser();

        Issue issue = form.getBean();
        requiresUpdatePermission(user, issue);
        ViewURLHelper detailsUrl;

        try
        {
            validateRequiredFields(form);
            validateNotifyList(issue, form);

            if ("insert".equals(form.getAction()))
                return doInsert(form);

            detailsUrl = getForwardURL(issue);

            // if assigned to changes, append the new assignee to the notify list
            /*
            if (issue.getAssignedTo() != null && !issue.getAssignedTo().equals(((Issue)form.getOldValues()).getAssignedTo()))
            {
                final String notify = issue.getNotifyList();
                final String newName = UserManager.getEmailForId(issue.getAssignedTo());
                if (notify.indexOf(newName) == -1)
                {
                    issue.setNotifyList(notify + ';' + newName);
                }
            }
            */

            if ("resolve".equals(form.getAction()))
                issue.Resolve(user);
            else if ("open".equals(form.getAction()))
                issue.Open(c, user, true);
            else if ("close".equals(form.getAction()))
                issue.Close(user);
            else
                issue.Change(user);

            addComment(issue, (Issue)form.getOldValues(), user, form.getAction(), form.getComment(), getColumnCaptions());
            IssueManager.saveIssue(openSession(), user, c, issue);
        }
        catch (Exception x)
        {
            IssuePage page = (IssuePage) JspLoader.createPage(getRequest(), IssuesController.class, "updateView.jsp");
            JspView v = new JspView(page);

            CustomColumnConfiguration ccc = getCustomColumnConfiguration();

            page.setAction(form.getAction());
            page.setIssue(issue);
            page.setCustomColumnConfiguration(ccc);
            page.setEditable(getEditableFields(page.getAction(), ccc));
            page.setError(x.getMessage());

            return _renderInTemplate(v, "(update) " + issue.getTitle(), null);
        }

        // Send update email...
        //    ...if someone other than "created by" is closing a bug
        //    ...if someone other than "assigned to" is updating, reopening, or resolving a bug
        if ("close".equals(form.getAction()))
        {
            //if (issue.getCreatedBy() != user.getUserId())
                sendUpdateEmail(issue, detailsUrl, "closed", form.getAction());
        }
        else
        {
            String change = ("open".equals(form.getAction()) ? "reopened" : form.getAction() + "d");

            // Add "and assigned to you" in all cases except an update where "assigned to" hasn't changed
            //if (!"update".equals(form.getAction()))
            //    change = change + " and assigned to you";

            sendUpdateEmail(issue, detailsUrl, change, form.getAction());
        }

        return new ViewForward(detailsUrl);
    }

    private void validateRequiredFields(IssuesForm form) throws Exception
    {
        String requiredFields = IssueManager.getRequiredIssueFields();
        final Map<String, String> newFields = form.getStrings();
        if (StringUtils.isEmpty(requiredFields))
            return;

        if (newFields.containsKey("title"))
            validateRequired("title", newFields.get("title"), requiredFields);
        if (newFields.containsKey("assignedTo"))
            validateRequired("assignedto", newFields.get("assignedTo"), requiredFields);
        if (newFields.containsKey("type"))
            validateRequired("type", newFields.get("type"), requiredFields);
        if (newFields.containsKey("area"))
            validateRequired("area", newFields.get("area"), requiredFields);
        if (newFields.containsKey("priority"))
            validateRequired("priority", newFields.get("priority"), requiredFields);
        if (newFields.containsKey("milestone"))
            validateRequired("milestone", newFields.get("milestone"), requiredFields);
        if (newFields.containsKey("notifyList"))
            validateRequired("notifylist", newFields.get("notifyList"), requiredFields);
        if (newFields.containsKey("int1"))
            validateRequired("int1", newFields.get("int1"), requiredFields);
        if (newFields.containsKey("int2"))
            validateRequired("int2", newFields.get("int2"), requiredFields);
        if (newFields.containsKey("string1"))
            validateRequired("string1", newFields.get("string1"), requiredFields);
        if (newFields.containsKey("string2"))
            validateRequired("string2", newFields.get("string2"), requiredFields);
    }

    private void validateRequired(String columnName, String value, String requiredFields) throws Exception
    {
        if (requiredFields != null)
        {
            if (requiredFields.indexOf(columnName) != -1)
            {
                if (StringUtils.isEmpty(value))
                {
                    final CustomColumnConfiguration ccc = IssueManager.getCustomColumnConfiguration(getContainer());
                    String name = null;
                    if (ccc.getColumnCaptions().containsKey(columnName))
                        name = ccc.getColumnCaptions().get(columnName);
                    else
                    {
                        ColumnInfo column = IssuesSchema.getInstance().getTableInfoIssues().getColumn(columnName);
                        if (column != null)
                            name = column.getName();
                    }
                    throw new ServletException("Error: The field: " + (name != null ? name : columnName) + " is required");
                }
            }
        }
    }

    private void validateNotifyList(Issue issue, IssuesForm form) throws Exception
    {
        String[] rawEmails = _toString(form.getNotifyList()).split("\n");
        List<String> invalidEmails = new ArrayList<String>();
        List<ValidEmail> emails = org.labkey.api.security.SecurityManager.normalizeEmails(rawEmails, invalidEmails);

        StringBuffer message = new StringBuffer();

        for (String rawEmail : invalidEmails)
        {
            // Ignore lines of all whitespace, otherwise show an error.
            if (!"".equals(rawEmail.trim()))
            {
                message.append("Failed to add user ").append(rawEmail.trim()).append(": Invalid email address");
                throw new ServletException(message.toString());
            }
        }

        if (!emails.isEmpty())
        {
            StringBuffer notify = new StringBuffer();
            for (int i=0; i < emails.size(); i++)
            {
                notify.append(emails.get(i));
                if (i < emails.size()-1)
                    notify.append(';');
            }
            issue.setNotifyList(notify.toString());
        }
    }

    public static class CompleteUserForm extends FormData
    {
        private String _prefix;
        private String _issueId;

        public String getPrefix(){return _prefix;}
        public void setPrefix(String prefix){_prefix = prefix;}

        public String getIssueId(){return _issueId;}
        public void setIssueId(String issueId){_issueId = issueId;}
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward completeUser(CompleteUserForm form) throws Exception
    {
        Container c = getContainer();

        final int issueId = Integer.valueOf(form.getIssueId());
        Issue issue = getIssue(getContainer(), issueId);
        if (issue == null)
        {
            issue = new Issue();
            issue.Open(c, getUser());
        }
        User[] users = IssueManager.getAssignedToList(c, issue);
        List<AjaxCompletion> completions = UserManager.getAjaxCompletions(form.getPrefix(), users);
        return sendAjaxCompletions(completions);
    }


    private void sendUpdateEmail(Issue issue, ViewURLHelper detailsUrl, String change, String action)
    {
        try
        {
            final String to = getEmailAddresses(issue, action);
            if (to.length() > 0)
            {
                Issue.Comment lastComment = issue.getLastComment();
                String messageId = "<" + issue.getEntityId() + "." + lastComment.getCommentId() + "@" + AppProps.getInstance().getDefaultDomain() + ">";
                String references = messageId + " <" + issue.getEntityId() + "@" + AppProps.getInstance().getDefaultDomain() + ">";
                ViewMessage m = MailHelper.createMessage(AppProps.getInstance().getSystemEmailAddress(), to);
                if (m.getAllRecipients().length > 0)
                {
                    UpdateEmailPage page = (UpdateEmailPage) JspLoader.createPage(getRequest(), IssuesController.class, "updateEmail.jsp");
                    m.setSubject("Issue #" + issue.getIssueId() + ", \"" + issue.getTitle() + ",\" has been " + change);
                    page.url = detailsUrl.getURIString();
                    page.issue = issue;
                    page.isPlain = true;
                    final JspView view = new JspView(page);
                    m.setTemplateContent(getViewContext(), view, "text/plain");
                    page.isPlain = false;
                    m.setTemplateContent(getViewContext(), view, "text/html");
                    m.setHeader("References", references);

                    MailHelper.send(m);
                }
            }
        }
        catch (Exception e)
        {
            _log.error("sendUpdateEmail: " + e);
        }
    }

    /**
     * Builds the list of email address for notification based on the user
     * preferences and the explicit notification list.
     */
    private String getEmailAddresses(Issue issue, String action) throws ServletException
    {
        final Set<String> emailAddresses = new HashSet<String>();
        final int filter = getNotificationFilter(action);
        final Container c = getContainer();

        if ((filter & IssueManager.getUserEmailPreferences(c, issue.getAssignedTo())) != 0)
        {
            emailAddresses.add(UserManager.getEmailForId(issue.getAssignedTo()));
        }

        if ((filter & IssueManager.getUserEmailPreferences(c, issue.getCreatedBy())) != 0)
        {
            emailAddresses.add(UserManager.getEmailForId(issue.getCreatedBy()));
        }

        // add any explicit notification list addresses
        final String notify = issue.getNotifyList();
        if (notify != null)
        {
            for (String email : notify.split(";"))
            {
                emailAddresses.add(email);
            }
        }

        final String current = getUser().getEmail();
        final StringBuffer sb = new StringBuffer();

        boolean selfSpam = !((IssueManager.NOTIFY_SELF_SPAM & IssueManager.getUserEmailPreferences(c, getUser().getUserId())) == 0);
        if (selfSpam)
            emailAddresses.add(current);

        // build up the final semicolon delimited list, excluding the current user
        for (String email : emailAddresses.toArray(new String[0]))
        {
            if (selfSpam || !email.equals(current))
            {
                sb.append(email);
                sb.append(';');
            }
        }
        return sb.toString();
    }

    private int getNotificationFilter(String action)
    {
        if ("insert".equals(action))
            return IssueManager.NOTIFY_ASSIGNEDTO_OPEN;
        else
            return IssueManager.NOTIFY_ASSIGNEDTO_UPDATE | IssueManager.NOTIFY_CREATED_UPDATE;
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward emailPrefs(EmailPrefsForm form) throws Exception
    {
        return emailPrefs((String)null);
    }


    private Forward emailPrefs(String message) throws Exception
    {
        requiresLogin();

        EmailPreferencesPage page = (EmailPreferencesPage)JspLoader.createPage(getRequest(), IssuesController.class, "emailPreferences.jsp");
        JspView v = new JspView(page);

        if (message != null)
            page._message = message;
        return _renderInTemplate(v, "Email preferences for issues in: " + getContainer().getPath(), null);
    }


    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward updateEmailPrefs(EmailPrefsForm form) throws Exception
    {
        int emailPref = 0;
        for (int pref : form.getEmailPreference())
        {
            emailPref |= pref;
        }
        IssueManager.setUserEmailPreferences(getContainer(), getUser().getUserId(),
                emailPref, getUser().getUserId());
        return emailPrefs("Settings updated successfully");
    }


    private ViewForward adminForward() throws URISyntaxException, ServletException
    {
        return new ViewForward("Issues", "admin", getContainer());
    }

    public static final String REQUIRED_FIELDS_COLUMNS = "Title,AssignedTo,Type,Area,Priority,Milestone,NotifyList";
    public static final String DEFAULT_REQUIRED_FIELDS = "title;assignedto";

    @Jpf.Action @RequiresPermission(ACL.PERM_ADMIN)
    protected Forward admin(AdminForm form) throws Exception
    {
        // TODO: This hack ensures that priority & resolution option defaults get populated if first reference is the admin page.  Fix this.
        IssuePage page = new IssuePage()
        {
            public void _jspService(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException
            {
            }
        };
        page.getPriorityOptions(getContainer());
        page.getResolutionOptions(getContainer());

        AdminView adminView = new AdminView(getContainer(), getCustomColumnConfiguration());

        _includeView(adminView, "Issues Admin Page");
        return null;
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_ADMIN)
    protected Forward addKeyword(AdminForm form) throws Exception
    {
        IssueManager.addKeyword(getContainer(), form.getType(), form.getKeyword());
        return adminForward();
    }


    @Jpf.Action @RequiresPermission(ACL.PERM_ADMIN)
    protected Forward deleteKeyword(AdminForm form) throws Exception
    {
        IssueManager.deleteKeyword(getContainer(), form.getType(), form.getKeyword());
        return adminForward();
    }


    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward rss() throws Exception
    {
        try
        {
            DataRegion r = new DataRegion();
            TableInfo tinfo = IssuesSchema.getInstance().getTableInfoIssues();
            ColumnInfo[] cols = tinfo.getColumns("IssueId,Created,Area,Title,AssignedTo,Priority,Status,Milestone");
            r.addColumns(cols);

            ResultSet rs = r.getResultSet(new RenderContext(getViewContext()));
            ObjectFactory f = ObjectFactory.Registry.getFactory(Issue.class);
            Issue[] issues = (Issue[]) f.handleArray(rs);

            HttpView v = new GroovyView("/org/labkey/issue/rss.gm");
            v.addObject("issues", issues);
            ViewURLHelper url = cloneViewURLHelper();
            url.deleteParameters();
            url.setAction("details.view");
            v.addObject("url", url.getURIString() + "issueId=");
            v.addObject("homePageUrl", ViewURLHelper.getBaseServerURL(getRequest()));

            getResponse().setContentType("text/xml");
            includeView(v);
        }
        catch (SQLException x)
        {
            x.printStackTrace();
            throw new ServletException(x);
        }
        return null;
    }


    @Jpf.Action @RequiresPermission(ACL.PERM_ADMIN)
    protected Forward purge() throws ServletException, SQLException, IOException
    {
        requiresGlobalAdmin();

        String message = IssueManager.purge();
        getResponse().getWriter().println(message);

        return null;
    }


    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    public Forward jumpToIssue(DetailsForm form) throws Exception
    {
        Container c = getContainer();

        String issueId = form.getIssueId();
        if (issueId != null)
        {
            issueId = issueId.trim();
            try
            {
                int id = Integer.parseInt(issueId);
                Issue issue = IssueManager.getIssue(null, c, id);
                if (issue != null)
                {
                    ViewURLHelper urlHelper = getViewURLHelper().clone();
                    urlHelper.deleteParameters();
                    urlHelper.addParameter("issueId", Integer.toString(id));
                    urlHelper.setAction("details.view");
                    return new ViewForward(urlHelper);
                }
            }
            catch (NumberFormatException e)
            {
                // fall through
            }
        }
        ViewURLHelper urlHelper = getViewURLHelper().clone();
        urlHelper.deleteParameters();
        urlHelper.addParameter("error", "Invalid issue id '" + issueId + "'");
        urlHelper.setAction("list.view");
        urlHelper.addParameter(".lastFilter", "true");
        return new ViewForward(urlHelper);
    }


    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward search() throws Exception
    {
        Container c = getContainer();
        String searchTerm = (String)getViewContext().get("search");

        Module module = ModuleLoader.getInstance().getCurrentModule();
        List<Search.Searchable> l = new ArrayList<Search.Searchable>();
        l.add((Search.Searchable)module);

        String html = Search.search(getUser(), c, searchTerm, l);
        HtmlView view = new HtmlView(html);

        NavTrailConfig trailConfig = new NavTrailConfig(getViewContext(), c);
        trailConfig.setTitle("Search Results");

        HttpView template = new HomeTemplate(getViewContext(), c, view, trailConfig);
        return includeView(template);
    }

    static boolean _equal(String a, String b)
    {
        return _toString(a).equals(_toString(b));
    }


    static String _toString(Object a)
    {
        return null == a ? "" : a.toString();
    }


    static void _appendChange(StringBuffer sb, String field, String from, String to)
    {
        from = _toString(from);
        to = _toString(to);
        if (!from.equals(to))
        {
            String encFrom = PageFlowUtil.filter(from);
            String encTo = PageFlowUtil.filter(to);
            sb.append("<tr><td>").append(field).append("</td><td>").append(encFrom).append("</td><td>&raquo;</td><td>").append(encTo).append("</td></tr>\n");
        }
    }


    static void addComment(Issue issue, Issue previous, User user, String action, String comment, Map<String, String> customColumns)
    {
        StringBuffer sbChanges = new StringBuffer();
        if (!action.equals("insert") && !action.equals("update"))
            sbChanges.append("<b>").append(action).append("</b><br>\n");

        // CONSIDER: write changes in wiki
        // CONSIDER: and postpone formatting until render
        if (null != previous)
        {
            // issueChanges is not defined yet, but it leaves things flexible
            sbChanges.append("<table class=issues-Changes>");
            _appendChange(sbChanges, "Title", previous.getTitle(), issue.getTitle());
            _appendChange(sbChanges, "Status", previous.getStatus(), issue.getStatus());
            _appendChange(sbChanges, "Assigned To", previous.getAssignedToName(), issue.getAssignedToName());
            _appendChange(sbChanges, "Notify", previous.getNotifyList(), issue.getNotifyList());
            _appendChange(sbChanges, "Type", previous.getType(), issue.getType());
            _appendChange(sbChanges, "Area", previous.getArea(), issue.getArea());
            _appendChange(sbChanges, "Priority", String.valueOf(previous.getPriority()), String.valueOf(issue.getPriority()));
            _appendChange(sbChanges, "Milestone", previous.getMilestone(), issue.getMilestone());

            _appendCustomColumnChange(sbChanges, "int1", _toString(previous.getInt1()), _toString(issue.getInt1()), customColumns);
            _appendCustomColumnChange(sbChanges, "int2", _toString(previous.getInt2()), _toString(issue.getInt2()), customColumns);
            _appendCustomColumnChange(sbChanges, "string1", previous.getString1(), issue.getString1(), customColumns);
            _appendCustomColumnChange(sbChanges, "string2", previous.getString2(), issue.getString2(), customColumns);

            sbChanges.append("</table>\n");
        }

        //why we are wrapping issue comments in divs???
        StringBuilder formattedComment = new StringBuilder();
        formattedComment.append("<div class=\"wiki\">");
        formattedComment.append(sbChanges);
        //render issues as plain text with links
        WikiRenderer w = WikiService.get().getRenderer(WikiRendererType.TEXT_WITH_LINKS);
        w.format(comment, formattedComment);
        formattedComment.append("</div>");

        issue.addComment(user, formattedComment.toString());
    }


    private Forward _renderInTemplate(HttpView view, String title, String helpTopic) throws Exception
    {
        if (helpTopic == null)
            helpTopic = "issues";

        Container navTrailContainer = getViewContext().getContainer();
        NavTrailConfig trailConfig = new NavTrailConfig(getViewContext(), navTrailContainer);
        if (title != null)
            trailConfig.setTitle(title);
        trailConfig.setHelpTopic(new HelpTopic(helpTopic, HelpTopic.Area.COMMON));

        HomeTemplate template = new HomeTemplate(getViewContext(), getContainer(), view, trailConfig);
        includeView(template);
        return null;
    }


    private static void _appendCustomColumnChange(StringBuffer sb, String field, String from, String to, Map<String, String> columnCaptions)
    {
        String caption = columnCaptions.get(field);

        if (null != caption)
            _appendChange(sb, caption, from, to);
    }


    //
    // VIEWS
    //
    private static String helpTopic = "issues";

    private void _includeView(HttpView v, String title)
            throws Exception
    {
        NavTrailConfig navtrail = new NavTrailConfig(getViewContext());
        navtrail.setTitle(title);
        navtrail.setHelpTopic(new HelpTopic(helpTopic, HelpTopic.Area.COMMON));

        HttpView template = new HomeTemplate(getViewContext(), v, navtrail);
        template.addObject("pageTitle", title);
        includeView(template);
    }


    private void _includeFastView(HttpView v, String title)
            throws Exception
    {
        NavTrailConfig navtrail = new NavTrailConfig(getViewContext());
        navtrail.setTitle(title);
        navtrail.setHelpTopic(new HelpTopic(helpTopic, HelpTopic.Area.COMMON));

        HttpView template = new HomeTemplate(getViewContext(), v, navtrail);

        includeView(template);
    }

    public static class AdminView extends GroovyView
    {
        Container _c;
        CustomColumnConfiguration _ccc;
        KeywordAdminView _keywordAdminView;
        JspView<IssuesPreference> _requiredFieldsView;

        public AdminView(Container c, CustomColumnConfiguration ccc)
        {
            super("/org/labkey/issue/admin.gm");

            _ccc = ccc;

            _keywordAdminView = new KeywordAdminView(c);
            _keywordAdminView.addKeyword("Type", ISSUE_TYPE);
            _keywordAdminView.addKeyword("Area", ISSUE_AREA);
            _keywordAdminView.addKeyword("Priority", ISSUE_PRIORITY);
            _keywordAdminView.addKeyword("Milestone", ISSUE_MILESTONE);
            _keywordAdminView.addKeyword("Resolution", ISSUE_RESOLUTION);

            addCustomColumn("string1", ISSUE_STRING1);
            addCustomColumn("string2", ISSUE_STRING2);

            List<String> columnNames = new ArrayList<String>();
            for (String name : REQUIRED_FIELDS_COLUMNS.split(","))
            {
                columnNames.add(name);
            }
            for (String name : IssuesTable.getCustomColumnCaptions(c).keySet())
            {
                columnNames.add(name);
            }
            ColumnInfo[] cols = IssuesSchema.getInstance().getTableInfoIssues().getColumns(columnNames.toArray(new String[0]));

            IssuesPreference bean = new IssuesPreference(cols, IssueManager.getRequiredIssueFields());
            _requiredFieldsView = new JspView<IssuesPreference>("/org/labkey/issue/requiredFields.jsp", bean);
        }


        // Add keyword admin for custom columns with column picker enabled
        private void addCustomColumn(String tableColumn, int type)
        {
            if (_ccc.getPickListColumns().contains(tableColumn))
            {
                String caption = _ccc.getColumnCaptions().get(tableColumn);
                _keywordAdminView.addKeyword(caption, type);
            }
        }


        @Override
        protected void prepareWebPart(Object model) throws ServletException
        {
            this.setView("keywordView", _keywordAdminView);
            this.setView("requiredFieldsView", _requiredFieldsView);

            addObject("captions", _ccc.getColumnCaptions());
            addObject("pickLists", _ccc.getPickListColumns());
            addObject("pickListName", CustomColumnConfiguration.PICK_LIST_NAME);

            super.prepareWebPart(model);
        }
    }


    // Renders the pickers for all keywords; would be nice to render each picker independently, but that makes it hard to align
    // all the top and bottom sections with each other.
    public static class KeywordAdminView extends GroovyView
    {
        private Container _c;
        private List<Keyword> _keywords = new ArrayList<Keyword>(5);

        public KeywordAdminView(Container c)
        {
            super("/org/labkey/issue/keywordAdmin.gm");
            _c = c;
        }

        public void addKeyword(String name, int type)
        {
            _keywords.add(new Keyword(_c, name, type));
        }

        protected void prepareWebPart(Object context) throws ServletException
        {
            addObject("keywords", _keywords);
        }

        public static class Keyword
        {
            public String name;
            public String plural;
            public int type;
            public String[] values;

            Keyword(Container c, String name, int type)
            {
                this.name = name;
                this.plural = name.endsWith("y") ? name.substring(0, name.length() - 1) + "ies" : name + "s";
                this.type = type;
                this.values = IssueManager.getKeywords(c.getId(), type);
            }
        }
    }

    public static class EmailPrefsForm extends ViewForm
    {
        private Integer[] _emailPreference = new Integer[0];

        public Integer[] getEmailPreference()
        {
            return _emailPreference;
        }

        public void setEmailPreference(Integer[] emailPreference)
        {
            _emailPreference = emailPreference;
        }
    }

    public static class AdminForm extends ViewForm
    {
        private int type;
        private String keyword;


        public int getType()
        {
            return type;
        }


        public void setType(int type)
        {
            this.type = type;
        }


        public String getKeyword()
        {
            return keyword;
        }


        public void setKeyword(String keyword)
        {
            this.keyword = keyword;
        }
    }

    public static class IssuesForm extends BeanViewForm<Issue>
    {
        public IssuesForm()
        {
            super(Issue.class, IssuesSchema.getInstance().getTableInfoIssues(), new String[]{"action", "comment"});
        }

        public String getAction()
        {
            return _stringValues.get("action");
        }

        public String getComment()
        {
            return _stringValues.get("comment");
        }

        public String getNotifyList()
        {
            return _stringValues.get("notifyList");
        }
    }


    public static class SummaryWebPart extends GroovyView
    {
        public SummaryWebPart()
        {
            super("/org/labkey/issue/summary_webpart.gm");
            addObject("isGuest", Boolean.TRUE);
            addObject("hasPermission", Boolean.TRUE);
            addObject("title", null);
        }


        @Override
        protected void prepareWebPart(Object model) throws ServletException
        {
            ViewContext context = getViewContext();
            // TODO: parameterize container
            Container c = context.getContainer();

            //set specified web part title
            Object title = context.get("title");
            if(title == null)
                title = "Issues Summary";
            setTitle(title.toString());

            User u = context.getUser();
            boolean hasPermission = c.hasPermission(u, ACL.PERM_READ);
            context.put("hasPermission", hasPermission);
            context.put("isGuest", u.isGuest());
            if (!hasPermission)
                return;

            ViewURLHelper url = getListUrl(c);
            setTitleHref(url.getLocalURIString());

            url.deleteParameters();
            context.put("url", url);

            try
            {
                Map[] bugs = IssueManager.getSummary(c);
                context.put("bugs", bugs);
            }
            catch (SQLException x)
            {
                setVisible(false);
            }
        }
    }


    public static class TestCase extends junit.framework.TestCase
    {
        public TestCase()
        {
            super("IssueController.jpf");
        }


        public TestCase(String name)
        {
            super(name);
        }


        public void testIssue()
                throws SQLException, ServletException
        {
        }


        public static Test suite()
        {
            return new TestSuite(TestCase.class);
        }
    }

    //
    // Hibernate session handling
    //

//    Object _s = null;

    Object openSession()
    {
//        if (null == _s)
//            _s = IssueManager.openSession();
//        return _s;
        return null;
    }


    void closeSession()
    {
//        if (null != _s)
//            _s.close();
//        _s = null;
    }


    protected synchronized void afterAction() throws Exception
    {
        super.afterAction();
        closeSession();
    }

    /**
     * Does this user have permission to update this issue?
     */
    private boolean hasUpdatePermission(User user, Issue issue)
    {
        // If we have full Update rights on the container, continue
        if (getViewContext().hasPermission(ACL.PERM_UPDATE))
            return true;

        // If UpdateOwn on the container AND we created this Issue, continue
        //noinspection RedundantIfStatement
        if (getViewContext().hasPermission(ACL.PERM_UPDATEOWN)
                && issue.getCreatedBy() == user.getUserId())
            return true;

        return false;
    }


    /**
     * Throw an exception if user does not have permission to update issue
     */
    private void requiresUpdatePermission(User user, Issue issue)
            throws ServletException
    {
        if (!hasUpdatePermission(user, issue))
            HttpView.throwUnauthorized();
    }


    public static class InsertForm extends ViewForm
    {
        private String _body;
        private Integer _assignedto;
        private String _callbackURL;
        private String _title;

        public String getBody()
        {
            return _body;
        }

        public void setBody(String body)
        {
            _body = body;
        }

        public Integer getAssignedto()
        {
            return _assignedto;
        }

        public void setAssignedto(Integer assignedto)
        {
            _assignedto = assignedto;
        }

        public String getCallbackURL()
        {
            return _callbackURL;
        }

        public void setCallbackURL(String callbackURL)
        {
            _callbackURL = callbackURL;
        }

        public String getTitle()
        {
            return _title;
        }

        public void setTitle(String title)
        {
            _title = title;
        }
    }


    public static class ListForm extends FormData
    {
        private boolean _export;
        private boolean _print;
        private ViewURLHelper _customizeURL;
        private Map<String, CustomView> _views;

        public boolean getExport()
        {
            return _export;
        }

        public void setExport(boolean export)
        {
            _export = export;
        }

        public boolean getPrint()
        {
            return _print;
        }

        public void setPrint(boolean print)
        {
            _print = print;
        }

        public ViewURLHelper getCustomizeURL() {return _customizeURL;}
        public void setCustomizeURL(ViewURLHelper url) {_customizeURL = url;}
        public Map<String, CustomView> getViews() {return _views;}
        public void setViews(Map<String, CustomView> views) {_views = views;}
    }


    public static class DetailsForm extends ViewForm
    {
        private String _issueId;
        private boolean _print;

        public String getIssueId()
        {
            return _issueId;
        }

        public void setIssueId(String issueId)
        {
            _issueId = issueId;
        }

        public boolean isPrint()
        {
            return _print;
        }

        public void setPrint(boolean print)
        {
            _print = print;
        }
    }


    public static class CustomizeIssuesPartView extends AbstractCustomizeWebPartView<Object>
    {
        public CustomizeIssuesPartView()
        {
            super("/org/labkey/issue/issues_customize.gm");
        }

        @Override
        public void prepareWebPart(Object model) throws ServletException
        {
            super.prepareWebPart(model);
            addObject("containerName", getViewContext().getContainer().getName());
        }
    }


    public static class IssuesPreference
    {
        private ColumnInfo[] _columns;
        private String _requiredFields;

        public IssuesPreference(ColumnInfo[] columns, String requiredFields)
        {
            _columns = columns;
            _requiredFields = requiredFields;
        }

        public ColumnInfo[] getColumns(){return _columns;}
        public String getRequiredFields(){return _requiredFields;}
    }


    public static class IssuePreferenceForm extends ViewForm
    {
        private String[] _requiredFields = new String[0];

        public void setRequiredFields(String[] requiredFields){_requiredFields = requiredFields;}
        public String[] getRequiredFields(){return _requiredFields;}
    }
}

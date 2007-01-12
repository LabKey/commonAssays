package org.labkey.issue;

import org.labkey.api.jsp.JspBase;
import org.labkey.issue.model.Issue;

abstract public class UpdateEmailPage extends JspBase
{
    public String url;
    public Issue issue;
    public boolean isPlain;        
}

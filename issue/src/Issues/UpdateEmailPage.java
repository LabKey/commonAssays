package Issues;

import org.labkey.api.jsp.JspBase;
import Issues.model.Issue;

abstract public class UpdateEmailPage extends JspBase
{
    public String url;
    public Issue issue;
    public boolean isPlain;        
}

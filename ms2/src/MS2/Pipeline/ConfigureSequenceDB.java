package MS2.Pipeline;

import org.fhcrc.cpas.jsp.JspBase;

/**
 * User: brittp
 * Date: Dec 13, 2005
 * Time: 1:15:47 PM
 */
public abstract class ConfigureSequenceDB extends JspBase
{
    private String _localPathRoot;
    private boolean _allowUpload;
    private String _error;

    public String getError()
    {
        return _error;
    }

    public void setError(String error)
    {
        _error = error;
    }

    public boolean isAllowUpload()
    {
        return _allowUpload;
    }

    public void setAllowUpload(boolean allowUpload)
    {
        _allowUpload = allowUpload;
    }

    public String getLocalPathRoot()
    {
        return _localPathRoot;
    }

    public void setLocalPathRoot(String localPathRoot)
    {
        _localPathRoot = localPathRoot;
    }
}

package org.labkey.ms2.pipeline;

import org.labkey.api.jsp.FormPage;

/**
 * User: brittp
 * Date: Dec 13, 2005
 * Time: 1:15:47 PM
 */
public abstract class ConfigureSequenceDB extends FormPage<PipelineController.SequenceDBRootForm>
{
    private String _localPathRoot;
    private boolean _allowUpload;

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

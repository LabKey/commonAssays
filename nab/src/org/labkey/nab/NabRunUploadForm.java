package org.labkey.nab;

import org.labkey.api.study.actions.AssayRunUploadForm;

/**
 * User: brittp
 * Date: Sep 27, 2007
 * Time: 4:00:02 PM
 */
public class NabRunUploadForm extends AssayRunUploadForm
{
    private Integer _replaceRunId;

    public Integer getReplaceRunId()
    {
        return _replaceRunId;
    }

    public void setReplaceRunId(Integer replaceRunId)
    {
        _replaceRunId = replaceRunId;
    }
}

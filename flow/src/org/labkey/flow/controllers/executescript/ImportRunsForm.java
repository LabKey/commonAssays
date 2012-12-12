/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

package org.labkey.flow.controllers.executescript;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.util.ReturnURLString;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.URLException;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

public class ImportRunsForm extends PipelinePathForm
{
    private ReturnURLString _returnUrl;
    private boolean _current;
    private boolean _confirm;

    private String _displayPath;
    private Map<String, String> _newPaths = Collections.emptyMap();
    private String _targetStudy;

    public boolean isConfirm()
    {
        return _confirm;
    }

    public void setConfirm(boolean confirm)
    {
        _confirm = confirm;
    }

    /** Import the current directory named by .getPath() */
    public boolean isCurrent()
    {
        return _current;
    }

    public void setCurrent(boolean current)
    {
        _current = current;
    }

    public String getDisplayPath()
    {
        return _displayPath;
    }

    public void setDisplayPath(String displayPath)
    {
        _displayPath = displayPath;
    }

    public Map<String, String> getNewPaths()
    {
        return _newPaths;
    }

    public void setNewPaths(Map<String, String> newPaths)
    {
        _newPaths = newPaths;
    }

    public String getTargetStudy()
    {
        return _targetStudy;
    }

    public void setTargetStudy(String targetStudy)
    {
        _targetStudy = targetStudy;
    }

    public ReturnURLString getReturnUrl()
    {
        return _returnUrl;
    }

    public void setReturnUrl(ReturnURLString returnUrl)
    {
        _returnUrl = returnUrl;
    }

    @Nullable
    public URLHelper getReturnURLHelper()
    {
        try
        {
            return (null == _returnUrl ? null : new URLHelper(_returnUrl));
        }
        catch (URISyntaxException e)
        {
            throw new URLException(_returnUrl.getSource(), "returnUrl parameter", e);
        }
    }
}

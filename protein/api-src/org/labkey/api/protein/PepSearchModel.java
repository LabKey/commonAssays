/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
package org.labkey.api.protein;

import org.labkey.api.data.Container;
import org.labkey.api.view.ActionURL;

/**
 * Model for the PepSearchView.jsp
 */
public class PepSearchModel
{
    private final ActionURL _resultsUri;
    private String _pepSeq = null;
    private boolean _exact = false;
    private boolean _subfolders = false;
    private String _errorMsg = null;
    private String _runIds = null;

    public PepSearchModel(Container container)
    {
        _resultsUri = ProteinService.get().getPeptideSearchUrl(container);
    }

    public PepSearchModel(Container container, String pepSeq, boolean exact, boolean includeSubfolders, String runIds)
    {
        this(container);
        _pepSeq = pepSeq;
        _exact = exact;
        _subfolders = includeSubfolders;
        _runIds = runIds;
    }

    public ActionURL getResultsUri()
    {
        return _resultsUri;
    }

    public String getPepSeq()
    {
        return null != _pepSeq ? _pepSeq : "";
    }

    public void setPepSeq(String pepSeq)
    {
        _pepSeq = pepSeq;
    }

    public boolean isExact()
    {
        return _exact;
    }

    public void setExact(boolean exact)
    {
        _exact = exact;
    }

    public void setIncludeSubfolders(boolean subfolders)
    {
        _subfolders = subfolders;
    }

    public boolean includeSubfolders()
    {
        return _subfolders;
    }

    public boolean noSearchTerms()
    {
        return (null == _pepSeq || _pepSeq.isEmpty());
    }

    public String getErrorMsg()
    {
        return _errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        _errorMsg = errorMsg;
    }

    public boolean hasErrorMsg()
    {
        return (null != _errorMsg && !_errorMsg.isEmpty());
    }

    public String getRunIds()
    {
        return null == _runIds ? "" : _runIds;
    }
}

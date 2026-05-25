/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.api.protein.search;

import org.labkey.api.action.QueryViewAction;
import org.labkey.api.data.SimpleFilter;

public abstract class PeptideSearchForm extends QueryViewAction.QueryExportForm
{
    public enum ParamNames
    {
        pepSeq,
        exact,
        subfolders,
        runIds
    }

    private String _pepSeq = "";
    private boolean _exact = false;
    private boolean _subfolders = false;
    private String _runIds = null;

    public String getPepSeq()
    {
        return _pepSeq;
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

    public boolean isSubfolders()
    {
        return _subfolders;
    }

    public void setSubfolders(boolean subfolders)
    {
        _subfolders = subfolders;
    }

    public String getRunIds()
    {
        return _runIds;
    }

    public void setRunIds(String runIds)
    {
        _runIds = runIds;
    }

    public abstract SimpleFilter.FilterClause createFilter(String sequenceColumnName);
}

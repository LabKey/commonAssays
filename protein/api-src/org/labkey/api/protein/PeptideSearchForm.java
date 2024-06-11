package org.labkey.api.protein;

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

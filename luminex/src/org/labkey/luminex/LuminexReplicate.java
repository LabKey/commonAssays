package org.labkey.luminex;

/**
 * User: jeckels
 * Date: Sep 1, 2011
 */
public class LuminexReplicate
{
    private String _description;
    private Double _dilution;
    private int _dataId;
    private Double _expConc;

    public LuminexReplicate(String description, Double dilution, int dataId)
    {
        _description = description;
        _dilution = dilution;
        _dataId = dataId;
    }

    public LuminexReplicate(LuminexWell well)
    {
        _description = well._dataRow.getDescription();
        _dilution = well._dataRow.getDilution();
        _dataId = well._dataRow.getData();
        _expConc = well._dataRow.getExpConc();
    }

    public String getDescription()
    {
        return _description;
    }

    public Double getDilution()
    {
        return _dilution;
    }

    public int getDataId()
    {
        return _dataId;
    }

    public Double getExpConc()
    {
        return _expConc;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LuminexReplicate that = (LuminexReplicate) o;

        if (_dataId != that._dataId) return false;
        if (_description != null ? !_description.equals(that._description) : that._description != null) return false;
        if (_dilution != null ? !_dilution.equals(that._dilution) : that._dilution != null) return false;
        if (_expConc != null ? !_expConc.equals(that._expConc) : that._expConc != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _description != null ? _description.hashCode() : 0;
        result = 31 * result + (_dilution != null ? _dilution.hashCode() : 0);
        result = 31 * result + _dataId;
        result = 31 * result + (_expConc != null ? _expConc.hashCode() : 0);
        return result;
    }
}

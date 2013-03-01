package org.labkey.nab;

import org.labkey.api.exp.LsidType;

/**
 * Created with IntelliJ IDEA.
 * User: davebradlee
 * Date: 2/20/13
 * Time: 10:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class NabSpecimen
{
    private int _rowId;
    private int _dataId;
    private int _runId;
    private String _specimenLsid;
    private double _fitError;
    private String _wellgroupName;
    private double _aucPoly;
    private double _positiveAucPoly;
    private double _auc4pl;
    private double _positiveAuc4pl;
    private double _auc5pl;
    private double _positiveAuc5pl;
    private String _objectUri;
    private int _objectId;          // TODO: remove when we remove use of exp.Object
    private int _protocolId;

    public int getDataId()
    {
        return _dataId;
    }

    public void setDataId(int dataId)
    {
        _dataId = dataId;
    }

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public String getSpecimenLsid()
    {
        return _specimenLsid;
    }

    public void setSpecimenLsid(String specimenLsid)
    {
        _specimenLsid = specimenLsid;
    }

    public double getFitError()
    {
        return _fitError;
    }

    public void setFitError(double fitError)
    {
        _fitError = fitError;
    }

    public String getWellgroupName()
    {
        return _wellgroupName;
    }

    public void setWellgroupName(String wellgroupName)
    {
        _wellgroupName = wellgroupName;
    }

    public double getAucPoly()
    {
        return _aucPoly;
    }

    public void setAucPoly(double aucPoly)
    {
        _aucPoly = aucPoly;
    }

    public double getPositiveAucPoly()
    {
        return _positiveAucPoly;
    }

    public void setPositiveAucPoly(double positiveAucPoly)
    {
        _positiveAucPoly = positiveAucPoly;
    }

    public double getAuc4pl()
    {
        return _auc4pl;
    }

    public void setAuc4pl(double auc4pl)
    {
        _auc4pl = auc4pl;
    }

    public double getPositiveAuc4pl()
    {
        return _positiveAuc4pl;
    }

    public void setPositiveAuc4pl(double positiveAuc4pl)
    {
        _positiveAuc4pl = positiveAuc4pl;
    }

    public double getAuc5pl()
    {
        return _auc5pl;
    }

    public void setAuc5pl(double auc5pl)
    {
        _auc5pl = auc5pl;
    }

    public double getPositiveAuc5pl()
    {
        return _positiveAuc5pl;
    }

    public void setPositiveAuc5pl(double positiveAuc5pl)
    {
        _positiveAuc5pl = positiveAuc5pl;
    }

    public String getObjectUri()
    {
        return _objectUri;
    }

    public void setObjectUri(String objectUri)
    {
        _objectUri = objectUri;
    }

    public int getObjectId()
    {
        return _objectId;
    }

    public void setObjectId(int objectId)
    {
        _objectId = objectId;
    }

    public int getProtocolId()
    {
        return _protocolId;
    }

    public void setProtocolId(int protocolId)
    {
        _protocolId = protocolId;
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }
}

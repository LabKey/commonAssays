package org.fhcrc.cpas.flow.data;

import org.fhcrc.cpas.exp.*;
import org.fhcrc.cpas.exp.api.*;
import org.fhcrc.cpas.data.*;
import org.fhcrc.cpas.flow.persist.AttributeSet;
import org.fhcrc.cpas.flow.persist.FlowManager;
import org.fhcrc.cpas.flow.persist.AttrObject;
import org.apache.commons.lang.ObjectUtils;

import java.util.List;
import java.util.ArrayList;

abstract public class FlowDataObject extends FlowObject<ExpData>
{
    static public List<FlowDataObject> fromDatas(ExpData[] datas)
    {
        List<FlowDataObject> ret = new ArrayList();
        for (ExpData data : datas)
        {
            FlowDataObject flowObject = fromData(data);
            if (flowObject == null)
                continue;
            ret.add(flowObject);
        }
        return ret;
    }

    static public FlowDataObject fromData(ExpData data)
    {
        if (data == null)
            return null;
        DataType type = data.getDataType();
        if (!(type instanceof FlowDataType))
            return null;
        AttrObject obj = FlowManager.get().getAttrObject(data);
        if (obj == null)
            return null;
        return ((FlowDataType) type).newInstance(data);
    }

    static public FlowObject fromRowId(int id)
    {
        return fromData(ExperimentService.get().getData(id));
    }

    static public FlowObject fromLSID(String lsid)
    {
        return fromData(ExperimentService.get().getData(lsid));
    }

    static public FlowObject fromAttrObjectId(int id)
    {
        AttrObject obj = FlowManager.get().getAttrObjectFromRowId(id);
        if (obj == null)
            return null;
        return fromRowId(obj.getDataId());
    }

    static public void addDataOfType(ExpData[] datas, FlowDataType typeFilter, List list)
    {
        for (ExpData data : datas)
        {
            DataType type = data.getDataType();
            if (!(type instanceof FlowDataType))
                continue;
            if (typeFilter != null && typeFilter != type)
                continue;
            FlowDataObject obj = fromData(data);
            if (obj != null)
                list.add(obj);
        }
    }

    public FlowDataObject(ExpData data)
    {
        super(data);
    }

    public ExpData getData()
    {
        return getExpObject();
    }
    
    public FlowRun getRun()
    {
        ExpRun run = getData().getRun();
        if (run == null)
            return null;
        return new FlowRun(run);
    }

    public int getRowId()
    {
        return getData().getRowId();
    }

    public ExpProtocolApplication getProtocolApplication()
    {
        return getExpObject().getSourceApplication();
    }

    public int getActionSequence()
    {
        return getExpObject().getSourceApplication().getActionSequence();
    }

    public FlowObject getParent()
    {
        return getRun();
    }

    public String getOwnerObjectLSID()
    {
        return getLSID();
    }

    static public String generateDataLSID(Container container, FlowDataType type)
    {
        return ExperimentService.get().generateGuidLSID(container, type);
    }

    static private FlowDataType dataTypeFromLSID(String lsid)
    {
        Lsid LSID = new Lsid(lsid);
        return FlowDataType.ofNamespace(LSID.getNamespacePrefix());
    }

    public FlowDataType getDataType()
    {
        return (FlowDataType) _expObject.getDataType();
    }

    public String getExperimentLSID()
    {
        FlowRun run = getRun();
        if (run == null)
            return null;
        return getRun().getExperiment().getLSID();
    }

    static public List<FlowDataObject> getForContainer(Container container, FlowDataType type)
    {
        return fromDatas(ExperimentService.get().getDatas(container, type));
    }

    /**
     * Returns true if all objs are in the same experiment.
     */
    static public boolean sameExperiment(List<? extends FlowDataObject> objs)
    {
        if (objs.size() < 2)
            return true;
        String lsidCompare = objs.get(0).getExperimentLSID();
        for (int i = 1; i < objs.size(); i ++)
        {
            if (!ObjectUtils.equals(lsidCompare, objs.get(i).getExperimentLSID()))
                return false;
        }
        return true;
    }

    public FlowRun[] getTargetRuns()
    {
        return FlowRun.fromRuns(getData().getTargetRuns());
    }

    public AttributeSet getAttributeSet()
    {
        return AttributeSet.fromData(getData());
    }
}

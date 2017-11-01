/*
 * Copyright (c) 2005-2017 LabKey Corporation
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

package org.labkey.flow.data;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.flow.persist.AttrObject;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.AttributeSetHelper;
import org.labkey.flow.persist.FlowManager;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

abstract public class FlowDataObject extends FlowObject<ExpData>
{
    static public List<FlowDataObject> fromDatas(List<? extends ExpData> datas)
    {
        List<ExpData> flowDatas = new ArrayList<>(datas.size());
        for (ExpData data : datas)
        {
            if (data.getDataType() instanceof FlowDataType)
                flowDatas.add(data);
        }

        List<AttrObject> attrs = FlowManager.get().getAttrObjects(flowDatas);
        Map<Integer, AttrObject> attrMap = new HashMap<>(2*datas.size());
        for (AttrObject attr : attrs)
        {
            attrMap.put(attr.getDataId(), attr);
        }

        List<FlowDataObject> ret = new ArrayList<>(attrs.size());
        for (ExpData data : flowDatas)
        {
            FlowDataType dataType = (FlowDataType)data.getDataType();
            if (!dataType.isRequireAttrObject() || attrMap.containsKey(data.getRowId()))
            {
                ret.add(((FlowDataType) data.getDataType()).newInstance(data));
            }
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
        if (((FlowDataType)type).isRequireAttrObject())
        {
            AttrObject obj = FlowManager.get().getAttrObject(data);
            if (obj == null)
                return null;
        }
        return ((FlowDataType) type).newInstance(data);
    }

    static public FlowDataObject fromRowId(int id)
    {
        return fromData(ExperimentService.get().getExpData(id));
    }

    static public List<FlowDataObject> fromRowIds(int... ids)
    {
        return fromDatas(ExperimentService.get().getExpDatas(ids));
    }

    static public List<FlowDataObject> fromRowIds(Collection<Integer> ids)
    {
        return fromDatas(ExperimentService.get().getExpDatas(ids));
    }

    static public FlowDataObject fromLSID(String lsid)
    {
        return fromData(ExperimentService.get().getExpData(lsid));
    }

    /** Get FlowObject from ExpData URL */
    static public FlowDataObject fromURI(Container c, URI uri)
    {
        return fromData(ExperimentService.get().getExpDataByURL(uri.toString(), c));
    }

    /** Get FlowObjects that have the same flow.object.uri */
    static public Collection<FlowDataObject> fromAttrObjectURI(Container c, URI uri)
    {
        Collection<AttrObject> objs = FlowManager.get().getAttrObjectsFromURI(c, uri);
        Collection<FlowDataObject> ret = new ArrayList<>(objs.size());
        for (AttrObject obj : objs)
        {
            FlowDataObject flowDataObject = fromRowId(obj.getDataId());
            if (flowDataObject != null)
                ret.add(flowDataObject);
        }
        return ret;
    }

    static public FlowDataObject fromAttrObjectId(int id)
    {
        AttrObject obj = FlowManager.get().getAttrObjectFromRowId(id);
        if (obj == null)
            return null;
        return fromRowId(obj.getDataId());
    }

    static public void addDataOfType(List<? extends ExpData> datas, FlowDataType typeFilter, Collection list)
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

    static public List<FlowDataObject> fromDataType(Container container, @Nullable FlowDataType type)
    {
        return fromDatas(ExperimentService.get().getExpDatas(container, type, null));
    }

    static public List<FlowDataObject> fromName(Container container, @Nullable FlowDataType type, @Nullable String name)
    {
        return fromDatas(ExperimentService.get().getExpDatas(container, type, name));
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
        FlowExperiment experiment = getRun().getExperiment();
        if (experiment == null)
            return null;
        return experiment.getLSID();
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
            if (!Objects.equals(lsidCompare, objs.get(i).getExperimentLSID()))
                return false;
        }
        return true;
    }

    public List<FlowRun> getTargetRuns()
    {
        return FlowRun.fromRuns(getData().getTargetRuns());
    }

    public AttributeSet getAttributeSet()
    {
        return AttributeSetHelper.fromData(getData());
    }

    /*
    public Map<ObjectType, AttributeSet> getAttributeSet(boolean includeKeywords, boolean includeGraphBytes)
    {
        Map<ObjectType, AttributeSet> map = new HashMap<ObjectType, AttributeSet>();
        AttributeSet attrs = AttributeSetHelper.fromData(getData(), includeGraphBytes);
        map.put(attrs.getType(), attrs);

        if (includeKeywords && (this instanceof FlowWell))
        {
            FlowFCSFile fcsFile = ((FlowWell)this).getFCSFileInput();
            if (fcsFile != null && fcsFile != this)
            {
                AttributeSet keywords = fcsFile.getAttributeSet();
                assert keywords.getType() == ObjectType.fcsKeywords;
                if (keywords.getType() == ObjectType.fcsKeywords)
                    map.put(ObjectType.fcsKeywords, keywords);
            }
        }

        return map;
    }
    */

    public AttributeSet getAttributeSet(boolean includeGraphBytes)
    {
        return AttributeSetHelper.fromData(getData(), includeGraphBytes);
    }

}

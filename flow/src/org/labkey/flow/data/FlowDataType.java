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

package org.labkey.flow.data;

import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.settings.AppProps;
import org.labkey.flow.persist.ObjectType;

abstract public class FlowDataType extends DataType
{
    // The prefix of the LSID namepsace prefix :)
    public static final String FLOW_DATA_PREFIX = "Flow-";

    String _name;
    String _label;
    ObjectType _objType;
    boolean _requireAttrObject;

    private FlowDataType(String type, String label, ObjectType objType, boolean requireAttrObject)
    {
        super(FLOW_DATA_PREFIX + type);
        _name = type;
        _label = label;
        _objType = objType;
        _requireAttrObject = requireAttrObject;
    }

    static final public FlowDataType FCSFile = new FlowDataType("FCSFile", "FCS File", ObjectType.fcsKeywords, true)
    {
        public FlowDataObject newInstance(ExpData data)
        {
            return new FlowFCSFile(data);
        }
    };
    static final public FlowDataType FCSAnalysis = new FlowDataType("FCSAnalysis", "FCS Analysis", ObjectType.fcsAnalysis, true)
    {
        public FlowDataObject newInstance(ExpData data)
        {
            return new FlowFCSAnalysis(data);
        }
    };
    static final public FlowDataType CompensationControl = new FlowDataType("CompensationControl", "Comp. Control", ObjectType.compensationControl, true)
    {
        public FlowDataObject newInstance(ExpData data)
        {
            return new FlowCompensationControl(data);
        }
    };
    static final public FlowDataType CompensationMatrix = new FlowDataType("CompensationMatrix", "Comp. Matrix", ObjectType.compensationMatrix, true)
    {
        public FlowDataObject newInstance(ExpData data)
        {
            return new FlowCompensationMatrix(data);
        }
    };
    static final public FlowDataType Script = new FlowDataType("AnalysisScript", "Script", ObjectType.script, true)
    {
        public FlowDataObject newInstance(ExpData data)
        {
            return new FlowScript(data);
        }
    };
    static final public FlowDataType Workspace = new FlowDataType("Workspace", "Workspace", ObjectType.workspace, false)
    {
        public FlowDataObject newInstance(ExpData data)
        {
            return new FlowWorkspace(data);
        }
    };


    static public void register()
    {
        ExperimentService.get().registerDataType(FCSFile);
        ExperimentService.get().registerDataType(FCSAnalysis);
        ExperimentService.get().registerDataType(CompensationControl);
        ExperimentService.get().registerDataType(CompensationMatrix);
        ExperimentService.get().registerDataType(Script);
        ExperimentService.get().registerDataType(Workspace);
    }

    public ObjectType getObjectType()
    {
        return _objType;
    }

    public String getLabel()
    {
        return _label;
    }

    public String getName()
    {
        return _name;
    }

    public boolean isRequireAttrObject()
    {
        return _requireAttrObject;
    }

    public String urlFlag(boolean flagged)
    {
        StringBuilder ret = new StringBuilder();
        ret.append(AppProps.getInstance().getContextPath());
        ret.append("/Flow/");
        if (flagged)
        {
            ret.append("flag");
        }
        else
        {
            ret.append("unflag");
        }
        ret.append(_name);
        ret.append(".gif");
        return ret.toString();
    }

    static public FlowDataType ofNamespace(String namespace)
    {
        DataType ret = ExperimentService.get().getDataType(namespace);
        if (ret instanceof FlowDataType)
        {
            return (FlowDataType) ret;
        }
        return null;
    }
    abstract public FlowDataObject newInstance(ExpData data);
}

/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.LsidManager;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpDataRunInput;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.settings.AppProps;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.persist.ObjectType;

abstract public class FlowDataType extends AssayDataType
{
    // The prefix of the LSID namespace prefix :)
    private static final String FLOW_DATA_PREFIX = "Flow-";

    private final String _name;
    private final String _label;
    private final ObjectType _objType;
    private final boolean _requireAttrObject;

    private FlowDataType(String type, String label, ObjectType objType, boolean requireAttrObject)
    {
        super(FLOW_DATA_PREFIX + type, null, null);
        _name = type;
        _label = label;
        _objType = objType;
        _requireAttrObject = requireAttrObject;

        LsidManager.get().registerHandler(getNamespacePrefix(), new LsidManager.ExpObjectLsidHandler()
        {
            @Override
            public ExpData getObject(Lsid lsid)
            {
                FlowDataObject fdo = FlowDataObject.fromLSID(lsid.toString());
                if (fdo != null)
                    return fdo.getData();

                return null;
            }

            @Nullable
            @Override
            public ActionURL getDisplayURL(Lsid lsid)
            {
                FlowDataObject fdo = FlowDataObject.fromLSID(lsid.toString());
                if (fdo != null)
                {
                    ActionURL url = fdo.urlShow();
                    if (url != null)
                        return url;
                }

                return null;
            }
        });
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

    @Override
    public URLHelper getDetailsURL(ExpData dataObject)
    {
        FlowDataObject fdo = FlowDataObject.fromData(dataObject);
        if (fdo != null)
            return fdo.urlShow();

        return null;
    }

    public ActionURL getDownloadURL(ExpData dataObject)
    {
        FlowDataObject fdo = FlowDataObject.fromData(dataObject);
        if (fdo != null)
            return fdo.urlDownload();

        return null;
    }

    @Override
    public String getRole()
    {
        if (_objType.getInputRole() != null)
            return _objType.getInputRole().toString();
        return ExpDataRunInput.DEFAULT_ROLE;
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

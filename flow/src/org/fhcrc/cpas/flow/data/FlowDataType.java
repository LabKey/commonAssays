package org.fhcrc.cpas.flow.data;

import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.util.AppProps;
import org.labkey.api.util.UnexpectedException;
import org.fhcrc.cpas.flow.persist.ObjectType;

import java.net.URI;
import java.net.URISyntaxException;

abstract public class FlowDataType extends DataType
{
    String _name;
    ObjectType _objType;
    private FlowDataType(String type, ObjectType objType)
    {
        super("Flow-" + type);
        _name = type;
        _objType = objType;
    }

    static final public FlowDataType Log = new FlowDataType("Log", null){
        public FlowDataObject newInstance(ExpData data)
        {
            return null;
        }
    };
    static final public FlowDataType FCSFile = new FlowDataType("FCSFile", ObjectType.fcsKeywords)
    {
        public FlowDataObject newInstance(ExpData data)
        {
            return new FlowFCSFile(data);
        }
    };
    static final public FlowDataType FCSAnalysis = new FlowDataType("FCSAnalysis", ObjectType.fcsAnalysis)
    {
        public FlowDataObject newInstance(ExpData data)
        {
            return new FlowFCSAnalysis(data);
        }
    };
    static final public FlowDataType CompensationControl = new FlowDataType("CompensationControl", ObjectType.compensationControl)
    {
        public FlowDataObject newInstance(ExpData data)
        {
            return new FlowCompensationControl(data);
        }
    };
    static final public FlowDataType CompensationMatrix = new FlowDataType("CompensationMatrix", ObjectType.compensationMatrix)
    {
        public FlowDataObject newInstance(ExpData data)
        {
            return new FlowCompensationMatrix(data);
        }
    };
    static final public FlowDataType Script = new FlowDataType("AnalysisScript", ObjectType.script)
    {
        public FlowDataObject newInstance(ExpData data)
        {
            return new FlowScript(data);
        }
    };

    static public void register()
    {
        ExperimentService.registerDataType(FCSFile);
        ExperimentService.registerDataType(FCSAnalysis);
        ExperimentService.registerDataType(CompensationControl);
        ExperimentService.registerDataType(CompensationMatrix);
        ExperimentService.registerDataType(Script);
    }

    public ObjectType getObjectType()
    {
        return _objType;
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

    public String getNamespace()
    {
        return "Flow-" + toString();
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

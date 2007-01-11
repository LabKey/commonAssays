package Flow.Well;

import org.labkey.api.view.ViewForm;
import org.fhcrc.cpas.flow.data.FlowWell;
import org.fhcrc.cpas.flow.data.FlowScript;
import org.fhcrc.cpas.flow.data.FlowCompensationMatrix;
import org.fhcrc.cpas.flow.data.FlowProtocolStep;

public class ChooseGraphForm extends ViewForm
{
    private int _wellId;
    private int _compId;
    private int _scriptId;
    private int _actionSequence;
    private String _xaxis;
    private String _yaxis;
    private String _subset;


    public void setWellId(int wellId)
    {
        _wellId = wellId;
    }
    public void setCompId(int compId)
    {
        _compId = compId;
    }
    public void setScriptId(int scriptId)
    {
        _scriptId = scriptId;
    }

    public int getWellId()
    {
        return _wellId;
    }

    public int getCompId()
    {
        if (_compId != 0)
            return _compId;
        FlowCompensationMatrix matrix = getWell().getCompensationMatrix();
        if (matrix != null)
            return matrix.getRowId();
        return 0;
    }

    public int getScriptId()
    {
        if (_scriptId != 0)
            return _scriptId;
        FlowScript script = getWell().getScript();
        if (script != null)
            return script.getRowId();
        return 0;
    }

    private boolean isActionSequence(int i)
    {
        return i == FlowProtocolStep.calculateCompensation.getDefaultActionSequence() ||
                i == FlowProtocolStep.analysis.getDefaultActionSequence();
    }

    public int getActionSequence()
    {
        int ret = _actionSequence;
        if (!isActionSequence(ret))
        {
            ret = getWell().getProtocolApplication().getActionSequence();
        }
        if (!isActionSequence(ret))
        {
            ret = FlowProtocolStep.analysis.getDefaultActionSequence();
        }
        return ret;
    }

    public void setActionSequence(int step)
    {
        _actionSequence = step;
    }

    public String getXaxis()
    {
        return _xaxis;
    }

    public void setXaxis(String xaxis)
    {
        _xaxis = xaxis;
    }
    public void setYaxis(String yaxis)
    {
        _yaxis = yaxis;
    }
    public String getYaxis()
    {
        return _yaxis;
    }
    public void setSubset(String subset)
    {
        _subset = subset;
    }
    public String getSubset()
    {
        return _subset;
    }

    public FlowWell getWell()
    {
        return FlowWell.fromWellId(getWellId());
    }

    public FlowScript getScript()
    {
        if (_scriptId != 0)
        {
            return FlowScript.fromScriptId(_scriptId);
        }
        return getWell().getScript();
    }

    public FlowCompensationMatrix getCompensationMatrix()
    {
        if (_compId != 0)
        {
            return FlowCompensationMatrix.fromCompId(_compId);
        }
        return getWell().getCompensationMatrix();
    }
}

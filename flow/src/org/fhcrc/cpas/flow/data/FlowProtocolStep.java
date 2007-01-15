package org.fhcrc.cpas.flow.data;

import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpProtocolAction;
import org.labkey.api.security.User;
import org.labkey.api.data.*;
import org.labkey.api.view.ViewURLHelper;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import Flow.FlowParam;

public class FlowProtocolStep
{
    static public FlowProtocolStep keywords = new FlowProtocolStep("Keywords", "Read a directory containing FCS files", ExpProtocol.ApplicationType.ProtocolApplication, 10);
    static public FlowProtocolStep calculateCompensation = new FlowProtocolStep("Compensation", "Calculate the compensation matrix", ExpProtocol.ApplicationType.ProtocolApplication, 20);
    static public FlowProtocolStep analysis = new FlowProtocolStep("Analysis", "Calculate statistics and generate graphs for Flow Data", ExpProtocol.ApplicationType.ProtocolApplication, 30);
    static public FlowProtocolStep markRunOutputs = new FlowProtocolStep("MarkRunOutputData", "", ExpProtocol.ApplicationType.ExperimentRunOutput, 1000, keywords, calculateCompensation, analysis);

    final String lsidName;
    final String description;
    final ExpProtocol.ApplicationType applicationType;
    final int actionSequence;
    final FlowProtocolStep[] predecessors;

    public FlowProtocolStep(String lsidName, String protocolDescription, ExpProtocol.ApplicationType applicationType, int actionSequence, FlowProtocolStep ... predecessors)
    {
        this.lsidName = lsidName;
        this.description = protocolDescription;
        this.applicationType = applicationType;
        this.actionSequence = actionSequence;
        this.predecessors = predecessors;
    }

    public ExpProtocolAction addAction(User user, ExpProtocol parentProtocol) throws Exception
    {
        return parentProtocol.addStep(user, ensureForContainer(user, parentProtocol.getContainer()).getExpObject(), getDefaultActionSequence());
    }

    static public void addProtocolSteps(User user, Container container, Protocol parentProtocol) throws SQLException
    {
        /*ProtocolAction runInputsAction = addAction(user, parentProtocol, parentProtocol, 0);
        ProtocolAction loadRunAction = loadRun.addAction(user, container, parentProtocol);
        ProtocolAction calculateCompensationAction = calculateCompensation.addAction(user, container, parentProtocol);
        ProtocolAction analysisAction = analysis.addAction(user, container, parentProtocol);
        ProtocolAction markRunOutputsAction = markRunOutputs.addAction(user, container, parentProtocol);
        insertPredecessors(user, runInputsAction, runInputsAction); // ? every action must have a predecessor, or we hit
                                                                    // xml errors exporting the protocol. 

        insertPredecessors(user, loadRunAction, runInputsAction);
        insertPredecessors(user, calculateCompensationAction, runInputsAction, loadRunAction);
        insertPredecessors(user, analysisAction, runInputsAction, calculateCompensationAction, loadRunAction);
        insertPredecessors(user, markRunOutputsAction, runInputsAction, loadRunAction, calculateCompensationAction, analysisAction);*/
    }

    public String getLSID(Container container)
    {
        return FlowObject.generateLSID(container, "Protocol", lsidName);
    }

    public String getLSID()
    {
        throw new UnsupportedOperationException();
    }

    public String getOwnerObjectLSID()
    {
        throw new UnsupportedOperationException();
    }

    public String getName()
    {
        return lsidName;
    }

    public int getDefaultActionSequence()
    {
        return actionSequence;
    }

    public void addParams(ViewURLHelper url)
    {
        url.addParameter(FlowParam.actionSequence.toString(), Integer.toString(getDefaultActionSequence()));
    }

    static public FlowProtocolStep fromActionSequence(Integer actionSequence)
    {
        if (actionSequence == null)
            return null;
        if (actionSequence == keywords.getDefaultActionSequence())
            return keywords;
        if (actionSequence == calculateCompensation.getDefaultActionSequence())
            return calculateCompensation;
        if (actionSequence == analysis.getDefaultActionSequence())
            return analysis;
        return null;
    }

    static public FlowProtocolStep fromRequest(HttpServletRequest request)
    {
        String strActionSequence = request.getParameter("actionSequence");
        if (strActionSequence == null || strActionSequence.length() == 0)
            return null;
        return fromActionSequence(Integer.valueOf(strActionSequence));
    }

    public FlowProtocol ensureForContainer(User user, Container container) throws Exception
    {
        FlowProtocol ret = getForContainer(container);
        if (ret != null)
            return ret;
        ExpProtocol protocol = ExperimentService.get().createExpProtocol(container, getName(), applicationType);
        protocol.setDescription(description);
        protocol.save(user);
        return new FlowProtocol(protocol);
    }

    public FlowProtocol getForContainer(Container container)
    {
        return FlowProtocol.getForContainer(container, getName());
    }

    public void addParams(Map<FlowParam, Object> map)
    {
        map.put(FlowParam.actionSequence, getDefaultActionSequence());
    }

    public String getContainerId()
    {
        throw new UnsupportedOperationException();
    }

    public String getLabel()
    {
        return getName();
    }

    public FlowObject getParent()
    {
        return null;
    }

    public ViewURLHelper urlShow()
    {
        throw new UnsupportedOperationException();
    }

    static public void initProtocol(User user, FlowProtocol flowProtocol) throws Exception
    {
        ExpProtocol protocol = flowProtocol.getExpObject();
        ExpProtocolAction stepRun = protocol.addStep(user, protocol, 0);
        stepRun.addSuccessor(user, stepRun);
        List<ExpProtocolAction> steps = new ArrayList();
        steps.add(keywords.addAction(user, protocol));
        steps.add(calculateCompensation.addAction(user, protocol));
        steps.add(analysis.addAction(user, protocol));

        ExpProtocolAction stepMarkRunOutputs = markRunOutputs.addAction(user, protocol);
        for (ExpProtocolAction step : steps)
        {
            stepRun.addSuccessor(user, step);
            step.addSuccessor(user, stepMarkRunOutputs);
        }
    }

    static public FlowProtocolStep fromLSID(Container container, String lsid)
    {
        if (lsid.equals(keywords.getLSID(container)))
            return keywords;
        if (lsid.equals(calculateCompensation.getLSID(container)))
            return calculateCompensation;
        if (lsid.equals(analysis.getLSID(container)))
            return analysis;
        return null;
    }
}

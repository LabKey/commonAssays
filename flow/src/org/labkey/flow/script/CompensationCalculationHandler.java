package org.labkey.flow.script;

import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.CompensationCalculation;
import org.labkey.flow.analysis.web.CompensationResult;
import org.labkey.flow.analysis.web.*;
import org.labkey.flow.analysis.web.CompSign;
import org.fhcrc.cpas.flow.script.xml.CompensationCalculationDef;
import org.fhcrc.cpas.flow.script.xml.SettingsDef;
import org.labkey.flow.data.*;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.FlowDataHandler;
import org.labkey.flow.persist.ObjectType;
import org.fhcrc.cpas.exp.xml.ExperimentRunType;
import org.fhcrc.cpas.exp.xml.ProtocolApplicationBaseType;
import org.fhcrc.cpas.exp.xml.DataBaseType;

import java.util.*;
import java.io.File;

public class CompensationCalculationHandler extends BaseHandler
{
    SettingsDef _settings;
    CompensationCalculationDef _compensationCalculationElement;

    public CompensationCalculationHandler(ScriptJob job, SettingsDef settings, CompensationCalculationDef calc) throws Exception
    {
        super(job, FlowProtocolStep.calculateCompensation);
        _settings = settings;
        _compensationCalculationElement = calc;
    }

    public void processRun(FlowRun run, ExperimentRunType runElement, File outputDirectory) throws Exception
    {
        _job.addStatus("Calculating compensation matrix for " + run.getName());
        List<FCSRef> uris = FlowAnalyzer.getFCSRefs(run);
        CompensationCalculation calc = FlowAnalyzer.makeCompensationCalculation(_settings, _compensationCalculationElement);
        List<CompensationResult> results = new ArrayList();
        CompensationMatrix matrix = FCSAnalyzer.get().calculateCompensationMatrix(uris, calc, results);
        ProtocolApplicationBaseType appComp = addProtocolApplication(runElement);
        AttributeSet attrsComp = new AttributeSet(matrix);
        DataBaseType dbtComp = appComp.getOutputDataObjects().addNewData();
        dbtComp.setName(run.getName());
        dbtComp.setAbout(FlowDataObject.generateDataLSID(getContainer(), FlowDataType.CompensationMatrix));
        dbtComp.setSourceProtocolLSID(_step.getLSID(_job.getContainer()));
        attrsComp.save(_job.decideFileName(outputDirectory, "Compensation", FlowDataHandler.EXT_DATA), dbtComp);
        _job.addRunOutput(dbtComp.getAbout(), InputRole.CompensationMatrix);

        for (CompensationResult result : results)
        {
            ProtocolApplicationBaseType app = addProtocolApplication(runElement);
            FlowWell well = run.findFCSFile(result.getURI());
            AttributeSet attrs = new AttributeSet(ObjectType.compensationControl, result.getURI());
            if (well != null)
            {
                addDataLSID(app.getInputRefs(), well.getLSID(), InputRole.FCSFile);
                addDataLSID(appComp.getInputRefs(), well.getLSID(), null);
                addDataLSID(app.getInputRefs(), dbtComp.getAbout(), InputRole.CompensationMatrix);
            }
            List<FCSAnalyzer.Result> wellResults = result.getResults();
            if (wellResults.size() != 0)
            {
                DataBaseType dbtWell = duplicateWell(app, well, FlowDataType.CompensationControl);
                String name = result.getChannelName();
                if (result.getSign() == CompSign.positive)
                {
                    name += "+";
                }
                else
                {
                    name += "-";
                }
                dbtWell.setName(name);
                addResults(dbtWell, attrs, wellResults);
                attrs.save(_job.decideFileName(outputDirectory, "control", FlowDataHandler.EXT_DATA), dbtWell);
            }
        }

    }
}

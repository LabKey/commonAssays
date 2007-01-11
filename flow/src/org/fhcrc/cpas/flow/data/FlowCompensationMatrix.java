package org.fhcrc.cpas.flow.data;

import org.labkey.api.exp.api.ExpData;
import org.fhcrc.cpas.exp.xml.DataBaseType;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.data.Container;
import org.fhcrc.cpas.flow.persist.AttributeSet;
import com.labkey.flow.model.CompensationMatrix;
import com.labkey.flow.web.StatisticSpec;

import java.util.*;
import java.sql.SQLException;

import Flow.Run.RunController;
import Flow.FlowParam;
import Flow.Compensation.CompensationController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class FlowCompensationMatrix extends FlowDataObject
{
    public FlowCompensationMatrix(ExpData data)
    {
        super(data);
    }

    static public FlowCompensationMatrix fromCompId(int id)
    {
        return (FlowCompensationMatrix) FlowDataObject.fromRowId(id);
    }

    static public FlowCompensationMatrix fromURL(ViewURLHelper url, HttpServletRequest request) throws Exception
    {
        FlowCompensationMatrix ret = fromCompId(getIntParam(url, request, FlowParam.compId));
        ret.checkContainer(url);
        return ret;
    }

    public CompensationMatrix getCompensationMatrix() throws SQLException
    {
        return getCompensationMatrix(getAttributeSet());
    }

    static public CompensationMatrix getCompensationMatrix(AttributeSet attrs)
    {
        TreeSet<String> channelNames = new TreeSet();
        Map<String, Double> values = new HashMap();
        for (Map.Entry<StatisticSpec, Double> entry : attrs.getStatistics().entrySet())
        {
            StatisticSpec spec = entry.getKey();
            if (spec.getStatistic() != StatisticSpec.STAT.Spill)
                continue;
            String strParameter = spec.getParameter();
            int ichColon = strParameter.indexOf(":");

            String strChannel = strParameter.substring(0, ichColon);
            channelNames.add(strChannel);
            values.put(spec.getParameter(), entry.getValue());
        }
        if (channelNames.size() == 0)
            return null;
        CompensationMatrix ret = new CompensationMatrix("Compensation");
        String[] arrChannelNames = channelNames.toArray(new String[0]);

        for (int iChannel = 0; iChannel < arrChannelNames.length; iChannel ++)
        {
            Map<String, Double> channelValues = new TreeMap();
            for (int iChannelValue = 0; iChannelValue < arrChannelNames.length; iChannelValue ++)
            {
                String key = arrChannelNames[iChannel] + ":" + arrChannelNames[iChannelValue];
                channelValues.put(arrChannelNames[iChannelValue], values.get(key));
            }
            ret.setChannel(arrChannelNames[iChannel], channelValues);
        }
        return ret;
    }

    public ViewURLHelper urlShow()
    {
        return urlFor(CompensationController.Action.showCompensation);
    }

    public void addParams(Map<FlowParam, Object> map)
    {
        map.put(FlowParam.compId, getCompId());
    }


    public String getLabel()
    {
        return getLabel(false);
    }

    public String getLabel(boolean includeExperiment)
    {
        FlowRun run = getRun();
        if (run != null)
        {
            String strLabel = run.getLabel();
            if (includeExperiment)
            {
                FlowExperiment experiment = run.getExperiment();
                if (experiment != null)
                {
                    return strLabel + " (" + experiment.getLabel() + ")";
                }
            }
            return strLabel;
        }
        return getName();
    }
    public int getCompId()
    {
        return getRowId();
    }

    static public List<FlowCompensationMatrix> getCompensationMatrices(Container container)
    {
        return (List) FlowDataObject.getForContainer(container, FlowDataType.CompensationMatrix);
    }
    static public List<FlowCompensationMatrix> getUploadedCompensationMatrices(Container container)
    {
        List<FlowCompensationMatrix> all = getCompensationMatrices(container);
        List<FlowCompensationMatrix> ret = new ArrayList();
        for (FlowCompensationMatrix comp : all)
        {
            if (comp.getRun() == null)
                ret.add(comp);
        }
        return ret;
    }
}

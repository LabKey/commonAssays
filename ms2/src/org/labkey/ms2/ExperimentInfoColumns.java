package org.labkey.ms2;

import org.labkey.api.exp.Data;
import org.labkey.api.exp.Material;
import org.labkey.api.exp.ExperimentRun;
import org.labkey.api.exp.Protocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.util.PageFlowUtil;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.SQLException;
import java.io.Writer;
import java.io.IOException;

/**
     * Class holds a bunch of info about all the experiments in this folder.
 */
public class ExperimentInfoColumns
{
    private Map<String, List<Data>> runInputData;
    private Map<String, List<Material>> runInputMaterial;
    private Map<String, String> dataCreatingRuns;

    public ExperimentInfoColumns(Container c)
    {
        runInputData = ExperimentService.get().getRunInputData(c);
        runInputMaterial = ExperimentService.get().getRunInputMaterial(c);
        dataCreatingRuns = ExperimentService.get().getDataCreatingRuns(c);
    }

    private String getDataCreatingRun(String lsid)
    {
        if (dataCreatingRuns.containsKey(lsid))
        {
            return dataCreatingRuns.get(lsid);
        }
        String result = ExperimentService.get().getDataCreatingRun(lsid);
        dataCreatingRuns.put(lsid, result);
        return result;
    }

    private List<Data> getRunInputData(String lsid)
    {
        List<Data> result = runInputData.get(lsid);
        if (result == null)
        {
            try
            {
                result = ExperimentService.get().getRunInputData(lsid);
                runInputData.put(lsid, result);
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        return result;
    }

    private List<Material> getRunInputMaterial(String lsid)
    {
        List<Material> result = runInputMaterial.get(lsid);
        if (result == null)
        {
            try
            {
                result = ExperimentService.get().getRunInputMaterial(lsid);
                runInputMaterial.put(lsid, result);
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        return result;
    }

    public SampleColumn getSampleColumn()
    {
        return new SampleColumn();
    }

    public ProtocolColumn getProtocolColumn()
    {
        return new ProtocolColumn();
    }

    public class SampleColumn extends SimpleDisplayColumn
    {
        public SampleColumn()
        {
            setWidth(null);
            setCaption("Sample");
        }

        @Override
        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            assert ctx.containsKey("ExperimentRunLSID") : "Data region should select ExperimentRunLSID column";
            String runLSID = (String) ctx.get("ExperimentRunLSID");
            int run = ((Integer)ctx.get("Run")).intValue();
            calculateAndRenderLinks(runLSID, ctx.getViewContext().getViewURLHelper(), run, out, true);
        }

        public void calculateAndRenderLinks(String runLSID, ViewURLHelper urlHelper, int run, Writer out, boolean collapse)
                throws IOException
        {
            //TODO: Should be able to annotate run from here.
            if (null == runLSID)
                return;

            //Render links for all the direct and indirect inputs.
            List<Material> inputMaterials = new ArrayList<Material>(getRunInputMaterial(runLSID));
            List<Data> inputData = getRunInputData(runLSID);
            for (Data d : inputData)
            {
                String creatingRunLsid = getDataCreatingRun(d.getLSID());
                if (null != creatingRunLsid)
                {
                    List<Material> tmpM = getRunInputMaterial(creatingRunLsid);
                    for (Material m : tmpM)
                    {
                        boolean foundMatch = false;
                        for (Material m2 : inputMaterials)
                        {
                            if (m2.getLSID().equals(m.getLSID()))
                            {
                                foundMatch = true;
                                break;
                            }
                        }
                        if (!foundMatch)
                        {
                            inputMaterials.add(m);
                        }
                    }
                }
            }
            renderMaterialLinks(inputMaterials, urlHelper, run, out, collapse);
        }

        private void renderMaterialLinks(List<Material> inputMaterials, ViewURLHelper currentURL, int run, Writer out, boolean collapse) throws IOException
        {
            ViewURLHelper resolveHelper = currentURL.clone();
            String resolveURL = resolveHelper.relativeUrl("resolveLSID.view", "lsid=", "Experiment");
            ViewURLHelper fullMaterialListHelper = currentURL.clone();
            String fullMaterialListURL = fullMaterialListHelper.relativeUrl("showFullMaterialList.view", "run=" + run, "MS2");

            if (collapse && inputMaterials.size() > 2)
            {
                renderMaterialLink(out, resolveURL, inputMaterials.get(0));
                out.write(",<br/><a href=\"");
                out.write(fullMaterialListURL);
                StringBuilder sb = new StringBuilder();
                String sep = "";
                for (int i = 1; i < inputMaterials.size(); i++)
                {
                    sb.append(sep);
                    sb.append(PageFlowUtil.filter(inputMaterials.get(i).getName()));
                    sep = ", ";
                }
                out.write("\" title=\"");
                out.write(sb.toString());
                out.write("\">");
                out.write(Integer.toString(inputMaterials.size() - 1));
                out.write(" more samples...</a>");
            }
            else
            {
                String sep = "";
                for (Material mat : inputMaterials)
                {
                    out.write(sep);
                    renderMaterialLink(out, resolveURL, mat);
                    sep = ",<br/>";
                }
            }
        }

        private void renderMaterialLink(Writer out, String resolveURL, Material mat)
                throws IOException
        {
            out.write("<a href='");
            out.write(resolveURL);
            out.write(PageFlowUtil.filter(mat.getLSID()));
            out.write("'>");
            out.write(PageFlowUtil.filter(mat.getName()));
            out.write("</a>");
        }

    }

    public class ProtocolColumn extends SimpleDisplayColumn
    {
        public ProtocolColumn()
        {
            setWidth(null);
            setCaption("Protocol");
        }

        @Override
        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            assert ctx.containsKey("ExperimentRunLSID") : "Data region should select ExperimentRunLSID column";
            assert ctx.containsKey("ExperimentRunRowId") : "Data region should select ExperimentRunRowId column";
            assert ctx.containsKey("ProtocolName") : "Data region should select ProtocolName column";
            String runLSID = (String) ctx.get("ExperimentRunLSID");
            if (null == runLSID)
                return;
            Integer runRowId = (Integer) ctx.get("ExperimentRunRowId");
            String protocolName = (String) ctx.get("ProtocolName");

            String runLink = getRunLink(ctx, runRowId, protocolName);
            List<Data> inputData = getRunInputData(runLSID);
            if (null == inputData)
            {
                out.write(runLink);
                return;
            }

            Map<String, String> predecessorLinks = new HashMap<String, String>();

            for (Data data : inputData)
            {
                //TODO: Make this faster. All of this should be queried at the same time..
                //ExperimentRun and Protocol are cached however, so in usual cases this isn't tragic
                String creatingRunLsid = getDataCreatingRun(data.getLSID());
                if (null == creatingRunLsid)
                    continue;
                if (predecessorLinks.containsKey(creatingRunLsid))
                    continue;

                ExperimentRun run = ExperimentService.get().getExperimentRun(creatingRunLsid);
                Protocol p = ExperimentService.get().getProtocol(run.getProtocolLSID());
                predecessorLinks.put(creatingRunLsid, getRunLink(ctx, run.getRowId(), p.getName()));
            }

            if (predecessorLinks.values().size() == 0)
                out.write(runLink);
            else
            {
                String sep = "";
                for (String s : predecessorLinks.values())
                {
                    out.write(sep);
                    out.write(s);
                    sep = ", ";
                }
                out.write(" -> ");
                out.write(runLink);
            }
        }

        public String getRunLink(RenderContext ctx, Integer experimentRunRowId, String protocolName)
        {
            ViewURLHelper helper = ctx.getViewContext().getViewURLHelper();
            String resolveURL = helper.relativeUrl("showRunGraph.view", "rowId=", "Experiment");

            return "<a href='" + resolveURL + experimentRunRowId + "'>" + PageFlowUtil.filter(protocolName) + "</a>";
        }
    }
}

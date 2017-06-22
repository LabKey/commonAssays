/*
 * Copyright (c) 2005-2017 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2.pipeline.mascot;

import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Peptide;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2RunType;
import org.labkey.ms2.peptideview.AbstractMS2RunView;
import org.labkey.ms2.peptideview.MS2RunViewType;
import org.labkey.ms2.peptideview.QueryPeptideMS2RunView;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

/**
 * User: arauch
 * Date: Jul 21, 2005
 * Time: 10:19:11 PM
 */
public class MascotRun extends MS2Run
{
    private String mascotFile;
    private String distillerRawFile;

    @Override
    public void adjustScores(Map<String, String> map)
    {
        // Mascot exported pepXML can exclude "homologyscore"
        if (null == map.get("homologyscore"))
            map.put("homologyscore", "-1");
        // Issue 30322 - ProteomeDiscoverer pep.xml files use a different name for the score value
        if (null == map.get("ionscore") && map.containsKey("Ions Score"))
            map.put("ionscore", map.get("Ions Score"));
    }

    public MS2RunType getRunType()
    {
        return MS2RunType.Mascot;
    }

    public String getParamsFileName()
    {
        return "mascot.xml";
    }


    public String getChargeFilterColumnName()
    {
        return "Ion";
    }


    public String getChargeFilterParamName()
    {
        return "ion";
    }

    public String getDiscriminateExpressions()
    {
        return "-Identity";
    }

    public String[] getGZFileExtensions()
    {
        return new String[]{"out", "dta"};
    }

    public String getMascotFile()
    {
        return mascotFile;
    }

    public void setMascotFile(String mascotFile)
    {
        this.mascotFile = mascotFile;
    }

    public String getDistillerRawFile()
    {
        return distillerRawFile;
    }

    public void setDistillerRawFile(String distillerRawFile)
    {
        this.distillerRawFile = distillerRawFile;
    }

    @Override
    protected ModelAndView getAdditionalRunSummaryView(MS2Controller.RunForm form)
    {
        // Add the Mascot decoy Summary

        MS2Manager.DecoySummaryBean decoySummary = MS2Manager.getDecoySummaryForRun(getRun(), form.desiredFdrToFloat());
        if (null != decoySummary)
        {
            JspView<MS2Manager.DecoySummaryBean> decoySummaryView = new JspView<>("/org/labkey/ms2/decoySummary.jsp", decoySummary);
            decoySummaryView.setFrame(WebPartView.FrameType.PORTAL);
            decoySummaryView.setTitle("Decoy Summary");
            return decoySummaryView;
        }
        else return null;
    }

    @Override
    protected ModelAndView getAdditionalPeptideSummaryView(ViewContext viewContext, MS2Peptide peptide, String grouping)
    {
        final String title = "All Matches To This Query";
        AbstractMS2RunView altPeptideView = MS2RunViewType.getViewType(grouping).createView(viewContext, this);

        if (altPeptideView instanceof QueryPeptideMS2RunView)
        {
            SimpleFilter altPeptideFilter = new SimpleFilter(FieldKey.fromParts("scan"), peptide.getScan());
            altPeptideFilter.addCondition(FieldKey.fromParts("fraction"), peptide.getFraction());
            altPeptideFilter.addCondition(FieldKey.fromParts("charge"), peptide.getCharge());

            QueryPeptideMS2RunView.PeptideQueryView altPeptideGrid = (QueryPeptideMS2RunView.PeptideQueryView) ((QueryPeptideMS2RunView)altPeptideView).createGridView(altPeptideFilter);
            altPeptideGrid.setTitle(title);
            altPeptideGrid.addDisplayColumn(new CurrentPeptideColumn(peptide.getRowId()));
            return altPeptideGrid;
        }
        else
        {
            return new WebPartView(title) {
                @Override
                protected void renderView(Object model, PrintWriter out) throws Exception
                {
                    out.write("<div>Use the 'Standard' grouping to view this information.</div>");
                }
            };
        }
    }

    private static final class CurrentPeptideColumn extends SimpleDisplayColumn
    {
        private final Long _currentPeptideId;

        public CurrentPeptideColumn(long currentPeptideId)
        {
            _currentPeptideId = currentPeptideId;
            setCaption("Current View");
        }

        public void renderDetailsCellContents(RenderContext ctx, Writer out) throws IOException
        {
            renderGridCellContents(ctx, out);
        }

        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            if (_currentPeptideId.equals(ctx.getRow().get("rowId")))
            {
                out.write("<b>&#x2714;</b>"); // html checkmark
            }
        }
    }
}

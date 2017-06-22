/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
package org.labkey.flow.analysis.web;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.writer.PrintWriters;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.persist.AnalysisSerializer;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 * Date: 2/21/12
 *
 * Manual plot tests:  This test doesn't assert anything, but generates
 * a series of plots into 'flow-plots' directory in the user's home directory
 * and opens a browser for examination.
 */
public class PlotTests extends Assert
{
    private Workspace loadWorkspace(File file) throws Exception
    {
        return Workspace.readWorkspace(file.getName(), file.getPath(), new FileInputStream(file));
    }

    private File dataDir()
    {
        return new File(System.getProperty("user.home") + "/flow/deident");
    }

    private File outDir()
    {
        //return FileUtil.createTempDirectory("flow-temp");
        return new File(System.getProperty("user.home") + "/flow-plots");
    }


    private Map<String, GraphSpec> dumpPlots(File outDir, CompensationMatrix comp, Analysis analysis, File fcsFile) throws Exception
    {
        if (outDir.exists())
            FileUtil.deleteDir(outDir);
        if (!outDir.mkdirs())
            throw new Exception("Failed to create output dir: " + outDir);
        System.out.println("Generating graphs for '" + fcsFile.getName() + "'...");

        Map<String, GraphSpec> images = new LinkedHashMap<>();
        URI uri = fcsFile.toURI();
        List<FCSAnalyzer.GraphResult> graphs = FCSAnalyzer.get().generateGraphs(uri, comp, analysis, analysis.getGraphs());
        for (FCSAnalyzer.GraphResult graph : graphs)
        {
            String imgName = AnalysisSerializer.generateFriendlyImageName(graph.spec);
            File image = new File(outDir, imgName);
            FileOutputStream fos = new FileOutputStream(image);
            IOUtils.write(graph.bytes, fos);
            images.put(imgName, graph.spec);
            System.out.println("  " + graph.spec);
        }
        return images;
    }

    private Map<String, GraphSpec> generatePlots(File outDir, Workspace workspace, File fcsFile) throws Exception
    {
        Workspace.SampleInfo sample = workspace.getSample(fcsFile.getName());
        Analysis analysis = workspace.getSampleAnalysis(sample);
        CompensationMatrix comp = sample.getCompensationMatrix();

        return dumpPlots(outDir, comp, analysis, fcsFile);
    }

    private void compare(File outDir, File workspaceFile, File fcsFile, File expectedImageDir, Map<String, GraphSpec> generatedImages) throws IOException
    {
        generateHtml(outDir, workspaceFile, fcsFile, expectedImageDir, generatedImages);

        Desktop desktop = Desktop.getDesktop();
        desktop.browse(new File(outDir, "index.html").toURI());
    }

    private void generateHtml(File outDir, File workspaceFile, File fcsFile, File expectedImageDir, Map<String, GraphSpec> generatedImages) throws IOException
    {
        String[] expectedImages = expectedImageDir.list((file, s) -> s.endsWith(".png"));

        Map<String, GraphSpec> allImages = new LinkedHashMap<>(generatedImages);
        for (String expectedImage : expectedImages)
        {
            if (!allImages.containsKey(expectedImage))
                allImages.put(expectedImage, null);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<title>").append(workspaceFile.getName()).append(" plot comparison</title>");
        sb.append("<body>");

        sb.append("<div><b>Workspace:</b> ").append(workspaceFile.getAbsolutePath()).append("</div>");
        sb.append("<div><b>FCS File:</b> ").append(fcsFile.getAbsolutePath()).append("</div>");
        sb.append("<div><b>Expected Images:</b> ").append(expectedImageDir.getAbsolutePath()).append("</div>");
        sb.append("<div><b>Generated Images:</b> ").append(outDir.getAbsolutePath()).append("</div>");

        sb.append("<p>");
        sb.append("Things to look for:");
        sb.append("<ul>");
        sb.append("<li>Population name matches");
        sb.append("<li>Axis names match");
        sb.append("<li>Axis range is the same (min and max values; number of decades)");
        sb.append("<li>Axis scale is the same (lin, log, fancy log)");
        sb.append("<li>Gate location approximately the same");
        sb.append("<li>Plotted events look approximately the same (compensation applied only once, etc.)");
        sb.append("<li>No &lt;Unexpected Population&gt; plots");
        sb.append("</ul>");

        sb.append("<center>");
        sb.append("<table cellpadding=0 cellspacing=0 border=0>");
        sb.append("<tr>");
        sb.append("<td align=center><b>FlowJo</b></td><td align=center><b>LabKey</b></td>");
        sb.append("</tr>");

        for (Map.Entry<String, GraphSpec> image : allImages.entrySet())
        {
            String imageName = image.getKey();
            GraphSpec spec = image.getValue();

            sb.append("<tr style='padding-bottom:2em;'>");
            sb.append("<td colspan=2 align=center><b>");
            if (spec == null)
                sb.append("&lt;Unexpected Population&gt; ").append(imageName);
            else
                sb.append(spec.toString().replaceAll("<", "&lt;"));
            sb.append("</b></td>");
            sb.append("</tr>");
            sb.append("<tr>");
            sb.append("<td valign=top><img src='").append(new File(expectedImageDir, imageName)).append("'></td>");
            sb.append("<td valign=top><img src='").append(new File(outDir, imageName)).append("'></td>");
            sb.append("</tr>");
        }

        sb.append("</tr>");
        sb.append("</table>");
        sb.append("</center>");

        sb.append("</body>");
        sb.append("</html>");

        try (PrintWriter writer = PrintWriters.getPrintWriter(new File(outDir, "index.html")))
        {
            writer.append(sb.toString());
            writer.flush();
        }
    }

    public void generatePlotsAndCompare(File outDir, File workspaceFile, File fcsFile, File expectedImageDir) throws Exception
    {
        assertTrue("Workspace doesn't exist: " + workspaceFile, workspaceFile.exists());
        assertTrue("FCS file doesn't exist: " + fcsFile, fcsFile.exists());
        assertTrue("Expected image dir does't exist: " + expectedImageDir, expectedImageDir.exists());

        Workspace workspace = loadWorkspace(workspaceFile);
        Map<String, GraphSpec> imageNames = generatePlots(outDir, workspace, fcsFile);
        compare(outDir, workspaceFile, fcsFile, expectedImageDir, imageNames);
    }

    // BUGBUG: Histogram plots have multiple gates drawn on them.
    // BUGBUG: Histogram counts are low but the curve looks correct. Scaling issue?
    // BUGBUG: ForSc is not scaled by gain (see comment in FCSHeader.createDataFrame())
    @Test
    public void advanced() throws Exception
    {
        File outDir         = new File(outDir(), "flow/advanced");
        File workspaceFile  = JunitUtil.getSampleData(null, "flow/advanced/advanced-v7.6.5.wsp");
        File fcsFile        = JunitUtil.getSampleData(null, "flow/advanced/931115-B02- Sample 01.fcs");
        File expectedImages = JunitUtil.getSampleData(null, "flow/advanced/931115-B02_graphs_v7.6.5");

        generatePlotsAndCompare(outDir, workspaceFile, fcsFile, expectedImages);
    }

    // Issue 14170:
    // S(Time:<Pacific Blue-A> plot uses range of 0-4000 instead of FlowJo's 0-40
    // which causes the Exclude gate to be squished.
    @Test
    public void HVTN078() throws Exception
    {
        File outDir         = new File(outDir(), "HVTN/HVTN078");
        File workspaceFile  = new File(dataDir(), "HVTN/HVTN078/1325-L-078.xml");
        File fcsFile        = new File(dataDir(), "HVTN/HVTN078/1012833.fcs");
        File expectedImages = new File(dataDir(), "HVTN/HVTN078/1012833_graphs_v9.4.10");

        generatePlotsAndCompare(outDir, workspaceFile, fcsFile, expectedImages);
    }

    // Issue 14170:
    // Ungated(Time:SSC-A) not scaled properly
    // Time/singlets/AVID/Lymph/CD3(<APC-Cy7-A>:<PerCP-Cy5-5-A>) doesn't go negative enough
    // Time/singlets/AVID/Lymph/CD3/CD4(<PE-Cy5-A>:<APC-A>) doesn't go negative enough
    @Test
    public void IAVI315() throws Exception
    {
        File outDir         = new File(outDir(), "IAVI/315");
        File workspaceFile  = new File(dataDir(), "IAVI/315/workspace.xml");
        File fcsFile        = new File(dataDir(), "IAVI/315/SEB_SEB315_A12.fcs");
        File expectedImages = new File(dataDir(), "IAVI/315/SEB_SEB315_A12_graphs_v9.4.10");

        generatePlotsAndCompare(outDir, workspaceFile, fcsFile, expectedImages);
    }

    // NOTE: This workspace and only load cleanly in FlowJo 8.8.7.  Later versions
    // display the data on a completely different range and the gates will be very far off.
    @Test
    public void ITN027AI() throws Exception
    {
        File outDir         = new File(outDir(), "ITN/ITN027AI");
        File workspaceFile  = new File(dataDir(), "ITN/ITN027AI/ITN027AI_tube131.xml");
        File fcsFile        = new File(dataDir(), "ITN/ITN027AI/ITN-131-01.LMD");
        File expectedImages = new File(dataDir(), "ITN/ITN027AI/ITN-131-01_graphs_v8.8.7");

        generatePlotsAndCompare(outDir, workspaceFile, fcsFile, expectedImages);
    }

    @Test
    public void ITN030ST() throws Exception
    {
        File outDir         = new File(outDir(), "ITN/ITN030ST");
        File workspaceFile  = new File(dataDir(), "ITN/ITN030ST/workspace.wsp");
        File fcsFile        = new File(dataDir(), "ITN/ITN030ST/10047201_SH01_I007.fcs");
        File expectedImages = new File(dataDir(), "ITN/ITN030ST/10047201_SH01_I007_graphs_v7.6.5");

        generatePlotsAndCompare(outDir, workspaceFile, fcsFile, expectedImages);
    }

    @Test
    public void ITNPilot() throws Exception
    {
        File outDir         = new File(outDir(), "ITN/Pilot");
        File workspaceFile  = new File(dataDir(), "ITN/Pilot/workspace.xml");
        File fcsFile        = new File(dataDir(), "ITN/Pilot/ITN64.fcs");
        File expectedImages = new File(dataDir(), "ITN/Pilot/ITN64_graphs_v9.4.10");

        generatePlotsAndCompare(outDir, workspaceFile, fcsFile, expectedImages);
    }

    @Test
    public void LabKeyDemo() throws Exception
    {
        File outDir         = new File(outDir(), "labkey-demo");
        File workspaceFile  = new File(dataDir(), "labkey-demo/labkey-demo.xml");
        File fcsFile        = new File(dataDir(), "labkey-demo/119166.fcs");
        File expectedImages = new File(dataDir(), "labkey-demo/119166_graphs_v9.4.10");

        generatePlotsAndCompare(outDir, workspaceFile, fcsFile, expectedImages);
    }

    @Test
    public void LetvinFACSCalibur() throws Exception
    {
        File outDir         = new File(outDir(), "Letvin/FACSCalibur");
        File workspaceFile  = new File(dataDir(), "Letvin/FACSCalibur/workspace.xml");
        File fcsFile        = new File(dataDir(), "Letvin/FACSCalibur/64.001");
        File expectedImages = new File(dataDir(), "Letvin/FACSCalibur/64.001_graphs_v9.4.10");

        generatePlotsAndCompare(outDir, workspaceFile, fcsFile, expectedImages);
    }

    @Test
    public void LetvinLargeFCS() throws Exception
    {
        File outDir         = new File(outDir(), "Letvin/LargeFCS");
        File workspaceFile  = new File(dataDir(), "Letvin/LargeFCS/workspace.xml");
        File fcsFile        = new File(dataDir(), "Letvin/LargeFCS/BLOOD P11C STIM_AS31_B04.fcs");
        File expectedImages = new File(dataDir(), "Letvin/LargeFCS/AS31_B04_graphs_v9.4.10");

        generatePlotsAndCompare(outDir, workspaceFile, fcsFile, expectedImages);
    }

    @Test
    public void YirongWang() throws Exception
    {
        File outDir         = new File(outDir(), "YirongWang");
        File workspaceFile  = new File(dataDir(), "YirongWang/workspace.wsp");
        File fcsFile        = new File(dataDir(), "YirongWang/001.fcs");
        File expectedImages = new File(dataDir(), "YirongWang/001_graphs_v7.6.5");

        generatePlotsAndCompare(outDir, workspaceFile, fcsFile, expectedImages);
    }
}

/*
 * Copyright (c) 2005 LabKey Software, LLC
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
package org.labkey.ms2.pipeline.tandem;

import org.apache.beehive.netui.pageflow.Forward;
import org.labkey.api.pipeline.PipelineProviderCluster;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.security.ACL;
import org.labkey.api.util.AppProps;
import org.labkey.api.view.*;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.pipeline.MS2SearchPipelineProvider;
import org.labkey.ms2.pipeline.PipelineController;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * XTandemCPipelineProvider class
 * <p/>
 * Created: Nov 1, 2005
 *
 * @author bmaclean
 */
public class XTandemCPipelineProvider extends PipelineProviderCluster implements MS2SearchPipelineProvider
{
    public static String name = "X! Tandem";

    public XTandemCPipelineProvider()
    {
        super(name);
    }

    public boolean isStatusViewableFile(String name, String basename)
    {
        String nameParameters = XTandemSearchProtocolFactory.get().getParametersFileName();
        if (nameParameters.equals(name) || (nameParameters + ".err").equals(name))
            return true;

        return super.isStatusViewableFile(name, basename);
    }

    public Forward handleStatusAction(ViewContext ctx, String name, PipelineStatusFile sf)
            throws HandlerException
    {
        if ("Retry".equals(name) &&
                "ERROR".equals(sf.getStatus()) &&
                "type=database".equals(sf.getInfo()))
        {
            String nameParameters = XTandemSearchProtocolFactory.get().getParametersFileName();
            File analysisDir = new File(sf.getFilePath()).getParentFile();
            File tandemXml = new File(analysisDir, nameParameters);
            File tandemErr = new File(analysisDir, nameParameters + ".err");
            tandemErr.renameTo(tandemXml);
        }

        return super.handleStatusAction(ctx, name, sf);
    }

    public void updateFileProperties(ViewContext context, List<FileEntry> entries)
    {
        for (ListIterator<FileEntry> it = entries.listIterator(); it.hasNext();)
        {
            FileEntry entry = it.next();
            if (!entry.isDirectory())
            {
                continue;
            }

            addAction("MS2-Pipeline", "searchXTandem", "X!Tandem Peptide Search",
                    entry, entry.listFiles(MS2PipelineManager.getAnalyzeFilter()));
        }
    }

    public HttpView getSetupWebPart()
    {
        if (AppProps.getInstance().hasPipelineCluster())
        {
            // No extra setup for cluster.
            return null;
        }
        return new SetupWebPart();
    }

    private static class SetupWebPart extends WebPartView
    {
        @Override
        protected void renderView(Object model, PrintWriter out) throws Exception
        {
            ViewContext context = getViewContext();
            if (!context.hasPermission(ACL.PERM_INSERT))
                return;
            StringBuilder html = new StringBuilder();
            if (!AppProps.getInstance().hasPipelineCluster())
            {
                html.append("<table><tr><td class=\"normal\" style=\"font-weight:bold;\">X! Tandem specific settings:</td></tr>");
                ActionURL setDefaultsURL = new ActionURL(PipelineController.SetTandemDefaultsAction.class, context.getContainer());  // TODO: Should be method in PipelineController
                html.append("<tr><td class=\"normal\">&nbsp;&nbsp;&nbsp;&nbsp;")
                        .append("<a href=\"").append(setDefaultsURL.getLocalURIString()).append("\">Set defaults</a>")
                        .append(" - Specify the default XML parameters file for X! Tandem.</td></tr></table>");
            }
            out.write(html.toString());
        }
    }

    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return XTandemSearchProtocolFactory.get();
    }

    public Map<String, String[]> getSequenceFiles(URI sequenceRoot) throws IOException
    {
        return MS2PipelineManager.addSequenceDBNames(new File(sequenceRoot), "", new LinkedHashMap<String, String[]>());
    }

    public String getHelpTopic()
    {
        return "pipelineXTandem";
    }

    public void ensureEnabled() throws PipelineValidationException
    {
        // Always enabled.
    }
}

/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

import org.labkey.api.pipeline.*;
import org.labkey.api.security.ACL;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.api.data.Container;
import org.labkey.ms2.pipeline.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;
import java.sql.SQLException;

/**
 * XTandemCPipelineProvider class
 * <p/>
 * Created: Nov 1, 2005
 *
 * @author bmaclean
 */
public class XTandemCPipelineProvider extends AbstractMS2SearchPipelineProvider
{
    public static String name = "X! Tandem";

    public PipelinePerlClusterSupport _clusterSupport;

    public XTandemCPipelineProvider()
    {
        super(name);

        _clusterSupport = new PipelinePerlClusterSupport();
    }

    public void preDeleteStatusFile(PipelineStatusFile sf) throws StatusUpdateException
    {
        super.preDeleteStatusFile(sf);
        _clusterSupport.preDeleteStatusFile(sf);
    }

    public void preCompleteStatusFile(PipelineStatusFile sf) throws StatusUpdateException
    {
        super.preCompleteStatusFile(sf);
        _clusterSupport.preCompleteStatusFile(sf);
    }

    public boolean isStatusViewableFile(Container container, String name, String basename)
    {
        String nameParameters = XTandemSearchProtocolFactory.get().getParametersFileName();
        if (nameParameters.equals(name) || (nameParameters + ".err").equals(name))
            return true;

        if (_clusterSupport.isStatusViewableFile(null, name, basename))
            return true;

        return super.isStatusViewableFile(container, name, basename);
    }

    public List<StatusAction> addStatusActions(Container container)
    {
        List<StatusAction> actions = super.addStatusActions(container);
        return _clusterSupport.addStatusActions(container, actions);
    }

    public ActionURL handleStatusAction(ViewContext ctx, String name, PipelineStatusFile sf)
            throws HandlerException
    {        
        ActionURL url = super.handleStatusAction(ctx, name, sf);
        if (url != null)
            return url;

        if (PipelineProvider.CAPTION_RETRY_BUTTON.equals(name) &&
                "ERROR".equals(sf.getStatus()) &&
                "type=database".equals(sf.getInfo()))
        {
            String nameParameters = XTandemSearchProtocolFactory.get().getParametersFileName();
            File analysisDir = new File(sf.getFilePath()).getParentFile();
            File tandemXml = new File(analysisDir, nameParameters);
            File tandemErr = new File(analysisDir, nameParameters + ".err");
            tandemErr.renameTo(tandemXml);
        }

        return _clusterSupport.handleStatusAction(ctx, name, sf);
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, List<FileEntry> entries)
    {
        for (ListIterator<FileEntry> it = entries.listIterator(); it.hasNext();)
        {
            FileEntry entry = it.next();
            if (!entry.isDirectory())
            {
                continue;
            }

            addAction("ms2-pipeline", "searchXTandem", "X!Tandem Peptide Search",
                    entry, entry.listFiles(MS2PipelineManager.getAnalyzeFilter(pr.isPerlPipeline())));
        }
    }

    public HttpView getSetupWebPart(Container container)
    {
        // No extra setup for cluster.
        try
        {
            if (PipelineService.get().usePerlPipeline(container))
                return null;
        }
        catch (SQLException e)
        {
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
            html.append("<table><tr><td style=\"font-weight:bold;\">X! Tandem specific settings:</td></tr>");
            ActionURL setDefaultsURL = new ActionURL(PipelineController.SetTandemDefaultsAction.class, context.getContainer());  // TODO: Should be method in PipelineController
            html.append("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<a href=\"").append(setDefaultsURL.getLocalURIString()).append("\">Set defaults</a>")
                    .append(" - Specify the default XML parameters file for X! Tandem.</td></tr></table>");
            out.write(html.toString());
        }
    }

    public boolean supportsDirectories()
    {
        return true;
    }

    public boolean remembersDirectories()
    {
        return true;
    }

    public boolean hasRemoteDirectories()
    {
        return false;
    }

    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return XTandemSearchProtocolFactory.get();
    }

    public List<String> getSequenceDbPaths(URI sequenceRoot) throws IOException
    {
        return MS2PipelineManager.addSequenceDbPaths(new File(sequenceRoot), "", new ArrayList<String>());
    }

    public List<String> getSequenceDbDirList(URI sequenceRoot) throws IOException
    {
        return MS2PipelineManager.getSequenceDirList(new File(sequenceRoot), "");
    }

    public List<String> getTaxonomyList() throws IOException
    {
        //"X! Tandem does not support Mascot style taxonomy.
        return null;
    }

    public Map<String, String> getEnzymes() throws IOException
    {
        return SearchFormUtil.getDefaultEnzymeMap();
    }

    public Map<String, String> getResidue0Mods() throws IOException
    {
        return SearchFormUtil.getDefaultStaticMods();
    }

    public Map<String, String> getResidue1Mods() throws IOException
    {
        return SearchFormUtil.getDefaultDynamicMods();
    }
    public String getHelpTopic()
    {
        return "pipelineXTandem";
    }

    public void ensureEnabled() throws PipelineProtocol.PipelineValidationException
    {
        // Always enabled.
    }
}

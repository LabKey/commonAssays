/*
 * Copyright (c) 2005-2009 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineProtocol;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.api.module.Module;
import org.labkey.ms2.pipeline.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public XTandemCPipelineProvider(Module owningModule)
    {
        super(name, owningModule);
    }

    public boolean isStatusViewableFile(Container container, String name, String basename)
    {
        String nameParameters = XTandemSearchProtocolFactory.get().getParametersFileName();
        if (nameParameters.equals(name) || (nameParameters + ".err").equals(name))
            return true;

        return super.isStatusViewableFile(container, name, basename);
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

        return null;
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            return;
        }
        
        addAction(PipelineController.SearchXTandemAction.class, "X!Tandem Peptide Search",
                directory, directory.listFiles(MS2PipelineManager.getAnalyzeFilter()), true, includeAll);
    }

    public HttpView getSetupWebPart(Container container)
    {
        return new SetupWebPart();
    }

    private static class SetupWebPart extends WebPartView
    {
        @Override
        protected void renderView(Object model, PrintWriter out) throws Exception
        {
            ViewContext context = getViewContext();
            if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
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

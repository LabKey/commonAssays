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

package org.labkey.flow.script;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineAction;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.module.Module;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.FlowModule;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

public class FlowPipelineProvider extends PipelineProvider
{
    public static final String NAME = "flow";

    public FlowPipelineProvider(Module owningModule)
    {
        super(NAME, owningModule);
    }

    private boolean hasFlowModule(ViewContext context)
    {
        return FlowModule.isActive(context.getContainer());
    }

    static class WorkspaceRecognizer extends DefaultHandler
    {
        boolean _isWorkspace = false;

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            _isWorkspace = "Workspace".equals(qName);
            throw new SAXException("Stop parsing");
        }
        boolean isWorkspace()
        {
            return _isWorkspace;
        }
    }

    private class IsFlowJoWorkspaceFilter extends FileEntryFilter
    {
        public boolean accept(File pathname)
        {
            if (pathname.isDirectory())
                return false;
            if (pathname.getName().endsWith(".wsp"))
                return true;
            if (!pathname.getName().endsWith(".xml"))
                return false;
            WorkspaceRecognizer recognizer = new WorkspaceRecognizer();
            try
            {
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

                parser.parse(pathname, recognizer);
            }
            catch (Exception e)
            {
                // suppress
            }
            return recognizer.isWorkspace();
        }
    }


    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory)
    {
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
            return;
        if (!hasFlowModule(context))
            return;

        PipeRoot root;
        Set<File> usedPaths = new HashSet<File>();

        try
        {
            // UNDONE: is this ever different than pr???
            root = PipelineService.get().findPipelineRoot(context.getContainer());
            for (FlowRun run : FlowRun.getRunsForContainer(context.getContainer(), FlowProtocolStep.keywords))
                usedPaths.add(run.getExperimentRun().getFilePathRoot());
        }
        catch (SQLException e)
        {
            return;
        }

        ActionURL importRunsURL = new ActionURL(AnalysisScriptController.ChooseRunsToUploadAction.class, context.getContainer());
        String srcURL = context.getActionURL().toString();
        importRunsURL.replaceParameter("srcURL", srcURL);
        URI rootURI = root != null ? root.getUri() : pr.getUri();

        ActionURL importWorkspaceURL = new ActionURL(AnalysisScriptController.ImportAnalysisAction.class, context.getContainer());
        importWorkspaceURL.addParameter("step", String.valueOf(AnalysisScriptController.ImportAnalysisStep.ASSOCIATE_FCSFILES.getNumber()));

        int flowDirCount = 0;
        
        File[] dirs = directory.listFiles(DirectoryFileFilter.INSTANCE);
        for (File dir : dirs)
        {
            if (usedPaths.contains(dir))
                continue;
            File[] fcsFiles = dir.listFiles((FileFilter)FCS.FCSFILTER);
            if (null == fcsFiles || 0 == fcsFiles.length)
                continue;
            importRunsURL.replaceParameter("path", URIUtil.relativize(rootURI, dir.toURI()).toString());
            PipelineAction action = new PipelineAction("Import Flow Run", importRunsURL, new File[] {dir}, false);
            action.setDescription("" + fcsFiles.length + "&nbsp;FCS&nbsp;file" + ((fcsFiles.length>1)?"s":""));
            directory.addAction(action);
            flowDirCount++;
        }

        File[] workspaces = directory.listFiles(new IsFlowJoWorkspaceFilter());
        for (File workspace : workspaces)
        {
            importWorkspaceURL.replaceParameter("workspace.path", root.relativePath(workspace));
            PipelineAction importWorkspaceAction = new PipelineAction("Import FlowJo Workspace", importWorkspaceURL, new File[] { workspace }, false);
            importWorkspaceAction.setDescription("Import analysis from a FlowJo workspace");
            directory.addAction(importWorkspaceAction);

            // UNDONE: create workspace from FlowJo workspace
        }

        // UNDONE: import FlowJo exported compensation matrix file: CompensationController.UploadAction


        if (flowDirCount > 0)
        {
            File file = new File(directory.getURI());
            importRunsURL.replaceParameter("path", URIUtil.relativize(rootURI, file.toURI()).toString());
            PipelineAction action = new PipelineAction("Import Multiple Runs", importRunsURL, null, false);
            action.setDescription("<p><b>Flow Instructions:</b><br>Navigate to the directories containing FCS files.  Click the button to upload FCS files in the directories shown.</p>");
            directory.addAction(action);
        }
    }

    public boolean suppressOverlappingRootsWarning(ViewContext context)
    {
        if (!hasFlowModule(context))
            return super.suppressOverlappingRootsWarning(context);
        return true;
    }
}

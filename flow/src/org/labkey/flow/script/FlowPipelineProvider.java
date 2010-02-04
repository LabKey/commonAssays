/*
 * Copyright (c) 2005-2010 LabKey Corporation
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
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.pipeline.*;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
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


    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
            return;
        if (!hasFlowModule(context))
            return;

        PipeRoot root;
        final Set<File> usedPaths = new HashSet<File>();

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

        URI rootURI = root != null ? root.getUri() : pr.getUri();

        File[] dirs = directory.listFiles(new FileFilter()
        {
            public boolean accept(File dir)
            {
                if (!dir.isDirectory())
                    return false;

                if (usedPaths.contains(dir))
                    return false;

                File[] fcsFiles = dir.listFiles((FileFilter)FCS.FCSFILTER);
                if (null == fcsFiles || 0 == fcsFiles.length)
                    return false;

                return true;
            }
        });

        ActionURL importRunsURL = new ActionURL(AnalysisScriptController.ConfirmImportRunsAction.class, context.getContainer());

        String path = directory.getPathParameter();
        ActionURL returnUrl = PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(context.getContainer(), null, path.toString());
        importRunsURL.addReturnURL(returnUrl);
        importRunsURL.replaceParameter("path", path);

        if (includeAll || (dirs != null && dirs.length > 0))
        {
            NavTree selectedDirsNavTree = new NavTree("FCS Files");
            selectedDirsNavTree.addChild("Directory of FCS Files", importRunsURL);
            directory.addAction(new PipelineAction(selectedDirsNavTree, dirs, true));
        }

        File currentDir = new File(directory.getURI().getPath());
        if (includeAll || !usedPaths.contains(currentDir))
        {
            File[] fcsFiles = directory.listFiles((FileFilter)FCS.FCSFILTER);
            if (includeAll || (fcsFiles != null && fcsFiles.length > 0))
            {
                ActionURL url = importRunsURL.clone().addParameter("current", true);
                NavTree tree = new NavTree("FCS Files");
                tree.addChild("Current directory of " + fcsFiles.length + " FCS Files", url);
                directory.addAction(new PipelineAction(tree, new File[] { currentDir }, false));
            }
        }

        File[] workspaces = directory.listFiles(new IsFlowJoWorkspaceFilter());
        if (includeAll || (workspaces != null && workspaces.length > 0))
        {
            ActionURL importWorkspaceURL = new ActionURL(AnalysisScriptController.ImportAnalysisFromPipelineAction.class, context.getContainer());
            importWorkspaceURL.replaceParameter("path", path);
            addAction(importWorkspaceURL, "FlowJo Workspace", directory, workspaces, false, includeAll);

            // UNDONE: create workspace from FlowJo workspace
        }

        // UNDONE: import FlowJo exported compensation matrix file: CompensationController.UploadAction
    }

    public boolean suppressOverlappingRootsWarning(ViewContext context)
    {
        if (!hasFlowModule(context))
            return super.suppressOverlappingRootsWarning(context);
        return true;
    }
}

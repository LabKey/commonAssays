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

package org.labkey.flow.script;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.log4j.Logger;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.ACL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlowPipelineProvider extends PipelineProvider
{
    private static final Logger _log = Logger.getLogger(FlowPipelineProvider.class);
    public static final String NAME = "flow";

    public FlowPipelineProvider()
    {
        super(NAME);
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
            if ("Workspace".equals(qName))
            {
                _isWorkspace = true;
            }
            else
            {
                _isWorkspace = false;
            }
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
            if (pathname.getName().endsWith(".wsp"))
                return true;
            if (!pathname.getName().endsWith(".xml"))
                return false;
            if (pathname.isDirectory())
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


    public void updateFileProperties(ViewContext context, PipeRoot pr, List<FileEntry> entries)
    {
        if (!context.hasPermission(ACL.PERM_INSERT))
            return;
        if (!hasFlowModule(context))
            return;
        if (entries.size() == 0)
            return;

        PipeRoot root;
        Set<String> usedPaths = new HashSet<String>();

        try
        {
            root = PipelineService.get().findPipelineRoot(context.getContainer());
            for (FlowRun run : FlowRun.getRunsForContainer(context.getContainer(), FlowProtocolStep.keywords))
                usedPaths.add(run.getExperimentRun().getFilePathRoot());
        }
        catch (SQLException e)
        {
            return;
        }

        ActionURL url = PageFlowUtil.urlFor(AnalysisScriptController.Action.chooseRunsToUpload, context.getContainer());
        String srcURL = context.getActionURL().toString();
        url.replaceParameter("srcURL", srcURL);
        URI rootURI = root.getUri();

        boolean hasFlowDir = false;
        
        for (FileEntry entry : entries)
        {
            File[] dirs = entry.listFiles(DirectoryFileFilter.INSTANCE);
            for (File dir : dirs)
            {
                if (usedPaths.contains(dir.getPath()))
                    continue;
                File[] fcsFiles = dir.listFiles((FileFilter)FCS.FCSFILTER);
                if (null == fcsFiles || 0 == fcsFiles.length)
                    continue;
                url.replaceParameter("path", URIUtil.relativize(rootURI, dir.toURI()).toString());
                FileAction action = new UploadRunAction("Import Flow Run", url, dir);
                action.setDescription("" + fcsFiles.length + "&nbsp;fcs&nbsp;file" + ((fcsFiles.length>1)?"s":""));
                entry.addAction(action);
                hasFlowDir = true;
            }
        }

        if (hasFlowDir)
        {
            FileEntry entryRoot = entries.get(0);
            File file = new File(entryRoot.getURI());

            url.replaceParameter("path", URIUtil.relativize(rootURI, file.toURI()).toString());
            FileAction action = new FileAction("Import Multiple Runs", url, null);
            action.setDescription("<p><b>Flow Instructions:</b><br>Navigate to the directories containing FCS files.  Click the button to upload FCS files in the directories shown.</p>");
            entryRoot.addAction(action);
        }
    }


    public boolean suppressOverlappingRootsWarning(ViewContext context)
    {
        if (!hasFlowModule(context))
            return super.suppressOverlappingRootsWarning(context);
        return true;
    }


    class UploadRunAction extends FileAction
    {
        UploadRunAction(String label, ActionURL url, File dir)
        {
            super(label, url, new File[] {dir});
        }

        @Override
        public String getDisplay()
        {
            String img = super.getDisplay();
            return img + "&nbsp;(" + getDescription() + ")";
        }
    }
}

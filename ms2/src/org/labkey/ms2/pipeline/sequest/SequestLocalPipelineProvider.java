/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.ms2.pipeline.sequest;

import org.apache.log4j.Logger;
import org.labkey.api.security.ACL;
import org.labkey.api.settings.AppProps;
import org.labkey.api.view.*;
import org.labkey.api.pipeline.PipelineProtocol;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.data.Container;
import org.labkey.ms2.pipeline.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;

/**
 * User: billnelson@uky.edu
 * Date: Aug 24, 2006
 * Time: 12:45:45 PM
 */
public class SequestLocalPipelineProvider extends AbstractMS2SearchPipelineProvider
{
    private static Logger _log = Logger.getLogger(SequestLocalPipelineProvider.class);

    public static String name = "Sequest";

    public SequestLocalPipelineProvider()
    {
        super(name);
    }

    public boolean isStatusViewableFile(Container container, String name, String basename)
    {
        return "sequest.xml".equals(name) || super.isStatusViewableFile(container, name, basename);
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, List<FileEntry> entries)
    {
        if (!AppProps.getInstance().hasSequest())
            return;

        for (ListIterator<FileEntry> it = entries.listIterator(); it.hasNext();)
        {
            FileEntry entry = it.next();
            if (!entry.isDirectory())
            {
                continue;
            }

            addAction("ms2-pipeline", "searchSequest", "Sequest Peptide Search",
                entry, entry.listFiles(MS2PipelineManager.getAnalyzeFilter(false)));
        }
    }

    public HttpView getSetupWebPart(Container container)
    {
        if (!AppProps.getInstance().hasSequest())
            return null;
        return new SetupWebPart();
    }

    class SetupWebPart extends WebPartView
    {
        @Override
        protected void renderView(Object model, PrintWriter out) throws Exception
        {
            ViewContext context = getViewContext();
            if (!context.hasPermission(ACL.PERM_INSERT))
                return;
            StringBuilder html = new StringBuilder();
            html.append("<table><tr><td style=\"font-weight:bold;\">Sequest specific settings:</td></tr>");
            ActionURL setDefaultsURL = new ActionURL(PipelineController.SetSequestDefaultsAction.class, context.getContainer());
            html.append("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;")
                .append("<a href=\"").append(setDefaultsURL.getLocalURIString()).append("\">Set defaults</a>")
                .append(" - Specify the default XML parameters file for Sequest.</td></tr></table>");
            out.write(html.toString());
        }
    }

    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return SequestSearchProtocolFactory.get();
    }

    public List<String> getSequenceDbPaths(URI sequenceRoot) throws IOException
    {
        AppProps appProps = AppProps.getInstance();
        SequestClientImpl sequestClient = new SequestClientImpl(appProps.getSequestServer(), _log);
        List dbList = sequestClient.addSequenceDbPaths("", new ArrayList<String>());
        if(dbList == null) throw new IOException("Trouble connecting to the Sequest server.");
        return dbList;
    }

    public List<String> getSequenceDbDirList(URI sequenceRoot) throws IOException
    {
        AppProps appProps = AppProps.getInstance();
        SequestClientImpl sequestClient = new SequestClientImpl(appProps.getSequestServer(), _log);
        List dbList = sequestClient.getSequenceDbDirList(sequenceRoot.getPath());
        if(dbList == null) throw new IOException("Trouble connecting to the Sequest server.");
        return dbList;
    }

    public List<String> getTaxonomyList() throws IOException 
    {
        //"Sequest does not support Mascot style taxonomy.
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
        return "pipelineSequest";
    }

    public void ensureEnabled() throws PipelineProtocol.PipelineValidationException
    {
        AppProps appProps = AppProps.getInstance();
        if (!appProps.hasSequest())
            throw new PipelineProtocol.PipelineValidationException("Sequest server has not been specified in site customization.");
    }

    public boolean supportsDirectories()
    {
        return true;
    }

    public boolean remembersDirectories()
    {
        return false;
    }

    public boolean hasRemoteDirectories()
    {
        return true;
    }

}

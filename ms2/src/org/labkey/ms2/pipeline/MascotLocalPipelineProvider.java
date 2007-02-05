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
package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.HttpView;
import org.labkey.api.security.ACL;
import org.labkey.api.util.AppProps;

import java.io.PrintWriter;
import java.io.File;
import java.util.List;
import java.util.ListIterator;

/**
 * MascotCPipelineProvider class
 * <p/>
 * Created: Nov 1, 2005
 *
 * @author bmaclean
 */
public class MascotLocalPipelineProvider extends PipelineProvider
{
    public static String name = "Mascot (Local)";

    public MascotLocalPipelineProvider()
    {
        super(name);
    }

    public boolean isStatusViewableFile(String name, String basename)
    {
        if ("mascot.xml".equals(name))
            return true;

        return super.isStatusViewableFile(name, basename);
    }

    public void updateFileProperties(ViewContext context, List<FileEntry> entries)
    {
        if (!AppProps.getInstance().hasMascotServer())
            return;

        if (AppProps.getInstance().hasPipelineCluster())
            return;

        for (ListIterator<FileEntry> it = entries.listIterator(); it.hasNext();)
        {
            FileEntry entry = it.next();
            if (!entry.isDirectory())
            {
                continue;
            }

            addAction("MS2-Pipeline", "searchMascot", "Mascot Peptide Search",
                    entry, entry.listFiles(MS2PipelineManager.getAnalyzeFilter()));
        }
    }

    public HttpView getSetupWebPart()
    {
        if (!AppProps.getInstance().hasMascotServer())
            return null;
        if (AppProps.getInstance().hasPipelineCluster())
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
            if (!AppProps.getInstance().hasPipelineCluster())
            {
                html.append("<table><tr><td class=\"ms-vb\" style=\"font-weight:bold;\">Mascot specific settings:</td></tr>");
                ViewURLHelper setDefaultsURL = new ViewURLHelper("MS2-Pipeline", "setMascotDefaults", context.getViewURLHelper().getExtraPath());
                html.append("<tr><td class=\"ms-vb\">&nbsp;&nbsp;&nbsp;&nbsp;")
                        .append("<a href=\"").append(setDefaultsURL.getLocalURIString()).append("\">Set defaults</a>")
                        .append(" - Specify the default XML parameters file for Mascot.</td></tr>");
            }
            out.write(html.toString());
        }
    }
}

/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.view.*;
import org.labkey.api.security.ACL;
import java.io.PrintWriter;
import java.io.File;
import java.util.List;

/**
 */
public class MS2PipelineProvider extends PipelineProvider
{
    static String name = "MS2";
    public static final String SEQUEST = "Sequest";

    public MS2PipelineProvider()
    {
        super(name);
    }

    public HttpView getSetupWebPart()
    {
        return new SetupWebPart();
    }

    public void updateFileProperties(ViewContext context, List<FileEntry> entries)
    {
        for (FileEntry entry : entries)
        {
            if (!entry.isDirectory())
                continue;

            addFileActions("MS2-Pipeline", "upload", "Import Peptides",
                    entry, entry.listFiles(MS2PipelineManager.getUploadFilter()));
        }
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
            html.append("<table><tr><td class=\"ms-vb\" style=\"font-weight:bold;\">MS2 specific settings:</td></tr>");
            ViewURLHelper buttonURL = new ViewURLHelper(context.getRequest(), "MS2-Pipeline",
                "setupClusterSequenceDB", context.getViewURLHelper().getExtraPath());
            html.append("<tr class=\"ms-vb\"><td>&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<a href=\"").append(buttonURL.getLocalURIString()).append("\">Set FASTA root</a>")
                    .append(" - Specify the location on the web server where FASTA sequence files will be located.</td></tr>");

            if (MS2PipelineManager.allowSequenceDatabaseUploads(context.getUser(), context.getContainer()))
            {
                buttonURL = new ViewURLHelper("MS2-Pipeline", "addSequenceDB", context.getViewURLHelper().getExtraPath());
                html.append("<tr><td class=\"ms-vb\">&nbsp;&nbsp;&nbsp;&nbsp;")
                        .append("<a href=\"").append(buttonURL.getLocalURIString()).append("\">Add FASTA file</a>")
                        .append(" - Add a FASTA sequence file to the current FASTA root location.</td></tr>");
            }

            html.append("</table>");
            out.write(html.toString());
        }
    }
}

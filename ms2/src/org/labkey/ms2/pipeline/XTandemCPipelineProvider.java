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

import org.labkey.api.pipeline.PipelineProviderCluster;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.HttpView;
import org.labkey.api.security.ACL;
import org.labkey.api.util.AppProps;
import org.apache.beehive.netui.pageflow.Forward;

import java.io.PrintWriter;
import java.io.File;
import java.util.List;
import java.util.ListIterator;

/**
 * XTandemCPipelineProvider class
 * <p/>
 * Created: Nov 1, 2005
 *
 * @author bmaclean
 */
public class XTandemCPipelineProvider extends PipelineProviderCluster
{
    public static String name = "X!Tandem (Cluster)";

    public XTandemCPipelineProvider()
    {
        super(name);
    }

    public boolean isStatusViewableFile(String name, String basename)
    {
        if ("tandem.xml".equals(name) || "tandem.xml.err".equals(name))
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
            File analysisDir = new File(sf.getFilePath()).getParentFile();
            File tandemXml = new File(analysisDir, "tandem.xml");
            File tandemErr = new File(analysisDir, "tandem.xml.err");
            tandemErr.renameTo(tandemXml);
        }

        return super.handleStatusAction(ctx, name, sf);
    }
    public void updateFileProperties(ViewContext context, List<FileEntry> entries)
    {
        if (!AppProps.getInstance().hasPipelineCluster())
            return;

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
        // No extra setup for cluster.
        return null;
    }
}

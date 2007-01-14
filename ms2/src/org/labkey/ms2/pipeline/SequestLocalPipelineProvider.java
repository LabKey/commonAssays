package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.util.AppProps;
import org.labkey.api.security.ACL;

import java.util.List;
import java.util.ListIterator;
import java.io.File;
import java.io.PrintWriter;

/**
 * User: billnelson@uky.edu
 * Date: Aug 24, 2006
 * Time: 12:45:45 PM
 */
public class SequestLocalPipelineProvider extends PipelineProvider
{
    public static String name = "Sequest (Local)";

    public SequestLocalPipelineProvider()
    {
        super(name);
    }

    public boolean isStatusViewableFile(String name, String basename)
    {
        return "sequest.xml".equals(name) || super.isStatusViewableFile(name, basename);

    }

    public void updateFileProperties(ViewContext context, List<FileEntry> entries)
    {
        if (!AppProps.getInstance().hasSequest())
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

            File dir = new File(entry.getURI());
            addAction("MS2-Pipeline", "searchSequest", "Sequest Peptide Search",
                entry, dir.listFiles(MS2PipelineManager.getAnalyzeFilter()));
        }
    }

    public HttpView getSetupWebPart()
    {
        if (!AppProps.getInstance().hasSequest())
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
                html.append("<table><tr><td class=\"ms-vb\" style=\"font-weight:bold;\">Sequest specific settings:</td></tr>");
                ViewURLHelper setDefaultsURL = new ViewURLHelper("MS2-Pipeline", "setSequestDefaults", context.getContainer());
                html.append("<tr><td class=\"ms-vb\">&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<a href=\"").append(setDefaultsURL.getLocalURIString()).append("\">Set defaults</a>")
                    .append(" - Specify the default XML parameters file for Sequest.</td></tr>");
            }
            out.write(html.toString());
        }
    }
}

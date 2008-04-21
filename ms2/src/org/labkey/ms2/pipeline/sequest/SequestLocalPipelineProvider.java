package org.labkey.ms2.pipeline.sequest;

import org.apache.log4j.Logger;
import org.labkey.api.security.ACL;
import org.labkey.api.util.AppProps;
import org.labkey.api.view.*;
import org.labkey.api.pipeline.PipelineProtocol;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineProvider;
import org.labkey.ms2.pipeline.PipelineController;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;
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

    public boolean isStatusViewableFile(String name, String basename)
    {
        return "sequest.xml".equals(name) || super.isStatusViewableFile(name, basename);
    }

    public void updateFileProperties(ViewContext context, List<FileEntry> entries)
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
                entry, entry.listFiles(MS2PipelineManager.getAnalyzeFilter()));
        }
    }

    public HttpView getSetupWebPart()
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
            if (!AppProps.getInstance().hasPipelineCluster())
            {
                html.append("<table><tr><td class=\"normal\" style=\"font-weight:bold;\">Sequest specific settings:</td></tr>");
                ActionURL setDefaultsURL = new ActionURL(PipelineController.SetSequestDefaultsAction.class, context.getContainer());
                html.append("<tr><td class=\"normal\">&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<a href=\"").append(setDefaultsURL.getLocalURIString()).append("\">Set defaults</a>")
                    .append(" - Specify the default XML parameters file for Sequest.</td></tr></table>");
            }
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

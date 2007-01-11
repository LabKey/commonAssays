package cpas.ms2.pipeline;

import org.fhcrc.cpas.pipeline.PipelineJob;
import org.fhcrc.cpas.view.ViewURLHelper;
import org.fhcrc.cpas.view.ViewBackgroundInfo;
import org.fhcrc.cpas.ms2.*;
import org.fhcrc.cpas.exp.ExperimentException;
import org.fhcrc.cpas.exp.XarContext;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Feb 17, 2006
 */
public class ProteinProphetPipelineJob extends PipelineJob
{
    private final File _file;

    public ProteinProphetPipelineJob(ViewBackgroundInfo info, File file)
    {
        super(ProteinProphetPipelineProvider.NAME, info);
        _file = file;

        setLogFile(new File(_file.getParentFile(), _file.getName() + ".log"));
    }

    public ViewURLHelper getStatusHref()
    {
        return null;
    }

    public String getDescription()
    {
        return _file.getName();
    }

    public void run()
    {
        setStatus("LOADING");
        boolean completeStatus = false;
        try
        {
            ProteinProphetImporter importer = new ProteinProphetImporter(_file, null, new XarContext());
            importer.importFile(this);
            setStatus(PipelineJob.COMPLETE_STATUS);
            completeStatus = true;
        }
        catch (SQLException e)
        {
            getLogger().error("ProteinProphet load failed", e);
        }
        catch (IOException e)
        {
            getLogger().error("ProteinProphet load failed", e);
        }
        catch (XMLStreamException e)
        {
            getLogger().error("ProteinProphet load failed", e);
        }
        catch (ExperimentException e)
        {
            getLogger().error("ProteinProphet load failed", e);
        }
        catch (RuntimeException e)
        {
            getLogger().error("ProteinProphet load failed", e);
        }
        finally
        {
            if (!completeStatus)
            {
                setStatus(PipelineJob.ERROR_STATUS);
            }
        }

    }
}

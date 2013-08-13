package org.labkey.ms2;

import org.labkey.api.data.SimpleFilter;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NotFoundException;
import org.labkey.ms2.peptideview.AbstractMS2RunView;

import java.io.IOException;
import java.util.List;

/**
* User: jeckels
* Date: 8/12/13
*/
public enum MS2ExportType
{
    Excel
    {
        @Override
        public void export(AbstractMS2RunView peptideView, MS2Controller.ExportForm form, List<String> exportRows, ActionURL currentURL, SimpleFilter baseFilter) throws IOException
        {
            peptideView.exportToExcel(form, form.getViewContext().getResponse(), exportRows);
        }
    },
    TSV
    {
        @Override
        public void export(AbstractMS2RunView peptideView, MS2Controller.ExportForm form, List<String> exportRows, ActionURL currentURL, SimpleFilter baseFilter) throws IOException
        {
            peptideView.exportToTSV(form, form.getViewContext().getResponse(), exportRows, null);
        }
    },
    PKL
    {
        @Override
        public void export(AbstractMS2RunView peptideView, MS2Controller.ExportForm form, List<String> exportRows, ActionURL currentURL, SimpleFilter baseFilter) throws IOException, RunListException
        {
            peptideView.exportSpectra(form, currentURL, new PklSpectrumRenderer(form.getViewContext().getResponse(), "spectra", "pkl"), exportRows);
        }
    },
    DTA
    {
        @Override
        public void export(AbstractMS2RunView peptideView, MS2Controller.ExportForm form, List<String> exportRows, ActionURL currentURL, SimpleFilter baseFilter) throws IOException, RunListException
        {
            peptideView.exportSpectra(form, currentURL, new DtaSpectrumRenderer(form.getViewContext().getResponse(), "spectra", "dta"), exportRows);
        }
    },
    AMT
    {
        @Override
        public void export(AbstractMS2RunView peptideView, MS2Controller.ExportForm form, List<String> exportRows, ActionURL currentURL, SimpleFilter baseFilter) throws IOException
        {
            peptideView.exportToAMT(form, form.getViewContext().getResponse(), exportRows);
        }
    },
    Bibliospec
    {
        @Override
        public void export(AbstractMS2RunView peptideView, MS2Controller.ExportForm form, List<String> exportRows, ActionURL currentURL, SimpleFilter baseFilter) throws IOException
        {
            throw new UnsupportedOperationException();
        }
    },
    MS2Ions("MS2 Ions TSV")
    {
        @Override
        public boolean supportsSelectedOnly()
        {
            return false;
        }

        @Override
        public String getDescription()
        {
            return "Ignores all peptide and protein filters, exports a row for each peptide's y and b ion/charge state combinations";
        }

        @Override
        public void export(AbstractMS2RunView peptideView, MS2Controller.ExportForm form, List<String> exportRows, ActionURL currentURL, SimpleFilter baseFilter) throws IOException
        {
            MS2Controller.exportMS2Ions(form, form.getViewContext().getResponse());
        }
    };

    private final String _name;

    MS2ExportType(String name)
    {
        _name = name;
    }

    MS2ExportType()
    {
        this(null);
    }

    @Override
    public String toString()
    {
        return _name == null ? super.toString() : _name;
    }

    public static MS2ExportType valueOfOrNotFound(String name)
    {
        if (name == null)
        {
            throw new NotFoundException("No export format specified");
        }
        try
        {
            return MS2ExportType.valueOf(name);
        }
        catch (IllegalArgumentException e)
        {
            throw new NotFoundException("Unknown export format specified: " + name);
        }
    }

    public boolean supportsSelectedOnly()
    {
        return true;
    }

    public String getDescription()
    {
        return null;
    }

    public abstract void export(AbstractMS2RunView peptideView, MS2Controller.ExportForm form, List<String> exportRows, ActionURL currentURL, SimpleFilter baseFilter) throws IOException, RunListException;
}

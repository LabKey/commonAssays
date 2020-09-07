/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
package org.labkey.ms2;

import org.labkey.api.data.Filter;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NotFoundException;
import org.labkey.ms2.peptideview.AbstractMS2RunView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
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
            peptideView.exportSpectra(form, currentURL, new PklSpectrumRenderer(form, "spectra", "pkl"), exportRows);
        }
    },
    DTA
    {
        @Override
        public void export(AbstractMS2RunView peptideView, MS2Controller.ExportForm form, List<String> exportRows, ActionURL currentURL, SimpleFilter baseFilter) throws IOException, RunListException
        {
            peptideView.exportSpectra(form, currentURL, new DtaSpectrumRenderer(form, "spectra", "dta"), exportRows);
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
        public void export(AbstractMS2RunView peptideView, MS2Controller.ExportForm form, List<String> exportRows, ActionURL currentURL, SimpleFilter baseFilter) throws IOException, RunListException
        {
            peptideView.exportSpectra(form, currentURL, new BibliospecSpectrumRenderer(form.getViewContext()), exportRows);
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
            HttpServletResponse response = form.getViewContext().getResponse();
            MS2Run run = form.validateRun();
            Filter filter = new SimpleFilter(FieldKey.fromParts("Run"), run.getRun());
            MS2Peptide[] peptides = new TableSelector(MS2Manager.getTableInfoPeptides(), filter, new Sort("Scan")).getArray(MS2Peptide.class);
            response.setContentType("text/tab-separated-values");
            response.setHeader("Content-disposition", "attachment; filename=\"" + run.getDescription() + ".MS2Ions.tsv");
            response.getWriter().write("Scan\tPeptide\tPeptideProphet\tProtein\tIonType\tFragmentLength\tAverageTheoreticalMass\tAverageObservedMass\tAverageDeltaMass\tAverageIntensity\tMonoisotopicTheoreticalMass\tMonoisotopicObservedMass\tMonoisotopicDeltaMass\tMonoisotopicIntesity\n");
            for (MS2Peptide peptide : peptides)
            {
                peptide.init(0.5, 0, 10000);
                exportPeptideIonRows(response.getWriter(), peptide, "y",
                        peptide.getYIons(MassType.Average),
                        peptide.getYIons(MassType.Monoisotopic));
                exportPeptideIonRows(response.getWriter(), peptide, "b",
                        peptide.getBIons(MassType.Average),
                        peptide.getBIons(MassType.Monoisotopic));
            }
        }

        private void exportPeptideIonRows(Writer writer, MS2Peptide peptide, String ionType,
                                MS2Peptide.FragmentIon[][] allAverageIons,
                                MS2Peptide.FragmentIon[][] allMonoisotopicIons) throws IOException
        {
            for (int i = 0; i < allAverageIons.length; i++)
            {
                MS2Peptide.FragmentIon[] averageIons = allAverageIons[i];
                MS2Peptide.FragmentIon[] monoisotopicIons = allMonoisotopicIons[i];

                for (int j = 0; j < averageIons.length; j++)
                {
                    writer.write(peptide.getScan() + "\t");
                    writer.write(peptide.getPeptide() + "\t");
                    writer.write(peptide.getPeptideProphet() == null ? "\t" : peptide.getPeptideProphet() + "\t");
                    writer.write(peptide.getProtein() + "\t");
                    writer.write(ionType + (i + 1) + "+\t");
                    writer.write((j + 1) + "\t");
                    writeIonColumns(writer, averageIons[j], i + 1);
                    writer.write("\t");
                    writeIonColumns(writer, monoisotopicIons[j], i + 1);

                    writer.write("\n");
                }
            }
        }

        private void writeIonColumns(Writer writer, MS2Peptide.FragmentIon ion, int chargeState) throws IOException
        {
            writer.write(ion.getTheoreticalMZ() * chargeState + "\t");
            if (ion.isMatch())
            {
                writer.write(ion.getObservedMZ() * chargeState + "\t");
                writer.write((ion.getTheoreticalMZ() * chargeState - ion.getObservedMZ() * chargeState) + "\t");
                writer.write(Double.toString(ion.getIntensity()));
            }
            else
            {
                writer.write("\t\t");
            }
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

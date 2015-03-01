/*
 * Copyright (c) 2008-2015 LabKey Corporation
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

package org.labkey.ms2.compare;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TSVGridWriter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.reports.ReportService;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.SpectraCountConfiguration;
import org.springframework.validation.BindException;

import java.io.IOException;

/**
 * User: jeckels
* Date: Jan 22, 2008
*/
public class SpectraCountQueryView extends QueryView
{
    private final MS2Schema _schema;
    private final SpectraCountConfiguration _config;
    private MS2Controller.SpectraCountForm _form;

    public SpectraCountQueryView(MS2Schema schema, QuerySettings settings, @Nullable BindException errors, SpectraCountConfiguration config, MS2Controller.SpectraCountForm form)
    {
        super(schema, settings, errors);
        _schema = schema;
        _config = config;
        _form = form;

        setViewItemFilter(new ReportService.ItemFilter() {
            public boolean accept(String type, String label)
            {
                return SpectraCountRReport.TYPE.equals(type);
            }
        });
    }

    protected TableInfo createTable()
    {
        return _schema.createSpectraCountTable(_config, getViewContext(), _form);
    }

    @Override
    public ExcelWriter getExcelWriter(ExcelWriter.ExcelDocumentType docType) throws IOException
    {
        ExcelWriter result = super.getExcelWriter(docType);
        String header = getExportHeader();
        if (header != null)
        {
            result.setHeaders(header);
        }
        return result;
    }

    @Override
    protected TSVGridWriter getTsvWriter(ColumnHeaderType headerType) throws IOException
    {
        TSVGridWriter result = super.getTsvWriter(headerType);
        String header = getExportHeader();
        if (header != null)
        {
            result.setFileHeader("# " + header);
        }
        return result;
    }

    /** Describe the peptide-level filters that were applied before the aggregation */
    private String getExportHeader()
    {
        StringBuilder sb = new StringBuilder();
        String separator = "";

        if (_form.getTargetProtein() != null)
        {
            sb.append("Target protein: ");
            sb.append(_form.getTargetProtein());
            separator = ". ";
        }

        if (_form.isPeptideProphetFilter())
        {
            sb.append(separator);
            sb.append("PeptideProphet >= ");
            sb.append(_form.getPeptideProphetProbability());
            separator = ". ";
        }
        
        if (_form.isCustomViewPeptideFilter())
        {
            UserSchema schema = new MS2Schema(getViewContext().getUser(), getViewContext().getContainer());
            QueryDefinition queryDef = QueryService.get().createQueryDefForTable(schema, MS2Schema.HiddenTableType.PeptidesFilter.toString());
            SimpleFilter filter = new SimpleFilter();

            CustomView view = queryDef.getCustomView(getUser(), getViewContext().getRequest(), _form.getPeptideCustomViewName(getViewContext()));
            if (view != null)
            {
                ActionURL url = new ActionURL();
                view.applyFilterAndSortToURL(url, "InternalName");
                filter.addUrlFilters(url, "InternalName");
            }
            sb.append(separator);
            sb.append("Peptide filter: ");
            sb.append(filter.getFilterText());
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}

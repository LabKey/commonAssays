/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
package org.labkey.ms2.peptideview;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.protein.search.PeptideSequenceFilter;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.reports.ReportService;
import org.labkey.api.view.DataView;
import org.labkey.ms2.query.MS2Schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PeptidesView extends QueryView
{
    public static final String DATAREGION_NAME = "pv";

    private boolean _searchSubfolders = true;
    private PeptideSequenceFilter _pepFilter;

    public PeptidesView(UserSchema schema, String queryName)
    {
        super(schema);

        QuerySettings settings = getSchema().getSettings(getViewContext(), DATAREGION_NAME, queryName);
        setSettings(settings);

        setShowRecordSelectors(false);

        setViewItemFilter(ReportService.EMPTY_ITEM_LIST);
    }

    public boolean isSearchSubfolders()
    {
        return _searchSubfolders;
    }

    public void setSearchSubfolders(boolean searchSubfolders)
    {
        _searchSubfolders = searchSubfolders;
    }

    public void setPeptideFilter(PeptideSequenceFilter pepFilter)
    {
        _pepFilter = pepFilter;
    }

    @Override
    protected TableInfo createTable()
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addClause(_pepFilter);

        //add a clause to filter on container(s)
        Collection<Container> containers = _searchSubfolders ?
                ContainerManager.getAllChildren(getViewContext().getContainer(), getViewContext().getUser())
                : Collections.singleton(getViewContext().getContainer());

        StringBuilder sql = new StringBuilder("Fraction IN (SELECT Fraction FROM ms2.Fractions WHERE Run IN (SELECT Run FROM ms2.Runs WHERE Container IN (");
        String sep = "";
        for(Container container : containers)
        {
            sql.append(sep);
            sql.append("'");
            sql.append(container.getId());
            sql.append("'");
            sep = ",";
        }
        sql.append(")))");
        filter.addWhereClause(sql.toString(), null, FieldKey.fromParts("Fraction"));

        List<FieldKey> defCols = new ArrayList<>();
        defCols.add(FieldKey.fromParts("Fraction", "Run", "Description"));
        defCols.add(FieldKey.fromParts("Scan"));
        defCols.add(FieldKey.fromParts("Charge"));
        defCols.add(FieldKey.fromParts("PeptideProphet"));
        defCols.add(FieldKey.fromParts("Peptide"));
        defCols.add(FieldKey.fromParts("ProteinHits"));
        defCols.add(FieldKey.fromParts("Protein"));

        ContainerFilter.Type type = _searchSubfolders ? ContainerFilter.Type.CurrentAndSubfolders : ContainerFilter.Type.Current;
        ContainerFilter containerFilter = type.create(getSchema());
        return new MS2Schema(getViewContext().getUser(),
                getViewContext().getContainer()).createPeptidesTableInfo(false, containerFilter, filter, defCols);
    }

    @Override
    public DataView createDataView()
    {
        DataView view = super.createDataView();
        view.getRenderContext().setBaseSort(new Sort("Fraction/Run/Description,Scan,Charge"));
        return view;
    }
}

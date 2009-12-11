/*
 * Copyright (c) 2008-2009 LabKey Corporation
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
package org.labkey.ms1.view;

import org.labkey.api.data.*;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.reports.ReportService;
import org.labkey.api.security.User;
import org.labkey.api.view.DataView;
import org.labkey.ms1.query.PeptideFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Feb 29, 2008
 * Time: 11:01:37 AM
 */
public class PeptidesView extends QueryView
{
    public static final String DATAREGION_NAME = "pv";

    private boolean _searchSubfolders = true;
    private PeptideFilter _pepFilter;

    public PeptidesView(String schemaName, String queryName, User user, Container container)
    {
        super(QueryService.get().getUserSchema(user, container, schemaName));

        QuerySettings settings = new QuerySettings(getViewContext(), DATAREGION_NAME);
        settings.setSchemaName(schemaName);
        settings.setQueryName(queryName);
        settings.setAllowChooseQuery(false);
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

    public void setPeptideFilter(PeptideFilter pepFilter)
    {
        _pepFilter = pepFilter;
    }

    protected TableInfo createTable()
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addClause(_pepFilter);

        //add a clause to filter on container(s)
        Set<Container> containers = _searchSubfolders ?
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
        filter.addWhereClause(sql.toString(), null, "Fraction");

        List<FieldKey> defCols = new ArrayList<FieldKey>();
        defCols.add(FieldKey.fromParts("Fraction", "Run", "Description"));
        defCols.add(FieldKey.fromParts("Scan"));
        defCols.add(FieldKey.fromParts("Charge"));
        defCols.add(FieldKey.fromParts("PeptideProphet"));
        defCols.add(FieldKey.fromParts("Peptide"));
        defCols.add(FieldKey.fromParts("ProteinHits"));
        defCols.add(FieldKey.fromParts("Protein"));

        return MS2Service.get().createPeptidesTableInfo(getViewContext().getUser(),
                getViewContext().getContainer(), false, !_searchSubfolders, filter, defCols);
    }

    protected DataRegion createDataRegion()
    {
        DataRegion region = super.createDataRegion();
        region.setShadeAlternatingRows(true);
        return region;
    }

    public DataView createDataView()
    {
        DataView view = super.createDataView();
        view.getRenderContext().setBaseSort(new Sort("Fraction/Run/Description,Scan,Charge"));
        return view;
    }

}

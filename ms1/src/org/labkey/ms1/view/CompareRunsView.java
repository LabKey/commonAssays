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
package org.labkey.ms1.view;

import org.labkey.api.data.CrosstabTable;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.AggregateColumnInfo;
import org.labkey.api.query.*;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ActionURL;
import org.labkey.ms1.query.FeaturesTableInfo;
import org.labkey.ms1.query.MS1Schema;

/**
 * User: Dave
 * Date: Jan 22, 2008
 * Time: 3:56:33 PM
 */
public class CompareRunsView extends ComparisonCrosstabView
{
    private MS1Schema _schema = null;
    private int[] _runIds = null;

    public CompareRunsView(MS1Schema schema, int[] runIds, ActionURL url)
    {
        super(schema);
        _schema = schema;
        _runIds = runIds;

        getViewContext().setActionURL(url);
        
        QuerySettings settings = schema.getSettings(url.getPropertyValues(), QueryView.DATAREGIONNAME_DEFAULT);
        settings.setQueryName(MS1Schema.TABLE_COMPARE_PEP);
        settings.setAllowChooseView(true);
        setSettings(settings);

        setShowRecordSelectors(false);
        setAllowExportExternalQuery(false);
    }

    protected TableInfo createTable()
    {
        return _schema.getComparePeptideTableInfo(_runIds);
    }

    public DataView createDataView()
    {
        DataView view = super.createDataView();
        view.getRenderContext().setViewContext(getViewContext());
        String sortString = CrosstabTable.getDefaultSortString() + "," + AliasManager.makeLegalName(FeaturesTableInfo.COLUMN_PEPTIDE_INFO + "/Peptide", null);
        view.getRenderContext().setBaseSort(new Sort(sortString));
        return view;
    }

    protected FieldKey getComparisonColumn()
    {
        return FieldKey.fromParts(AggregateColumnInfo.NAME_PREFIX + "MIN_FeatureId");
    }
}

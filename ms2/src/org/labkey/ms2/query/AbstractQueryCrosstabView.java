/*
 * Copyright (c) 2011-2019 LabKey Corporation
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
package org.labkey.ms2.query;

import org.labkey.api.data.Sort;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;

/**
 * User: jeckels
 * Date: Sep 14, 2010
 */
public abstract class AbstractQueryCrosstabView extends ComparisonCrosstabView
{
    protected final MS2Schema _schema;
    protected final MS2Controller.PeptideFilteringComparisonForm _form;

    public AbstractQueryCrosstabView(MS2Schema schema, MS2Controller.PeptideFilteringComparisonForm form, ViewContext viewContext, MS2Schema.HiddenTableType tableType)
    {
        super(schema);
        _schema = schema;
        _form = form;

        setViewContext(viewContext);

        QuerySettings settings = schema.getSettings(viewContext.getBindPropertyValues(), QueryView.DATAREGIONNAME_DEFAULT);
        settings.setQueryName(tableType.toString());
        settings.setAllowChooseView(true);
        setSettings(settings);
        setAllowExportExternalQuery(false);

        setShowRecordSelectors(false);
    }

    protected abstract Sort getBaseSort();

    @Override
    public DataView createDataView()
    {
        DataView view = super.createDataView();
        view.getRenderContext().setViewContext(getViewContext());
        view.getRenderContext().setBaseSort(getBaseSort());
        return view;
    }
}

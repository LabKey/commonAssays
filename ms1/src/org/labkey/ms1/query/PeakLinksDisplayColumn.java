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
package org.labkey.ms1.query;

import org.labkey.api.data.*;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.util.Pair;
import org.labkey.ms1.MS1Controller;
import org.labkey.ms1.view.FeaturesView;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.Collections;

/**
 * User: Dave
 * Date: Jan 11, 2008
 * Time: 3:01:59 PM
 */
public class PeakLinksDisplayColumn extends DataColumn
{
    private ActionURL _basePeaksUrl = null;
    private ActionURL _baseDetailsUrl = null;

    private ColumnInfo _featureIdCol = null;

    public PeakLinksDisplayColumn(ColumnInfo colinfo)
    {
        super(colinfo);
    }

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        //if the base peaks or details URLs have not been set, do so
        if(null == _basePeaksUrl || null == _baseDetailsUrl)
            setBaseUrls(ctx.getViewContext().getActionURL());

        //column value should be a number (0 or 1)
        Number peaksAvailable = (Number)getValue(ctx);
        if(null == peaksAvailable || peaksAvailable.intValue() == 0)
            return;

        //get the corresponding feature id
        Number featureId = null;
        if(null != _featureIdCol)
            featureId = (Number)_featureIdCol.getValue(ctx);

        if(null == featureId)
            return;

        ActionURL detailsUrl = _baseDetailsUrl.clone();
        detailsUrl.addParameter("featureId", featureId.intValue());

        out.write(PageFlowUtil.textLink("details", detailsUrl.getLocalURIString()));

        ActionURL peaksUrl = _basePeaksUrl.clone();
        peaksUrl.addParameter("featureId", featureId.intValue());

        out.write("&nbsp;" + PageFlowUtil.textLink("peaks", peaksUrl.getLocalURIString()));
    }

    public void renderTitle(RenderContext ctx, Writer out) throws IOException
    {
        //don't render the caption
        out.write("&nbsp;");
    }

    private void setBaseUrls(ActionURL baseUrl)
    {
        Container container = ContainerManager.getForPath(baseUrl.getExtraPath());

        _basePeaksUrl = new ActionURL(MS1Controller.ShowPeaksAction.class, container);

        _baseDetailsUrl = new ActionURL(MS1Controller.ShowFeatureDetailsAction.class, container);
        _baseDetailsUrl.addParameter(MS1Controller.FeatureDetailsForm.ParamNames.srcUrl.name(), baseUrl.getLocalURIString());
        addQueryParams(_baseDetailsUrl, baseUrl);
    }

    private void addQueryParams(ActionURL url, ActionURL baseUrl)
    {
        String queryParamPrefix = FeaturesView.DATAREGION_NAME + ".";
        for(Pair<String,String> param : baseUrl.getParameters())
        {
            if(param.getKey().startsWith(queryParamPrefix))
                url.addParameter(param.getKey(), param.getValue());
        }
    }

    public boolean isFilterable()
    {
        return false;
    }

    public boolean isSortable()
    {
        return false;
    }

    public void addQueryColumns(Set<ColumnInfo> columns)
    {
        super.addQueryColumns(columns);
        TableInfo table = getBoundColumn().getParentTable();
        FieldKey currentKey = FieldKey.fromString(getBoundColumn().getName());
        FieldKey parentKey = currentKey.getParent();

        FieldKey featureIdKey = new FieldKey(parentKey, "FeatureId");

        _featureIdCol = QueryService.get().getColumns(table, Collections.singleton(featureIdKey)).get(featureIdKey);
        columns.add(_featureIdCol);
    }
}

/*
 * Copyright (c) 2007 LabKey Software Foundation
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
import org.labkey.ms1.MS1Controller;
import org.labkey.ms1.MS1Module;
import org.labkey.ms1.view.FeaturesView;
import org.labkey.common.util.Pair;

import java.io.Writer;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 11, 2008
 * Time: 3:01:59 PM
 */
public class PeakLinksDisplayColumn extends DataColumn
{
    private ActionURL _basePeaksUrl = null;
    private ActionURL _baseDetailsUrl = null;

    public PeakLinksDisplayColumn(ColumnInfo colinfo)
    {
        super(colinfo);
    }

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        //if the base peaks or details URLs have not been set, do so
        if(null == _basePeaksUrl || null == _baseDetailsUrl)
            setBaseUrls(ctx.getViewContext().getActionURL());

        Number peaksAvailable = (Number)ctx.get(PeaksAvailableColumnInfo.COLUMN_NAME);
        Integer featureId = (Integer)ctx.get("FeatureId");

        if(null != peaksAvailable && 0 != peaksAvailable.intValue() && null != featureId)
        {
            ActionURL detailsUrl = _baseDetailsUrl.clone();
            detailsUrl.addParameter("featureId", featureId.intValue());

            out.write("[<a href=\"");
            out.write(detailsUrl.getLocalURIString());
            out.write("\">details</a>]");

            ActionURL peaksUrl = _basePeaksUrl.clone();
            peaksUrl.addParameter("featureId", featureId.intValue());

            out.write("&nbsp;[<a href=\"");
            out.write(peaksUrl.getLocalURIString());
            out.write("\">peaks</a>]");
        }
    }

    private void setBaseUrls(ActionURL baseUrl)
    {
        _basePeaksUrl = baseUrl.clone();
        _basePeaksUrl.setPageFlow(MS1Module.CONTROLLER_NAME);
        _basePeaksUrl.setAction(MS1Controller.ShowPeaksAction.ACTION_NAME);
        _basePeaksUrl.deleteParameters();

        _baseDetailsUrl = baseUrl.clone();
        _baseDetailsUrl.setPageFlow(MS1Module.CONTROLLER_NAME);
        _baseDetailsUrl.setAction(MS1Controller.ShowFeatureDetailsAction.ACTION_NAME);
        _baseDetailsUrl.deleteParameters();
        addDetailsParams(_baseDetailsUrl, baseUrl);

    }

    private void addDetailsParams(ActionURL baseDetailsUrl, ActionURL baseUrl)
    {
        //add all query params as is, but prefix all the other params with
        //the features filter factory namespace prefix
        for(Pair<String,String> param : baseUrl.getParameters())
        {
            if(param.getKey().startsWith(FeaturesView.DATAREGION_NAME))
                baseDetailsUrl.addParameter(param.getKey(), param.getValue());
            else
                baseDetailsUrl.addParameter(FeaturesFilterFactory.NAMESPACE_PREFIX + param.getKey(), param.getValue());
        }

        //add one for the current container
        //results from sub-containers will need to know the container from which the search
        //occurred so that they can re-initialize the search list for prev/next buttons
        baseDetailsUrl.addParameter(FeaturesFilterFactory.NAMESPACE_PREFIX + FeaturesFilterFactory.PARAM_SOURCE_CONTAINER,
                baseUrl.getExtraPath());
    }

    public boolean isFilterable()
    {
        return false;
    }

    public boolean isSortable()
    {
        return false;
    }
}

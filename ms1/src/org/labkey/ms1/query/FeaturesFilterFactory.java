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

import org.labkey.api.protein.ProteinService;
import org.labkey.api.view.ActionURL;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.ms1.MS1Controller;
import org.labkey.ms1.MS1Manager;
import org.labkey.ms1.model.Feature;

import java.util.ArrayList;
import java.sql.SQLException;

/**
 * Can be used to recreate a set of features filters based on a set of query string parameters
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 14, 2008
 * Time: 12:47:33 PM
 */
public class FeaturesFilterFactory
{
    public static final String NAMESPACE = "fvf";
    public static final String NAMESPACE_PREFIX = NAMESPACE + ".";
    public static final String PARAM_SOURCE_CONTAINER = "sc";

    public static ArrayList<FeaturesFilter> createFilters(ActionURL url, User user)
    {
        ArrayList<FeaturesFilter> filters = new ArrayList<FeaturesFilter>();

        //start with the container filter
        String scParam = url.getParameter(NAMESPACE_PREFIX + PARAM_SOURCE_CONTAINER);
        String subfoldersParam = url.getParameter(NAMESPACE_PREFIX + ProteinService.PeptideSearchForm.ParamNames.subfolders.name());
        Container root = null != scParam && scParam.length() > 0 
                ? ContainerManager.getForPath(scParam)
                : ContainerManager.getForPath(url.getExtraPath());
        filters.add(new ContainerFeaturesFilter(root, null != subfoldersParam && subfoldersParam.length() > 0, user));

        //RunFilter
        String runParam = url.getParameter(NAMESPACE_PREFIX + MS1Controller.ShowFeaturesForm.ParamNames.runId.name());
        if(null != runParam && runParam.length() > 0)
            filters.add(new RunFilter(runParam));

        //PeptideFilter
        String pepParam = url.getParameter(NAMESPACE_PREFIX + ProteinService.PeptideSearchForm.ParamNames.pepSeq.name());
        String exactParam = url.getParameter(ProteinService.PeptideSearchForm.ParamNames.exact.name());
        if(null != pepParam && pepParam.length() > 0)
            filters.add(new PeptideFilter(pepParam, null != exactParam && exactParam.length() > 0));

        //find similar filters (requires knowledge of the source feature)
        String srcFeatureParam = url.getParameter(NAMESPACE_PREFIX + MS1Controller.SimilarSearchForm.ParamNames.featureId.name());
        if(null != srcFeatureParam && srcFeatureParam.length() > 0)
        {
            try
            {
                Feature feature = MS1Manager.get().getFeature(Integer.parseInt(srcFeatureParam));
                if(null != feature)
                {
                    //mz filter
                    String mzOffsetParam = url.getParameter(NAMESPACE_PREFIX + MS1Controller.SimilarSearchForm.ParamNames.mzOffset.name());
                    if(null != mzOffsetParam && mzOffsetParam.length() > 0)
                        filters.add(new MzFilter(feature.getMz().doubleValue(), Double.parseDouble(mzOffsetParam),
                                MS1Controller.SimilarSearchForm.MzOffsetUnits.valueOf(
                                        url.getParameter(NAMESPACE_PREFIX + MS1Controller.SimilarSearchForm.ParamNames.mzUnits.name()))));

                    //time offsets and units
                    String timeUnitsParam = url.getParameter(NAMESPACE_PREFIX + MS1Controller.SimilarSearchForm.ParamNames.timeUnits.name());
                    String timeOffsetParam = url.getParameter(NAMESPACE_PREFIX + MS1Controller.SimilarSearchForm.ParamNames.timeOffset.name());
                    if(null != timeUnitsParam && timeUnitsParam.length() > 0 &&
                            null != timeOffsetParam && timeOffsetParam.length() > 0 &&
                            timeUnitsParam.equals(MS1Controller.SimilarSearchForm.TimeOffsetUnits.rt.name()))
                    {
                        //retention time filter
                        filters.add(new RetentionTimeFilter(feature.getTime().doubleValue() - Double.parseDouble(timeOffsetParam),
                                feature.getTime().doubleValue() + Double.parseDouble(timeOffsetParam)));
                    }

                    //scan filter
                    if(null != timeUnitsParam && timeUnitsParam.length() > 0 &&
                            null != timeOffsetParam && timeOffsetParam.length() > 0 &&
                            timeUnitsParam.equals(MS1Controller.SimilarSearchForm.TimeOffsetUnits.scans.name()))
                    {
                        //retention time filter
                        filters.add(new ScanFilter(feature.getScan().intValue() - Integer.parseInt(timeOffsetParam),
                                feature.getScan().intValue() + Integer.parseInt(timeOffsetParam)));
                    }
                }
            }
            catch(SQLException e)
            {
                //if we can't get the feature, we can't set the filters, and the view is going to fail anyway
            }
        }

        return filters;
    }

}

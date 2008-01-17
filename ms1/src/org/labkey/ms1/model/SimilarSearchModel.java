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
package org.labkey.ms1.model;

import org.labkey.api.data.Container;
import org.labkey.api.view.ActionURL;
import org.labkey.ms1.MS1Controller;

import java.text.DecimalFormat;

/**
 * Model used by SimilarSearchView.jsp
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 16, 2008
 * Time: 11:17:19 AM
 */
public class SimilarSearchModel
{
    private String _resultsUri;
    private Feature _feature;
    private DecimalFormat _numberFormat = new DecimalFormat("0.0000");
    private double _mzOffset = 0;
    private MS1Controller.SimilarSearchForm.MzOffsetUnits _mzUnits = MS1Controller.SimilarSearchForm.MzOffsetUnits.ppm;
    private double _tOffset = 0;
    private MS1Controller.SimilarSearchForm.TimeOffsetUnits _tUnits = MS1Controller.SimilarSearchForm.TimeOffsetUnits.rt;
    private boolean _subfolders = false;

    public SimilarSearchModel(Feature feature, Container container,
                              double mzOffset, MS1Controller.SimilarSearchForm.MzOffsetUnits mzUnits,
                              double timeOffset, MS1Controller.SimilarSearchForm.TimeOffsetUnits timeUnits,
                              boolean searchSubfolders)
    {
        _feature = feature;
        _resultsUri = new ActionURL(MS1Controller.SimilarSearchAction.class, container).getLocalURIString();
        _mzOffset = mzOffset;
        _mzUnits = mzUnits;
        _tOffset = timeOffset;
        _tUnits = timeUnits;
        _subfolders = searchSubfolders;
    }

    public String getResultsUri()
    {
        return _resultsUri;
    }

    public Feature getFeature()
    {
        return _feature;
    }

    public MS1Controller.SimilarSearchForm.MzOffsetUnits getMzUnits()
    {
        return _mzUnits;
    }

    public void setMzUnits(MS1Controller.SimilarSearchForm.MzOffsetUnits mzUnits)
    {
        _mzUnits = mzUnits;
    }

    public double getMzOffset()
    {
        return _mzOffset;
    }

    public void setMzOffset(double mzOffset)
    {
        _mzOffset = mzOffset;
    }

    public double getTimeOffset()
    {
        return _tOffset;
    }

    public void setTimeOffset(double tOffset)
    {
        _tOffset = tOffset;
    }

    public MS1Controller.SimilarSearchForm.TimeOffsetUnits getTimeUnits()
    {
        return _tUnits;
    }

    public void setTimeUnits(MS1Controller.SimilarSearchForm.TimeOffsetUnits tUnits)
    {
        _tUnits = tUnits;
    }

    public String formatNumber(Number num)
    {
        return _numberFormat.format(num);
    }

    public boolean searchSubfolders()
    {
        return _subfolders;
    }

    public void setSearchSubfolders(boolean searchSubfolders)
    {
        _subfolders = searchSubfolders;
    }
}

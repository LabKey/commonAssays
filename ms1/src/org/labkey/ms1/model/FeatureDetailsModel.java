/*
 * Copyright (c) 2007-2013 LabKey Corporation
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
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.ms2.MS2Urls;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.ms1.MS1Controller;
import org.labkey.ms1.MS1Manager;
import org.labkey.ms1.view.FeaturesView;

import java.text.DecimalFormat;

/**
 * Used as a context object for the FeatureDetailsView
 * User: Dave
 * Date: Oct 17, 2007
 * Time: 3:03:23 PM
 */
public class FeatureDetailsModel
{
    private Feature _feature;
    private int _prevFeatureId = -1;
    private int _nextFeatureId = -1;
    private Integer[] _prevNextScans;
    private String _srcUrl;
    private double _mzWindowLow = 0;
    private double _mzWindowHigh = 0;
    private int _scanWindowLow = 0;
    private int _scanWindowHigh = 0;
    private DecimalFormat _decimalFormat = new DecimalFormat("#,##0.0000");
    private ActionURL _peaksUrl = null;
    private ActionURL _pepUrl = null;
    private ActionURL _findSimilarUrl = null;
    private ActionURL _pepSearchUrl = null;
    private ActionURL _runDetailsUrl = null;
    private int _scan = -1;
    private ActionURL _url;
    private Container _container;
    private int _runId = -1;

    public FeatureDetailsModel(Feature feature, int prevFeatureId, int nextFeatureId, String srcUrl,
                               double mzWindowLow, double mzWindowHigh, int scanWindowLow, int scanWindowHigh,
                               int scan, Container container, ActionURL url)
    {
        assert null != feature : "Null feature passed to FeatureDetailsModel!";
        assert null != feature.getScan() : "Feature has no apex scan!";
        assert null != feature.getScanFirst() && null != feature.getScanLast() : "Feature has no defined scan range!";
        assert null != feature.getRunId() : "Feature has no experiment run!";

        _feature = feature;
        _runId = _feature.getRunId();
        _container = container;
        _prevFeatureId = prevFeatureId;
        _nextFeatureId = nextFeatureId;
        _srcUrl = srcUrl;
        _url = url;
        _mzWindowLow = mzWindowLow;
        _mzWindowHigh = mzWindowHigh;
        _scanWindowLow = scanWindowLow;
        _scanWindowHigh = scanWindowHigh;
        _scan = scan < 0 ? feature.getScan().intValue() : scan;

        //adjust scan so that it is within the requested scan window
        _scan = Math.max(_scan, feature.getScanFirst().intValue() + scanWindowLow);
        _scan = Math.min(_scan, feature.getScanLast().intValue() + scanWindowHigh);
        
        if(null != _feature.getScanFirst() && null != _feature.getScanLast()
                && null != _feature.getRunId())
        {
            _peaksUrl = new ActionURL(MS1Controller.ShowPeaksAction.class, container);
            _peaksUrl.addParameter("runId", _feature.getRunId().intValue());
            _peaksUrl.addParameter("featureId", _feature.getFeatureId());
            _peaksUrl.addParameter("scanFirst", _feature.getScanFirst().intValue() + _scanWindowLow);
            _peaksUrl.addParameter("scanLast", _feature.getScanLast().intValue() + _scanWindowHigh);
        }

        _pepUrl = new ActionURL(MS1Controller.ShowMS2PeptideAction.class,  container);
        _pepUrl.addParameter("featureId", _feature.getFeatureId());

        _findSimilarUrl = new ActionURL(MS1Controller.SimilarSearchAction.class, container);
        _findSimilarUrl.addParameter(MS1Controller.SimilarSearchForm.ParamNames.featureId.name(), _feature.getFeatureId());

        _pepSearchUrl = new ActionURL(MS1Controller.PepSearchAction.class, container);

        _prevNextScans = getPrevNextScans();

        _runDetailsUrl = PageFlowUtil.urlProvider(ExperimentUrls.class).getRunGraphURL(container, _feature.getRunId());
    }

    public Feature getFeature()
    {
        return _feature;
    }

    public int getPrevFeatureId()
    {
        return _prevFeatureId;
    }

    public int getNextFeatureId()
    {
        return _nextFeatureId;
    }

    public Integer[] getPrevNextScans()
    {
        if(null == _feature || null == _feature.getRunId() || null == _feature.getMz()
                || null == _feature.getScanFirst() || null == _feature.getScanLast())
            return new Integer[0];

        return MS1Manager.get().getPrevNextScan(_feature.getRunId().intValue(),
                _feature.getMz().doubleValue() + _mzWindowLow,
                _feature.getMz().doubleValue() + _mzWindowHigh,
                _feature.getScanFirst().intValue() + _scanWindowLow, 
                _feature.getScanLast().intValue() + _scanWindowHigh,
                _scan);
    }

    public String getSrcUrl()
    {
        return _srcUrl;
    }

    public double getMzWindowLow()
    {
        return _mzWindowLow;
    }

    public double getMzWindowHigh()
    {
        return _mzWindowHigh;
    }

    public int getScanWindowLow()
    {
        return _scanWindowLow;
    }

    public int getScanWindowHigh()
    {
        return _scanWindowHigh;
    }

    public String formatNumber(Number number)
    {
        if(null == number)
            return "&nbsp;";
        else
            return _decimalFormat.format(number);
    }

    public String getPeaksUrl(boolean limitScan)
    {
        return getPeaksUrl(_mzWindowLow, _mzWindowHigh, limitScan);
    }

    public String getPeaksUrl(double mzWindowLow, double mzWindowHigh, boolean limitScan)
    {
        if(null == _peaksUrl)
            return null;

        String url = _peaksUrl.getLocalURIString()
                + "&query.MZ~gte=" + (_feature.getMz().doubleValue()+mzWindowLow)
                + "&query.MZ~lte=" + (_feature.getMz().doubleValue()+mzWindowHigh);
        if(limitScan)
            return url + "&query.ScanId/Scan~eq=" + _scan;
        else
            return url;
    }

    public String getPepUrl()
    {
        return null == _pepUrl ? "" : _pepUrl.getLocalURIString();
    }

    public String getPepUrl(int runId, long peptideId, int rowIndex, int scan)
    {
        MS2Urls ms2urls = PageFlowUtil.urlProvider(MS2Urls.class);
        assert null != ms2urls : "Couldn't get the MS2 Url Provider!";

        ActionURL url = ms2urls.getShowPeptideUrl(_container);
        url.addParameter("run", runId);
        url.addParameter("peptideId", String.valueOf(peptideId));
        url.addParameter("rowIndex", rowIndex);
        return url.getLocalURIString() + "&MS2Peptides.Scan~eq=" + String.valueOf(scan);
    }

    public String getPepSearchUrl(String sequence)
    {
        _pepSearchUrl.replaceParameter(ProteinService.PeptideSearchForm.ParamNames.pepSeq.name(), sequence);
        return _pepSearchUrl.getLocalURIString();
    }

    public int getScan()
    {
        return _scan;
    }

    public String getFindSimilarUrl()
    {
        return _findSimilarUrl.getLocalURIString();
    }

    public String formatWindowExtent(Number number, boolean zeroAsNeg)
    {
        if(0 == number.doubleValue() && zeroAsNeg)
            return "-" + number.toString();
        else if(number.doubleValue() >= 0)
            return "+" + number.toString();
        else
            return number.toString();
    }

    public String formatWindowExtent(Number number)
    {
        return formatWindowExtent(number, false);
    }

    public ActionURL getUrl()
    {
        return _url;
    }

    public String getPrevFeatureUrl()
    {
        return getFeatureDetailsUrl(_prevFeatureId);
    }

    public String getNextFeatureUrl()
    {
        return getFeatureDetailsUrl(_nextFeatureId);
    }

    private String getFeatureDetailsUrl(int featureId)
    {
        ActionURL url = _url.clone();
        //scan is specific to a feature, so remove that from the new URL
        url.deleteParameter(MS1Controller.FeatureDetailsForm.ParamNames.scan.name());
        //replace feature id param
        url.replaceParameter(MS1Controller.FeatureDetailsForm.ParamNames.featureId.name(), String.valueOf(featureId));
        return url.getLocalURIString();
    }

    public String getPrevScanUrl()
    {
        if(null != _prevNextScans && null != _prevNextScans[0])
        {
            ActionURL url = _url.clone();
            url.replaceParameter(MS1Controller.FeatureDetailsForm.ParamNames.scan.name(),
                    String.valueOf(_prevNextScans[0].intValue()));
            return url.getLocalURIString();
        }
        else
            return null;
    }

    public String getNextScanUrl()
    {
        if(null != _prevNextScans && null != _prevNextScans[1])
        {
            ActionURL url = _url.clone();
            url.replaceParameter(MS1Controller.FeatureDetailsForm.ParamNames.scan.name(),
                    String.valueOf(_prevNextScans[1]));
            return url.getLocalURIString();
        }
        else
            return null;
    }

    public String getChartUrl(String type)
    {
        ActionURL url = new ActionURL(MS1Controller.ShowChartAction.class, _container);
        url.addParameter("type", type);
        url.addParameter("featureId", _feature.getFeatureId());
        url.addParameter("runId", _runId);
        url.addParameter("scan", _scan);
        url.addParameter("scanFirst", _feature.getScanFirst().intValue() + _scanWindowLow);
        url.addParameter("scanLast", _feature.getScanLast().intValue() + _scanWindowHigh);

        //elution chart uses a fixed 0.02 mz window
        double mzWindowLow = type.equalsIgnoreCase("elution") ? -0.02 : _mzWindowLow;
        double mzWindowHigh = type.equalsIgnoreCase("elution") ? 0.02 : _mzWindowHigh;

        url.addParameter("mzLow", String.valueOf(_feature.getMz().doubleValue() + mzWindowLow));
        url.addParameter("mzHigh", String.valueOf(_feature.getMz().doubleValue() + mzWindowHigh));

        return url.getLocalURIString();
    }

    public String getMzWindow()
    {
        return formatWindowExtent(_mzWindowLow, true) + "/" + formatWindowExtent(_mzWindowHigh);
    }

    public String getScanWindow()
    {
        return formatWindowExtent(_scanWindowLow, true) + "/" + formatWindowExtent(_scanWindowHigh);
    }

    public String getQueryFiltersAsInputs()
    {
        StringBuilder sb = new StringBuilder();
        for(Pair<String,String> param : _url.getParameters())
        {
            if(param.getKey().startsWith(FeaturesView.DATAREGION_NAME + "."))
            {
                sb.append("<input type=\"hidden\" name=\"");
                sb.append(param.getKey());
                sb.append("\" value=\"");
                sb.append(param.getValue());
                sb.append("\"/>");
            }
        }
        return sb.toString();
    }

    public String getRunDetailsUrl()
    {
        return _runDetailsUrl.getLocalURIString();
    }
}

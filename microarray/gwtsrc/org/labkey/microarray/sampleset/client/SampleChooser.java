package org.labkey.microarray.sampleset.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.*;
import org.labkey.api.gwt.client.util.ServiceUtil;
import org.labkey.api.gwt.client.util.PropertyUtil;
import org.labkey.microarray.sampleset.client.model.GWTSampleSet;

/**
 * User: jeckels
 * Date: Feb 8, 2008
 */
public class SampleChooser implements EntryPoint
{
    public static final String PROP_NAME_SAMPLE_COUNT = "sampleCount";
    public static final String PROP_NAME_DEFAULT_SAMPLE_SET_LSID = "defaultSampleSetLSID";
    public static final String PROP_NAME_DEFAULT_SAMPLE_SET_NAME = "defaultSampleSetName";
    public static final String PROP_NAME_DEFAULT_SAMPLE_ROW_ID = "defaultSampleRowId";

    public static final GWTSampleSet NONE_SAMPLE_SET = new GWTSampleSet("<None>", "--DUMMY-LSID--");

    private SampleInfo[] _sampleInfos;
    private SampleCache _cache;

    private FlexTable _table = new FlexTable();

    public void onModuleLoad()
    {
        RootPanel rootPanel = ServiceUtil.findRootPanel("org.labkey.microarray.sampleset.client.SampleChooser");
        int sampleCount = Integer.parseInt(PropertyUtil.getServerProperty(PROP_NAME_SAMPLE_COUNT));

        _table.setWidget(0, 1, new HTML("<b>Sample Set</b>"));
        _table.setWidget(0, 2, new HTML("<b>Sample Name</b>"));

        _sampleInfos = new SampleInfo[sampleCount];
        _cache = new SampleCache(_sampleInfos);

        for (int i = 0; i < sampleCount; i++)
        {
            _sampleInfos[i] = new SampleInfo(i, _cache);

            int tableRow = i + 1;
            _table.setWidget(tableRow, 0, new Label(_sampleInfos[i].getName()));
            _table.setWidget(tableRow, 1, _sampleInfos[i].getSampleSetListBox());

            HorizontalPanel materialPanel = new HorizontalPanel();
            materialPanel.add(_sampleInfos[i].getMaterialListBox());
            materialPanel.add(_sampleInfos[i].getMaterialTextBox());
            _table.setWidget(tableRow, 2, materialPanel);
        }

        String activeSampleSetLSID = PropertyUtil.getServerProperty(PROP_NAME_DEFAULT_SAMPLE_SET_LSID);
        if (activeSampleSetLSID != null)
        {
            String activeSampleSetName = PropertyUtil.getServerProperty(PROP_NAME_DEFAULT_SAMPLE_SET_NAME);
            int activeSampleSetRowId = Integer.parseInt(PropertyUtil.getServerProperty(PROP_NAME_DEFAULT_SAMPLE_ROW_ID));
            GWTSampleSet activeSampleSet = new GWTSampleSet(activeSampleSetName, activeSampleSetLSID);
            activeSampleSet.setRowId(activeSampleSetRowId);
            _cache.addSampleSet(activeSampleSet);
            GWTSampleSet[] sets = new GWTSampleSet[] { activeSampleSet };
            for (int i = 0; i < _sampleInfos.length; i++)
            {
                _sampleInfos[i].setSampleSets(sets, activeSampleSet);
            }
        }

        rootPanel.add(_table);
    }
}

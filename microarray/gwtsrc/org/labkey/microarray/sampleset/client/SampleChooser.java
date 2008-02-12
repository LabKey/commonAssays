package org.labkey.microarray.sampleset.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.DOM;
import org.labkey.api.gwt.client.util.ServiceUtil;
import org.labkey.api.gwt.client.util.PropertyUtil;
import org.labkey.api.gwt.client.ui.FormUtil;
import org.labkey.microarray.sampleset.client.model.GWTSampleSet;

/**
 * User: jeckels
 * Date: Feb 8, 2008
 */
public class SampleChooser implements EntryPoint
{
    public static final String PROP_NAME_MAX_SAMPLE_COUNT = "maxSampleCount";
    public static final String PROP_NAME_MIN_SAMPLE_COUNT = "minSampleCount";
    public static final String PROP_NAME_DEFAULT_SAMPLE_SET_LSID = "defaultSampleSetLSID";
    public static final String PROP_NAME_DEFAULT_SAMPLE_SET_NAME = "defaultSampleSetName";
    public static final String PROP_NAME_DEFAULT_SAMPLE_ROW_ID = "defaultSampleRowId";

    public static final String SAMPLE_COUNT_ELEMENT_NAME = "__sampleCount";
    
    public static final GWTSampleSet NONE_SAMPLE_SET = new GWTSampleSet("<None>", "--DUMMY-LSID--");

    private SampleInfo[] _sampleInfos;
    private SampleCache _cache;

    private FlexTable _table = new FlexTable();
    private int _maxSampleCount;
    private int _minSampleCount;

    public void onModuleLoad()
    {
        RootPanel rootPanel = ServiceUtil.findRootPanel("org.labkey.microarray.sampleset.client.SampleChooser");
        _maxSampleCount = Integer.parseInt(PropertyUtil.getServerProperty(PROP_NAME_MAX_SAMPLE_COUNT));
        _minSampleCount = Integer.parseInt(PropertyUtil.getServerProperty(PROP_NAME_MIN_SAMPLE_COUNT));

        if (_minSampleCount < _maxSampleCount)
        {
            final ListBox sampleCountListBox = new ListBox();
            for (int i = _minSampleCount; i <= _maxSampleCount; i++)
            {
                sampleCountListBox.addItem(i + " sample" + (i == 1 ? "" : "s"), Integer.toString(i));
            }
            sampleCountListBox.setSelectedIndex(sampleCountListBox.getItemCount() - 1);
            sampleCountListBox.addChangeListener(new ChangeListener()
            {
                public void onChange(Widget sender)
                {
                    updateSampleCount(sampleCountListBox);
                }
            });
            _table.setWidget(0, 0, sampleCountListBox);
        }
        
        setSampleCount(_maxSampleCount);

        _table.setWidget(0, 1, new HTML("<b>Sample Set</b>"));
        _table.setWidget(0, 2, new HTML("<b>Sample Name</b>"));

        _sampleInfos = new SampleInfo[_maxSampleCount];
        _cache = new SampleCache(_sampleInfos);

        for (int i = 0; i < _maxSampleCount; i++)
        {
            _sampleInfos[i] = new SampleInfo(i, _cache);
            _sampleInfos[i].setVisible(i < _maxSampleCount);

            int tableRow = i + 1;
            _table.setWidget(tableRow, 0, _sampleInfos[i].getLabel());
            _table.getFlexCellFormatter().setHorizontalAlignment(tableRow, 0, HasHorizontalAlignment.ALIGN_RIGHT);
            
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

    private void updateSampleCount(ListBox sampleCountListBox)
    {
        int count = Integer.parseInt(sampleCountListBox.getValue(sampleCountListBox.getSelectedIndex()));
        for (int i = 0; i < _maxSampleCount; i++)
        {
            _sampleInfos[i].setVisible(i < count);
        }
        setSampleCount(count);
    }

    private void setSampleCount(int count)
    {
        FormUtil.setValueInForm(Integer.toString(count), DOM.getElementById(SAMPLE_COUNT_ELEMENT_NAME));
    }
}

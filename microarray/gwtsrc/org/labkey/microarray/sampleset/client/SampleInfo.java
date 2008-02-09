package org.labkey.microarray.sampleset.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import org.labkey.microarray.sampleset.client.model.GWTSampleSet;
import org.labkey.microarray.sampleset.client.model.GWTMaterial;
import org.labkey.api.gwt.client.ui.FormUtil;

/**
 * User: jeckels
 * Date: Feb 8, 2008
 */
public class SampleInfo
{
    private ListBox _sampleSetListBox;
    private ListBox _materialListBox;
    private TextBox _materialTextBox;
    private String _name;
    private SampleCache _cache;
    private int _index;

    public SampleInfo(int index, SampleCache cache)
    {
        _name = "Sample " + (index + 1);
        _index = index;
        _cache = cache;

        _sampleSetListBox = new ListBox();
        _materialListBox = new ListBox();
        _materialTextBox = new TextBox();

        _sampleSetListBox.addChangeListener(new ChangeListener()
        {
            public void onChange(Widget sender)
            {
                updateMaterialListBox(getSelectedSampleSet());
            }
        });

        ChangeListener changeListener = new ChangeListener()
        {
            public void onChange(Widget sender)
            {
                pushToForm();
            }
        };
        _sampleSetListBox.addChangeListener(changeListener);
        _materialListBox.addChangeListener(changeListener);
        _materialTextBox.addChangeListener(changeListener);

        KeyboardListener keyListener = new KeyboardListenerAdapter()
        {
            public void onKeyPress(Widget sender, char keyCode, int modifiers)
            {
                pushToForm();
            }
        };
        _materialTextBox.addKeyboardListener(keyListener);

        setSampleSets(new GWTSampleSet[0], SampleChooser.NONE_SAMPLE_SET);
    }

    public void setSampleSets(GWTSampleSet[] sets, GWTSampleSet selectedSet)
    {
        _sampleSetListBox.clear();
        _sampleSetListBox.addItem(SampleChooser.NONE_SAMPLE_SET.getName(), SampleChooser.NONE_SAMPLE_SET.getLsid());
        for (int i = 0; i < sets.length; i++)
        {
            GWTSampleSet set = sets[i];
            _sampleSetListBox.addItem(set.getName(), set.getLsid());
            if (set.equals(selectedSet))
            {
                _sampleSetListBox.setSelectedIndex(_sampleSetListBox.getItemCount() - 1);
            }
        }
        updateMaterialListBox(selectedSet);
    }

    public String getName()
    {
        return _name;
    }

    public ListBox getSampleSetListBox()
    {
        return _sampleSetListBox;
    }

    public ListBox getMaterialListBox()
    {
        return _materialListBox;
    }

    public TextBox getMaterialTextBox()
    {
        return _materialTextBox;
    }

    private void pushToForm()
    {
        String lsid;
        String name;
        if (_materialListBox.isVisible() && _materialListBox.getSelectedIndex() != -1)
        {
            lsid = _materialListBox.getValue(_materialListBox.getSelectedIndex());
            name = "";
        }
        else
        {
            lsid = "";
            name = _materialTextBox.getText();
        }
        
        FormUtil.setValueInForm(lsid, DOM.getElementById(getLsidFormElementID(_index)));
        FormUtil.setValueInForm(name, DOM.getElementById(getNameFormElementID(_index)));
    }

    private void updateMaterialListBox(final GWTSampleSet sampleSet)
    {
        _materialListBox.clear();
        if (SampleChooser.NONE_SAMPLE_SET.equals(sampleSet))
        {
            _materialListBox.setVisible(false);
            // Do this to prevent layout changes
            DOM.setStyleAttribute(_materialTextBox.getElement(), "visibility", "visible");
        }
        else
        {
            populateMaterials(_cache.getMaterials(sampleSet));

            _materialListBox.setVisible(true);
            // Do this to prevent layout changes
            DOM.setStyleAttribute(_materialTextBox.getElement(), "visibility", "hidden");
        }

    }

    private void populateMaterials(GWTMaterial[] materials)
    {
        if (materials == null)
        {
            _materialListBox.addItem("<Loading...>");
            _materialListBox.setEnabled(false);
        }
        else
        {
            _materialListBox.clear();
            for (int i = 0; i < materials.length; i++)
            {
                _materialListBox.addItem(materials[i].getName(), materials[i].getLsid());
            }
            _materialListBox.setEnabled(true);
            _materialListBox.setSelectedIndex(0);
            pushToForm();
        }
    }

    private GWTSampleSet getSelectedSampleSet()
    {
        int index = _sampleSetListBox.getSelectedIndex();
        if (index != -1)
        {
            return _cache.getSampleSet(_sampleSetListBox.getValue(index));
        }
        return null;
    }

    public void updateMaterials(GWTSampleSet sampleSet, GWTMaterial[] materials)
    {
        if (sampleSet.equals(getSelectedSampleSet()))
        {
            populateMaterials(materials);
        }
    }

    public void updateSampleSets(GWTSampleSet[] sets)
    {
        setSampleSets(sets, getSelectedSampleSet());
    }

    public static String getLsidFormElementID(int index)
    {
        return "__sample" + index + "LSID";
    }

    public static String getNameFormElementID(int index)
    {
        return "__sample" + index + "Name";
    }
}

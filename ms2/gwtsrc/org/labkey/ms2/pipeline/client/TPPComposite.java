/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.ms2.pipeline.client;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.labkey.api.gwt.client.pipeline.GWTPipelineConfig;
import org.labkey.api.gwt.client.pipeline.GWTPipelineTask;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * UI to let user configure selected TPP parameters
 * User: jeckels
 * Date: Jan 20, 2012
 */
public class TPPComposite extends SearchFormComposite implements PipelineConfigCallback
{
    protected FlexTable _instance = new FlexTable();
    private TextBox _peptideProphetTextBox = new TextBox();
    private TextBox _proteinProphetTextBox = new TextBox();
    private ListBox _quantitationAlgorithmListBox = new ListBox();
    private TextBox _massToleranceTextBox = new TextBox();
    private TextBox _residueLabeLMassTextBox = new TextBox();
    private TextBox _libraConfigNameTextBox = new TextBox();
    private ListBox _libraNormalizationChannelListBox = new ListBox();
    private final int _massToleranceRow;
    private final int _residueLabelMassRow;
    private final int _libraConfigNameRow;
    private final int _libraNormalizationChannelRow;
    private boolean _visible = false;
    public static final int MAX_LIBRA_CHANNELS = 16;

    public TPPComposite()
    {
        int row = 0;
        _instance.setWidget(row, 0, new Label("Minimum PeptideProphet prob"));
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        _peptideProphetTextBox.setVisibleLength(4);
        _instance.setText(row, 1, "<default>");

        _instance.setWidget(++row, 0, new Label("Minimum ProteinProphet prob"));
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        _proteinProphetTextBox.setVisibleLength(4);
        _instance.setText(row, 1, "<default>");

        _instance.setWidget(++row, 0, new Label("Quantitation engine"));
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        _quantitationAlgorithmListBox.addItem("<none>");
        _quantitationAlgorithmListBox.addItem("Libra");
        _quantitationAlgorithmListBox.addItem("Q3");
        _quantitationAlgorithmListBox.addItem("XPRESS");
        _instance.setText(row, 1, "<none>");

        _instance.setWidget(++row, 0, new Label("Quantitation mass tolerance"));
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        _massToleranceTextBox.setVisibleLength(4);
        _instance.setText(row, 1, "<default>");
        _instance.getRowFormatter().setVisible(row, false);
        _massToleranceRow = row;

        _instance.setWidget(++row, 0, new Label("Quantitation residue mass label"));
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        _residueLabeLMassTextBox.setVisibleLength(20);
        _instance.setText(row, 1, "");
        _instance.getRowFormatter().setVisible(row, false);
        _residueLabelMassRow = row;

        _instance.setWidget(++row, 0, new Label("Libra config name"));
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        _libraConfigNameTextBox.setVisibleLength(20);
        _instance.setText(row, 1, "");
        _instance.getRowFormatter().setVisible(row, false);
        _libraConfigNameRow = row;

        _instance.setWidget(++row, 0, new Label("Libra normalization channel"));
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        for (int i = 1; i <= MAX_LIBRA_CHANNELS; i++)
        {
            _libraNormalizationChannelListBox.addItem(Integer.toString(i));
        }
        _instance.setText(row, 1, "");
        _instance.getRowFormatter().setVisible(row, false);
        _libraNormalizationChannelRow = row;

        initWidget(_instance);
    }

    @Override
    public void setWidth(String width)
    {
    }

    @Override
    public Widget getLabel()
    {
        Label result = new Label("TPP");
        result.setStyleName(LABEL_STYLE_NAME);
        return result;
    }

    @Override
    public String validate()
    {
        if (!validateNumber(_peptideProphetTextBox, 0, 1, true))
            return "Minimum PeptideProphet probability must be a number between 0 and 1, inclusive";
        if (!validateNumber(_proteinProphetTextBox, 0, 1, true))
            return "Minimum ProteinProphet probability must be a number between 0 and 1, inclusive";
        if (isLibra())
        {
            if (_libraConfigNameTextBox.getText().trim().isEmpty())
                return "Libra configuration name is required";
        }
        if (isXPRESS() || isQ3())
        {
            if (!validateNumber(_massToleranceTextBox, 0, Double.MIN_VALUE, true))
                return "Mass tolerance must be a non-negative number";
            if (_residueLabeLMassTextBox.getText().trim().isEmpty())
                return "Residue label mass is required when using XPRESS or Q3";
        }
        return "";
    }

    private boolean isXPRESS()
    {
        return "xpress".equalsIgnoreCase(_quantitationAlgorithmListBox.getItemText(_quantitationAlgorithmListBox.getSelectedIndex()));
    }

    private boolean isLibra()
    {
        return "libra".equalsIgnoreCase(_quantitationAlgorithmListBox.getItemText(_quantitationAlgorithmListBox.getSelectedIndex()));
    }

    private boolean isQ3()
    {
        return "q3".equalsIgnoreCase(_quantitationAlgorithmListBox.getItemText(_quantitationAlgorithmListBox.getSelectedIndex()));
    }

    private void setQuantitationVisibility()
    {
        boolean xpress = isXPRESS();
        boolean q3 = isQ3();
        boolean libra = isLibra();
        _instance.getRowFormatter().setVisible(_massToleranceRow, xpress || q3);
        _instance.getRowFormatter().setVisible(_residueLabelMassRow, xpress || q3);
        _instance.getRowFormatter().setVisible(_libraConfigNameRow, libra);
        _instance.getRowFormatter().setVisible(_libraNormalizationChannelRow, libra);
    }

    private boolean validateNumber(TextBox textBox, double min, double max, boolean isDouble)
    {
        String s = textBox.getText().trim();
        if (!s.isEmpty())
        {
            try
            {
                double d = isDouble ? Double.parseDouble(s) : Integer.parseInt(s);
                if (d < min || d > max)
                {
                    return false;
                }
            }
            catch (NumberFormatException e)
            {
                return false;
            }
        }
        return true;
    }

    public void setName(String name)
    {

    }

    public String getName()
    {
        return null;
    }

    /** Callback from requesting info on the pipeline tasks and potential execution locations */
    public void setPipelineConfig(GWTPipelineConfig result)
    {
        for (GWTPipelineTask task : result.getTasks())
        {
            if ("tpp".equalsIgnoreCase(task.getGroupName()))
            {
                _visible = true;
                setVisibilityInParentTable();
                return;
            }
        }
    }

    @Override
    public void syncFormToXml(ParamParser params) throws SearchFormException
    {
        syncFormToXml(_peptideProphetTextBox, ParameterNames.MIN_PEPTIDE_PROPHET_PROBABILITY, params);
        syncFormToXml(_proteinProphetTextBox, ParameterNames.MIN_PROTEIN_PROPHET_PROBABILITY, params);
        syncFormToXml(_massToleranceTextBox, ParameterNames.QUANTITATION_MASS_TOLERANCE, params);
        syncFormToXml(_residueLabeLMassTextBox, ParameterNames.QUANTITATION_RESIDUE_LABEL_MASS, params);
        if (isLibra())
        {
            syncFormToXml(_libraConfigNameTextBox, ParameterNames.LIBRA_CONFIG_NAME_PARAM, params);
            params.setInputParameter(ParameterNames.LIBRA_NORMALIZATION_CHANNEL_PARAM, _libraNormalizationChannelListBox.getItemText(_libraNormalizationChannelListBox.getSelectedIndex()));
        }
        else
        {
            params.removeInputParameter(ParameterNames.LIBRA_CONFIG_NAME_PARAM);
            params.removeInputParameter(ParameterNames.LIBRA_NORMALIZATION_CHANNEL_PARAM);
        }

        if (_quantitationAlgorithmListBox.getSelectedIndex() == 0)
        {
            params.removeInputParameter(ParameterNames.QUANTITATION_ALGORITHM);
        }
        else
        {
            String selected = _quantitationAlgorithmListBox.getItemText(_quantitationAlgorithmListBox.getSelectedIndex()).toLowerCase();
            params.setInputParameter(ParameterNames.QUANTITATION_ALGORITHM, selected);
        }
    }

    private void syncFormToXml(TextBox textBox, String paramName, ParamParser params) throws SearchFormException
    {
        String value = textBox.getText().trim();
        if (value.isEmpty())
        {
            params.removeInputParameter(paramName);
        }
        else
        {
            params.setInputParameter(paramName, value);
        }
    }

    @Override
    public String syncXmlToForm(ParamParser params)
    {
        String minPeptide = params.getInputParameter(ParameterNames.MIN_PEPTIDE_PROPHET_PROBABILITY);
        _peptideProphetTextBox.setText(minPeptide == null ? "" : minPeptide);

        String minProtein = params.getInputParameter(ParameterNames.MIN_PROTEIN_PROPHET_PROBABILITY);
        _proteinProphetTextBox.setText(minProtein == null ? "" : minProtein);

        String massTolerance = params.getInputParameter(ParameterNames.QUANTITATION_MASS_TOLERANCE);
        _massToleranceTextBox.setText(massTolerance == null ? "" : massTolerance);

        String residueLabelMass = params.getInputParameter(ParameterNames.QUANTITATION_RESIDUE_LABEL_MASS);
        _residueLabeLMassTextBox.setText(residueLabelMass == null ? "" : residueLabelMass);

        String libraConfigName = params.getInputParameter(ParameterNames.LIBRA_CONFIG_NAME_PARAM);
        _libraConfigNameTextBox.setText(libraConfigName == null ? "" : libraConfigName);

        String libraNormalizationChannel = params.getInputParameter(ParameterNames.LIBRA_NORMALIZATION_CHANNEL_PARAM);
        if (libraNormalizationChannel != null && !libraNormalizationChannel.isEmpty())
        {
            try
            {
                int channel = Integer.parseInt(libraNormalizationChannel);
                // Drop-down is 1-16, to translate to index
                int index = channel - 1;
                if (index < 0 || index >= _libraNormalizationChannelListBox.getItemCount())
                {
                    return "Invalid Libra normalization channel: " + libraNormalizationChannel;
                }
                _libraNormalizationChannelListBox.setSelectedIndex(index);
            }
            catch (NumberFormatException e)
            {
                return "Invalid Libra normalization channel: " + libraNormalizationChannel;
            }
        }
        else
        {
            _libraNormalizationChannelListBox.setSelectedIndex(0);
        }

        String quantEngine = params.getInputParameter(ParameterNames.QUANTITATION_ALGORITHM);
        if (quantEngine != null && !quantEngine.isEmpty())
        {
            boolean found = false;
            for (int i = 0; i < _quantitationAlgorithmListBox.getItemCount(); i++)
            {
                if (_quantitationAlgorithmListBox.getItemText(i).equalsIgnoreCase(quantEngine))
                {
                    found = true;
                    _quantitationAlgorithmListBox.setSelectedIndex(i);
                }
            }
            if (!found)
            {
                return "Unknown quantitation engine: " + quantEngine;
            }
        }
        else
        {
            _quantitationAlgorithmListBox.setSelectedIndex(0);
        }
        setQuantitationVisibility();
        
        // Check the probability values
        return validate();
    }

    public void setReadOnly(boolean readOnly)
    {
        super.setReadOnly(readOnly);
        int row = 0;
        if (readOnly)
        {
            _instance.setText(0, 1, _peptideProphetTextBox.getText().trim().isEmpty() ? "<default>" : _peptideProphetTextBox.getText());
            _instance.setText(++row, 1, _proteinProphetTextBox.getText().trim().isEmpty() ? "<default>" : _proteinProphetTextBox.getText());
            _instance.setText(++row, 1, _quantitationAlgorithmListBox.getItemText(_quantitationAlgorithmListBox.getSelectedIndex()));
            _instance.setText(++row, 1, _massToleranceTextBox.getText().trim().isEmpty() ? "<default>" : _massToleranceTextBox.getText());
            _instance.setText(++row, 1, _residueLabeLMassTextBox.getText().trim().isEmpty() ? "" : _residueLabeLMassTextBox.getText());
            _instance.setText(++row, 1, _libraConfigNameTextBox.getText().trim().isEmpty() ? "" : _libraConfigNameTextBox.getText());
            _instance.setText(++row, 1, _libraNormalizationChannelListBox.getItemText(_libraNormalizationChannelListBox.getSelectedIndex()));
        }
        else
        {
            _instance.setWidget(row, 1, _peptideProphetTextBox);
            _instance.setWidget(++row, 1, _proteinProphetTextBox);
            _instance.setWidget(++row, 1, _quantitationAlgorithmListBox);
            _instance.setWidget(++row, 1, _massToleranceTextBox);
            _instance.setWidget(++row, 1, _residueLabeLMassTextBox);
            _instance.setWidget(++row, 1, _libraConfigNameTextBox);
            _instance.setWidget(++row, 1, _libraNormalizationChannelListBox);
            setQuantitationVisibility();
        }
    }

    public void addChangeListener(ChangeHandler handler)
    {
        _peptideProphetTextBox.addChangeHandler(handler);
        _proteinProphetTextBox.addChangeHandler(handler);
        _quantitationAlgorithmListBox.addChangeHandler(handler);
        _quantitationAlgorithmListBox.addChangeHandler(new ChangeHandler()
        {
            public void onChange(ChangeEvent event)
            {
                setQuantitationVisibility();
            }
        });
        _massToleranceTextBox.addChangeHandler(handler);
        _residueLabeLMassTextBox.addChangeHandler(handler);
        _libraConfigNameTextBox.addChangeHandler(handler);
        _libraNormalizationChannelListBox.addChangeHandler(handler);
    }

    @Override
    public void configureCompositeRow(FlexTable table, int row)
    {
        super.configureCompositeRow(table, row);
        setVisibilityInParentTable();
    }

    private void setVisibilityInParentTable()
    {
        _parentTable.getRowFormatter().setVisible(_parentTableRow, _visible);
    }

    @Override
    public Set<String> getHandledParameterNames()
    {
        return new HashSet<String>(Arrays.asList(
                ParameterNames.MIN_PROTEIN_PROPHET_PROBABILITY,
                ParameterNames.MIN_PEPTIDE_PROPHET_PROBABILITY,
                ParameterNames.QUANTITATION_ALGORITHM,
                ParameterNames.QUANTITATION_MASS_TOLERANCE,
                ParameterNames.QUANTITATION_RESIDUE_LABEL_MASS,
                ParameterNames.LIBRA_CONFIG_NAME_PARAM,
                ParameterNames.LIBRA_NORMALIZATION_CHANNEL_PARAM));
    }
}

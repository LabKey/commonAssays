/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.labkey.api.gwt.client.pipeline.GWTPipelineConfig;
import org.labkey.api.gwt.client.pipeline.GWTPipelineTask;
import org.labkey.api.gwt.client.ui.HelpPopup;

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

        HorizontalPanel minPepPropLabel = new HorizontalPanel();
        minPepPropLabel.add(new Label("Minimum PeptideProphet prob"));
        minPepPropLabel.add(new HelpPopup("Minimum PeptideProphet prob", "The minimum value for a peptide's probability, as determined by <a href=\"http://tools.proteomecenter.org/wiki/index.php?title=Software:PeptideProphet\" target=\"_blank\">PeptideProphet</a>, to be retained in the analysis results. Values should be between 0 and 1, inclusive."));
        _instance.setStylePrimaryName("lk-fields-table");
        _instance.setWidget(row, 0, minPepPropLabel);
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        _peptideProphetTextBox.setVisibleLength(4);
        _peptideProphetTextBox.setName("minPeptideProphetProb");
        _instance.setText(row, 1, "<default>");

        HorizontalPanel minProtPropLabel = new HorizontalPanel();
        minProtPropLabel.add(new Label("Minimum ProteinProphet prob"));
        minProtPropLabel.add(new HelpPopup("Minimum ProteinProphet prob", "The minimum value for a protein group's probability, as determined by <a href=\"http://tools.proteomecenter.org/wiki/index.php?title=Software:ProteinProphet\" target=\"_blank\">ProteinProphet</a>, to be retained in the analysis results. Values should be between 0 and 1, inclusive."));
        _instance.setWidget(++row, 0, minProtPropLabel);
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        _proteinProphetTextBox.setVisibleLength(4);
        _proteinProphetTextBox.setName("minProteinProphetProb");
        _instance.setText(row, 1, "<default>");

        HorizontalPanel quantEngineLabel = new HorizontalPanel();
        quantEngineLabel.add(new Label("Quantitation engine"));
        quantEngineLabel.add(new HelpPopup("Quantitation engine", "<p>The tool to use for performing quantitation on isotopically labelled samples.</p><p><a href=\"http://tools.proteomecenter.org/wiki/index.php?title=Software:XPRESS\" target=\"_blank\">XPRESS</a> and <a href=\"http://proteomics.fhcrc.org/CPL/msinspect/index.html\" target=\"_blank\">Q3</a> can be used for <a href=\"http://en.wikipedia.org/wiki/Isotope-coded_affinity_tag\" target=\"_blank\">Isotope-coded affinity tag</a> (ICAT) or <a href=\"http://en.wikipedia.org/wiki/SILAC\" target=\"_blank\">Stable isotope labeling by amino acids in cell culture</a> (SILAC) experiments.</p><p><a href=\"http://tools.proteomecenter.org/wiki/index.php?title=Software:Libra\" target=\"_blank\">Libra</a> analyzes <a href=\"http://en.wikipedia.org/wiki/Isobaric_tag_for_relative_and_absolute_quantitation\" target=\"_blank\">Isobaric tag for relative and absolute quantitation</a> (iTRAQ) samples.</p>"));
        _instance.setWidget(++row, 0, quantEngineLabel);
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        _quantitationAlgorithmListBox.addItem("<none>");
        _quantitationAlgorithmListBox.addItem("Libra");
        _quantitationAlgorithmListBox.addItem("Q3");
        _quantitationAlgorithmListBox.addItem("XPRESS");
        _quantitationAlgorithmListBox.setName("quantitationEngine");
        _instance.setText(row, 1, "<none>");

        HorizontalPanel quantResidueMasslLabel = new HorizontalPanel();
        quantResidueMasslLabel.add(new Label("Quantitation residue mass label"));
        quantResidueMasslLabel.add(new HelpPopup("Quantitation residue mass label", "The mass of the quantitation label modification for the each modified amino acid. The format is M1@X1,M2@X2,..., Mn@Xn\n" +
                "where Mi is a floating point number (modification mass in Daltons) and Xi is a single letter abbreviation for a type of amino acid residue. For example, '9.0@C'. See the <a href=\"http://thegpm.org/TANDEM/api/rmm.html\" target=\"_blank\">X!Tandem documentation</a> for more information."));
        _instance.setWidget(++row, 0, quantResidueMasslLabel);
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        _residueLabeLMassTextBox.setVisibleLength(20);
        _residueLabeLMassTextBox.setName("quantitationResidueLabel");
        _instance.setText(row, 1, "");
        _instance.getRowFormatter().setVisible(row, false);
        _residueLabelMassRow = row;

        HorizontalPanel quantMassTolLabel = new HorizontalPanel();
        quantMassTolLabel.add(new Label("Quantitation mass tolerance"));
        quantMassTolLabel.add(new HelpPopup("Quantitation mass tolerance", "The mass tolerance for XPRESS or Q3 quantitation. The default value is 1.0 daltons."));
        _instance.setWidget(++row, 0, quantMassTolLabel);
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        _massToleranceTextBox.setVisibleLength(4);
        _massToleranceTextBox.setName("quantitationMassTolerance");
        _instance.setText(row, 1, "<default>");
        _instance.getRowFormatter().setVisible(row, false);
        _massToleranceRow = row;

        HorizontalPanel libraConfigNameLabel = new HorizontalPanel();
        libraConfigNameLabel.add(new Label("Libra config name"));
        libraConfigNameLabel.add(new HelpPopup("Libra config name", "The name of the <a href=\"http://sashimi.svn.sourceforge.net/viewvc/sashimi/trunk/trans_proteomic_pipeline/src/Quantitation/Libra/docs/libra_info.html\" target=\"_blank\">Libra configuration file</a>. Must be available on server's file system in <File Root>/.labkey/protocols/libra/"));
        _instance.setWidget(++row, 0, libraConfigNameLabel);
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        _libraConfigNameTextBox.setVisibleLength(20);
        _libraConfigNameTextBox.setName("libraConfigName");
        _instance.setText(row, 1, "");
        _instance.getRowFormatter().setVisible(row, false);
        _libraConfigNameRow = row;

        HorizontalPanel libraChannelLabel = new HorizontalPanel();
        libraChannelLabel.add(new Label("Libra normalization channel"));
        libraChannelLabel.add(new HelpPopup("Libra normalization channel", "The Libra quantitation channel number to be used for normalization."));
        _instance.setWidget(++row, 0, libraChannelLabel);
        _instance.getCellFormatter().setStyleName(row, 0, "labkey-form-label-nowrap");
        for (int i = 1; i <= MAX_LIBRA_CHANNELS; i++)
        {
            _libraNormalizationChannelListBox.addItem(Integer.toString(i));
        }
        _libraNormalizationChannelListBox.setName("libraNormalizationChannel");
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
        Label label = new Label("Trans-Proteomic Pipeline");
        label.setStyleName(LABEL_STYLE_NAME);
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(label);
        panel.add(new HelpPopup("Trans-Proteomic Pipeline", "The <a href=\"http://tools.proteomecenter.org/wiki/index.php?title=Software:TPP\" target=\"_blank\">Trans-Proteomic Pipeline</a> (TPP) is a suite of analysis tools from the <a href=\"http://www.systemsbiology.org/\" target=\"_blank\">Institute for Systems Biology</a> (ISB). It provides key functionality for the MS2 analysis pipeline, including PeptideProphet, ProteinProphet, and quantitation tools."));
        return panel;
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
            if (!validateNumber(_massToleranceTextBox, 0, Double.MAX_VALUE, true))
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
            if ("tpp".equalsIgnoreCase(task.getGroupName()) || "tpp fractions".equalsIgnoreCase(task.getGroupName()))
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
            _instance.setText(++row, 1, _residueLabeLMassTextBox.getText().trim().isEmpty() ? "" : _residueLabeLMassTextBox.getText());
            _instance.setText(++row, 1, _massToleranceTextBox.getText().trim().isEmpty() ? "<default>" : _massToleranceTextBox.getText());
            _instance.setText(++row, 1, _libraConfigNameTextBox.getText().trim().isEmpty() ? "" : _libraConfigNameTextBox.getText());
            _instance.setText(++row, 1, _libraNormalizationChannelListBox.getItemText(_libraNormalizationChannelListBox.getSelectedIndex()));
        }
        else
        {
            _instance.setWidget(row, 1, _peptideProphetTextBox);
            _instance.setWidget(++row, 1, _proteinProphetTextBox);
            _instance.setWidget(++row, 1, _quantitationAlgorithmListBox);
            _instance.setWidget(++row, 1, _residueLabeLMassTextBox);
            _instance.setWidget(++row, 1, _massToleranceTextBox);
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

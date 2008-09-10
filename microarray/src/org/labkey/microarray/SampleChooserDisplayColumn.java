/*
 * Copyright (c) 2008 LabKey Corporation
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
package org.labkey.microarray;

import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.ProtocolParameter;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.view.GWTView;
import org.labkey.microarray.sampleset.client.SampleChooser;
import org.labkey.microarray.sampleset.client.SampleInfo;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.labkey.microarray.designer.client.MicroarrayAssayDesigner;

import java.io.Writer;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * User: jeckels
* Date: Sep 10, 2008
*/
class SampleChooserDisplayColumn extends SimpleDisplayColumn
{
    private final ExpProtocol _protocol;
    private final Integer _channelCount;
    private final String _barcode;

    public SampleChooserDisplayColumn(ExpProtocol protocol, Integer channelCount, String barcode)
    {
        _protocol = protocol;
        _channelCount = channelCount;
        _barcode = barcode;
        setCaption("Samples");
    }

    public boolean isEditable()
    {
        return true;
    }

    public void renderInputHtml(RenderContext ctx, Writer out, Object value) throws IOException
    {
        Map<String, String> props = new HashMap<String, String>();

        out.write("<input type=\"hidden\" name=\"" + SampleChooser.SAMPLE_COUNT_ELEMENT_NAME + "\" id=\"" + SampleChooser.SAMPLE_COUNT_ELEMENT_NAME + "\"/>\n");

        int maxCount = _channelCount == null ? MicroarrayAssayProvider.MAX_SAMPLE_COUNT : _channelCount.intValue();
        int minCount = _channelCount == null ? MicroarrayAssayProvider.MIN_SAMPLE_COUNT : _channelCount.intValue();

        for (int i = 0; i < maxCount; i++)
        {
            String lsidID = SampleInfo.getLsidFormElementID(i);
            String nameID = SampleInfo.getNameFormElementID(i);
            out.write("<input type=\"hidden\" name=\"" + lsidID + "\" id=\"" + lsidID + "\"/>\n");
            out.write("<input type=\"hidden\" name=\"" + nameID + "\" id=\"" + nameID + "\"/>\n");
        }

        props.put(SampleChooser.PROP_NAME_MAX_SAMPLE_COUNT, Integer.toString(maxCount));
        props.put(SampleChooser.PROP_NAME_MIN_SAMPLE_COUNT, Integer.toString(minCount));
        ExpSampleSet sampleSet = ExperimentService.get().lookupActiveSampleSet(ctx.getContainer());
        ProtocolParameter selectedSampleSetLSIDParam = _protocol.getProtocolParameters().get(MicroarrayAssayDesigner.SAMPLE_SET_LSID_PARAMETER_URI);
        if (selectedSampleSetLSIDParam != null)
        {
            sampleSet = ExperimentService.get().getSampleSet(selectedSampleSetLSIDParam.getStringValue());
        }

        if (sampleSet != null)
        {
            ProtocolParameter barcodeColumnNameParam = _protocol.getProtocolParameters().get(MicroarrayAssayDesigner.SAMPLE_BARCODE_COLUMN_NAME_PARAMETER_URI);
            String barcodeColumnName = "Barcode";
            if (barcodeColumnNameParam != null)
            {
                barcodeColumnName = barcodeColumnNameParam.getStringValue();
            }
            if (_barcode != null)
            {
                List<ExpMaterial> matchingMaterials = new ArrayList<ExpMaterial>();
                DomainProperty barcodeProperty = sampleSet.getType().getPropertyByName(barcodeColumnName);
                if (barcodeProperty != null)
                {
                    for (ExpMaterial material : sampleSet.getSamples())
                    {
                        Object barcodeValue = material.getProperty(barcodeProperty);
                        if (barcodeValue instanceof String && _barcode.equalsIgnoreCase((String)barcodeValue))
                        {
                            matchingMaterials.add(material);
                        }
                    }
                }

                if (matchingMaterials.size() <= maxCount)
                {
                    for (int i = 0; i < matchingMaterials.size(); i++)
                    {
                        ExpMaterial material = matchingMaterials.get(i);
                        props.put(SampleChooser.PROP_PREFIX_SELECTED_SAMPLE_LSID + i, material.getLSID());
                    }
                }
            }

            props.put(SampleChooser.PROP_NAME_DEFAULT_SAMPLE_SET_LSID, sampleSet.getLSID());
            props.put(SampleChooser.PROP_NAME_DEFAULT_SAMPLE_SET_NAME, sampleSet.getName());
            props.put(SampleChooser.PROP_NAME_DEFAULT_SAMPLE_SET_ROW_ID, Integer.toString(sampleSet.getRowId()));
        }
        GWTView view = new GWTView(SampleChooser.class, props);
        try
        {
            view.render(ctx.getRequest(), ctx.getViewContext().getResponse());
        }
        catch (Exception e)
        {
            throw (IOException)new IOException().initCause(e);
        }
    }
}
/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.ms2;

import org.labkey.common.tools.PeptideProphetSummary;
import org.labkey.common.tools.SimpleXMLStreamReader;
import org.labkey.common.tools.SensitivitySummary;

import javax.xml.stream.XMLStreamException;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class ProteinProphetFile extends SensitivitySummary
{
    private int _rowId;
    private String _filePath;
    private boolean _uploadCompleted = false;
    private int[] _predictedNumberCorrect = new int[0];
    private int[] _predictedNumberIncorrect = new int[0];

    public ProteinProphetFile()
    {
    }

    public ProteinProphetFile(SimpleXMLStreamReader parser) throws XMLStreamException
    {
        List<Float> minimumProbabilities = new ArrayList<Float>();
        List<Float> sensitivities = new ArrayList<Float>();
        List<Float> falsePositiveErrorRate = new ArrayList<Float>();
        List<Integer> predictedNumberCorrect = new ArrayList<Integer>();
        List<Integer> predictedNumberIncorrect = new ArrayList<Integer>();

        while (parser.hasNext() && !(parser.isEndElement() && "proteinprophet_details".equals(parser.getLocalName())))
        {
            parser.next();

            if (parser.isStartElement() && "protein_summary_data_filter".equals(parser.getLocalName()))
            {
                minimumProbabilities.add(new Float(parser.getAttributeValue(null, "min_probability")));
                sensitivities.add(new Float(parser.getAttributeValue(null, "sensitivity")));
                falsePositiveErrorRate.add(new Float(parser.getAttributeValue(null, "false_positive_error_rate")));
                predictedNumberCorrect.add(new Integer(parser.getAttributeValue(null, "predicted_num_correct")));
                predictedNumberIncorrect.add(new Integer(parser.getAttributeValue(null, "predicted_num_incorrect")));
            }
        }
        _minProb = toFloatArray(minimumProbabilities);
        _sensitivity = toFloatArray(sensitivities);
        _error = toFloatArray(falsePositiveErrorRate);
        _predictedNumberCorrect = toIntArray(predictedNumberCorrect);
        _predictedNumberIncorrect = toIntArray(predictedNumberIncorrect);
    }

    public float[] toFloatArray(List<Float> floats)
    {
        float[] result = new float[floats.size()];
        for (int i = 0; i < floats.size(); i++)
        {
            result[i] = floats.get(i).floatValue();
        }
        return result;
    }

    public int[] toIntArray(List<Integer> ints)
    {
        int[] result = new int[ints.size()];
        for (int i = 0; i < ints.size(); i++)
        {
            result[i] = ints.get(i).intValue();
        }
        return result;
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public String getFilePath()
    {
        return _filePath;
    }

    public void setFilePath(String filePath)
    {
        _filePath = filePath;
    }

    public boolean isUploadCompleted()
    {
        return _uploadCompleted;
    }

    public void setUploadCompleted(boolean uploadCompleted)
    {
        _uploadCompleted = uploadCompleted;
    }

    public byte[] getPredictedNumberCorrect()
    {
        return PeptideProphetSummary.toByteArray(_predictedNumberCorrect);
    }

    public byte[] getPredictedNumberIncorrect()
    {
        return PeptideProphetSummary.toByteArray(_predictedNumberIncorrect);
    }

    public void setPredictedNumberCorrect(byte[] b)
    {
        _predictedNumberCorrect = PeptideProphetSummary.toIntArray(b);
    }

    public void setPredictedNumberIncorrect(byte[] b)
    {
        _predictedNumberIncorrect = PeptideProphetSummary.toIntArray(b);
    }

    public ProteinGroupWithQuantitation lookupGroup(int groupNumber, int indistinguishableCollectionId) throws SQLException
    {
        return MS2Manager.getProteinGroup(_rowId, groupNumber, indistinguishableCollectionId);
    }
}

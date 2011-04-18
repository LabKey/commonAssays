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

package org.labkey.flow.analysis.web;

import org.labkey.flow.analysis.model.*;

import java.util.*;
import java.net.URI;
import java.io.IOException;

public class CompHandler
{
    class FileData
    {
        public FileData(FCSKeywordData header) throws IOException
        {
            this.header = header;
            this.subsetMap = new HashMap();
            FCS fcs = _analyzer.readFCS(header.getURI());
            this.subsetMap.put(null, new Subset(fcs, _calc.getSettings()));
        }
        final FCSKeywordData header;
        Map<SubsetSpec,Subset> subsetMap;
    }

    CompensationCalculation _calc;
    CompensationMatrix _comp;
    List<FCSKeywordData> _headers;
    Map<URI, FileData> _fileMap;
    Map<ChannelKey, ChannelData> _channelList;
    FCSAnalyzer _analyzer;

    public CompHandler(FCSAnalyzer analyzer, CompensationCalculation calc, List<FCSKeywordData> headers)
    {
        _calc = calc;
        _fileMap = new HashMap();
        _channelList = new LinkedHashMap();
        _headers = headers;
        _analyzer = analyzer;
    }

    private ChannelData addChannel(CompSign sign, int channel, FCSKeywordData header) throws IOException
    {
        ChannelData data = new ChannelData(this, sign, channel);
        FileData file = _fileMap.get(header.getURI());
        if (file == null)
        {
            file = new FileData(header);
            _fileMap.put(header.getURI(), file);
        }
        data.setFile(file);
        _channelList.put(data._key, data);
        return data;
    }

    public void determineCompWells() throws IOException
    {
        int channelCount = _calc.getChannels().size();
        for (int i = 0; i < channelCount; i ++)
        {
            CompensationCalculation.ChannelInfo info = _calc.getChannels().get(i);
            FCSKeywordData positiveHeader = _analyzer.findHeader(_headers, info.getPositive().getCriteria());
            addChannel(CompSign.positive, i, positiveHeader);

            FCSKeywordData negativeHeader = _analyzer.findHeader(_headers, info.getNegative().getCriteria());
            addChannel(CompSign.negative, i, negativeHeader);
        }
    }

    public void calculateValues()
    {
        for (ChannelData cd : _channelList.values())
        {
            cd.calculateUncompensatedValues();
        }
    }

    public CompensationMatrix calculateCompensationMatrix(List<CompensationResult> results) throws IOException
    {
        determineCompWells();
        calculateValues();
        int channelCount = _calc.getChannelCount();
        _comp = new CompensationMatrix("comp");
        for (int iChannel = 0; iChannel < channelCount; iChannel ++)
        {
            String channelName = _calc.getChannelInfo(iChannel).getName();
            ChannelData cdPositive = _channelList.get(new ChannelKey(CompSign.positive, channelName));
            ChannelData cdNegative = _channelList.get(new ChannelKey(CompSign.negative, channelName));

            double[] positives = new double[channelCount];
            double[] negatives = new double[channelCount];
            double[] differences = new double[channelCount];

            for (int iChannelValue = 0; iChannelValue < _calc.getChannels().size(); iChannelValue ++)
            {
                positives[iChannelValue] = cdPositive._medians[iChannelValue];
                negatives[iChannelValue] = cdNegative._medians[iChannelValue];
                differences[iChannelValue] = positives[iChannelValue] - negatives[iChannelValue];
            }
            Map<String, Double> values = new LinkedHashMap();
            for (int iChannelValue = 0; iChannelValue < channelCount; iChannelValue ++)
            {
                String channel = _calc.getChannelName(iChannelValue);
                values.put(channel, differences[iChannelValue] / differences[iChannel]);
            }
            _comp.setChannel(_calc.getChannelName(iChannel), values);
        }
        calculateCompensatedValues();
        for (ChannelData cd : _channelList.values())
        {
            results.add(cd.getCompensationResult());
        }
        return _comp;
    }

    public void calculateCompensatedValues()
    {
        for (ChannelData cd : _channelList.values())
        {
            cd.calculateCompensatedValues(_comp);
        }
    }
}

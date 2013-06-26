/*
 * Copyright (c) 2005-2013 LabKey Corporation
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

import java.net.URI;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Iterator;

/**
 */
public class FCSViewer
    {
    FCS _fcs;
    Subset _subset;
    public FCSViewer(URI uriFCSFile) throws IOException
        {
        _fcs = FCSAnalyzer.get().readFCS(uriFCSFile);
        _subset = new Subset(_fcs, new ScriptSettings());
        }
    public void applyCompensationMatrix(URI uriCompensationMatrix) throws Exception
        {
        CompensationMatrix comp = new CompensationMatrix(new File(uriCompensationMatrix));
        DataFrame data = comp.getCompensatedData(_subset.getDataFrame(), false);
        _subset = new Subset(_subset.getParent(), _subset.getName(), _subset.getFCSHeader(), data);
        }
    public void writeValues(Writer writer) throws IOException
        {
        DataFrame data = _subset.getDataFrame();
        for (int i = 0; i < data.getColCount(); i ++)
            {
            if (i != 0)
                {
                writer.write("\t");
                }
            writer.write(data.getField(i).getName());
            }
        writer.write("\n");
        for (int row = 0; row < data.getRowCount(); row ++)
            {
            for (int col = 0; col < data.getColCount(); col ++)
                {
                if (col != 0)
                    writer.write("\t");
                writer.write(data.getColumn(col).get(row).toString());
                }
            writer.write("\n");
            }
        }
    public void writeKeywords(Writer writer) throws IOException
        {
        FCS fcs = _fcs;
        for (Iterator it = fcs.getKeywords().entrySet().iterator();it.hasNext();)
            {
            Map.Entry entry = (Map.Entry) it.next();
            writer.write((String)entry.getKey());
            writer.write("=");
            if (entry.getValue() != null)
                writer.write((String)entry.getValue());
            writer.write("\n");
            }
        }
    }

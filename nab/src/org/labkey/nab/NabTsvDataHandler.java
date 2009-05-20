/*
 * Copyright (c) 2009 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.nab;

import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.TabLoader;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: May 18, 2009
 */
public class NabTsvDataHandler extends AbstractNabDataHandler
{
    public static final DataType NAB_TSV_DATA_TYPE = new DataType("AssayRunNabTsvData");

    static class NabTsvParser implements NabDataFileParser
    {
        private File _dataFile;
        private List<Map<String, Object>> _results = new ArrayList<Map<String, Object>>();
        private boolean _fileParsed;

        public NabTsvParser(File dataFile)
        {
            _dataFile = dataFile;
        }

        public List<Map<String, Object>> getResults() throws ExperimentException
        {
            if (!_fileParsed)
            {
                assert(_dataFile.getName().toLowerCase().endsWith(".tsv"));
                DataLoader<Map<String, Object>> loader = null;

                try {
                    loader = new TabLoader(_dataFile, true);
                    _results = loader.load();
                }
                catch (IOException ioe)
                {
                    throw new ExperimentException(ioe);
                }
                finally
                {
                    if (loader != null)
                        loader.close();
                }
            }
            return _results;
        }
    }

    public NabDataFileParser getDataFileParser(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        return new NabTsvParser(dataFile);
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (NAB_TSV_DATA_TYPE.matches(lsid))
        {
            return Priority.HIGH;
        }
        return null;
    }
}

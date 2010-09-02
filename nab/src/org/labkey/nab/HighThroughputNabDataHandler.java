package org.labkey.nab;

import org.apache.log4j.Logger;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.security.User;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.Plate;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.util.FileType;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2010 LabKey Corporation
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * <p/>
 * User: brittp
 * Date: Aug 27, 2010 11:07:33 AM
 */
public class HighThroughputNabDataHandler extends AbstractNabDataHandler
{
    public static final AssayDataType NAB_HIGH_THROUGHPUT_DATA_TYPE = new AssayDataType("HighThroughputAssayRunNabData", new FileType(".csv"));

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (NAB_HIGH_THROUGHPUT_DATA_TYPE.matches(lsid))
        {
            return Priority.HIGH;
        }
        return null;
    }

    private static class HighThroughputNabDataParser implements NabDataFileParser
    {
        @Override
        public List<Map<String, Object>> getResults() throws ExperimentException
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    @Override
    protected NabAssayRun createNabAssayRun(NabAssayProvider provider, ExpRun run, Plate plate, User user, List<Integer> cutoffs, DilutionCurve.FitType renderCurveFitType)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NabDataFileParser getDataFileParser(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

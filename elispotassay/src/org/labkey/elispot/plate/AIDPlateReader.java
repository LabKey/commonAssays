/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
package org.labkey.elispot.plate;

import org.labkey.api.exp.ExperimentException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.DataLoaderFactory;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.assay.plate.PlateUtils;
import org.labkey.api.study.assay.plate.TextPlateReader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by klum on 12/14/14.
 */
public class AIDPlateReader extends TextPlateReader
{
    public static final String TYPE = "aid_txt";

    public String getType()
    {
        return TYPE;
    }

    @Override
    public double convertWellValue(String token) throws ValidationException
    {
        if ("TNTC".equalsIgnoreCase(token))
        {
            return WELL_NOT_COUNTED;
        }
        else if ("--".equalsIgnoreCase(token))
        {
            return WELL_OFF_SCALE;
        }
        return super.convertWellValue(token);
    }

    @Override
    public double[][] loadFile(PlateTemplate template, File dataFile) throws ExperimentException
    {
        String fileName = dataFile.getName().toLowerCase();
        if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx"))
        {
            try
            {
                DataLoaderFactory factory = DataLoader.get().findFactory(dataFile, null);
                DataLoader loader = factory.createLoader(dataFile, false);

                return PlateUtils.parseGrid(dataFile, loader.load(), template.getRows(), template.getColumns(), this);
            }
            catch (IOException ioe)
            {
                throw new ExperimentException(ioe);
            }
        }
        else
        {
            return super.loadFile(template, dataFile);
        }
    }

    @Override
    public Map<String, double[][]> loadMultiGridFile(PlateTemplate template, File dataFile) throws ExperimentException
    {
        String fileName = dataFile.getName().toLowerCase();
        if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx"))
        {
            try
            {
                DataLoaderFactory factory = DataLoader.get().findFactory(dataFile, null);
                DataLoader loader = factory.createLoader(dataFile, false);

                return PlateUtils.parseAllGrids(dataFile, loader.load(), template.getRows(), template.getColumns(), this);
            }
            catch (IOException ioe)
            {
                throw new ExperimentException(ioe);
            }
        }
        else
        {
            // TODO: may need to implement this for tsv's since this would be the format coming back through a transform
            throw new UnsupportedOperationException("multiple grid parsing is not supported for non-excel files");
        }
    }
}

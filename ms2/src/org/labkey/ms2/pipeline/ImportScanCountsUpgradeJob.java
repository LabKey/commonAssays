/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
package org.labkey.ms2.pipeline;

import org.labkey.api.data.CompareType;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.MS2Fraction;
import org.labkey.ms2.MS2Importer;
import org.labkey.ms2.MS2Manager;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;

/**
 * User: jeckels
 * Date: 7/1/11
 */
public class ImportScanCountsUpgradeJob extends PipelineJob implements Serializable
{
    private static final String UPGRADE_RUN = "Attempting to import scan counts for MS2 fraction #%s %s";
    private static final String UPGRADE_EXCEPTION = "An unexpected error occurred attempting to import scan counts for the MS2 fraction: %s, skipping.";
    private static final String UPGRADE_STATS = "Upgrade job complete. Number of fractions checked: %s. Number of records update: %s.";

    public ImportScanCountsUpgradeJob(ViewBackgroundInfo info, PipeRoot root) throws IOException
    {
        super(null, info, root);

        File logFile = File.createTempFile("importScanCounts", ".log", root.ensureSystemDirectory());
        setLogFile(logFile);
    }

    @Override
    public URLHelper getStatusHref()
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return "Load MS2 scan counts for existing data";
    }

    @Override
    public void run()
    {
        setStatus("IMPORTING", "Job started at: " + DateUtil.nowISO());

        int count = 0;
        int imported = 0;

        try
        {
            SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("ScanCount"), null, CompareType.ISBLANK);
            filter.addCondition(FieldKey.fromParts("MzXmlURL"), null, CompareType.NONBLANK);
            MS2Fraction[] fractions = new TableSelector(MS2Manager.getTableInfoFractions(), filter, new Sort("Fraction")).getArray(MS2Fraction.class);
            info("Found " + fractions.length + " fractions to process");

            for (MS2Fraction fraction : fractions)
            {
                info(String.format(UPGRADE_RUN, count++, fraction.getMzXmlURL()));
                try
                {
                    File f = new File(new URI(fraction.getMzXmlURL()));
                    MS2Importer.loadScanCounts(f, fraction);
                    if (fraction.getScanCount() != null)
                    {
                        Table.update(null, MS2Manager.getTableInfoFractions(), fraction, fraction.getFraction());
                        imported++;
                    }
                    else
                    {
                        info("Unable to load scan counts for " + fraction.getMzXmlURL() + ", file may have been deleted");
                    }
                }
                catch (Exception e)
                {
                    error(String.format(UPGRADE_EXCEPTION, fraction.getMzXmlURL()), e);
                }
            }
        }
        catch (Exception e)
        {
            error("Import failure", e);
            setStatus(TaskStatus.error, "Job finished at: " + DateUtil.nowISO());
        }
        finally
        {
            info(String.format(UPGRADE_STATS, count, imported));
            setStatus(TaskStatus.complete, "Job finished at: " + DateUtil.nowISO());
        }
    }
}

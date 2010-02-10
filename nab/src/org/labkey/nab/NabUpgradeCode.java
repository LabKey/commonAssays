/*
 * Copyright (c) 2009 LabKey Corporation
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

package org.labkey.nab;

import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Aug 31, 2009
 */
public class NabUpgradeCode implements UpgradeCode
{
    public static class NabAUCUpgradeJob extends PipelineJob implements Serializable
    {
        public static final String PROCESSING_STATUS = "Processing";
        public static final String LOG_FILE_NAME = "upgradeAUC.log";

        private static final String UPGRADE_NAB_RUN = "Checking NAb run: %s/%s for AUC columns to be inserted";
        private static final String UPGRADE_NAB_EXCEPTION = "An unexpected error occurred attempting to upgrade the NAb run: %s, skipping.";
        private static final String UPGRADE_NAB_STATS = "Upgrade job complete. Number of assay instances checked: %s. Number of assay runs checked: %s.";

        private UpgradeType _type;

        public enum UpgradeType {
            AUC,
            pAUC,
        }

        public NabAUCUpgradeJob(String provider, ViewBackgroundInfo info, UpgradeType type) throws IOException
        {
            super(provider, info);
            _type = type;
            init();
        }

        private void init() throws IOException
        {
            PipeRoot root = PipelineService.get().findPipelineRoot(getInfo().getContainer());
            if (root == null)
            {
                throw new FileNotFoundException("Could not find pipeline root on disk: " + (root == null ? null : root.getRootPath()));
            }
            File logFile = File.createTempFile("upgradeAUC", ".log", root.getRootPath());
            setLogFile(logFile);
        }

        public ActionURL getStatusHref()
        {
            return null;
        }

        public String getDescription()
        {
            return "NAb AUC upgrade job";
        }

        @Override
        public void run()
        {
            setStatus(PROCESSING_STATUS, "Job started at: " + DateUtil.nowISO());
            int assayInstances = 0;
            int runsProcessed = 0;

            try {
                ViewContext context = new ViewContext(getInfo());

                // get all the nab instances
                for (ExpProtocol protocol : ExperimentService.get().getAllExpProtocols())
                {
                    AssayProvider provider = AssayService.get().getProvider(protocol);
                    if (provider instanceof NabAssayProvider)
                    {
                        assayInstances++;
                        for (ExpRun run : protocol.getExpRuns())
                        {
                            try {
                                runsProcessed++;
                                info(String.format(UPGRADE_NAB_RUN, run.getContainer().getPath(), run.getName()));
                                NabAssayRun nab = NabDataHandler.getAssayResults(run, context.getUser());

                                switch (_type)
                                {
                                    case AUC:
                                        nab.upgradeAUCValues(this);
                                        break;
                                    case pAUC:
                                        nab.upgradePositiveAUCValues(this);
                                        break;
                                }
                            }
                            catch (Exception e)
                            {
                                error(String.format(UPGRADE_NAB_EXCEPTION, run.getName()), e);
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                error("Error occurred running the NAb background job", e);
                setStatus(PipelineJob.ERROR_STATUS, "Job finished at: " + DateUtil.nowISO());
            }
            finally
            {
                info(String.format(UPGRADE_NAB_STATS, assayInstances, runsProcessed));
                setStatus(PipelineJob.COMPLETE_STATUS, "Job finished at: " + DateUtil.nowISO());
            }
        }
    }
}

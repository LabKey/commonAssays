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
package org.labkey.ms2;

import org.labkey.api.data.UpgradeCode;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.exp.ExperimentException;
import org.apache.log4j.Logger;

/**
 * User: jeckels
 * Date: Mar 10, 2009
 */
public class MS2UpgradeCode implements UpgradeCode
{
    private static final Logger _log = Logger.getLogger(MS2UpgradeCode.class);

    /** Wrap any plain MS2 runs with an experiment run. Run as part of 8.34-8.35 upgrade */
    public void wrapRuns(ModuleContext context)
    {
        if (!context.isNewInstall())
        {
            int attemptCount = 0;
            int successCount = 0;
            MS2Run[] runs = MS2Manager.getUnwrappedRuns();
            for (MS2Run run : runs)
            {
                if (++attemptCount % 500 == 0)
                {
                    _log.info("Wrapping MS2 run " + attemptCount + " of " + runs.length);
                }
                try
                {
                    MS2Manager.ensureWrapped(run, context.getUpgradeUser());
                    successCount++;
                }
                catch (ExperimentException e)
                {
                    _log.error("Failed to wrap MS2 run " + run.getRun(), e);
                }
                catch (Exception e)
                {
                    _log.error("Failed to wrap MS2 run " + run.getRun(), e);
                }
            }
            _log.info("Successfully wrapped " + successCount + " of " + attemptCount + " attempted MS2 runs");
        }
    }
}

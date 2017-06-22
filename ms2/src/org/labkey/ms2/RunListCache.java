/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

import org.labkey.api.view.ViewContext;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpExperiment;

import javax.servlet.ServletException;
import java.util.*;

/**
 * User: jeckels
 * Date: Jan 23, 2008
 */
public class RunListCache
{
    private static final String NO_RUNS_MESSAGE = "Run list is empty. Please reselect the runs.";

    public static List<MS2Run> getCachedRuns(int index, boolean requireSameType, ViewContext ctx) throws RunListException
    {
        ExpExperiment group = ExperimentService.get().getExpExperiment(index);
        if (group == null || !group.getContainer().equals(ctx.getContainer()))
        {
            throw new RunListException(NO_RUNS_MESSAGE);
        }
        List<MS2Run> ms2Runs = new ArrayList<>();
        for (ExpRun expRun : group.getRuns())
        {
            ms2Runs.add(MS2Manager.getRunByExperimentRunLSID(expRun.getLSID()));
        }

        MS2Manager.validateRuns(ms2Runs, requireSameType, ctx.getUser());
        return ms2Runs;
    }

    // We store just the list of run IDs, not the runs themselves. Even though we're
    // just storing the list, we do all error & security checks upfront to alert the user early.
    public static int cacheSelectedRuns(boolean requireSameType, MS2Controller.RunListForm form, ViewContext ctx) throws ServletException, RunListException
    {
        String selectionKey = ctx.getRequest().getParameter(DataRegionSelection.DATA_REGION_SELECTION_KEY);
        if (selectionKey == null)
        {
            throw new RunListException(NO_RUNS_MESSAGE);
        }
        
        Set<String> stringIds = DataRegionSelection.getSelected(ctx, selectionKey, true, true);

        if (stringIds.isEmpty())
        {
            throw new RunListException(NO_RUNS_MESSAGE);
        }

        List<String> parseErrors = new ArrayList<>();

        List<ExpRun> expRuns = new ArrayList<>();
        List<MS2Run> ms2Runs = new ArrayList<>();

        for (String stringId : stringIds)
        {
            try
            {
                int id = Integer.parseInt(stringId);
                if (form.isExperimentRunIds())
                {
                    ExpRun expRun = ExperimentService.get().getExpRun(id);
                    if (expRun != null)
                    {
                        expRuns.add(expRun);
                        MS2Run ms2Run = MS2Manager.getRunByExperimentRunLSID(expRun.getLSID());
                        if (ms2Run == null)
                        {
                            parseErrors.add("Could not find MS2 run for run LSID: " + expRun.getLSID());
                        }
                    }
                    else
                    {
                        parseErrors.add("Could not find experiment run with RowId " + id);
                    }
                }
                else
                {
                    MS2Run ms2Run = MS2Manager.getRun(id);
                    if (ms2Run == null)
                    {
                        parseErrors.add("Could not find MS2 run with id " + id);
                    }
                    else
                    {
                        ms2Runs.add(ms2Run);
                        ExpRun expRun = ExperimentService.get().getExpRun(ms2Run.getExperimentRunLSID());
                        if (expRun == null)
                        {
                            parseErrors.add("Could not find experiment run with LSID " + ms2Run.getExperimentRunLSID());
                        }
                        else
                        {
                            expRuns.add(expRun);
                        }
                    }
                }
            }
            catch (NumberFormatException e)
            {
                parseErrors.add("Run " + stringId + ": Number format error");
            }
        }
        if (!parseErrors.isEmpty())
        {
            throw new RunListException(parseErrors);
        }

        MS2Manager.validateRuns(ms2Runs, requireSameType, ctx.getUser());

        ExpExperiment group = ExperimentService.get().createHiddenRunGroup(ctx.getContainer(), ctx.getUser(), expRuns.toArray(new ExpRun[expRuns.size()]));

        return group.getRowId();
    }
}

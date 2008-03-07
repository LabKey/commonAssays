package org.labkey.ms2;

import org.labkey.api.view.ViewContext;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;

import javax.servlet.ServletException;
import java.util.*;

/**
 * User: jeckels
 * Date: Jan 23, 2008
 */
public class RunListCache
{

    private static final String NO_RUNS_MESSAGE = "Run list is empty; session may have timed out.  Please reselect the runs.";
    private static final String RUN_LIST_CACHE_KEY = MS2Controller.class.getName() + ":RunListCache";

    private static Map<Integer, List<Integer>> getRunListCache(ViewContext context)
    {
        Map<Integer, List<Integer>> cache = (Map<Integer, List<Integer>>)context.getRequest().getSession(true).getAttribute(RUN_LIST_CACHE_KEY);
        if (cache == null)
        {
            cache = new HashMap<Integer, List<Integer>>();
            context.getRequest().getSession(true).setAttribute(RUN_LIST_CACHE_KEY, cache);
        }
        return cache;
    }

    public static List<MS2Run> getCachedRuns(int index, boolean requireSameType, ViewContext ctx) throws RunListException, ServletException
    {
        List<Integer> runIds = getRunListCache(ctx).get(index);

        if (null == runIds)
        {
            throw new RunListException(NO_RUNS_MESSAGE);
        }

        return MS2Manager.lookupRuns(runIds, requireSameType, ctx.getUser());
    }

    private static int RUN_LIST_ID = 0;

    // We cache just the list of run IDs, not the runs themselves.  This keeps things small and eases mem tracking.  Even though we're
    // just caching the list, we do all error & security checks upfront to alert the user early.
    public static int cacheSelectedRuns(boolean requireSameType, MS2Controller.RunListForm form, ViewContext ctx) throws ServletException, RunListException
    {
        Set<String> stringIds = DataRegionSelection.getSelected(ctx, true);

        if (null == stringIds || stringIds.isEmpty())
        {
            throw new RunListException(NO_RUNS_MESSAGE);
        }

        List<Integer> runIds = new ArrayList<Integer>(stringIds.size());

        List<String> parseErrors = new ArrayList<String>();
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
                        MS2Run run = MS2Manager.getRunByExperimentRunLSID(expRun.getLSID());
                        if (run == null)
                        {
                            parseErrors.add("Could not find run with id " + id);
                        }
                        else
                        {
                            runIds.add(run.getRun());
                        }
                    }
                    else
                    {
                        parseErrors.add("Could not find experiment run with RowId " + id);
                    }
                }
                else
                {
                    runIds.add(id);
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

        MS2Manager.lookupRuns(runIds, requireSameType, ctx.getUser());

        int index;
        synchronized(RunListCache.class)
        {
            index = RUN_LIST_ID++;
        }

        getRunListCache(ctx).put(index, runIds);
        return index;
    }




}

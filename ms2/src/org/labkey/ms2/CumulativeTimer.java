/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.labkey.api.util.Formats;

/**
* User: adam
* Date: Mar 6, 2008
* Time: 2:40:36 PM
*/

// Tracks cumulative time for a set of sequential tasks.  Each task invocation logs elapsed time for previous
// task and starting information for current task.  Summary logs cumulative time for each task.
// NOTE: This approach assumes that only one task is active at a time (no parallel tasks)
public class CumulativeTimer
{
    // Map of TimerTask -> Long (cumulative task time)
    private final OrderedMap<TimerTask, Long> _cumulativeTime = new ListOrderedMap<>();
    private Task _currentTask = null;
    private Logger _log;

    public CumulativeTimer(Logger log)
    {
        _log = log;
    }

    public void setCurrentTask(TimerTask tt, String extraDescription)
    {
        endCurrentTask();
        _currentTask = new Task(tt, extraDescription);
        _currentTask.start();
    }

    public void setCurrentTask(TimerTask tt)
    {
        setCurrentTask(tt, null);
    }

    public void endCurrentTask()
    {
        if (null != _currentTask)
            _currentTask.end();

        _currentTask = null;
    }

    public boolean hasTask(TimerTask tt)
    {
        return null != _cumulativeTime.get(tt);
    }

    public void logSummary(String description)
    {
        endCurrentTask();
        long totalTime = 0;

        _log.debug("========================================");
        _log.debug("Summary of all timed tasks:");
        _log.debug("");

        for (Object key : _cumulativeTime.keySet())
        {
            TimerTask tt = (TimerTask)key;
            long time = (_cumulativeTime.get(tt)).longValue();
            logElapsedTime(time, tt.getAction());
            totalTime += time;
        }

        _log.debug("");
        logElapsedTime(totalTime, description);
        _log.debug("========================================");
    }

    protected void logElapsedTime(long elapsedTimeNano, String action)
    {
        double seconds = (double)elapsedTimeNano / 1000000000;
        double minutes = seconds / 60;

        _log.debug(Formats.f2.format(seconds) + " seconds " + ((minutes > 1) ? ("(" + Formats.f2.format(seconds / 60) + " minutes) ") : "") + "to " + action);
    }

    private class Task
    {
        private TimerTask _tt;
        private long _startTime;
        private String _extraDescription;

        private Task(TimerTask tt, String extraDescription)
        {
            _tt = tt;
            _extraDescription = extraDescription;
        }

        private void start()
        {
            _startTime = System.nanoTime();
            _log.info("Starting to " + getDescription());
        }

        private void end()
        {
            long elapsed = System.nanoTime() - _startTime;

            synchronized(_cumulativeTime)
            {
                Long cumulative = _cumulativeTime.get(_tt);

                _cumulativeTime.put(_tt, (null == cumulative ? elapsed : cumulative.longValue() + elapsed));
            }

            logElapsedTime(elapsed, getDescription());
        }

        private String getDescription()
        {
            return _tt.getAction() + (null != _extraDescription ? " " + _extraDescription : "");
        }
    }

    public interface TimerTask
    {
        String getAction();
    }
}

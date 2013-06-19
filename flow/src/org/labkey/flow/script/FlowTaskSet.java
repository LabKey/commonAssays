/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.flow.script;

import java.util.LinkedList;
import java.util.Arrays;

public class FlowTaskSet
{
    private LinkedList<Runnable> _pendingTasks;
    private LinkedList<Runnable> _runningTasks;

    public FlowTaskSet(Runnable[] tasks)
    {
        _pendingTasks = new LinkedList<>(Arrays.asList(tasks));
        _runningTasks = new LinkedList<>();
    }

    public boolean runNextTask()
    {
        Runnable task = nextTask();
        if (task == null)
            return false;
        try
        {
            task.run();
        }
        finally
        {
            taskCompleted(task);
        }
        return true;
    }

    public void runAllTasks()
    {
        while (runNextTask())
        {
            // Nothing
        }
        synchronized(this)
        {
            while (!_pendingTasks.isEmpty() || !_runningTasks.isEmpty())
            {
                try
                {
                    this.wait();
                }
                catch (InterruptedException e)
                {
                    // do nothing?
                }
            }
        }
    }

    synchronized private Runnable nextTask()
    {
        if (_pendingTasks.isEmpty())
            return null;
        Runnable ret = _pendingTasks.removeFirst();
        _runningTasks.addLast(ret);
        return ret;
    }

    synchronized private void taskCompleted(Runnable task)
    {
        boolean b = _runningTasks.remove(task);
        notifyAll();
    }

}

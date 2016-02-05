/*
 * Copyright (c) 2007-2016 LabKey Corporation
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

public class FlowThreadPool
{
    private static FlowThreadPool instance;
    private Thread[] _threads;
    private LinkedList<FlowTaskSet> _taskSets = new LinkedList<>();
    private boolean _alive = true;
    private int _idleCount;

    private FlowThreadPool(FlowTaskSet taskSet)
    {
        _taskSets.add(taskSet);
        int processorCount = Runtime.getRuntime().availableProcessors();
        int threadCount = processorCount - 1;
        if (threadCount > 4)
            threadCount = 4;

        // As a rule of thumb, we assume each thread may take up to 200MB of RAM.
        // Restrict the number of threads to that value
        long memoryCount = Runtime.getRuntime().maxMemory() / (200 * ( 1 << 20));
        if (threadCount > memoryCount && memoryCount >= 0)
        {
            threadCount = (int) memoryCount;
        }

        _threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i ++)
        {
            _threads[i] = new Thread(new FlowThreadRunner(), "Flow-Thread-" + i);
            _threads[i].start();
        }

    }

    synchronized private void addTaskSet(FlowTaskSet taskSet)
    {
        _taskSets.add(taskSet);
        notifyAll();
    }

    synchronized static private FlowThreadPool getInstance(FlowTaskSet taskSet)
    {
        if (instance == null)
        {
            instance = new FlowThreadPool(taskSet);
        }
        else
        {
            instance.addTaskSet(taskSet);
        }
        return instance;
    }

    static public void runTaskSet(FlowTaskSet taskSet)
    {
        getInstance(taskSet);
        taskSet.runAllTasks();
    }

    synchronized private FlowTaskSet[] getTaskSets()
    {
        return _taskSets.toArray(new FlowTaskSet[_taskSets.size()]);
    }

    class FlowThreadRunner implements Runnable
    {
        public void run()
        {
outer:
            for (;;)
            {
                FlowTaskSet[] taskSets = getTaskSets();
                for (FlowTaskSet taskSet : taskSets)
                {
                    if (taskSet.runNextTask())
                    {
                        continue outer;
                    }
                    else
                    {
                        removeTaskSet(taskSet);
                    }
                }
                enterIdle();
                synchronized(FlowThreadPool.this)
                {
                    if (!isAlive())
                        return;
                    try
                    {
                        FlowThreadPool.this.wait();
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
                exitIdle();
            }
        }
    }

    synchronized private boolean isAlive()
    {
        return _alive;
    }

    synchronized private void removeTaskSet(FlowTaskSet taskSet)
    {
        _taskSets.remove(taskSet);
    }

    private void enterIdle()
    {
        synchronized(this)
        {
            _idleCount ++;
        }
        if (killThreadPool(this))
        {
            synchronized(this)
            {
                _alive = false;
                notifyAll();
            }
        }
    }

    synchronized private void exitIdle()
    {
        _idleCount --;
    }

    synchronized private boolean isIdle()
    {
        return _idleCount == _threads.length && _taskSets.isEmpty();
    }

    synchronized static private boolean killThreadPool(FlowThreadPool pool)
    {
        if (pool != instance)
            return false;
        if (!pool.isIdle())
            return false;
        instance = null;
        return true;
    }
}

package org.labkey.flow.script;

import java.util.LinkedList;

public class FlowThreadPool
{
    static private FlowThreadPool instance;
    private Thread[] _threads;
    private LinkedList<FlowTaskSet> _taskSets = new LinkedList();
    private boolean _alive = true;
    private int _idleCount;

    private FlowThreadPool(FlowTaskSet taskSet)
    {
        _taskSets.add(taskSet);
        int processorCount = Runtime.getRuntime().availableProcessors();
        int threadCount = processorCount - 1;

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
        return _taskSets.toArray(new FlowTaskSet[0]);
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
                        e.printStackTrace();
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

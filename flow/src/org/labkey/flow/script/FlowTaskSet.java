package org.labkey.flow.script;

import java.util.LinkedList;
import java.util.Arrays;

public class FlowTaskSet
{
    private LinkedList<Runnable> _pendingTasks;
    private LinkedList<Runnable> _runningTasks;

    public FlowTaskSet(Runnable[] tasks)
    {
        _pendingTasks = new LinkedList(Arrays.asList(tasks));
        _runningTasks = new LinkedList();
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

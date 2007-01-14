package org.labkey.ms2.protein;

import org.apache.log4j.Logger;

/**
 * User: brittp
 * Date: Dec 23, 2005
 * Time: 4:00:42 PM
 */
public abstract class DefaultAnnotationLoader implements AnnotationLoader
{
    protected static Logger _log;
    protected AnnotationLoader.Status _requestedThreadState = null;
    protected int _recoveryId;
    private boolean _paused;

    public DefaultAnnotationLoader()
    {
        _log = Logger.getLogger(getClass());
    }

    public void requestThreadState(AnnotationLoader.Status t)
    {
        _requestedThreadState = t;
    }

    public Status getRequestedThreadState()
    {
        return _requestedThreadState;
    }

    public void handleThreadStateChangeRequests()
    {
        if (_requestedThreadState != null)
        {
            switch (_requestedThreadState)
            {
                case KILLED:
                    throw new KillRequestedException();
                case PAUSED:
                    try
                    {
                        _paused = true;
                        while (_requestedThreadState == Status.PAUSED)
                        {
                            try
                            {
                                Thread.sleep(10000);
                            }
                            catch (InterruptedException e)
                            {
                                // continue sleeping?
                            }
                        }
                    }
                    finally
                    {
                        _paused = false;
                    }
            }
        }
    }

    public boolean isPaused()
    {
        return _paused;
    }

    public void handleThreadStateChangeRequests(String msg)
    {
        _log.debug(msg);
        handleThreadStateChangeRequests();
    }

    public void setRecoveryId(int rid)
    {
        _recoveryId = rid;
    }

    public int getRecoveryId()
    {
        return _recoveryId;
    }
}

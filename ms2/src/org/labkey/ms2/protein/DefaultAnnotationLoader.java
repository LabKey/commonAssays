/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.ms2.protein;

import org.apache.log4j.Logger;

/**
 * User: brittp
 * Date: Dec 23, 2005
 * Time: 4:00:42 PM
 */
public abstract class DefaultAnnotationLoader implements AnnotationLoader
{
    protected String _parseFName;
    protected String _comment = null;
    protected static Logger _log;
    protected AnnotationLoader.Status _requestedThreadState = null;
    protected int _recoveryId;
    private boolean _paused;

    public DefaultAnnotationLoader()
    {
        _log = Logger.getLogger(getClass());
    }

    public String getParseFName()
    {
        return _parseFName;
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

    public void setComment(String c)
    {
        this._comment = c;
    }

    public String getComment()
    {
        return _comment;
    }

    public void parseInBackground()
    {
        AnnotationUploadManager.getInstance().enqueueAnnot(this);
    }

    public void parseInBackground(int recoveryId)
    {
        setRecoveryId(recoveryId);
        AnnotationUploadManager.getInstance().enqueueAnnot(this);
    }
}

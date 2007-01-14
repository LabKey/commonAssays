/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
import org.labkey.api.data.Table;
import org.labkey.api.util.JobRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.sql.SQLException;

public class AnnotationUploadManager
{
    private static Logger _log = Logger.getLogger(AnnotationUploadManager.class);

    private static AnnotationUploadManager instance;
    private final JobRunner _runner;
    private List<AnnotationLoadJob> _loaderStatus = new ArrayList<AnnotationLoadJob>();

    public synchronized static AnnotationUploadManager getInstance()
    {
        if (instance == null)
            instance = new AnnotationUploadManager();
        return instance;
    }

    private AnnotationUploadManager()
    {
        _runner = new JobRunner();
    }

    public void enqueueAnnot(AnnotationLoader alt)
    {
        synchronized (_runner)
        {
            AnnotationLoadJob job = new AnnotationLoadJob(alt);
            _runner.submit(job);
            _loaderStatus.add(job);
        }
    }

    public AnnotationLoader getActiveLoader(int id)
    {
        synchronized (_runner)
        {
            for (AnnotationLoadJob job: _loaderStatus)
            {
                if (job.getLoader().getId() == id)
                    return job.getLoader();
            }
            return null;
        }
    }

    public AnnotationLoader.Status annotThreadStatus(int id) throws SQLException
    {
        Date idate = Table.executeSingleton(
                        ProteinManager.getSchema(),
                        "SELECT InsertDate FROM " +
                                ProteinManager.getTableInfoAnnotInsertions() +
                                " WHERE InsertId=" + id,
                        null, Date.class);

        if (idate == null) return AnnotationLoader.Status.UNKNOWN;

        Date cdate = Table.executeSingleton(
                        ProteinManager.getSchema(),
                        "SELECT CompletionDate FROM " +
                                ProteinManager.getTableInfoAnnotInsertions() +
                                " WHERE InsertId=" + id,
                        null, Date.class);

        if (cdate != null) return AnnotationLoader.Status.COMPLETE;

        synchronized (_runner)
        {
            for (AnnotationLoadJob job: _loaderStatus)
            {
                if (job.getLoader().getId() == id)
                {
                    if (job.isCancelled())
                        return AnnotationLoader.Status.DYING;
                    else if (job.isDone())
                    {
                        if (job.getLoader().getRequestedThreadState() == AnnotationLoader.Status.KILLED)
                            return AnnotationLoader.Status.KILLED;
                        else
                            return AnnotationLoader.Status.COMPLETE;
                    }
                    else
                    {
                        if (job.getLoader().isPaused())
                            return AnnotationLoader.Status.PAUSED;
                        else
                            return AnnotationLoader.Status.RUNNING;
                    }
                }
            }
            return AnnotationLoader.Status.INCOMPLETE;
        }
    }

    private class AnnotationLoadJob extends JobRunner.Job
    {
        private AnnotationLoader _loader;

        public AnnotationLoadJob(AnnotationLoader loader)
        {
            _loader = loader;
        }

        public void run()
        {
            try
            {
                _loader.parseFile();
            }
            catch (AnnotationLoader.KillRequestedException e)
            {
                _log.info("Kill requested for load of fasta file " + _loader.getParseFName());
            }
            catch (Exception e)
            {
                _log.error("Failed to parse file " + _loader.getParseFName(), e);
            }
            finally
            {
                _loader.cleanUp();
                _loaderStatus.remove(this);
            }
        }

        public AnnotationLoader getLoader()
        {
            return _loader;
        }
    }
}

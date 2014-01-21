/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.di.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.di.DataIntegrationService;
import org.labkey.api.di.ScheduledPipelineJobContext;
import org.labkey.api.di.ScheduledPipelineJobDescriptor;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleResourceCache;
import org.labkey.api.module.ModuleResourceCaches;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.resource.Resource;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.GUID;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.Path;
import org.labkey.api.util.TestContext;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.util.XmlBeansUtil;
import org.labkey.api.util.XmlValidationException;
import org.labkey.api.view.ViewServlet;
import org.labkey.api.writer.ContainerUser;
import org.labkey.di.DataIntegrationQuerySchema;
import org.labkey.di.VariableMap;
import org.labkey.di.VariableMapImpl;
import org.labkey.di.filters.FilterStrategy;
import org.labkey.di.filters.ModifiedSinceFilterStrategy;
import org.labkey.di.filters.RunFilterStrategy;
import org.labkey.di.filters.SelectAllFilterStrategy;
import org.labkey.di.steps.RemoteQueryTransformStepProvider;
import org.labkey.di.steps.SimpleQueryTransformStepProvider;
import org.labkey.di.steps.StepMeta;
import org.labkey.di.steps.StepProvider;
import org.labkey.di.steps.StoredProcedureStepProvider;
import org.labkey.di.steps.TestTaskProvider;
import org.labkey.etl.xml.EtlDocument;
import org.labkey.etl.xml.EtlType;
import org.labkey.etl.xml.FilterType;
import org.labkey.etl.xml.TransformType;
import org.labkey.etl.xml.TransformsType;
import org.quartz.CronExpression;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: jeckels
 * Date: 2/20/13
 */
public class TransformManager implements DataIntegrationService
{
    private static final TransformManager INSTANCE = new TransformManager();
    private static final Logger LOG = Logger.getLogger(TransformManager.class);
    private static final String JOB_GROUP_NAME = "org.labkey.di.pipeline.ETLManager";
    private static final ModuleResourceCache<ScheduledPipelineJobDescriptor> DESCRIPTOR_CACHE = ModuleResourceCaches.create(new Path(DescriptorCacheHandler.DIR_NAME), "ETL job descriptors", new DescriptorCacheHandler());

    private List<StepProvider> _providers = new ArrayList<>();

    public static TransformManager get()
    {
        return INSTANCE;
    }


    private TransformManager()
    {
    }


    @Nullable TransformDescriptor parseETL(Resource resource, Module module)
    {
        try
        {
            return parseETLThrow(resource, module);
        }
        catch (XmlValidationException|XmlException|IOException e)
        {
            LOG.warn("Unable to parse " + resource, e);
        }
        return null;
    }


    TransformDescriptor parseETLThrow(Resource resource, Module module) throws IOException, XmlException, XmlValidationException
    {
        FilterStrategy.Factory defaultFactory = null;
        Long interval = null;
        CronExpression cron = null;

        final ArrayList<StepMeta> stepMetaDatas = new ArrayList<>();
        final Path resourcePath = resource.getPath();
        final CaseInsensitiveHashSet stepIds = new CaseInsensitiveHashSet();
        final String configName = getConfigName(resourcePath.getName());

        String configId = createConfigId(module, configName);

        try (InputStream inputStream = resource.getInputStream())
        {
            if (inputStream == null)
            {
                throw new IOException("Unable to get InputStream from " + resource);
            }

            XmlOptions options = XmlBeansUtil.getDefaultParseOptions();
            options.setValidateStrict();
            EtlDocument document = EtlDocument.Factory.parse(inputStream, options);
            EtlType etlXML = document.getEtl();

            // handle default transform
            FilterType ft = etlXML.getIncrementalFilter();
            if (null != ft)
                defaultFactory = createFilterFactory(ft);
            if (null == defaultFactory)
                defaultFactory = new SelectAllFilterStrategy.Factory();

            // schedule
            if (null != etlXML.getSchedule())
            {
                if (null != etlXML.getSchedule().getPoll())
                {
                    String s = etlXML.getSchedule().getPoll().getInterval();
                    if (StringUtils.isNumeric(s))
                        interval = Long.parseLong(s) * 60 * 1000;
                    else
                        interval = DateUtil.parseDuration(s);
                }
                else if (null != etlXML.getSchedule().getCron())
                {
                    try
                    {
                        cron = new CronExpression(etlXML.getSchedule().getCron().getExpression());
                    }
                    catch (ParseException x)
                    {
                        throw new XmlException("Could not parse cron expression: " + etlXML.getSchedule().getCron().getExpression(), x);
                    }
                }
            }

            TransformsType transforms = etlXML.getTransforms();
            if (null != transforms)
            {
                TransformType[] transformTypes = transforms.getTransformArray();
                for (TransformType t : transformTypes)
                {
                    StepMeta meta = buildTransformStepMeta(t, stepIds);
                    stepMetaDatas.add(meta);
                }
            }

            // XmlSchema validate the document after we've attempted to parse it since we can provide better error messages.
            XmlBeansUtil.validateXmlDocument(document, "ETL '" + resource.getPath() + "'");

            return new TransformDescriptor(configId, etlXML.getName(), etlXML.getDescription(), module.getName(), interval, cron, defaultFactory, stepMetaDatas);
        }
    }


    boolean isConfigFile(String filename)
    {
        return filename.endsWith(".xml");
    }


    String getConfigName(String filename)
    {
        assert filename.endsWith(".xml") : "Configuration filename \"" + filename + "\" does not end with .xml";
        return FileUtil.getBaseName(filename);
    }


    String createConfigId(Module module, String configName)
    {
        return "{" + module.getName() + "}/" + configName;
    }


    private static final Pattern CONFIG_ID_PATTERN = Pattern.compile("\\{(\\w+)\\}/(.+)");

    Pair<Module, String> parseConfigId(String configId)
    {
        // Parse out the module name and the config name
        Matcher matcher = CONFIG_ID_PATTERN.matcher(configId);

        if (!matcher.matches() || matcher.groupCount() != 2)
            throw new IllegalStateException("Unrecognized configuration ID format: " + configId);

        String moduleName = matcher.group(1);
        String filename = matcher.group(2);
        Module module = ModuleLoader.getInstance().getModule(moduleName);

        if (null == module)
            throw new IllegalStateException("Module does not exist: " + moduleName);

        return new Pair<>(module, filename);
    }


    private FilterStrategy.Factory createFilterFactory(FilterType filterTypeXML)
    {
        String className = StringUtils.defaultString(filterTypeXML.getClassName().toString(), ModifiedSinceFilterStrategy.class.getName());
        if (!className.contains("."))
            className = "org.labkey.di.filters." + className;
        if (className.equals(ModifiedSinceFilterStrategy.class.getName()))
            return new ModifiedSinceFilterStrategy.Factory(filterTypeXML);
        else if (className.equals(RunFilterStrategy.class.getName()))
            return new RunFilterStrategy.Factory(filterTypeXML);
        else if (className.equals(SelectAllFilterStrategy.class.getName()))
            return new SelectAllFilterStrategy.Factory();
        throw new IllegalArgumentException("Class is not a recognized filter strategy: " + className);
    }


    // errors
    static final String INVALID_TYPE = "Invalid transform type specified";
    static final String TYPE_REQUIRED = "Transform type attribute is required";
    static final String ID_REQUIRED = "Id attribute is required";
    static final String DUPLICATE_ID = "Id attribute must be unique for each Transform";
    // errors used by stepMeta's
    public static final String INVALID_TARGET_OPTION = "Invalid targetOption attribute value specified";
    public static final String INVALID_SOURCE_OPTION = "Invalid sourceOption attribute value specified";
    public static final String INVALID_SOURCE = "No source element specified.";
    public static final String INVALID_DESTINATION = "No destination element specified.";
    public static final String INVALID_PROCEDURE = "No procedure element specified.";

    private StepMeta buildTransformStepMeta(TransformType transformXML, Set<String> stepIds) throws XmlException
    {
        if (null == transformXML.getId())
            throw new XmlException(ID_REQUIRED);
        if (stepIds.contains(transformXML.getId()))
            throw new XmlException(DUPLICATE_ID);

        if (null == transformXML.getType())
            throw new XmlException(TYPE_REQUIRED);

        StepProvider provider = getStepProvider(transformXML.getType());
        if (null == provider)
            throw new XmlException(INVALID_TYPE);

        Class taskClass = provider.getStepClass();
        StepMeta meta;
        if (!isValidTaskClass(taskClass))
            throw new XmlException(INVALID_TYPE);

        meta = provider.createMetaInstance();
        meta.setProvider(provider);
        meta.parseConfig(transformXML); // will throw XmlException on validation error

        // Only add to the list of steps after passing validation, including whatever extra validation was in StepMeta
        stepIds.add(transformXML.getId());

        return meta;
    }

    private boolean isValidTaskClass(Class taskClass)
    {
        return TransformTask.class.isAssignableFrom(taskClass);
    }

    @NotNull
    public Collection<ScheduledPipelineJobDescriptor> getDescriptors(Container c)
    {
        return DESCRIPTOR_CACHE.getResources(c);
    }


    @Nullable
    public ScheduledPipelineJobDescriptor getDescriptor(String id)
    {
        return DESCRIPTOR_CACHE.getResource(id);
    }


    public synchronized Integer runNowPipeline(ScheduledPipelineJobDescriptor descriptor, Container container, User user)
            throws PipelineJobException
    {
        if (ViewServlet.isShuttingDown())
            throw new PipelineJobException("Could not create job: server is shutting down");

        try
        {
            ContainerUser context = descriptor.getJobContext(container, user);

            // see if we have work to do before directly scheduling the pipeline job
            boolean hasWork = descriptor.checkForWork(context, false, true);
            if (!hasWork)
                return null;
            PipelineJob job = descriptor.getPipelineJob(context);
            if (null == job)
                throw new PipelineJobException("Could not create job: " + descriptor.toString());

            try
            {
                PipelineService.get().setStatus(job, PipelineJob.WAITING_STATUS, null, true);
                PipelineService.get().queueJob(job);
                return PipelineService.get().getJobId(user, container, job.getJobGUID());
            }
            catch (Exception e)
            {
                throw new PipelineJobException(e);
            }
        }
        finally
        {
            assert dumpScheduler();
        }
    }


    public synchronized void runNowQuartz(ScheduledPipelineJobDescriptor descriptor, Container container, User user)
    {
        if (ViewServlet.isShuttingDown())
            return;

        try
        {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            ScheduledPipelineJobContext info = (ScheduledPipelineJobContext)descriptor.getJobContext(container, user);
            info.setVerbose(true);

            // find job
            JobKey jobKey = jobKeyFromDescriptor(descriptor);
            JobDetail job = null;
            if (!scheduler.checkExists(jobKey))
            {
                job = jobFromDescriptor(descriptor);
            }

            TriggerKey triggerKey = TriggerKey.triggerKey(info.getKey() + GUID.makeHash(), JOB_GROUP_NAME);
            Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobKey)
                .startNow()
                .usingJobData(info.getQuartzJobDataMap())
                .build();

            if (null != job)
                scheduler.scheduleJob(job, trigger);
            else
                scheduler.scheduleJob(trigger);
        }
        catch (SchedulerException e)
        {
            throw new UnexpectedException(e);
        }
        finally
        {
            assert dumpScheduler();
        }
    }


    public synchronized void schedule(ScheduledPipelineJobDescriptor descriptor, Container container, User user, boolean verbose)
    {
        if (ViewServlet.isShuttingDown())
            return;

        try
        {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            ScheduledPipelineJobContext info = (ScheduledPipelineJobContext)descriptor.getJobContext(container, user);
            info.setVerbose(verbose);
            info.setLocked();

            // find job
            JobKey jobKey = jobKeyFromDescriptor(descriptor);
            JobDetail job = null;
            if (!scheduler.checkExists(jobKey))
            {
                job = jobFromDescriptor(descriptor);
            }

            // find trigger
            // Trigger the job to run now, and then every 60 seconds
            TriggerKey triggerKey = TriggerKey.triggerKey(info.getKey(), JOB_GROUP_NAME);
            Trigger trigger = scheduler.getTrigger(triggerKey);
            if (null != trigger)
                return;
            trigger = TriggerBuilder.newTrigger()
                .forJob(jobKey)
                .withIdentity(triggerKey)
                .startNow()
                .withSchedule(descriptor.getScheduleBuilder())
                .usingJobData(info.getQuartzJobDataMap())
                .build();

            if (null != job)
                scheduler.scheduleJob(job, trigger);
            else
                scheduler.scheduleJob(trigger);
        }
        catch (SchedulerException e)
        {
            throw new UnexpectedException(e);
        }
        finally
        {
            assert dumpScheduler();
        }
    }


    public synchronized void unschedule(ScheduledPipelineJobDescriptor etlDescriptor, Container container, User user)
    {
        try
        {
            TransformJobContext info = new TransformJobContext(etlDescriptor, container, user);
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            TriggerKey triggerKey = TriggerKey.triggerKey(info.getKey(), JOB_GROUP_NAME);
            scheduler.unscheduleJob(triggerKey);
        }
        catch (SchedulerException e)
        {
            throw new UnexpectedException(e);
        }
        finally
        {
            assert dumpScheduler();
        }
    }


    public synchronized void unscheduleAll(Container container)
    {
        try
        {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.<TriggerKey>groupEquals(JOB_GROUP_NAME));
            String containerPrefix = "" + container.getRowId() + "/";
            for (TriggerKey key : keys)
            {
                if (key.getName().startsWith(containerPrefix))
                    scheduler.unscheduleJob(key);
            }
        }
        catch (SchedulerException e)
        {
            throw new UnexpectedException(e);
        }
        finally
        {
            assert dumpScheduler();
        }
    }



    private static JobKey jobKeyFromDescriptor(ScheduledPipelineJobDescriptor descriptor)
    {
        return JobKey.jobKey(descriptor.getId(), JOB_GROUP_NAME);
    }


    private JobDetail jobFromDescriptor(ScheduledPipelineJobDescriptor descriptor)
    {
        JobKey jobKey = JobKey.jobKey(descriptor.getId(), JOB_GROUP_NAME);
        JobDetail job = JobBuilder.newJob(descriptor.getQuartzJobClass())
            .withIdentity(jobKey)
            .storeDurably()
            .build();
        // Consider using a facade for the descriptor instead of the full descriptor. Really only need id and
        // pointer to correct descriptor class, and the descriptor is responsible for getting the current
        // version from the cache.
        job.getJobDataMap().put(ScheduledPipelineJobDescriptor.class.getName(),descriptor);
        return job;
    }


    boolean dumpScheduler()
    {
        try
        {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

            {
            LOG.debug("Jobs");
            Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.<JobKey>groupEquals(JOB_GROUP_NAME));
            for (JobKey key : keys)
                LOG.debug("\t" + key.toString());
            }

            {
            LOG.debug("Triggers");
            Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.<TriggerKey>groupEquals(JOB_GROUP_NAME));
            for (TriggerKey key : keys)
                LOG.debug("\t" + key.toString());
            }
        }
        catch(SchedulerException x)
        {
            LOG.warn(x);
        }
        return true;
    }


    public List<TransformConfiguration> getTransformConfigurations(Container c)
    {
        DbScope scope = DbSchema.get("dataintegration").getScope();
        SQLFragment sql = new SQLFragment("SELECT * FROM dataintegration.transformconfiguration WHERE container=?", c.getId());
        return new SqlSelector(scope, sql).getArrayList(TransformConfiguration.class);
    }


    public TransformConfiguration getTransformConfiguration(Container c, ScheduledPipelineJobDescriptor etl)
    {
        DbScope scope = DbSchema.get("dataintegration").getScope();
        SQLFragment sql = new SQLFragment("SELECT * FROM dataintegration.transformconfiguration WHERE container=? and transformid=?", c.getId(), etl.getId());
        TransformConfiguration ret = new SqlSelector(scope, sql).getObject(TransformConfiguration.class);
        if (null != ret)
            return ret;
        return saveTransformConfiguration(null, new TransformConfiguration(etl, c));
    }


    public TransformConfiguration saveTransformConfiguration(User user, TransformConfiguration config)
    {
        try
        {
            TableInfo t = DbSchema.get("dataintegration").getTable("TransformConfiguration");
            if (-1 == config.rowId)
                return Table.insert(user, t, config);
            else
                return Table.update(user, t, config, new Object[] {config.rowId});
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }



    /* Should only be called once at startup */
    public void startAllConfigurations()
    {
        DbSchema schema = DataIntegrationQuerySchema.getSchema();
        SQLFragment sql = new SQLFragment("SELECT * FROM dataintegration.transformconfiguration WHERE enabled=?", true);

        new SqlSelector(schema, sql).forEach(new Selector.ForEachBlock<TransformConfiguration>(){
            @Override
            public void exec(TransformConfiguration config) throws SQLException
            {
                // CONSIDER explicit runAs user
                int runAsUserId = config.getModifiedBy();
                User runAsUser = UserManager.getUser(runAsUserId);

                ScheduledPipelineJobDescriptor descriptor = DESCRIPTOR_CACHE.getResource(config.getTransformId());
                if (null == descriptor)
                    return;
                Container c = ContainerManager.getForId(config.getContainerId());
                if (null == c)
                    return;
                schedule(descriptor, c, runAsUser, config.isVerboseLogging());
                }
        }, TransformConfiguration.class);
    }


    public void shutdownPre()
    {
        try
        {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.standby();
        }
        catch (SchedulerException x)
        {
        }
    }


    public void shutdownStarted()
    {
        try
        {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.shutdown(true);
        }
        catch (SchedulerException x)
        {
        }
    }


    public TransformRun getTransformRun(Container c, int runId)
    {
        TransformRun run;
        run = new SqlSelector(DataIntegrationQuerySchema.getSchema(),
                "SELECT * FROM " + DataIntegrationQuerySchema.getTransformRunTableInfo().getFromSQL("x") +
                " WHERE container=? and transformrunid=?", c.getId(), runId).getObject(TransformRun.class);
        return run;
    }

    public TransformRun getTransformRunForJob(Container c, int jobId)
    {
        TransformRun run;
        run = new SqlSelector(DataIntegrationQuerySchema.getSchema(),
                "SELECT * FROM " + DataIntegrationQuerySchema.getTransformRunTableInfo().getFromSQL("x") +
                        " WHERE container=? and jobid=?", c.getId(), jobId).getObject(TransformRun.class);
        return run;
    }

    public TransformRun insertTransformRun(User user, TransformRun run) throws SQLException
    {
        run = Table.insert(user, DataIntegrationQuerySchema.getTransformRunTableInfo(), run);
        return run;
    }


    public void updateTransformRun(User user, TransformRun run) throws SQLException
    {
        Table.update(user, DataIntegrationQuerySchema.getTransformRunTableInfo(), run, run.getTransformRunId());
    }


    public Integer getLastSuccessfulTransformExpRun(String transformId, int version)
    {
        SimpleFilter f = new SimpleFilter();
        Sort s = new Sort("-StartTime");
        TableInfo ti = DataIntegrationQuerySchema.getTransformRunTableInfo();

        f.addCondition(new FieldKey(null, "TransformId"), transformId, CompareType.EQUAL);
        f.addCondition(new FieldKey(null, "TransformVersion"), version, CompareType.EQUAL);
        f.addCondition(new FieldKey(null, "status"), "Complete", CompareType.EQUAL);

        Integer[] expRunIds = new TableSelector(ti.getColumn("ExpRunId"), f, s).getArray(Integer.class);

        if (expRunIds.length > 0)
            return expRunIds[0];

        return null;
   }

    public VariableMap getVariableMapForTransformJob(Integer expRunId)
    {
        ExpRun run = ExperimentService.get().getExpRun(expRunId);
        return new VariableMapImpl(null, run.getObjectProperties());
    }


    public VariableMap getVariableMapForTransformStep(Integer expRunId, String transformStepId)
    {
        ExpProtocolApplication[] protocolApps = ExperimentService.get().getExpProtocolApplicationsForRun(expRunId);
        for (ExpProtocolApplication protocolApp : protocolApps)
        {
            if (StringUtils.equals(protocolApp.getName(), transformStepId))
            {
                return new VariableMapImpl(null, protocolApp.getObjectProperties());
            }
        }

        return null;
    }

    //
    // DataIntegrationService
    //


    @Override
    public void registerStepProviders()
    {
        _providers.add(new SimpleQueryTransformStepProvider());
        _providers.add(new RemoteQueryTransformStepProvider());
        _providers.add(new StoredProcedureStepProvider());
        _providers.add(new TestTaskProvider());

    }

    @Nullable
    public StepProvider getStepProvider(String providerName)
    {
        for (StepProvider potential : _providers)
        {
            if (potential.getName().equals(providerName) || potential.getLegacyNames().contains(providerName))
            {
                return potential;
            }
        }

        return null;
    }

    //
    // Tests
    //

    public static class TestCase extends Assert
    {
        @Test
        public void scheduler() throws Exception
        {
            final AtomicInteger counter = new AtomicInteger(0);

            final class TestTransformDescriptor extends TransformDescriptor
            {
                private TestTransformDescriptor(String id, String name, String moduleName) throws XmlException, IOException
                {
                    super(id, name, null, moduleName, null, null, null, null);
                }

                @Override
                public int getVersion()
                {
                    return 0;
                }

                @Override
                public ScheduleBuilder getScheduleBuilder()
                {
                    return SimpleScheduleBuilder.repeatSecondlyForever();
                }

                @Override
                public String getScheduleDescription()
                {
                    return "1s";
                }

                @Override
                public boolean checkForWork(ScheduledPipelineJobContext context, boolean background, boolean verbose)
                {
                    counter.incrementAndGet();
                    return false;
                }

                @Override
                public PipelineJob getPipelineJob(ScheduledPipelineJobContext context) throws PipelineJobException
                {
                    return null;
                }

                @Override
                public ScheduledPipelineJobDescriptor getDescriptorFromCache()
                {
                    return this;
                }
            }

            final ScheduledPipelineJobDescriptor d = new TestTransformDescriptor(GUID.makeHash(), "TestCase", "dataintegration");
            final User user = TestContext.get().getUser();
            final Container c = JunitUtil.getTestContainer();

            assertEquals(0, counter.get());
            TransformManager.get().runNowQuartz(d, c, user);
            for (int i=0 ; i<10 && counter.get()<1 ; i++)
                sleep(1);
            assertEquals(1, counter.get());
            TransformManager.get().runNowQuartz(d, c, user);
            for (int i=0 ; i<10 && counter.get()<2 ; i++)
                sleep(2);
            assertEquals(2, counter.get());

            TransformManager.get().schedule(d, c, user, false);
            sleep(5);
            assertTrue(counter.get() <= 10);
            for (int i=0 ; i<10 && counter.get() < 12 ; i++)
                sleep(1);
            assertTrue(counter.get() >= 12);

            TransformManager.get().unschedule(d, c, user);
            sleep(1);
            int count = counter.get();
            sleep(2);
            assertEquals(count, counter.get());

            JobKey jobKey = jobKeyFromDescriptor(d);
            assertTrue(StdSchedulerFactory.getDefaultScheduler().checkExists(jobKey));
            StdSchedulerFactory.getDefaultScheduler().deleteJob(jobKey);
            assertFalse(StdSchedulerFactory.getDefaultScheduler().checkExists(jobKey));
        }


       void sleep(int sec)
       {
           try
           {
               Thread.sleep(sec * 1000);
           }
           catch (InterruptedException x)
           {
           }
       }
    }


    /*
    private static class TestDescriptorWrapper implements ScheduledPipelineJobDescriptor
    {
        private final ScheduledPipelineJobDescriptor _d;
        private final Runnable _done;

        TestDescriptorWrapper(ScheduledPipelineJobDescriptor d, Runnable done)
        {
            _d = d;
            _done = done;
        }

        public String getId()
        {
            return _d.getId();
        }

        public String getName()
        {
            return _d.getName();
        }

        public String getDescription()
        {
            return _d.getDescription();
        }

        public String getModuleName()
        {
            return _d.getModuleName();
        }

        public int getVersion()
        {
            return _d.getVersion();
        }

        public ScheduleBuilder getScheduleBuilder()
        {
            return _d.getScheduleBuilder();
        }

        public String getScheduleDescription()
        {
            return _d.getScheduleDescription();
        }

        public Class<? extends Job> getQuartzJobClass()
        {
            return _d.getQuartzJobClass();
        }

        public ContainerUser getJobContext(Container c, User user)
        {
            return _d.getJobContext(c, user);
        }

        public Callable<Boolean> getChecker(ContainerUser context)
        {
            return _d.getChecker(context);
        }

        public PipelineJob getPipelineJob(ContainerUser context) throws PipelineJobException
        {
            return _d.getPipelineJob(context);
        }
    }
    */
}

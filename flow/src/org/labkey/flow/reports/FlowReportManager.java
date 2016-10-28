/*
 * Copyright (c) 2011-2016 LabKey Corporation
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
package org.labkey.flow.reports;

import org.labkey.api.collections.CaseInsensitiveTreeMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.OntologyObject;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.XarFormatException;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.xar.LsidUtils;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.reports.Report;
import org.labkey.api.reports.ReportService;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Tuple3;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.data.FlowObject;
import org.labkey.flow.query.FlowTableType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/**
 * User: kevink
 * Date: 7/6/11
 */
public class FlowReportManager
{
    public static Collection<FlowReport> getFlowReports(Container c, User user)
    {
        Collection<Report> all = ReportService.get().getReports(user, c);
        TreeMap<String, FlowReport> reports = new CaseInsensitiveTreeMap<>();

        for (Report r : all)
        {
            if (!r.getType().startsWith("Flow.") && !(r instanceof FlowReport))
                continue;
            FlowReport flowReport = (FlowReport)r;
            reports.put(r.getDescriptor().getReportName(), flowReport);
        }

        return reports.values();
    }

    public static Collection<FilterFlowReport> getPostAnalysisReports(Container c, User user)
    {
        Collection<FlowReport> reports = FlowReportManager.getFlowReports(c, user);
        List<FilterFlowReport> ret = new ArrayList<>(reports.size());
        for (FlowReport report : reports)
        {
            // UNDONE: Need a ReportDescriptor property to identify post-analysis reports from other reports.
            // UNDONE: For now, only allow PositivityFlowReport.
            if (report instanceof PositivityFlowReport)
            {
                ret.add((FilterFlowReport)report);
            }
        }
        return ret;
    }

    public static List<FlowReportJob> createReportJobs(ViewBackgroundInfo info, PipeRoot root)
            throws Exception
    {
        Collection<FilterFlowReport> reports = getPostAnalysisReports(info.getContainer(), info.getUser());
        List<FlowReportJob> jobs = new ArrayList<>(reports.size());
        for (FilterFlowReport report : reports)
        {
            jobs.add(new FlowReportJob(report, info, root));
        }

        return jobs;
    }

    /** Get all domains from all reports. */
    public static Collection<Tuple3<FlowReport, Domain, FlowTableType>> getReportDomains(Container c, User user)
    {
        Collection<Tuple3<FlowReport, Domain, FlowTableType>> tuples = new ArrayList<>();
        for (FlowReport report : getFlowReports(c, user))
        {
            if (!report.saveToDomain())
                continue;

            for (FlowTableType tableType : FlowTableType.values())
            {
                Domain domain = getDomain(report, tableType);
                if (domain != null)
                    tuples.add(Tuple3.of(report, domain, tableType));
            }
        }
        return tuples;
    }

    /** Get FlowTableType domains from all reports. */
    public static Collection<Domain> getReportDomains(Container c, User user, FlowTableType tableType)
    {
        Collection<Domain> domains = new ArrayList<>();
        for (FlowReport report : getFlowReports(c, user))
        {
            if (!report.saveToDomain())
                continue;

            Domain domain = getDomain(report, tableType);
            if (domain != null)
                domains.add(domain);
        }
        return domains;
    }

    private static String FLOW_REPORT_DOMAIN_PREFIX = "FlowReportDomain-";
    private static String FLOW_REPORT_TABLE_TYPE_SUBST = "FlowTableType";
    private static String FLOW_REPORT_TYPE_SUBST = "ReportType";
    private static String FLOW_REPORT_DOMAIN_URI_TEMPLATE =
            "urn:lsid:" + XarContext.LSID_AUTHORITY_SUBSTITUTION +
                    ":" + FLOW_REPORT_DOMAIN_PREFIX + XarContext.createSubstitution(FLOW_REPORT_TYPE_SUBST) + ".Folder-" + XarContext.CONTAINER_ID_SUBSTITUTION +
                    ":" + XarContext.createSubstitution(FLOW_REPORT_TABLE_TYPE_SUBST);


    protected static String getDomainURI(FlowReport report, FlowTableType tableType)
    {
        if (!report.saveToDomain())
            return null;

        // Only domains attached to the FCSAnalyses table are supported right now
        if (tableType != FlowTableType.FCSAnalyses)
            return null;

        Container shared = ContainerManager.getSharedContainer();
        XarContext context = new XarContext("Domains", shared, null);
        context.addSubstitution(FLOW_REPORT_TABLE_TYPE_SUBST, PageFlowUtil.encode(tableType.toString()));
        context.addSubstitution(FLOW_REPORT_TYPE_SUBST, PageFlowUtil.encode(report.getDescriptor().getReportType()));
        try
        {
            return LsidUtils.resolveLsidFromTemplate(FLOW_REPORT_DOMAIN_URI_TEMPLATE, context);
        }
        catch (XarFormatException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Domain getDomain(FlowReport report, FlowTableType tableType)
    {
        if (!report.saveToDomain())
            return null;

        String typeURI = getDomainURI(report, tableType);
        if (typeURI == null)
            return null;

        Container shared = ContainerManager.getSharedContainer();
        return PropertyService.get().getDomain(shared, typeURI);
    }

    /** Create the shared Domain for the report type. */
    public static Domain ensureDomain(FlowReport report, User user, FlowTableType tableType)
    {
        if (!report.saveToDomain())
            return null;

        String typeURI = getDomainURI(report, tableType);
        if (typeURI == null)
            return null;

        Container shared = ContainerManager.getSharedContainer();
        Domain domain = PropertyService.get().getDomain(shared, typeURI);
        if (domain != null)
            return domain;

        Collection<PropertyDescriptor> properties = report.getDomainPrototypeProperties();
        assert properties != null && properties.size() > 0;

        DbSchema schema = ExperimentService.get().getSchema();
        try (DbScope.Transaction transaction = schema.getScope().ensureTransaction())
        {
            String name = report.getDescriptor().getReportType();
            domain = PropertyService.get().createDomain(shared, typeURI, name);
            domain.setDescription("Domain for " + report.getDescriptor().getDescriptorType() + " reports on flow table " + tableType);
            domain.save(user);

            domain = PropertyService.get().getDomain(domain.getTypeId());

            for (PropertyDescriptor prop : properties)
            {
                DomainProperty dp = domain.addProperty();
                PropertyDescriptor pd = dp.getPropertyDescriptor();
                prop.copyTo(pd);

                String uri = domain.getTypeURI() + "#" + prop.getName();
                pd.setPropertyURI(uri);
            }

            domain.save(user);

            transaction.commit();
            return domain;
        }
        catch (ExperimentException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String FLOW_REPORT_EXPOBJECT_PREFIX = "FlowReportObject-";
    private static String FLOW_REPORT_ID_SUBST = "ReportId";
    private static String FLOW_REPORT_EXPOBJECT_URI_TEMPLATE =
            "urn:lsid:" + XarContext.LSID_AUTHORITY_SUBSTITUTION +
                    ":" + FLOW_REPORT_EXPOBJECT_PREFIX + XarContext.createSubstitution(FLOW_REPORT_TYPE_SUBST) + ".Folder-" + XarContext.CONTAINER_ID_SUBSTITUTION +
                    ":" + XarContext.createSubstitution(FLOW_REPORT_ID_SUBST);

    private static String getReportExpObjectURI(FlowReport report, Container container)
    {
        if (!report.saveToDomain())
            return null;

        XarContext context = new XarContext("Domains", container, null);
        context.addSubstitution(FLOW_REPORT_TYPE_SUBST, PageFlowUtil.encode(report.getDescriptor().getReportType()));
        context.addSubstitution(FLOW_REPORT_ID_SUBST, PageFlowUtil.encode(report.getReportId().toString()));
        try
        {
            return LsidUtils.resolveLsidFromTemplate(FLOW_REPORT_EXPOBJECT_URI_TEMPLATE, context);
        }
        catch (XarFormatException e)
        {
            throw new RuntimeException(e);
        }
    }

    /** Get the exp.object for the FlowReport across all Containers. */
//    private static Integer[] getReportOntologyObjects(FlowReport report)
//    {
//        String uri = getReportExpObjectURI(report, null);
//        TableInfo table = OntologyManager.getTinfoObject();
//        Integer[] ids = Table.executeArray(table, table.getColumn("ObjectURI"), new SimpleFilter("ObjectURI", uri), null, Integer.class);
//    }

    public static Integer getReportOntologyObjectId(FlowReport report, Container container)
    {
        String uri = getReportExpObjectURI(report, container);
        if (uri == null)
            return null;

        OntologyObject obj = OntologyManager.getOntologyObject(container, uri);
        return obj == null ? null : obj.getObjectId();
    }

    public static Integer ensureReportOntologyObjectId(FlowReport report, Container container)
    {
        String uri = getReportExpObjectURI(report, container);
        if (uri == null)
            return null;

        return OntologyManager.ensureObject(container, uri, (Integer) null);
    }

    public static String FLOW_REPORT_RESULT_OBJECT_LSID_PART = "FlowReportResult";

    public static String getReportResultsLsid(FlowReport report, FlowObject expObject)
    {
        return getReportResultsLsid(report, expObject.getLSID());
    }

    // Keep in sync with FlowSchema.FastFlowDataTable.addReportColumns()
    public static String getReportResultsLsid(FlowReport report, String baseLsid)
    {
        return baseLsid +
                "-" + FlowReportManager.FLOW_REPORT_RESULT_OBJECT_LSID_PART +
                "-" + PageFlowUtil.encode(report.getReportId().toString());
                //"-" + tableType.toString();
    }

    // All results have ownerObjectId set to the report's ontology object in the container
    // so we can just delete that object and its children to remove all results.
    public static void deleteReportResults(FlowReport report, Container container)
    {
        // Delete all owned objects of the report's exp.object
        Integer objectId = getReportOntologyObjectId(report, container);
        if (objectId != null)
            OntologyManager.deleteOntologyObjects(container, true, objectId);
    }
}

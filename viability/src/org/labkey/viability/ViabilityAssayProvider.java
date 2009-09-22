/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.viability;

import org.labkey.api.study.assay.*;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.util.Pair;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.security.User;
import org.labkey.api.query.FieldKey;
import org.labkey.api.qc.DataExchangeHandler;
import org.labkey.api.qc.TsvDataExchangeHandler;

import java.util.*;

/**
 * User: kevink
 * Date: Sep 15, 2009
 */
public class ViabilityAssayProvider extends AbstractAssayProvider
{
    public static final String NAME = "Viability";
    public static final String SPECIMENIDS_PROPERTY_NAME = "SpecimenIDs";
    public static final String SPECIMENIDS_PROPERTY_CAPTION = "Specimen IDs";

    public static final String POOL_ID_PROPERTY_NAME = "PoolID";
    public static final String POOL_ID_PROPERTY_CAPTION = "Pool ID";

    private static final String RESULT_DOMAIN_NAME = "Result Fields";
    public static final String RESULT_LSID_PREFIX = "ViabilityResult";

    public ViabilityAssayProvider()
    {
        super("ViabilityAssayProtocol", "ViabilityAssayRun", ViabilityTsvDataHandler.DATA_TYPE, new ViabilityAssayTableMetadata());
    }

    private static class ViabilityAssayTableMetadata extends AssayTableMetadata
    {
        public ViabilityAssayTableMetadata()
        {
            super(null, FieldKey.fromParts("Run"), FieldKey.fromParts("RowId"));
        }
    }

    /*package*/ static class ResultDomainProperty
    {
        public String name, label, description;
        public PropertyType type;

        public boolean hideInUploadWizard = false;
        public boolean editableInUploadWizard = false;
        public int inputLength = 9;

        public ResultDomainProperty(String name, String label, PropertyType type, String description)
        {
            this.name = name;
            this.label = label;
            this.description = description;
            this.type = type;
        }
    }

    /*package*/ static Map<String, ResultDomainProperty> RESULT_DOMAIN_PROPERTIES;

    static {
        ResultDomainProperty[] props = new ResultDomainProperty[] {
            new ResultDomainProperty(PARTICIPANTID_PROPERTY_NAME, PARTICIPANTID_PROPERTY_CAPTION, PropertyType.STRING, "Used with either " + VISITID_PROPERTY_NAME + " or " + DATE_PROPERTY_NAME + " to identify subject and timepoint for assay."),
            new ResultDomainProperty(VISITID_PROPERTY_NAME,  VISITID_PROPERTY_CAPTION, PropertyType.DOUBLE, "Used with " + PARTICIPANTID_PROPERTY_NAME + " to identify subject and timepoint for assay."),
            new ResultDomainProperty(DATE_PROPERTY_NAME,  DATE_PROPERTY_CAPTION, PropertyType.DATE_TIME, "Used with " + PARTICIPANTID_PROPERTY_NAME + " to identify subject and timepoint for assay."),
            new ResultDomainProperty(SPECIMENIDS_PROPERTY_NAME,  SPECIMENIDS_PROPERTY_CAPTION, PropertyType.STRING, "When a matching specimen exists in a study, can be used to identify subject and timepoint for assay."),

            new ResultDomainProperty(POOL_ID_PROPERTY_NAME, POOL_ID_PROPERTY_CAPTION, PropertyType.STRING, "Unique identifier for each pool of specimens"),
            new ResultDomainProperty("TotalCells", "Total Cells", PropertyType.INTEGER, "Total cell count"),
            new ResultDomainProperty("ViableCells", "Viable Cells", PropertyType.INTEGER, "Total viable cell count"),
            // XXX: not in db, should be in domain?
            new ResultDomainProperty("Viability", "Viability", PropertyType.DOUBLE, "Percent viable cell count"),
            new ResultDomainProperty("Recovery", "Recovery", PropertyType.DOUBLE, "Percent recovered cell count (viable cells / (sum of specimen vials original cell count)"),
        };

        LinkedHashMap<String, ResultDomainProperty> map = new LinkedHashMap<String, ResultDomainProperty>();
        for (ResultDomainProperty prop : props)
        {
            map.put(prop.name, prop);
        }

        map.get(POOL_ID_PROPERTY_NAME).hideInUploadWizard = true;
        map.get("Viability").hideInUploadWizard = true;
        map.get("Recovery").hideInUploadWizard = true;

        map.get(PARTICIPANTID_PROPERTY_NAME).editableInUploadWizard = true;
        map.get(VISITID_PROPERTY_NAME).editableInUploadWizard = true;
        map.get(DATE_PROPERTY_NAME).editableInUploadWizard = true;
        map.get(SPECIMENIDS_PROPERTY_NAME).editableInUploadWizard = true;

        RESULT_DOMAIN_PROPERTIES = Collections.unmodifiableMap(map);
    }

    public String getName()
    {
        return NAME;
    }

    public String getDescription()
    {
        return "Imports Guava cell count and viability data.";
    }

    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new JspView<AssayRunUploadForm>("/org/labkey/study/assay/view/tsvDataDescription.jsp", form);
    }

    public TableInfo createDataTable(AssaySchema schema, ExpProtocol protocol)
    {
        ViabilityAssaySchema viabilitySchema = new ViabilityAssaySchema(schema.getUser(), schema.getContainer(), protocol);
        viabilitySchema.setTargetStudy(schema.getTargetStudy());
        AbstractTableInfo table = viabilitySchema.createResultsTable();
        addCopiedToStudyColumns(table, protocol, schema.getUser(), "rowId", true);
        return table;
    }

    public ExpData getDataForDataRow(Object resultRowId)
    {
        if (resultRowId == null)
            return null;

        Integer id;
        if (resultRowId instanceof Integer)
        {
            id = (Integer)resultRowId;
        }
        else
        {
            try
            {
                id = Integer.parseInt(resultRowId.toString());
            }
            catch (NumberFormatException nfe)
            {
                return null;
            }
        }

        return ViabilityManager.getResultExpData(id.intValue());
    }

    @Override
    protected Map<String, Set<String>> getRequiredDomainProperties()
    {
        Map<String, Set<String>> domainMap = super.getRequiredDomainProperties();
        domainMap.put(RESULT_DOMAIN_NAME, RESULT_DOMAIN_PROPERTIES.keySet());
        return domainMap;
    }

    @Override
    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);

        Pair<Domain, Map<DomainProperty, Object>> resultDomain = createResultDomain(c, user);
        if (resultDomain != null)
            result.add(resultDomain);

        return result;
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createResultDomain(Container c, User user)
    {
        String lsid = getPresubstitutionLsid(ExpProtocol.ASSAY_DOMAIN_DATA);
        Domain resultDomain = PropertyService.get().createDomain(c, lsid, RESULT_DOMAIN_NAME);
        resultDomain.setDescription("The user is prompted to enter data values for row of data associated with a run, typically done as uploading a file.  This is part of the second step of the upload process.");

        for (ResultDomainProperty rdp : RESULT_DOMAIN_PROPERTIES.values())
        {
            DomainProperty dp = addProperty(resultDomain, rdp.name, rdp.label, rdp.type, rdp.description);
            PropertyDescriptor pd = dp.getPropertyDescriptor();
            pd.setInputLength(rdp.inputLength);
        }

        return new Pair<Domain, Map<DomainProperty, Object>>(resultDomain, Collections.<DomainProperty, Object>emptyMap());
    }

    public DomainProperty[] getResultDomainUserProperties(ExpProtocol protocol)
    {
        Domain resultsDomain = getResultsDomain(protocol);
        DomainProperty[] allProperties = resultsDomain.getProperties();
        List<DomainProperty> filtered = new ArrayList<DomainProperty>(allProperties.length);
        for (DomainProperty property : allProperties)
        {
            if (RESULT_DOMAIN_PROPERTIES.containsKey(property.getName()))
                continue;
            filtered.add(property);
        }
        return filtered.toArray(new DomainProperty[filtered.size()]);
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Collections.emptyList();
    }

    public ActionURL copyToStudy(User user, ExpProtocol protocol, Container study, Map<Integer, AssayPublishKey> dataKeys, List<String> errors)
    {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public DataExchangeHandler getDataExchangeHandler()
    {
        return new TsvDataExchangeHandler();
    }

    @Override
    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, ViabilityAssayUploadWizardAction.class);
    }

    /*
    @Override
    protected void addOutputDatas(AssayRunUploadContext context, Map<ExpData, String> outputDatas, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        ViabilityAssayRunUploadForm form = (ViabilityAssayRunUploadForm)context;

        Map<String, File> files;
        try
        {
            files = context.getUploadedData();
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
        if (files.size() == 0)
        {
            throw new IllegalStateException("AssayRunUploadContext " + context + " provided no upload data");
        }

        for (Map.Entry<String, File> entry : files.entrySet())
        {
            ExpData data = createData(context.getContainer(), entry.getValue(), entry.getKey(), _dataType);
            outputDatas.put(data, "Data");
        }
    }
    */
}

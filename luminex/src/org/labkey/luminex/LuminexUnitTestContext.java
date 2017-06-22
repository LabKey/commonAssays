/*
 * Copyright (c) 2013-2017 LabKey Corporation
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

package org.labkey.luminex;

import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.User;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.pipeline.AssayRunAsyncContext;
import org.labkey.api.view.ActionURL;
import org.labkey.luminex.model.SinglePointControl;
import org.labkey.luminex.model.Titration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LuminexUnitTestContext extends AssayRunUploadForm<LuminexAssayProvider> implements LuminexRunContext
{
    private List<Titration> _titrations = new ArrayList<>();
    private List<SinglePointControl> _singlePointControls = new ArrayList<>();

    public LuminexUnitTestContext()
    {
        Titration toAdd = new Titration();
        toAdd.setName("Titration 1");
        toAdd.setQcControl(false);
        toAdd.setOtherControl(true);
        toAdd.setStandard(true);
        toAdd.setUnknown(false);
        _titrations.add(toAdd);

        toAdd.setName("Titration 2");
        _titrations.add(toAdd);

        SinglePointControl toAddSPC = new SinglePointControl();
        toAddSPC.setName("Single Point Control 1");
        _singlePointControls.add(toAddSPC);
    }

    @Override
    public String[] getAnalyteNames()
    {
        String[] ret = new String[2];
        ret[0] = "Analyte 1";   ret[1] = "Analyte 2";
        return ret;
    }

    @Override
    public Map<DomainProperty, String> getAnalyteProperties(String analyteName)
    {
        Map<DomainProperty, String> ret = new HashMap<>();

        Mockery mock = new Mockery();
        mock.setImposteriser(ClassImposteriser.INSTANCE);

        final Domain domain = mock.mock(Domain.class);
        mock.checking(new Expectations()
        {{
                allowing(domain).getTypeId();
                will(returnValue(1));
        }});

        final DomainProperty domainProperty = mock.mock(DomainProperty.class);
        mock.checking(new Expectations()
        {{
                allowing(domainProperty).getName();
                will(returnValue("Name"));
                allowing(domainProperty).getLabel();
                will(returnValue("Name"));
                allowing(domainProperty).getPropertyId();
                will(returnValue(1));
                allowing(domainProperty).getDomain();
                will(returnValue(domain));
        }});

        ret.put(domainProperty, "Name of the Project");

        return ret;
    }

    @Override
    public ActionURL getActionURL()
    {
        return null;
    }

    @Override
    public Map<DomainProperty, String> getBatchProperties()
    {
        Map<DomainProperty, String> ret = new HashMap<>();

        Mockery mock = new Mockery();
        mock.setImposteriser(ClassImposteriser.INSTANCE);

        final Domain domain = mock.mock(Domain.class);
        mock.checking(new Expectations()
        {{
            allowing(domain).getTypeId();
            will(returnValue(1));
        }});
        final DomainProperty domainProperty = mock.mock(DomainProperty.class);
        mock.checking(new Expectations()
        {{
                allowing(domainProperty).getName();
                will(returnValue("BatchName"));
                allowing(domainProperty).getLabel();
                will(returnValue("Batch Name"));
                allowing(domainProperty).getPropertyId();
                will(returnValue(1));
                allowing(domainProperty).getDomain();
                will(returnValue(domain));
            }});

        ret.put(domainProperty, "Name of the Batch");

        return ret;
    }

    @NotNull
    @Override
    public LuminexAssayProvider getProvider()
    {
        Mockery mock = new Mockery();
        mock.setImposteriser(ClassImposteriser.INSTANCE);

        final LuminexAssayProvider provider = mock.mock(LuminexAssayProvider.class);
        mock.checking(new Expectations()
        {{
                allowing(provider).getDataCollectors(with(any(Map.class)), with(any(AssayRunUploadForm.class)));
                will(returnValue(Collections.emptyList()));
        }});
        return provider;
    }

    @Override
    public Map<DomainProperty, String> getRunProperties()
    {
        Map<DomainProperty, String> ret = new HashMap<>();

        Mockery mock = new Mockery();
        mock.setImposteriser(ClassImposteriser.INSTANCE);

        final Domain domain = mock.mock(Domain.class);
        mock.checking(new Expectations()
        {{
                allowing(domain).getTypeId();
                will(returnValue(1));
        }});

        final DomainProperty domainProperty = mock.mock(DomainProperty.class);
        mock.checking(new Expectations()
        {{
                allowing(domainProperty).getName();
                will(returnValue("RunName"));
                allowing(domainProperty).getLabel();
                will(returnValue("Run Name"));
                allowing(domainProperty).getPropertyId();
                will(returnValue(1));
                allowing(domainProperty).getDomain();
                will(returnValue(domain));
            }});

        ret.put(domainProperty, "Name of the Run");

        return ret;
    }

    @Override
    public Map<ColumnInfo, String> getAnalyteColumnProperties(String analyteName)
    {
        Map<ColumnInfo, String> ret = new HashMap<>();
        ColumnInfo item = new ColumnInfo("PositivityThreshold");
        ret.put(item, "50.0");
        ColumnInfo item2 = new ColumnInfo("NegativeBead");
        ret.put(item2, "Blank (3)");

        return ret;
    }

    @Override
    public Set<String> getTitrationsForAnalyte(String analyteName) throws ExperimentException
    {
        Set<String> set = new HashSet<>();
       
        if(analyteName.equals("Analyte 1"))
        {
            set.add("Titration 1");
            return set;
        }
        else
        {
            set.add("Titration 2");
            return set;
        }
    }

    @Override
    public List<Titration> getTitrations() throws ExperimentException
    {
        return _titrations;
    }

    @Override
    public User getUser()
    {
        User user = new User();
        user.setUserId(148841);

        return user;
    }

    @Override
    public String getName()
    {
        return "Log Test";
    }

    @Override
    public String getComments()
    {
        return "Test Comments";
    }

    @Override @NotNull
    public ExpProtocol getProtocol()
    {
        Mockery mock = new Mockery();
        mock.setImposteriser(ClassImposteriser.INSTANCE);

        final ExpProtocol expProtocol = mock.mock(ExpProtocol.class);
        mock.checking(new Expectations()
        {{
                allowing(expProtocol).getRowId();
                will(returnValue(100));
                allowing(expProtocol).getName();
                will(returnValue(AssayRunAsyncContext.UNIT_TESTING_PROTOCOL_NAME));
         }});
        return expProtocol;
    }

    @Override @NotNull
    public Map<String, File> getUploadedData()
    {
        Map<String, File> map = new HashMap<>();
        map.put("New File", new File("New"));
        return map;
    }


    @Override
    public List<SinglePointControl> getSinglePointControls() throws ExperimentException
    {
        return _singlePointControls;
    }

    @Override
    public LuminexExcelParser getParser() throws ExperimentException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

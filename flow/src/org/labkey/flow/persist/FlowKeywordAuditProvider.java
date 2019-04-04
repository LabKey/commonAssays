/*
 * Copyright (c) 2018 LabKey Corporation
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
package org.labkey.flow.persist;

import org.labkey.api.audit.AbstractAuditTypeProvider;
import org.labkey.api.audit.AuditTypeEvent;
import org.labkey.api.audit.AuditTypeProvider;
import org.labkey.api.audit.query.AbstractAuditDomainKind;
import org.labkey.api.audit.query.DefaultAuditTypeTable;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.UserSchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlowKeywordAuditProvider extends AbstractAuditTypeProvider implements AuditTypeProvider
{
    public static final String EVENT_TYPE = "FlowKeyword";

    public static final String COLUMN_NAME_DIRECTORY = "Directory";
    public static final String COLUMN_NAME_FILE = "File";
    public static final String COLUMN_NAME_KEYWORD_NAME = "KeywordName";
    public static final String COLUMN_NAME_KEYWORD_OLD_VALUE = "OldValue";
    public static final String COLUMN_NAME_KEYWORD_NEW_VALUE = "NewValue";
    public static final String COLUMN_NAME_LSID = "LSID";

    static final List<FieldKey> defaultVisibleColumns = new ArrayList<>();

    static {
        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_CREATED));
        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_CREATED_BY));
        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_IMPERSONATED_BY));
        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_FILE));
        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_KEYWORD_NAME));
        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_KEYWORD_OLD_VALUE));
        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_KEYWORD_NEW_VALUE));
    }

    @Override
    public TableInfo createTableInfo(UserSchema userSchema, ContainerFilter cf)
    {
        DefaultAuditTypeTable table = new DefaultAuditTypeTable(this, createStorageTableInfo(), userSchema, cf, getDefaultVisibleColumns());


        DetailsURL url = DetailsURL.fromString("experiment/resolveLSID.view?lsid=${lsid}");
        url.setStrictContainerContextEval(true);
        table.setDetailsURL(url);
        table.getColumn(COLUMN_NAME_FILE).setURL(url);
        table.getColumn(COLUMN_NAME_FILE).setURLTargetWindow("_blank");

        return table;
    }

    @Override
    protected AbstractAuditDomainKind getDomainKind()
    {
        return new FlowKeywordAuditDomainKind();
    }

    @Override
    public String getEventName()
    {
        return EVENT_TYPE;
    }

    @Override
    public String getLabel()
    {
        return "Flow events";
    }

    @Override
    public String getDescription()
    {
        return "Displays information about keyword changes.";
    }

    @Override
    public Map<FieldKey, String> legacyNameMap()
    {
        Map<FieldKey, String> legacyNames = super.legacyNameMap();
        legacyNames.put(FieldKey.fromParts("key1"), COLUMN_NAME_DIRECTORY);
        legacyNames.put(FieldKey.fromParts("key2"), COLUMN_NAME_FILE);
        legacyNames.put(FieldKey.fromParts("key3"), COLUMN_NAME_KEYWORD_NAME);
        return legacyNames;
    }

    @Override
    public <K extends AuditTypeEvent> Class<K> getEventClass()
    {
        return (Class<K>)FlowKeywordAuditEvent.class;
    }

    @Override
    public List<FieldKey> getDefaultVisibleColumns()
    {
        return defaultVisibleColumns;
    }

    public static class FlowKeywordAuditEvent extends AuditTypeEvent
    {
        private String _file;           // the file name
        private String _keywordName;   // the webdav resource path
        private String _oldValue;
        private String _newValue;
        private String _lsid;

        public FlowKeywordAuditEvent()
        {
            super();
        }

        public FlowKeywordAuditEvent(String container, String comment)
        {
            super(EVENT_TYPE, container, comment);
        }

        public String getFile()
        {
            return _file;
        }

        public void setFile(String file)
        {
            _file = file;
        }

        public String getKeywordName()
        {
            return _keywordName;
        }

        public void setKeywordName(String keywordName)
        {
            _keywordName = keywordName;
        }

        public String getOldValue()
        {
            return _oldValue;
        }

        public void setOldValue(String oldValue)
        {
            _oldValue = oldValue;
        }

        public String getNewValue()
        {
            return _newValue;
        }

        public void setNewValue(String newValue)
        {
            _newValue = newValue;
        }

        public void setLsid(String lsid)
        {
            _lsid = lsid;
        }

        public String getLsid()
        {
            return _lsid;
        }

        @Override
        public Map<String, Object> getAuditLogMessageElements()
        {
            Map<String, Object> elements = new LinkedHashMap<>();
            elements.put("file", getFile());
            elements.put("keywordName", getKeywordName());
            elements.put("oldValue", getOldValue());
            elements.put("newValue", getNewValue());
            elements.put("lsid", getLsid());
            elements.putAll(super.getAuditLogMessageElements());
            return elements;
        }
    }

    public static class FlowKeywordAuditDomainKind extends AbstractAuditDomainKind
    {
        public static final String NAME = "FlowKeywordAuditDomain";
        public static String NAMESPACE_PREFIX = "Audit-" + NAME;

        private final Set<PropertyDescriptor> _fields;

        public FlowKeywordAuditDomainKind()
        {
            super(EVENT_TYPE);

            Set<PropertyDescriptor> fields = new LinkedHashSet<>();
            fields.add(createPropertyDescriptor(COLUMN_NAME_DIRECTORY, PropertyType.STRING));
            fields.add(createPropertyDescriptor(COLUMN_NAME_FILE, PropertyType.STRING));
            fields.add(createPropertyDescriptor(COLUMN_NAME_KEYWORD_NAME, PropertyType.STRING));
            fields.add(createPropertyDescriptor(COLUMN_NAME_KEYWORD_OLD_VALUE, PropertyType.STRING));
            fields.add(createPropertyDescriptor(COLUMN_NAME_KEYWORD_NEW_VALUE, PropertyType.STRING));
            fields.add(createPropertyDescriptor(COLUMN_NAME_LSID, PropertyType.STRING));
            _fields = Collections.unmodifiableSet(fields);
        }

        @Override
        public Set<PropertyDescriptor> getProperties()
        {
            return _fields;
        }

        @Override
        protected String getNamespacePrefix()
        {
            return NAMESPACE_PREFIX;
        }

        @Override
        public String getKindName()
        {
            return NAME;
        }
    }
}


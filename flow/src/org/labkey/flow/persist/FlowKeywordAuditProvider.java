package org.labkey.flow.persist;

import org.labkey.api.audit.AbstractAuditTypeProvider;
import org.labkey.api.audit.AuditTypeEvent;
import org.labkey.api.audit.AuditTypeProvider;
import org.labkey.api.audit.query.AbstractAuditDomainKind;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.query.FieldKey;

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
        private String _directory;      // the directory name
        private String _file;           // the file name
        private String _keywordName;   // the webdav resource path
        private String _oldValue;
        private String _newValue;

        public FlowKeywordAuditEvent()
        {
            super();
        }

        public FlowKeywordAuditEvent(String container, String comment)
        {
            super(EVENT_TYPE, container, comment);
        }

        public String getDirectory()
        {
            return _directory;
        }

        public void setDirectory(String directory)
        {
            _directory = directory;
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

        @Override
        public Map<String, Object> getAuditLogMessageElements()
        {
            Map<String, Object> elements = new LinkedHashMap<>();
            elements.put("directory", getDirectory());
            elements.put("file", getFile());
            elements.put("keywordName", getKeywordName());
            elements.put("oldValue", getOldValue());
            elements.put("newValue", getNewValue());
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


package org.labkey.elispot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.EnumTableInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProviderSchema;

import java.util.Collections;
import java.util.Set;

/**
 * Created by klum on 12/23/2014.
 */
public class ElispotProviderSchema extends AssayProviderSchema
{
    public static final String SCHEMA_NAME = "ELISpot";
    public static final String ELISPOT_PLATE_READER_TABLE = "ElispotPlateReader";

    public ElispotProviderSchema(User user, Container container, ElispotAssayProvider provider, @Nullable Container targetStudy)
    {
        super(user, container, provider, targetStudy);
    }

    @NotNull
    @Override
    public ElispotAssayProvider getProvider()
    {
        return (ElispotAssayProvider)super.getProvider();
    }

    @Override
    public Set<String> getTableNames()
    {
        return Collections.singleton(ELISPOT_PLATE_READER_TABLE);
    }

    @Override
    public TableInfo createTable(String name)
    {
        if (name.equalsIgnoreCase(ELISPOT_PLATE_READER_TABLE))
        {
            EnumTableInfo<ElispotAssayProvider.PlateReaderType> result = new EnumTableInfo<>(ElispotAssayProvider.PlateReaderType.class, getDbSchema(), new EnumTableInfo.EnumValueGetter<ElispotAssayProvider.PlateReaderType>()
            {
                public String getValue(ElispotAssayProvider.PlateReaderType e)
                {
                    return e.getLabel();
                }
            }, false, "List of possible plate reader types for the ELISpot assay.");
            result.setPublicSchemaName(this.getSchemaName());
            result.setPublicName(ELISPOT_PLATE_READER_TABLE);

            return result;
        }
        return super.createTable(name);
    }
}

package org.labkey.microarray.assay;

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayResultTable;
import org.labkey.microarray.MicroarraySchema;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * User: jeckels
 * Date: 10/19/12
 */
public class MicroarrayProtocolSchema extends AssayProtocolSchema
{
    public MicroarrayProtocolSchema(User user, Container container, ExpProtocol protocol, Container targetStudy)
    {
        super(user, container, protocol, targetStudy);
    }

    @Override
    public ExpRunTable createRunsTable()
    {
        ExpRunTable result = super.createRunsTable();

        new MicroarraySchema(getUser(), getContainer()).configureRunsTable(result);
        if (getProvider().isEditableRuns(getProtocol()))
        {
            result.addAllowablePermission(UpdatePermission.class);
        }

        return result;
    }

    @Override
    public AssayResultTable createDataTable(boolean includeCopiedToStudyColumns)
    {
        AssayResultTable result = new AssayResultTable(this, includeCopiedToStudyColumns);
        if (AbstractAssayProvider.getDomainByPrefix(getProtocol(), ExpProtocol.ASSAY_DOMAIN_DATA).getProperties().length > 0)
        {
            List<FieldKey> cols = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());
            Iterator<FieldKey> iterator = cols.iterator();
            while (iterator.hasNext())
            {
                FieldKey key = iterator.next();
                if ("Run".equals(key.getParts().get(0)))
                {
                    iterator.remove();
                }
            }
            result.setDefaultVisibleColumns(cols);
        }

        return result;
    }
}

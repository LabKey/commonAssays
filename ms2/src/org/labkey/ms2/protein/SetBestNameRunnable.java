package org.labkey.ms2.protein;

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Table;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.ms2.MS2Controller;

import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Mar 12, 2008
 */
public class SetBestNameRunnable implements Runnable
{
    private final int[] _fastaIds;
    private final MS2Controller.SetBestNameForm.NameType _nameType;

    public SetBestNameRunnable(int[] fastaIds, MS2Controller.SetBestNameForm.NameType nameType)
    {
        _fastaIds = fastaIds;
        _nameType = nameType;
    }

    public void run()
    {
        try
        {
            for (int fastaId : _fastaIds)
            {
                SQLFragment identifierSQL;
                switch (_nameType)
                {
                    case IPI:
                        identifierSQL = new SQLFragment();
                        identifierSQL.append("SELECT MAX(i.Identifier) FROM " + ProteinManager.getTableInfoIdentifiers() + " i, ");
                        identifierSQL.append(ProteinManager.getTableInfoIdentTypes() + " it WHERE i.IdentTypeId = it.IdentTypeId AND it.Name = '");
                        identifierSQL.append(IdentifierType.IPI + "' AND " + ProteinManager.getTableInfoSequences() + ".SeqId = i.SeqId");
                        break;
                    case SWISS_PROT:
                        identifierSQL = new SQLFragment();
                        identifierSQL.append("SELECT MAX(i.Identifier) FROM " + ProteinManager.getTableInfoIdentifiers() + " i, ");
                        identifierSQL.append(ProteinManager.getTableInfoIdentTypes() + " it WHERE i.IdentTypeId = it.IdentTypeId AND it.Name = '");
                        identifierSQL.append(IdentifierType.SwissProt + "' AND " + ProteinManager.getTableInfoSequences() + ".SeqId = i.SeqId");
                        break;
                    case SWISS_PROT_ACCN:
                        identifierSQL = new SQLFragment();
                        identifierSQL.append("SELECT MAX(i.Identifier) FROM " + ProteinManager.getTableInfoIdentifiers() + " i, ");
                        identifierSQL.append(ProteinManager.getTableInfoIdentTypes() + " it WHERE i.IdentTypeId = it.IdentTypeId AND it.Name = '");
                        identifierSQL.append(IdentifierType.SwissProtAccn + "' AND " + ProteinManager.getTableInfoSequences() + ".SeqId = i.SeqId");
                        break;
                    case LOOKUP_STRING:
                        identifierSQL = new SQLFragment();
                        String nameSubstring = ProteinManager.getSqlDialect().getSubstringFunction("MAX(fs.LookupString)", "0", "52");
                        identifierSQL.append("SELECT " + nameSubstring + " FROM " + ProteinManager.getTableInfoFastaSequences() + " fs ");
                        identifierSQL.append(" WHERE fs.SeqId = " + ProteinManager.getTableInfoSequences() + ".SeqId");
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected NameType: " + _nameType);
                }

                SQLFragment sql = new SQLFragment("UPDATE " + ProteinManager.getTableInfoSequences() + " SET BestName = (");
                sql.append(identifierSQL);
                sql.append(") WHERE " + ProteinManager.getTableInfoSequences() + ".SeqId IN (SELECT fs.SeqId FROM " + ProteinManager.getTableInfoFastaSequences() + " fs WHERE FastaId = " + fastaId + ") AND ");
                sql.append("(");
                sql.append(identifierSQL);
                sql.append(") IS NOT NULL");
                Table.execute(ProteinManager.getSchema(), sql);
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }
}

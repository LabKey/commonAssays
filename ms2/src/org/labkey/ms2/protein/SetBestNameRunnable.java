/*
 * Copyright (c) 2008-2013 LabKey Corporation
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

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.ms2.MS2Controller;

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
                case GEN_INFO:
                    identifierSQL = new SQLFragment();
                    identifierSQL.append("SELECT MAX(i.Identifier) FROM " + ProteinManager.getTableInfoIdentifiers() + " i, ");
                    identifierSQL.append(ProteinManager.getTableInfoIdentTypes() + " it WHERE i.IdentTypeId = it.IdentTypeId AND it.Name = '");
                    identifierSQL.append(IdentifierType.GI + "' AND " + ProteinManager.getTableInfoSequences() + ".SeqId = i.SeqId");
                    break;
                case LOOKUP_STRING:
                    identifierSQL = new SQLFragment();
                    String nameSubstring = ProteinManager.getSqlDialect().getSubstringFunction("MAX(fs.LookupString)", "0", "50");
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
            new SqlExecutor(ProteinManager.getSchema()).execute(sql);
        }
    }
}

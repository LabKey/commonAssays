/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

package org.labkey.ms2;

import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.security.User;
import org.labkey.ms2.query.MS2Schema;

import java.sql.SQLException;

public class MS2ServiceImpl implements MS2Service
{
    @Override
    public MS2Schema createSchema(User user, Container container)
    {
        return new MS2Schema(user, container);
    }

    @Override
    public void migrateRuns(int oldFastaId, int newFastaId) throws SQLException
    {
        SQLFragment mappingSQL = new SQLFragment("SELECT fs1.seqid AS OldSeqId, fs2.seqid AS NewSeqId\n");
        mappingSQL.append("FROM \n");
        mappingSQL.append("\t(SELECT ff.SeqId, s.Hash, ff.LookupString FROM " + ProteinSchema.getTableInfoFastaSequences() + " ff, " + ProteinSchema.getTableInfoSequences() + " s WHERE ff.SeqId = s.SeqId AND ff.FastaId = " + oldFastaId + ") fs1 \n");
        mappingSQL.append("\tLEFT OUTER JOIN \n");
        mappingSQL.append("\t(SELECT ff.SeqId, s.Hash, ff.LookupString FROM " + ProteinSchema.getTableInfoFastaSequences() + " ff, " + ProteinSchema.getTableInfoSequences() + " s WHERE ff.SeqId = s.SeqId AND ff.FastaId = " + newFastaId + ") fs2 \n");
        mappingSQL.append("\tON (fs1.Hash = fs2.Hash AND fs1.LookupString = fs2.LookupString)");

        SQLFragment missingCountSQL = new SQLFragment("SELECT COUNT(*) FROM (");
        missingCountSQL.append(mappingSQL);
        missingCountSQL.append(") Mapping WHERE OldSeqId IN (\n");
        missingCountSQL.append("(SELECT p.SeqId FROM " + MS2Manager.getTableInfoPeptides() + " p, " + MS2Manager.getTableInfoRuns() + " r WHERE p.run = r.Run AND r.FastaId = " + oldFastaId + ")\n");
        missingCountSQL.append("UNION\n");
        missingCountSQL.append("(SELECT pgm.SeqId FROM ").append(MS2Manager.getTableInfoProteinGroupMemberships()).append(" pgm, ").append(MS2Manager.getTableInfoProteinGroups()).append(" pg, ").append(MS2Manager.getTableInfoProteinProphetFiles()).append(" ppf, ").append(MS2Manager.getTableInfoRuns()).append(" r WHERE pgm.ProteinGroupId = pg.RowId AND pg.ProteinProphetFileId = ppf.RowId AND ppf.Run = r.Run AND r.FastaId = ").appendValue(oldFastaId).append("))\n");
        missingCountSQL.append("AND NewSeqId IS NULL");

        int missingCount = new SqlSelector(ProteinSchema.getSchema(), missingCountSQL).getObject(Integer.class);
        if (missingCount > 0)
        {
            throw new SQLException("There are " + missingCount + " protein sequences in the original FASTA file that are not in the new file");
        }

        SqlExecutor executor = new SqlExecutor(MS2Manager.getSchema());

        try (DbScope.Transaction transaction = MS2Manager.getSchema().getScope().ensureTransaction())
        {
            SQLFragment updatePeptidesSQL = new SQLFragment();
            updatePeptidesSQL.append("UPDATE " + MS2Manager.getTableInfoPeptidesData() + " SET SeqId = map.NewSeqId");
            updatePeptidesSQL.append("\tFROM " + MS2Manager.getTableInfoFractions() + " f \n");
            updatePeptidesSQL.append("\t, " + MS2Manager.getTableInfoRuns() + " r\n");
            updatePeptidesSQL.append("\t, " + MS2Manager.getTableInfoFastaRunMapping() + " frm\n");
            updatePeptidesSQL.append("\t, (");
            updatePeptidesSQL.append(mappingSQL);
            updatePeptidesSQL.append(") map \n");
            updatePeptidesSQL.append("WHERE f.Fraction = " + MS2Manager.getTableInfoPeptidesData() + ".Fraction\n");
            updatePeptidesSQL.append("\tAND r.Run = f.Run\n");
            updatePeptidesSQL.append("\tAND frm.Run = r.Run\n");
            updatePeptidesSQL.append("\tAND " + MS2Manager.getTableInfoPeptidesData() + ".SeqId = map.OldSeqId \n");
            updatePeptidesSQL.append("\tAND frm.FastaId = " + oldFastaId);

            executor.execute(updatePeptidesSQL);

            SQLFragment updateProteinsSQL = new SQLFragment();
            updateProteinsSQL.append("UPDATE " + MS2Manager.getTableInfoProteinGroupMemberships() + " SET SeqId= map.NewSeqId\n");
            updateProteinsSQL.append("FROM " + MS2Manager.getTableInfoProteinGroups() + " pg\n");
            updateProteinsSQL.append("\t, " + MS2Manager.getTableInfoProteinProphetFiles() + " ppf\n");
            updateProteinsSQL.append("\t, " + MS2Manager.getTableInfoRuns() + " r\n");
            updateProteinsSQL.append("\t, " + MS2Manager.getTableInfoFastaRunMapping() + " frm\n");
            updateProteinsSQL.append("\t, (");
            updateProteinsSQL.append(mappingSQL);
            updateProteinsSQL.append(") map \n");
            updateProteinsSQL.append("WHERE " + MS2Manager.getTableInfoProteinGroupMemberships() + ".ProteinGroupId = pg.RowId\n");
            updateProteinsSQL.append("\tAND pg.ProteinProphetFileId = ppf.RowId\n");
            updateProteinsSQL.append("\tAND r.Run = ppf.Run\n");
            updateProteinsSQL.append("\tAND frm.Run = r.Run\n");
            updateProteinsSQL.append("\tAND " + MS2Manager.getTableInfoProteinGroupMemberships() + ".SeqId = map.OldSeqId\n");
            updateProteinsSQL.append("\tAND frm.FastaId = " + oldFastaId);

            executor.execute(updateProteinsSQL);

            executor.execute("UPDATE " + MS2Manager.getTableInfoFastaRunMapping() + " SET FastaID = ? WHERE FastaID = ?", newFastaId, oldFastaId);
            transaction.commit();
        }
    }
}

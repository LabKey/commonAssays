/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
-- Simplify protein table names
ALTER TABLE prot.ProtAnnotations RENAME TO Annotations;
ALTER TABLE prot.ProtAnnotationTypes RENAME TO AnnotationTypes;
ALTER TABLE prot.ProtAnnotInsertions RENAME TO AnnotInsertions;
ALTER TABLE prot.ProteinDatabases RENAME TO FastaFiles;
ALTER TABLE prot.ProteinSequences RENAME TO FastaSequences;
ALTER TABLE prot.ProtFastas RENAME TO FastaLoads;
ALTER TABLE prot.ProtIdentifiers RENAME TO Identifiers;
ALTER TABLE prot.ProtIdentTypes RENAME TO IdentTypes;
ALTER TABLE prot.ProtInfoSources RENAME TO InfoSources;
ALTER TABLE prot.ProtOrganisms RENAME TO Organisms;
ALTER TABLE prot.ProtSequences RENAME TO Sequences;
ALTER TABLE prot.ProtSProtOrgMap RENAME TO SProtOrgMap;

-- Rename some columns
ALTER TABLE prot.FastaFiles RENAME DataBaseId TO FastaId;
ALTER TABLE prot.FastaFiles RENAME ProteinDataBase TO FileName;
ALTER TABLE ms2.MS2Runs RENAME DataBaseId TO FastaId;
ALTER TABLE prot.FastaSequences RENAME DataBaseId TO FastaId;

-- Drop obsolete table, PK and columns
DROP TABLE prot.ProteinNames;

ALTER TABLE prot.FastaSequences DROP CONSTRAINT PK_ProteinSequences;
ALTER TABLE prot.FastaSequences
    DROP COLUMN SequenceId,
    DROP COLUMN SequenceMass,
    DROP COLUMN Sequence;

-- Rename and rebuild Fasta admin view
DROP VIEW prot.ProteinDBs;

CREATE VIEW prot.FastaAdmin AS
    SELECT ff.FileName, ff.FastaId, ff.Loaded, runs.Runs
    FROM prot.FastaFiles ff LEFT OUTER JOIN
        (SELECT FastaId, COUNT(Run) AS Runs
        FROM ms2.MS2Runs
        GROUP BY FastaId) runs ON runs.FastaId = ff.FastaId;

-- Rebuild MS2ExperimentRuns view to pick up FastaId column name change
DROP VIEW ms2.MS2ExperimentRuns;

CREATE VIEW ms2.MS2ExperimentRuns AS
    SELECT ms2.MS2Runs.*, exp.ExperimentRun.RowId as ExperimentRunRowId, exp.Protocol.Name As ProtocolName
    FROM ms2.MS2Runs
        LEFT OUTER JOIN exp.ExperimentRun ON exp.ExperimentRun.LSID=ms2.MS2Runs.ExperimentRunLSID
        LEFT OUTER JOIN exp.Protocol ON exp.Protocol.LSID=exp.ExperimentRun.ProtocolLSID;

DROP VIEW ms2.MS2Peptides;

CREATE OR REPLACE VIEW ms2.MS2Peptides AS
 SELECT pep.*, seq.Description, seq.BestGeneName AS GeneName
   FROM ms2.MS2SimplePeptides pep
   LEFT JOIN prot.Sequences seq ON seq.SeqId = pep.SeqId;


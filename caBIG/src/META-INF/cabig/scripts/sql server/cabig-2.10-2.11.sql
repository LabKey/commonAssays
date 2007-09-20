/*
 * Copyright (c) 2007 LabKey Software Foundation
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


exec core.fn_dropifexists 'PeptideMemberships', 'cabig','VIEW'
go
exec core.fn_dropifexists 'ProteinGroupMembers', 'cabig','VIEW'
go
exec core.fn_dropifexists 'ProteinGroups', 'cabig','VIEW'
go
exec core.fn_dropifexists 'ProteinProphetFiles', 'cabig','VIEW'
go
exec core.fn_dropifexists 'ProtSequences', 'cabig','VIEW'
go
exec core.fn_dropifexists 'QuantSummaries', 'cabig','VIEW'
go
exec core.fn_dropifexists 'FastaSequences', 'cabig','VIEW'
go
exec core.fn_dropifexists 'FastaFiles', 'cabig','VIEW'
go
exec core.fn_dropifexists 'SpectraData', 'cabig','VIEW'
go
exec core.fn_dropifexists 'Modifications', 'cabig','VIEW'
go
exec core.fn_dropifexists 'XTandemScores', 'cabig','VIEW'
go
exec core.fn_dropifexists 'MascotScores', 'cabig','VIEW'
go
exec core.fn_dropifexists 'SequestScores', 'cabig','VIEW'
go
exec core.fn_dropifexists 'CometScores', 'cabig','VIEW'
go
exec core.fn_dropifexists 'PeptidesView', 'cabig','VIEW'
go
exec core.fn_dropifexists 'Fractions', 'cabig','VIEW'
go
exec core.fn_dropifexists 'MS2Runs', 'cabig','VIEW'
go



CREATE VIEW cabig.MS2Runs AS
SELECT mr.run, mr.description, mr.path, mr.filename,
	mr.type, mr.searchengine, mr.massspectype, mr.searchenzyme,
	mr.haspeptideprophet, mr.peptidecount, mr.spectrumcount, mr.negativehitcount,
	mr.fastaid, mr.status, mr.statusid,
	c.rowid AS containerid,
	er.rowid AS experimentrunid
FROM (ms2.runs mr
	INNER JOIN cabig.containers c ON (mr.container = c.entityid))
    	LEFT JOIN cabig.experimentrun er ON (er.lsid = mr.experimentrunlsid)
WHERE mr.deleted = 0
GO

CREATE VIEW cabig.Fractions AS
SELECT f.fraction, f.description, f.filename,
    	f.run, f.pepxmldatalsid, f.mzxmlurl
FROM ms2.Fractions f
	INNER JOIN cabig.MS2Runs mr on (f.run = mr.run)
GO

CREATE VIEW cabig.SpectraData AS
SELECT ((4294967296 * CAST(sd.fraction AS BigInt)) + sd.scan) AS spectrumid, sd.fraction, sd.scan, sd.spectrum
FROM ms2.spectradata sd	  
GO

CREATE VIEW cabig.PeptidesView AS
SELECT sp.rowid, sp.run, sp.fraction, sp.fractionname, sp.scan, sp.retentiontime, sp.charge,
	sp.ionpercent, sp.mass, sp.deltamass, sp.precursormass,
	sp.fractionaldeltamass, sp.fractionaldeltamassppm, sp.deltamassppm, sp.mz,
	sp.peptide, sp.proteinhits, sp.protein, sp.seqid, sp.sequenceposition,
	sp.prevaa, sp.trimmedpeptide, sp.nextaa, sp.strippedpeptide,

	-- quantitation
	sp.decimalratio, sp.heavy2lightratio, sp.heavyarea, sp.heavyfirstscan,
	sp.heavylastscan, sp.heavymass, sp.lightarea, sp.lightfirstscan,
	sp.lightlastscan, sp.lightmass, sp.ratio,

	-- peptide prophet
	sp.peptideprophet, sp.peptidepropheterrorrate, sp.prophetfval, sp.prophetdeltamass, sp.prophetnumtrypticterm, 
	sp.prophetnummissedcleav,

	sd.spectrumid

FROM (ms2.SimplePeptides sp	
	INNER JOIN cabig.ms2runs r ON (sp.run = r.run))
	LEFT JOIN cabig.SpectraData sd ON (sp.fraction = sd.fraction AND sp.scan = sd.scan)
GO

-- issue:  do we need to support XComet specifically?
CREATE VIEW cabig.XTandemScores AS
SELECT sp.rowid as xtandemscoreid, sp.hyper, sp.next, sp.b, sp.y, sp.expect 
FROM ms2.SimplePeptides sp	
	INNER JOIN cabig.ms2runs r ON (sp.run = r.run)
WHERE LOWER(SUBSTRING(r.type, 1, 7))='xtandem'
GO

CREATE VIEW cabig.MascotScores AS
SELECT sp.rowid as mascotscoreid, sp.ion, sp.[identity] as mascotidentity, sp.homology, sp.expect 
FROM ms2.SimplePeptides sp	
	INNER JOIN cabig.ms2runs r ON (sp.run = r.run)
WHERE LOWER(SUBSTRING(r.type, 1, 6))='mascot'
GO

CREATE VIEW cabig.SequestScores AS
SELECT sp.rowid AS sequestscoreid, sp.spscore, sp.sprank, sp.deltacn, sp.xcorr
FROM ms2.SimplePeptides sp	
	INNER JOIN cabig.ms2runs r ON (sp.run = r.run)
WHERE LOWER(SUBSTRING(r.type, 1, 7))='sequest'
GO

CREATE VIEW cabig.CometScores AS
SELECT sp.rowid as cometscoreid, sp.rawscore, sp.diffscore, sp.zscore 
FROM ms2.SimplePeptides sp	
	INNER JOIN cabig.ms2runs r ON (sp.run = r.run)
WHERE LOWER(SUBSTRING(r.type, 1, 5))='comet'
GO

CREATE VIEW cabig.Modifications AS
SELECT CAST(m.run AS BigInt) * 65536 + ASCII(AminoAcid) * 256 + ASCII(Symbol) AS ModificationId,
        m.run, m.aminoacid, m.massdiff, m.variable, m.symbol
FROM ms2.Modifications m 
	INNER JOIN cabig.ms2Runs r ON r.run = m.run
GO

CREATE VIEW cabig.FastaFiles AS
SELECT ff.FastaId, ff.FileName, ff.Loaded, ff.FileChecksum 
FROM prot.FastaFiles ff 
	INNER JOIN cabig.ms2Runs r ON r.FastaId = ff.FastaId
GO

CREATE VIEW cabig.FastaSequences AS
SELECT fs.FastaId, fs.LookupString, fs.SeqId , 
	(CAST((4294967296 * fs.FastaId) AS BigInt) + fs.seqid) AS FastaSequenceId
FROM prot.FastaSequences fs 
	INNER JOIN cabig.FastaFiles ff on (fs.FastaId = ff.FastaId)
GO

CREATE VIEW cabig.QuantSummaries AS
SELECT qs.QuantId, qs.Run, qs.AnalysisType, qs.AnalysisTime, qs.Version, qs.LabeledResidues, 
	qs.MassDiff, qs.MassTol, qs.SameScanRange, qs.XpressLight 
FROM ms2.QuantSummaries qs 
	INNER JOIN cabig.ms2Runs r on (r.run = qs.run)
GO


-- the protein sequences visible via cabig are those
-- described in a fasta file used by a run in a published container,
-- and also those that are not marked as deleted

CREATE VIEW cabig.ProtSequences AS
SELECT s.SeqId, s.ProtSequence, s.Hash, s.Description, 
	src.Name as SourceName, s.SourceVersion, src.Url as SourceUrl, s.InsertDate, s.OrgId, s.Mass, 
	s.BestName, s.BestGeneName, s.Length, o.CommonName as OrganismName, o.Genus, o.Species, o.Comments
FROM (prot.Sequences s
	INNER JOIN cabig.FastaSequences fs on (s.SeqId = fs.SeqId AND s.Deleted = 0 )
	-- join in Source info instead of exposing as a separate object
	INNER JOIN prot.InfoSources src on (src.SourceId = s.SourceId AND src.Deleted = 0)	)
	-- join in Org info if is is available
	LEFT JOIN prot.Organisms o ON (s.OrgId = o.OrgId AND o.Deleted = 0)
go

CREATE VIEW cabig.ProteinProphetFiles AS
SELECT pp.RowId, pp.FilePath, pp.Run, pp.UploadCompleted, pp.MinProbSeries, pp.SensitivitySeries, pp.ErrorSeries, 
	pp.PredictedNumberCorrectSeries, pp.PredictedNumberIncorrectSeries 
FROM ms2.ProteinProphetFiles pp 
	INNER JOIN cabig.ms2Runs r on (r.run = pp.run)
GO

CREATE VIEW cabig.ProteinGroups AS
SELECT pg.RowId, pg.GroupProbability, pg.ProteinProphetFileId, pp.Run, 
	pg.GroupNumber, pg.IndistinguishableCollectionId, 
	pg.UniquePeptidesCount, pg.TotalNumberPeptides, pg.PctSpectrumIds, pg.PercentCoverage, 
	pg.ProteinProbability, pg.ErrorRate
FROM (ms2.ProteinGroups pg 
	INNER JOIN cabig.ProteinProphetFiles pp ON pg.ProteinProphetFileId = pp.RowId )
	LEFT JOIN ms2.ProteinQuantitation pq ON pq.ProteinGroupId = pg.RowId	
go


CREATE VIEW cabig.ProteinGroupMembers AS
SELECT (CAST((4294967296 * pgm.ProteinGroupId) AS BigInt) + pgm.SeqId) AS GroupMemberId, 
	pgm.ProteinGroupId, pgm.Probability, s.*
FROM ms2.ProteinGroupMemberships pgm
	INNER JOIN cabig.ProtSequences s ON s.SeqId = pgm.SeqId 
	INNER JOIN cabig.ProteinGroups pg ON pgm.ProteinGroupId = pg.RowId
go

CREATE VIEW cabig.PeptideMemberships AS
SELECT pm.PeptideId, pm.ProteinGroupId, (CAST((4294967296 * pm.ProteinGroupId) AS BigInt) + pm.PeptideId) AS PeptideMemberId, 
	pm.NSPAdjustedProbability, pm.Weight, pm.NondegenerateEvidence, 
	pm.EnzymaticTermini, pm.SiblingPeptides, pm.SiblingPeptidesBin, pm.Instances, 
	pm.ContributingEvidence, pm.CalcNeutralPepMass 

FROM ms2.PeptideMemberships pm 
	INNER JOIN cabig.ProteinGroups pg ON pm.ProteinGroupId = pg.RowId

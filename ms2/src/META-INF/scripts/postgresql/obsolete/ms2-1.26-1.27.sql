ALTER TABLE ms2.ms2proteingroups ADD COLUMN proteinprobability REAL NOT NULL DEFAULT 0;

DROP TABLE ms2.ms2peptidememberships;

CREATE TABLE ms2.ms2peptidememberships
(
  peptideid int8 NOT NULL,
  proteingroupid int4 NOT NULL,
  nspadjustedprobability float4 NOT NULL,
  weight float4 NOT NULL,
  nondegenerateevidence bool NOT NULL,
  enzymatictermini int4 NOT NULL,
  siblingpeptides float4 NOT NULL,
  siblingpeptidesbin int4 NOT NULL,
  instances int4 NOT NULL,
  contributingevidence bool NOT NULL,
  calcneutralpepmass float4 NOT NULL,
  CONSTRAINT pk_ms2peptidememberships PRIMARY KEY (proteingroupid, peptideid),
  CONSTRAINT fk_ms2peptidemembership_ms2peptidesdata FOREIGN KEY (peptideid) REFERENCES ms2.ms2peptidesdata (rowid) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_ms2peptidemembership_ms2proteingroup FOREIGN KEY (proteingroupid) REFERENCES ms2.ms2proteingroups (rowid) ON UPDATE NO ACTION ON DELETE NO ACTION
); 
ALTER TABLE ms2.ms2proteingroups ADD proteinprobability REAL NOT NULL DEFAULT 0
GO

DROP TABLE ms2.ms2peptidememberships
GO

CREATE TABLE ms2.ms2peptidememberships
(
  peptideid BIGINT NOT NULL,
  proteingroupid INT NOT NULL,
  nspadjustedprobability REAL NOT NULL,
  weight REAL NOT NULL,
  nondegenerateevidence BIT NOT NULL,
  enzymatictermini INT NOT NULL,
  siblingpeptides REAL NOT NULL,
  siblingpeptidesbin INT NOT NULL,
  instances INT NOT NULL,
  contributingevidence BIT NOT NULL,
  calcneutralpepmass REAL NOT NULL,
  CONSTRAINT pk_ms2peptidememberships PRIMARY KEY (proteingroupid, peptideid),
  CONSTRAINT fk_ms2peptidemembership_ms2peptidesdata FOREIGN KEY (peptideid) REFERENCES ms2.ms2peptidesdata (rowid) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_ms2peptidemembership_ms2proteingroup FOREIGN KEY (proteingroupid) REFERENCES ms2.ms2proteingroups (rowid) ON UPDATE NO ACTION ON DELETE NO ACTION
)
GO

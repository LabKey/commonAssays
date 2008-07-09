-- DROP obsolete views.  Do not remove; these are needed when upgrading from older versions.
EXEC core.fn_dropifexists 'ProteinDBs', 'prot', 'VIEW', NULL
EXEC core.fn_dropifexists 'MS2Spectra', 'ms2', 'VIEW', NULL
EXEC core.fn_dropifexists 'MS2ExperimentRuns', 'ms2', 'VIEW', NULL
EXEC core.fn_dropifexists 'MS2Peptides', 'ms2', 'VIEW', NULL
EXEC core.fn_dropifexists 'MS2SimplePeptides', 'ms2', 'VIEW', NULL
GO

-- DROP current views.
EXEC core.fn_dropifexists 'Peptides', 'ms2', 'VIEW', NULL
EXEC core.fn_dropifexists 'SimplePeptides', 'ms2', 'VIEW', NULL
EXEC core.fn_dropifexists 'ProteinGroupsWithQuantitation', 'ms2', 'VIEW', NULL
EXEC core.fn_dropifexists 'ExperimentRuns', 'ms2', 'VIEW', NULL
EXEC core.fn_dropifexists 'FastaAdmin', 'prot', 'VIEW', NULL
EXEC core.fn_dropifexists 'Spectra', 'ms2', 'VIEW', NULL
GO

/* Create and Index on ms2.Runs(Container) to speed-up MS1 Feature to MS2 Peptide matching */
/* PostgreSQL Version */

SELECT core.fn_dropifexists('Runs', 'ms2', 'INDEX','MS2Runs_Container');

CREATE INDEX MS2Runs_Container ON ms2.Runs(Container);

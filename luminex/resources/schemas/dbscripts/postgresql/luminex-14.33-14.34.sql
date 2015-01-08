
ALTER TABLE luminex.GuideSet ADD IsTitration BOOLEAN;
UPDATE luminex.GuideSet SET IsTitration =
  (
  /* Set the control type to single point control if the GuideSet is used in the AnalyteSinglePointControl table.
     Default to setting the control type to titration if not found. */
    CASE WHEN RowId IN (SELECT DISTINCT GuideSetId FROM luminex.AnalyteSinglePointControl WHERE GuideSetId IS NOT NULL) THEN FALSE ELSE TRUE END
  )
;
ALTER TABLE luminex.GuideSet ALTER COLUMN IsTitration SET NOT NULL;
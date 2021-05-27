ALTER TABLE nab.nabspecimen ADD COLUMN FitParameters VARCHAR(500);
SELECT core.executeJavaUpgradeCode('populateFitParameters');
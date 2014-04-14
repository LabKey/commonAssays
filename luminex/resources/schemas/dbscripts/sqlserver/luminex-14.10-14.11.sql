
ALTER TABLE luminex.GuideSet ADD ValueBased BIT;
GO
UPDATE luminex.GuideSet SET ValueBased = 0;
ALTER TABLE luminex.GuideSet ALTER COLUMN ValueBased BIT NOT NULL;

ALTER TABLE luminex.GuideSet ADD EC504PLAverage REAL;
ALTER TABLE luminex.GuideSet ADD EC504PLStdDev REAL;
ALTER TABLE luminex.GuideSet ADD EC505PLAverage REAL;
ALTER TABLE luminex.GuideSet ADD EC505PLStdDev REAL;
ALTER TABLE luminex.GuideSet ADD AUCAverage REAL;
ALTER TABLE luminex.GuideSet ADD AUCStdDev REAL;
ALTER TABLE luminex.GuideSet ADD MaxFIAverage REAL;
ALTER TABLE luminex.GuideSet ADD MaxFIStdDev REAL;
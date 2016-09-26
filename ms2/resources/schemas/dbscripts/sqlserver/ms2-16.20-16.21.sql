-- Issue 27667 - Importing failed due to data type length limitation
ALTER TABLE ms2.Runs ALTER COLUMN mascotfile NVARCHAR(MAX);
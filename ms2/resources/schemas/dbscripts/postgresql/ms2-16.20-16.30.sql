/* ms2-16.20-16.21.sql */

-- Issue 27667 - Importing failed due to data type length limitation
ALTER TABLE ms2.Runs ALTER COLUMN mascotfile TYPE TEXT;
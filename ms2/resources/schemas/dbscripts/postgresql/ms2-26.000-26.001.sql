/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
ALTER TABLE ms2.Runs DROP COLUMN MascotFile;
ALTER TABLE ms2.Runs DROP COLUMN DistillerRawFile;

DELETE FROM prop.Properties WHERE Set IN (SELECT Set FROM prop.PropertySets WHERE Category = 'MascotConfig');
DELETE FROM prop.PropertySets WHERE Category = 'MascotConfig';

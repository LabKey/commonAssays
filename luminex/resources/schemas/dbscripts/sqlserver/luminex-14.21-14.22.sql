
ALTER TABLE luminex.Analyte ADD NegativeBead VARCHAR(50);
GO

-- assume that any run/dataid that has a "Blank" analyte was using that as the Negative Control bead
UPDATE luminex.analyte SET NegativeBead = (
  SELECT x.Name FROM luminex.analyte AS a
    JOIN (SELECT DISTINCT DataId, Name FROM luminex.analyte WHERE Name LIKE 'Blank %') AS x ON a.DataId = x.DataId
  WHERE a.DataId IN (SELECT DataId FROM luminex.analyte WHERE Name LIKE 'Blank %') AND a.Name NOT LIKE 'Blank %'
        AND luminex.analyte.RowId = a.RowId)
WHERE NegativeBead IS NULL;
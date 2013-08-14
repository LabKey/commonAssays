
SELECT spc.RunId, spc.Name, dr.Analyte, AVG(dr.FIBackground) FROM SinglePointControl AS spc, Data AS dr, exp.Data AS data
WHERE spc.name = dr.Description AND data.run = spc.RunId AND dr.data = data.RowId AND dr.FlaggedAsExcluded = FALSE
GROUP BY dr.Analyte, spc.RunId, spc.Name;

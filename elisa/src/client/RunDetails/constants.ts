export const X_AXIS_PROP = 'Concentration';
export const Y_AXIS_PROP = 'Absorption'; // TODO from Bob - Absorption could be changed to ECL
export const ID_COL_NAME = 'ID';
export const SAMPLE_COL_NAME = 'SpecimenLsid/Property/SpecimenId';
export const CONTROL_COL_NAME = 'ControlId';
export const SAMPLE_COLUMN_NAMES = [SAMPLE_COL_NAME, CONTROL_COL_NAME, 'WellLocation', Y_AXIS_PROP, X_AXIS_PROP, 'PlateName', 'Spot'];
export const HOVER_COLUMN_NAMES = [ID_COL_NAME, 'WellLocation', Y_AXIS_PROP, X_AXIS_PROP];
export const RUN_COLUMN_NAMES = ['Name','CurveFitMethod'];
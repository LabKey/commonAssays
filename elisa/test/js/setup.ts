/*
 * Copyright (c) 2020-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
import "@testing-library/jest-dom"; // add custom jest matchers from jest-dom
import { App } from "@labkey/components";

// Configure @labkey/components to recognize this as a testing environment
App.setIsTestEnv(true);

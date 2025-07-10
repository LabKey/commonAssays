import "@testing-library/jest-dom"; // add custom jest matchers from jest-dom
import { App } from "@labkey/components";

// Configure @labkey/components to recognize this as a testing environment
App.setIsTestEnv(true);

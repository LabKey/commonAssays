/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
module.exports = {
    globals: {
        LABKEY: {
            contextPath: ''
        }
    },
    moduleFileExtensions: [
        'tsx',
        'ts',
        'js'
    ],
    preset: 'ts-jest',
    setupFilesAfterEnv: [
        '<rootDir>/test/js/setup.ts'
    ],
    testEnvironment: 'jsdom',
    testMatch: null,
    testRegex: '(\\.(test))\\.(ts|tsx)$',
    testResultsProcessor: 'jest-teamcity-reporter',
    transform: {
        '^.+\\.tsx?$': [
            'ts-jest',
            {
                tsconfig: 'node_modules/@labkey/build/configs/tsconfig.test.json',
            }
        ]
    }
};

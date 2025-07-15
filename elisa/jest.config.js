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
                tsconfig: 'node_modules/@labkey/build/webpack/tsconfig.test.json',
            }
        ]
    }
};

import { createDefaultEsmPreset } from 'ts-jest';

const defaultEsmPreset = createDefaultEsmPreset();

/** @type {import('ts-jest').JestConfigWithTsJest} */
export default {
    // [...]
    ...defaultEsmPreset,
    moduleNameMapper: {
        '^(\\.{1,2}/.*)\\.js$': '$1',
    },
    collectCoverage: true,
    collectCoverageFrom: ["./src/**"],
    coverageReporters: ['html', "text-summary"],
    coverageDirectory: './.coverage'
};
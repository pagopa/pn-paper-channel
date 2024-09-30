import { createDefaultEsmPreset } from 'ts-jest';

const defaultEsmPreset = createDefaultEsmPreset();

/** @type {import('ts-jest').JestConfigWithTsJest} */
export default {
  // [...]
  ...defaultEsmPreset,
  moduleNameMapper: {
    '^(\\.{1,2}/.*)\\.js$': '$1',
    "^@middy/core$": "<rootDir>/node_modules/@middy/core",
  },
  collectCoverage: true,
  collectCoverageFrom: ["./src/**"],
  coverageReporters: ['html', "text-summary"],
  coverageDirectory: './.coverage'
};
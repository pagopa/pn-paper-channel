/** @type {import('ts-jest').JestConfigWithTsJest} **/
module.exports = {
  testEnvironment: "node",
  transform: {
    "^.+.tsx?$": ["ts-jest",{}],
  },
  collectCoverage: true,
  collectCoverageFrom: ["./src/**"],
  coverageReporters: ['html', "text-summary", "lcov"],
  coverageDirectory: './.coverage'
};
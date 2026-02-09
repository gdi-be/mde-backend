const Configuration = {
  extends: ["@commitlint/config-conventional"],
  rules: {
    'header-case': [2, 'always', ['lower-case', 'sentence-case', 'start-case']],
    'subject-case': [2, 'always', ['lower-case', 'sentence-case', 'start-case']]
  }
};

export default Configuration;

import antfu from "@antfu/eslint-config";

export default antfu({
  ignores: [
    "node_modules/**",
    "**/node_modules/**",
    "android/**",
    "dist/**",
    "coverage/**",
    ".agents/**",
    ".codeartsdoer/**",
    "docs/edu-auth/**",
  ],
  formatters: true,
  vue: true,
  stylistic: {
    indent: 2,
    quotes: "double",
    semi: true,
  },
  rules: {
    "semi": ["warn", "always"],
    "antfu/top-level-function": "off",
    "eslinttailwindcss/no-custom-classname": "off",
    "vue/max-lines-per-block": "off",
    "no-console": "off",
    "style/brace-style": [
      "error",
      "1tbs",
      { allowSingleLine: true },
    ],
    "node/prefer-global/process": "off",
  },
});

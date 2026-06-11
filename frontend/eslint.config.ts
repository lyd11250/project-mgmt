import pluginVue from 'eslint-plugin-vue'
import vueTsEslintConfig from '@vue/eslint-config-typescript'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

// ESLint 扁平配置（ESLint 9）。仅管「代码质量与规范」，格式交给 Prettier。
// 关键：vue/block-order 强制单文件组件块顺序为 template - script - style。
export default [
  {
    name: 'app/files-to-lint',
    files: ['**/*.{ts,mts,tsx,vue}'],
  },
  {
    name: 'app/files-to-ignore',
    ignores: ['dist/**', 'node_modules/**', 'public/**', '*.config.*'],
  },

  ...pluginVue.configs['flat/recommended'],
  ...vueTsEslintConfig(),

  {
    name: 'app/rules',
    rules: {
      // 单文件组件块顺序：template → script → style
      'vue/block-order': ['error', { order: ['template', 'script', 'style'] }],
      // 组件名允许单词（如 Home、Login 视图组件）
      'vue/multi-word-component-names': 'off',
    },
  },

  // 放在最后：关闭与 Prettier 冲突的格式类规则
  skipFormatting,
]

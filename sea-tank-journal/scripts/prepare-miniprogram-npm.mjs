/**
 * 将 Vant Weapp 声明的微信组件目录放入 DevTools 可解析的 npm 构建目录。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const packageDirectory = path.resolve(projectRoot, 'node_modules', '@vant', 'weapp', 'lib')
const outputDirectory = path.resolve(projectRoot, 'miniprogram_npm', '@vant', 'weapp')

export function prepareMiniProgramNpm() {
  const expectedOutput = path.join(projectRoot, 'miniprogram_npm', '@vant', 'weapp')
  if (outputDirectory !== expectedOutput || !outputDirectory.startsWith(`${projectRoot}${path.sep}`)) {
    throw new Error('Vant 小程序组件输出目录无效')
  }
  if (!fs.existsSync(packageDirectory)) throw new Error('未找到 Vant Weapp 小程序组件目录，请先执行 npm install')
  fs.rmSync(outputDirectory, { recursive: true, force: true })
  fs.mkdirSync(path.dirname(outputDirectory), { recursive: true })
  fs.cpSync(packageDirectory, outputDirectory, { recursive: true })
  process.stdout.write(`Vant Weapp 小程序组件已准备：${path.relative(projectRoot, outputDirectory)}\n`)
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) prepareMiniProgramNpm()

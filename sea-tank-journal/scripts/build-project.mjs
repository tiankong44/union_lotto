/**
 * 将原生页面和按需 Vant 组件组装为可导入的小程序目录。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const outputDirectory = path.resolve(projectRoot, 'dist')
if (outputDirectory !== path.join(projectRoot, 'dist')) throw new Error('构建目录不在小程序项目内')

fs.rmSync(outputDirectory, { recursive: true, force: true })
fs.mkdirSync(outputDirectory, { recursive: true })
copyProjectFiles(projectRoot, outputDirectory, true)

const vantLibrary = path.join(projectRoot, 'node_modules', '@vant', 'weapp', 'lib')
if (!fs.existsSync(vantLibrary)) throw new Error('未找到 Vant Weapp 构建目录，请先执行 npm install')
const miniProgramNpm = path.join(outputDirectory, 'miniprogram_npm', '@vant', 'weapp')
fs.mkdirSync(path.dirname(miniProgramNpm), { recursive: true })
fs.cpSync(vantLibrary, miniProgramNpm, { recursive: true })
process.stdout.write(`小程序构建文件已生成：${outputDirectory}\n`)

function copyProjectFiles(sourceDirectory, targetDirectory, isRoot = false) {
  for (const item of fs.readdirSync(sourceDirectory, { withFileTypes: true })) {
    if (isRoot && ['node_modules', 'dist', 'scripts', '.git'].includes(item.name)) continue
    if (isRoot && ['package.json', 'package-lock.json', 'tsconfig.json', 'README.md'].includes(item.name)) continue
    const source = path.join(sourceDirectory, item.name)
    const target = path.join(targetDirectory, item.name)
    if (item.isDirectory()) {
      fs.mkdirSync(target, { recursive: true })
      copyProjectFiles(source, target)
    } else {
      fs.copyFileSync(source, target)
    }
  }
}

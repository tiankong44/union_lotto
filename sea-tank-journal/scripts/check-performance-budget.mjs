/**
 * 按本次小程序构建产物检查主包大小预算。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const outputDirectory = path.join(projectRoot, 'dist')
const mainPackageLimit = 2 * 1024 * 1024

if (!fs.existsSync(outputDirectory)) throw new Error('请先运行 npm run build 生成本次检查产物')

function measure(directory) {
  let total = 0
  for (const item of fs.readdirSync(directory, { withFileTypes: true })) {
    const target = path.join(directory, item.name)
    total += item.isDirectory() ? measure(target) : fs.statSync(target).size
  }
  return total
}

const builtBytes = measure(outputDirectory)
process.stdout.write(`本次小程序主包产物：${(builtBytes / 1024).toFixed(1)} KB；检查上限：${(mainPackageLimit / 1024).toFixed(0)} KB。\n`)
if (builtBytes > mainPackageLimit) process.exitCode = 1

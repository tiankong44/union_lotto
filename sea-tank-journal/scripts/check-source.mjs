/**
 * 检查小程序页面配置、JSON 文件和 JavaScript 语法。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const appConfig = JSON.parse(fs.readFileSync(path.join(projectRoot, 'app.json'), 'utf8'))
const ignoredDirectories = new Set(['node_modules', 'dist', '.git'])
const jsonFiles = []
const javascriptFiles = []

function collectFiles(directory) {
  for (const item of fs.readdirSync(directory, { withFileTypes: true })) {
    if (item.isDirectory() && ignoredDirectories.has(item.name)) continue
    const absolutePath = path.join(directory, item.name)
    if (item.isDirectory()) collectFiles(absolutePath)
    else if (item.name.endsWith('.json')) jsonFiles.push(absolutePath)
    else if (item.name.endsWith('.js')) javascriptFiles.push(absolutePath)
  }
}

collectFiles(projectRoot)
for (const jsonPath of jsonFiles) JSON.parse(fs.readFileSync(jsonPath, 'utf8'))
for (const page of appConfig.pages) {
  const basePath = path.join(projectRoot, page)
  for (const extension of ['.js', '.json', '.wxml', '.wxss']) {
    if (!fs.existsSync(`${basePath}${extension}`)) throw new Error(`页面文件缺失：${page}${extension}`)
  }
}
for (const script of javascriptFiles) {
  const result = spawnSync(process.execPath, ['--check', script], { encoding: 'utf8' })
  if (result.status !== 0) throw new Error(`${path.relative(projectRoot, script)}\n${result.stderr}`)
}

process.stdout.write(`检查通过：${appConfig.pages.length} 个页面、${javascriptFiles.length} 个 JavaScript 文件和 ${jsonFiles.length} 个 JSON 文件。\n`)

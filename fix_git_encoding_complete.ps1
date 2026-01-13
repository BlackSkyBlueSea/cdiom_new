# ============================================
# Git 中文编码问题完整修复脚本
# ============================================

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Git 中文编码修复脚本" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# 1. 配置 Git 使用 UTF-8 编码
Write-Host "[1/4] 配置 Git 编码设置..." -ForegroundColor Yellow
git config --global core.quotepath false
git config --global i18n.commitencoding utf-8
git config --global i18n.logoutputencoding utf-8
git config --global core.autocrlf false

Write-Host "  ✓ core.quotepath: false" -ForegroundColor Green
Write-Host "  ✓ i18n.commitencoding: utf-8" -ForegroundColor Green
Write-Host "  ✓ i18n.logoutputencoding: utf-8" -ForegroundColor Green
Write-Host "  ✓ core.autocrlf: false" -ForegroundColor Green
Write-Host ""

# 2. 配置 PowerShell 使用 UTF-8 编码
Write-Host "[2/4] 配置 PowerShell 编码设置..." -ForegroundColor Yellow
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
chcp 65001 | Out-Null

Write-Host "  ✓ Console.OutputEncoding: UTF-8" -ForegroundColor Green
Write-Host "  ✓ OutputEncoding: UTF-8" -ForegroundColor Green
Write-Host "  ✓ 代码页: 65001 (UTF-8)" -ForegroundColor Green
Write-Host ""

# 3. 设置环境变量
Write-Host "[3/4] 设置环境变量..." -ForegroundColor Yellow
$env:LANG = "zh_CN.UTF-8"
$env:LC_ALL = "zh_CN.UTF-8"

Write-Host "  ✓ LANG: zh_CN.UTF-8" -ForegroundColor Green
Write-Host "  ✓ LC_ALL: zh_CN.UTF-8" -ForegroundColor Green
Write-Host ""

# 4. 显示当前提交信息
Write-Host "[4/4] 显示最新提交信息..." -ForegroundColor Yellow
Write-Host ""
Write-Host "提交哈希: " -NoNewline -ForegroundColor Cyan
git log -1 --format="%H"
Write-Host "作者: " -NoNewline -ForegroundColor Cyan
git log -1 --format="%an <%ae>"
Write-Host "提交时间: " -NoNewline -ForegroundColor Cyan
git log -1 --format="%ad" --date=format:"%Y年%m月%d日 %H:%M:%S"
Write-Host ""
Write-Host "提交信息: " -ForegroundColor Cyan
git log -1 --format="%B"
Write-Host ""

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "修复完成！" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "注意：" -ForegroundColor Yellow
Write-Host "  - 如果提交信息仍然显示乱码，说明提交时使用了错误的编码" -ForegroundColor Yellow
Write-Host "  - 可以使用以下命令修改最新提交信息：" -ForegroundColor Yellow
Write-Host "    git commit --amend -m '新的提交信息'" -ForegroundColor White
Write-Host "  - 或者使用交互式编辑器：" -ForegroundColor Yellow
Write-Host "    git commit --amend" -ForegroundColor White
Write-Host ""


# ============================================
# Git 中文编码问题完整解决方案
# ============================================

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Git 中文编码修复和配置脚本" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# 1. 配置 Git 使用 UTF-8
Write-Host "[1/3] 配置 Git 编码为 UTF-8..." -ForegroundColor Yellow
git config --global core.quotepath false
git config --global i18n.commitencoding utf-8
git config --global i18n.logoutputencoding utf-8
git config --global core.autocrlf false

Write-Host "  ✓ Git 编码配置完成" -ForegroundColor Green
Write-Host ""

# 2. 配置 PowerShell 编码
Write-Host "[2/3] 配置 PowerShell 编码为 UTF-8..." -ForegroundColor Yellow
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

Write-Host "  ✓ PowerShell 编码配置完成" -ForegroundColor Green
Write-Host ""

# 3. 显示当前提交信息（尝试用 UTF-8 显示）
Write-Host "[3/3] 显示最新提交信息..." -ForegroundColor Yellow
Write-Host ""
Write-Host "提交哈希: " -NoNewline -ForegroundColor Cyan
git log -1 --format="%H"
Write-Host "作者: " -NoNewline -ForegroundColor Cyan
git log -1 --format="%an <%ae>"
Write-Host "提交时间: " -NoNewline -ForegroundColor Cyan
git log -1 --format="%ad" --date=format:"%Y-%m-%d %H:%M:%S"
Write-Host ""
Write-Host "提交信息: " -ForegroundColor Cyan
Write-Host "--------------------------------------------" -ForegroundColor Gray
git log -1 --format="%B"
Write-Host "--------------------------------------------" -ForegroundColor Gray
Write-Host ""

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "配置完成！" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "注意：" -ForegroundColor Yellow
Write-Host "  - 如果提交信息仍然显示乱码，说明该提交在存储时使用了错误的编码" -ForegroundColor Yellow
Write-Host "  - 可以使用以下命令修改最新提交信息：" -ForegroundColor Yellow
Write-Host "    git commit --amend -m '新的提交信息（使用 UTF-8）'" -ForegroundColor White
Write-Host "  - 或者使用交互式编辑器：" -ForegroundColor Yellow
Write-Host "    git commit --amend" -ForegroundColor White
Write-Host ""


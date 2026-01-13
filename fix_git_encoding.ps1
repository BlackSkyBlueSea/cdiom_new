# 修复 Git 中文编码问题的脚本
# 设置 Git 编码配置
git config --global core.quotepath false
git config --global i18n.commitencoding utf-8
git config --global i18n.logoutputencoding utf-8

# 设置 PowerShell 编码
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$PSDefaultParameterValues['*:Encoding'] = 'utf8'

Write-Host "Git 编码配置已更新：" -ForegroundColor Green
Write-Host "  - core.quotepath: false" -ForegroundColor Yellow
Write-Host "  - i18n.commitencoding: utf-8" -ForegroundColor Yellow
Write-Host "  - i18n.logoutputencoding: utf-8" -ForegroundColor Yellow
Write-Host ""
Write-Host "PowerShell 编码已设置为 UTF-8" -ForegroundColor Green
Write-Host ""
Write-Host "最新提交信息：" -ForegroundColor Cyan
git log -1 --format="%H%n%an <%ae>%n%ad%n%s%n%b"


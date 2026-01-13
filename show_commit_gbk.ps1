# ============================================
# 使用 GBK 编码正确显示 Git 提交信息
# ============================================

# 设置 PowerShell 编码为 GBK
chcp 936 | Out-Null
[Console]::OutputEncoding = [System.Text.Encoding]::GetEncoding("GBK")
$OutputEncoding = [System.Text.Encoding]::GetEncoding("GBK")

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "使用 GBK 编码显示最新提交信息" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# 显示提交信息
Write-Host "提交哈希: " -NoNewline -ForegroundColor Yellow
git log -1 --format="%H"

Write-Host "作者: " -NoNewline -ForegroundColor Yellow
git log -1 --format="%an <%ae>"

Write-Host "提交时间: " -NoNewline -ForegroundColor Yellow
git log -1 --format="%ad" --date=format:"%Y年%m月%d日 %H:%M:%S"

Write-Host ""
Write-Host "提交信息: " -ForegroundColor Yellow
Write-Host "--------------------------------------------" -ForegroundColor Gray
git log -1 --format="%B"
Write-Host "--------------------------------------------" -ForegroundColor Gray
Write-Host ""


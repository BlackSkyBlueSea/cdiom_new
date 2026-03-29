/**
 * 列表页表格布局：与 Layout.css 中 .content 配合，主内容区占满、表体内滚动。
 * 若顶栏或筛选区变高，可只改 TABLE_SCROLL_Y 的减数。
 *
 * 工具栏两种形态（下方均为 tableAreaStyle + 表格 + 分页，占满主内容区剩余高度）：
 * - 筛选项较多：toolbarSectionStackedStyle → 第一行仅标题（toolbarPageTitleStyle），
 *   第二行 compactFilterRowFullWidthStyle 放全部筛选与按钮；表体高度用 TABLE_SCROLL_Y_STACKED。
 * - 筛选项较少：toolbarRowCompactStyle → 标题与 compactFilterRowStyle（筛选+按钮）同一行；
 *   表体高度用 TABLE_SCROLL_Y。
 */
export const TABLE_SCROLL_Y = 'calc(100vh - 260px)'

/** 工具栏为「标题一行 + 筛选一行」时表头略高，略减小表体固定高度避免溢出 */
export const TABLE_SCROLL_Y_STACKED = 'calc(100vh - 300px)'

/** 仪表盘/长页面底部卡片内表格，避免表体过高撑破布局 */
export const TABLE_SCROLL_Y_EMBEDDED = 'min(480px, calc(100vh - 300px))'

export const pageRootStyle = {
  height: '100%',
  minHeight: 0,
  display: 'flex',
  flexDirection: 'column',
  overflow: 'hidden',
}

export const tableAreaStyle = {
  flex: 1,
  minHeight: 0,
  overflow: 'hidden',
}

/** 标题 + 筛选工具条一行 */
export const toolbarRowStyle = {
  flexShrink: 0,
  marginBottom: 16,
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  gap: 16,
  flexWrap: 'wrap',
}

/** 标题与右侧筛选/按钮同一行不换行；与 compactFilterRowStyle 配合（筛选项较少时） */
export const toolbarRowCompactStyle = {
  ...toolbarRowStyle,
  flexWrap: 'nowrap',
  alignItems: 'center',
  gap: 12,
  minWidth: 0,
}

/**
 * 筛选项较多时：外层容器。第一行仅标题（toolbarPageTitleStyle），第二行为 compactFilterRowFullWidthStyle。
 * 下方 tableAreaStyle 仍占主内容区剩余高度。
 */
export const toolbarSectionStackedStyle = {
  flexShrink: 0,
  marginBottom: 16,
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'stretch',
  gap: 10,
  minWidth: 0,
}

/** 列表页主标题（堆叠模式第一行） */
export const toolbarPageTitleStyle = {
  margin: 0,
  flexShrink: 0,
  lineHeight: 1.35,
}

/** 筛选项较少时：与标题同一行，占标题右侧剩余空间 */
export const compactFilterRowStyle = {
  display: 'flex',
  flexWrap: 'nowrap',
  alignItems: 'center',
  gap: 6,
  flex: 1,
  minWidth: 0,
  overflowX: 'auto',
  WebkitOverflowScrolling: 'touch',
}

/** 筛选项较多时：独占工具栏第二行，整行横向排列，窄屏可横向滚动 */
export const compactFilterRowFullWidthStyle = {
  display: 'flex',
  flexWrap: 'nowrap',
  alignItems: 'center',
  gap: 6,
  width: '100%',
  minWidth: 0,
  overflowX: 'auto',
  WebkitOverflowScrolling: 'touch',
}

/**
 * 可伸缩筛选项外层（Input/Select/RangePicker 内设 width: '100%'）
 * @param {string} flex 如 '1.2 1 80px'
 */
export function filterCellFlex(flex, minW, maxW) {
  return {
    flex,
    minWidth: minW,
    maxWidth: maxW,
    flexShrink: 1,
  }
}

/** 用户管理等 inline Form 筛选区：单行 + 横向滚动 */
export const compactInlineFilterFormStyle = {
  marginBottom: 0,
  display: 'flex',
  flexWrap: 'nowrap',
  alignItems: 'center',
  columnGap: 6,
  minWidth: 0,
  overflowX: 'auto',
  WebkitOverflowScrolling: 'touch',
}

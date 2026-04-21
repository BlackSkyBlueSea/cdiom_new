import { useState, useEffect, useMemo, useCallback } from 'react'
import { Table, Button, Space, Input, Select, DatePicker, Tag, Modal, Form, message, AutoComplete, InputNumber, Alert, Tooltip } from 'antd'
import { SearchOutlined, ReloadOutlined, PlusOutlined, CheckCircleOutlined, CloseCircleOutlined, PlayCircleOutlined, DeleteOutlined, EyeOutlined, RollbackOutlined, PrinterOutlined, SafetyCertificateOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request from '../utils/request'
import logger from '../utils/logger'
import { hasPermission, PERMISSIONS, fetchUserPermissions } from '../utils/permission'
import { getUser } from '../utils/auth'
import {
  pageRootStyle,
  tableAreaStyle,
  toolbarRowCompactStyle,
  toolbarPageTitleStyle,
  compactFilterRowStyle,
  filterCellFlex,
  TABLE_SCROLL_Y,
} from '../utils/tablePageLayout'

const { RangePicker } = DatePicker
const { TextArea } = Input

/** INT-FE-02：出库申请—审批—执行全链路，浏览器控制台与后端日志前缀对齐，便于测试截图 */
function outboundTestTrace(step, detail) {
  console.log('[INT-FE-02][FE出库]', step, detail !== undefined ? detail : '')
}

/** 明细接口已带 drugName/specification 时优先使用，避免依赖药品信息管理页的药品全量缓存 */
function formatOutboundApplyItemDrugLabel(item, drugsList = []) {
  if (item?.drugName) {
    return item.specification ? `${item.drugName} (${item.specification})` : item.drugName
  }
  const drug = drugsList.find((d) => Number(d.id) === Number(item?.drugId))
  return drug ? `${drug.drugName} (${drug.specification || ''})` : `药品ID: ${item?.drugId ?? '-'}`
}

const OutboundManagement = () => {
  const [outboundApplies, setOutboundApplies] = useState([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [filters, setFilters] = useState({
    keyword: '',
    applicantId: undefined,
    approverId: undefined,
    department: '',
    status: undefined,
    startDate: undefined,
    endDate: undefined,
  })
  const [modalVisible, setModalVisible] = useState(false)
  const [approveModalVisible, setApproveModalVisible] = useState(false)
  const [executeModalVisible, setExecuteModalVisible] = useState(false)
  const [currentRecord, setCurrentRecord] = useState(null)
  const [applyItems, setApplyItems] = useState([])
  const [inventoryBatches, setInventoryBatches] = useState({}) // {drugId: [batches]}
  const [form] = Form.useForm()
  const [approveForm] = Form.useForm()
  const [executeForm] = Form.useForm()
  const [drugs, setDrugs] = useState([])
  const [applyFormItems, setApplyFormItems] = useState([{ drugId: undefined, quantity: undefined, batchNumber: undefined }])
  /** 新建申请：按药品 ID 缓存可选批次（选完药品后拉取 /inventory） */
  const [applyInventoryBatches, setApplyInventoryBatches] = useState({})
  const [applyInventoryBatchLoading, setApplyInventoryBatchLoading] = useState({})
  const [approveItems, setApproveItems] = useState([]) // 审批时查看的申请明细
  const [hasSpecialDrug, setHasSpecialDrug] = useState(false) // 是否包含特殊药品
  const [secondApproverCandidates, setSecondApproverCandidates] = useState([]) // 具备出库审核权限的用户（第二审批人）
  const [loadingUsers, setLoadingUsers] = useState(false)
  const [departmentOptions, setDepartmentOptions] = useState([]) // 已有科室列表（新建出库申请时下拉选择）
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [detailItems, setDetailItems] = useState([]) // 查看详情时的申请明细
  const [stockCheckResult, setStockCheckResult] = useState(null) // 审批前库存校验结果 { sufficient, message, details }
  const [medicalApplicants, setMedicalApplicants] = useState([]) // 代录出库：可选医护人员
  const [loadingMedicalApplicants, setLoadingMedicalApplicants] = useState(false)

  /** 出库拣货汇总（按批次/货位打印） */
  const [pickSummaryOpen, setPickSummaryOpen] = useState(false)
  const [pickSummaryLoading, setPickSummaryLoading] = useState(false)
  const [pickSummaryData, setPickSummaryData] = useState(null)
  const [pickSummaryDate, setPickSummaryDate] = useState(() => dayjs())
  const [pickSummaryScope, setPickSummaryScope] = useState('approve_day')

  const canApplyOutbound = hasPermission(PERMISSIONS.OUTBOUND_APPLY)
  const canProxyOutbound = hasPermission(PERMISSIONS.OUTBOUND_APPLY_ON_BEHALF)
  const canViewPickSummary = hasPermission([
    PERMISSIONS.OUTBOUND_EXECUTE,
    PERMISSIONS.OUTBOUND_VIEW,
    PERMISSIONS.OUTBOUND_APPROVE,
    PERMISSIONS.OUTBOUND_APPROVE_SPECIAL,
  ])

  const loadPickSummary = useCallback(async () => {
    setPickSummaryLoading(true)
    try {
      const params = { scope: pickSummaryScope }
      if (pickSummaryScope === 'approve_day') {
        params.date = pickSummaryDate.format('YYYY-MM-DD')
      }
      const res = await request.get('/outbound/pick-summary', { params })
      if (res.code === 200) {
        setPickSummaryData(res.data || {})
      } else {
        message.error(res.msg || '加载拣货汇总失败')
        setPickSummaryData(null)
      }
    } catch (error) {
      logger.error('拣货汇总:', error)
      message.error(error.response?.data?.msg || '加载拣货汇总失败')
      setPickSummaryData(null)
    } finally {
      setPickSummaryLoading(false)
    }
  }, [pickSummaryDate, pickSummaryScope])

  useEffect(() => {
    if (pickSummaryOpen) {
      loadPickSummary()
    }
  }, [pickSummaryOpen, loadPickSummary])

  const handlePrintPickSummary = () => {
    const data = pickSummaryData
    if (!data) return
    const scopeLabel = data.scope === 'all_pending' ? '全部待执行出库' : `审批日 ${data.date || ''}`
    const summary = Array.isArray(data.summary) ? data.summary : []
    const warnings = Array.isArray(data.warnings) ? data.warnings : []
    const lines = Array.isArray(data.pickLines) ? data.pickLines : []
    const w = window.open('', '_blank')
    if (!w) {
      message.warning('请允许弹出窗口以打印')
      return
    }
    const esc = (s) => String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    const rowHtml = (cells) =>
      `<tr>${cells.map((c) => `<td style="border:1px solid #333;padding:6px 8px">${esc(c)}</td>`).join('')}</tr>`
    let body = `
      <h2 style="text-align:center;margin:0 0 12px">出库拣货汇总表</h2>
      <p style="margin:4px 0;font-size:13px">${esc(scopeLabel)} · 待执行申领单 ${data.applyCount ?? 0} 笔</p>
      <p style="margin:4px 0 12px;font-size:12px;color:#555">按存储位置、药品、批次汇总数量，拣货后请在系统中按单执行出库并交接医护。</p>
      <table style="border-collapse:collapse;width:100%;font-size:13px;margin-bottom:16px">
        <thead><tr>
          <th style="border:1px solid #333;padding:6px 8px">存储位置</th>
          <th style="border:1px solid #333;padding:6px 8px">药品名称</th>
          <th style="border:1px solid #333;padding:6px 8px">规格</th>
          <th style="border:1px solid #333;padding:6px 8px">批次号</th>
          <th style="border:1px solid #333;padding:6px 8px">效期</th>
          <th style="border:1px solid #333;padding:6px 8px">数量</th>
        </tr></thead>
        <tbody>
    `
    for (const r of summary) {
      body += rowHtml([
        r.storageLocation || '—',
        r.drugName || '—',
        r.specification || '—',
        r.batchNumber || '—',
        r.expiryDate ? dayjs(r.expiryDate).format('YYYY-MM-DD') : '—',
        r.quantity ?? '—',
      ])
    }
    body += `</tbody></table>`
    if (lines.length > 0) {
      body += `<h3 style="font-size:14px;margin:12px 0 8px">按申领单明细（拣货核对）</h3>
      <table style="border-collapse:collapse;width:100%;font-size:12px">
        <thead><tr>
          <th style="border:1px solid #333;padding:4px 6px">申领单号</th>
          <th style="border:1px solid #333;padding:4px 6px">科室</th>
          <th style="border:1px solid #333;padding:4px 6px">药品</th>
          <th style="border:1px solid #333;padding:4px 6px">批次</th>
          <th style="border:1px solid #333;padding:4px 6px">货位</th>
          <th style="border:1px solid #333;padding:4px 6px">数量</th>
        </tr></thead><tbody>`
      for (const r of lines) {
        body += rowHtml([
          r.applyNumber,
          r.department,
          r.specification ? `${r.drugName || ''} ${r.specification}` : (r.drugName || '—'),
          r.batchNumber || '—',
          r.storageLocation || '—',
          r.quantity ?? '—',
        ])
      }
      body += `</tbody></table>`
    }
    if (warnings.length > 0) {
      body += `<div style="margin-top:12px;padding:8px;background:#fff7e6;border:1px solid #ffc53d;font-size:12px"><strong>提示：</strong><ul style="margin:4px 0 0 16px">`
      for (const t of warnings) {
        body += `<li>${esc(t)}</li>`
      }
      body += `</ul></div>`
    }
    body += `<p style="margin-top:16px;font-size:11px;color:#888">打印时间：${dayjs().format('YYYY-MM-DD HH:mm:ss')} · 未指定批次时按先到期先出（FIFO）模拟，若执行顺序与审批顺序不一致，实际批次可能略有差异。</p>`
    w.document.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>出库拣货汇总</title></head><body style="font-family:sans-serif;padding:16px">${body}</body></html>`)
    w.document.close()
    w.focus()
    w.print()
  }

  useEffect(() => {
    fetchOutboundApplies()
  }, [pagination.current, pagination.pageSize, filters])

  useEffect(() => {
    fetchDrugs()
  }, [])

  // 执行出库表单预填在「执行出库」Modal 的 afterOpenChange 中完成，避免 Modal 未挂载 Form 时调用 useForm 实例

  /** 第二审批人可选列表（仅含具备 outbound:approve / outbound:approve:special 的用户） */
  const fetchSecondApproverCandidates = async () => {
    setLoadingUsers(true)
    try {
      const res = await request.get('/outbound/second-approver-candidates')
      if (res.code === 200) {
        setSecondApproverCandidates(Array.isArray(res.data) ? res.data : [])
      }
    } catch (error) {
      logger.error('获取第二审批人候选失败:', error)
      message.error(error.response?.data?.msg || '获取第二审批人列表失败')
      setSecondApproverCandidates([])
    } finally {
      setLoadingUsers(false)
    }
  }

  // 药品选项（用于 AutoComplete）：value 须为字符串，避免 combobox 下 number 类型警告
  const drugOptions = useMemo(() => {
    return drugs.map((drug) => ({
      value: String(drug.id),
      label: `${drug.drugName || ''} (${drug.specification || ''})`.replace(/\s*\(\s*\)$/, '').trim() || String(drug.id),
      drug,
    }))
  }, [drugs])

  /** 新建申请：选定药品后加载可选批次（医护人员走 /outbound/drug-batches；仅代录权限走 /inventory） */
  const loadBatchesForApplyDrug = useCallback(async (drugId) => {
    if (drugId == null) return
    setApplyInventoryBatchLoading((prev) => ({ ...prev, [drugId]: true }))
    try {
      if (hasPermission(PERMISSIONS.OUTBOUND_APPLY)) {
        const res = await request.get('/outbound/drug-batches', { params: { drugId } })
        if (res.code === 200) {
          const records = Array.isArray(res.data) ? res.data : []
          setApplyInventoryBatches((prev) => ({ ...prev, [drugId]: records }))
        }
      } else {
        const res = await request.get('/inventory', {
          params: { page: 1, size: 500, drugId },
        })
        if (res.code === 200) {
          const raw = res.data?.records || []
          const records = raw.map((inv) => ({
            batchNumber: inv.batchNumber,
            quantity: inv.quantity,
            expiryDate: inv.expiryDate,
          }))
          setApplyInventoryBatches((prev) => ({ ...prev, [drugId]: records }))
        }
      }
    } catch (error) {
      logger.error(`获取药品 ${drugId} 的库存批次失败:`, error)
      message.warning(error.response?.data?.msg || '加载该药品批次失败，请稍后重试')
    } finally {
      setApplyInventoryBatchLoading((prev) => ({ ...prev, [drugId]: false }))
    }
  }, [])

  const fetchDrugs = async () => {
    try {
      const res = await request.get('/drugs/options-for-business', {
        params: { page: 1, size: 2000 },
      })
      if (res.code === 200) {
        setDrugs(res.data.records || [])
      }
    } catch (error) {
      if (!error?.isPermissionForbidden) {
        logger.error('获取药品列表失败:', error)
      }
    }
  }

  // 获取已有科室列表（供新建出库申请时下拉选择，无则仍可手动输入）
  const fetchDepartmentOptions = async () => {
    try {
      const res = await request.get('/outbound/departments')
      if (res.code === 200 && Array.isArray(res.data)) {
        setDepartmentOptions(res.data.filter(Boolean))
      }
    } catch (error) {
      logger.error('获取科室列表失败:', error)
    }
  }

  const fetchApplyItems = async (applyId) => {
    try {
      const res = await request.get(`/outbound/${applyId}/items`)
      if (res.code === 200) {
        const list = res.data || []
        setApplyItems(list)
        // 获取每个药品的可用批次
        const batchesMap = {}
        for (const item of list) {
          try {
            if (hasPermission(PERMISSIONS.INVENTORY_VIEW)) {
              const inventoryRes = await request.get('/inventory', {
                params: {
                  page: 1,
                  size: 100,
                  drugId: item.drugId,
                },
              })
              if (inventoryRes.code === 200) {
                batchesMap[item.drugId] = inventoryRes.data.records || []
              }
            } else {
              const batchRes = await request.get('/outbound/drug-batches', {
                params: { drugId: item.drugId },
              })
              if (batchRes.code === 200) {
                const records = Array.isArray(batchRes.data) ? batchRes.data : []
                batchesMap[item.drugId] = records
              }
            }
          } catch (error) {
            if (!error?.isPermissionForbidden) {
              logger.error(`获取药品${item.drugId}的库存批次失败:`, error)
            }
          }
        }
        setInventoryBatches(batchesMap)
      }
    } catch (error) {
      logger.error('获取申请明细失败:', error)
      message.error('获取申请明细失败')
    }
  }

  const fetchOutboundApplies = async () => {
    setLoading(true)
    try {
      const params = {
        page: pagination.current,
        size: pagination.pageSize,
        keyword: filters.keyword || undefined,
        applicantId: filters.applicantId,
        approverId: filters.approverId,
        department: filters.department || undefined,
        status: filters.status,
        startDate: filters.startDate ? filters.startDate.format('YYYY-MM-DD') : undefined,
        endDate: filters.endDate ? filters.endDate.format('YYYY-MM-DD') : undefined,
      }
      const res = await request.get('/outbound', { params })
      if (res.code === 200) {
        const records = res.data.records || []
        setOutboundApplies(records)
        setPagination({
          ...pagination,
          total: res.data.total || 0,
        })
        const byStatus = records.reduce((acc, r) => {
          const s = r.status || '?'
          acc[s] = (acc[s] || 0) + 1
          return acc
        }, {})
        outboundTestTrace('GET /outbound 列表成功', {
          total: res.data.total,
          page: params.page,
          pageSize: params.size,
          statusFilter: params.status ?? '(全部)',
          本页状态分布: byStatus,
        })
      } else {
        outboundTestTrace('GET /outbound 列表失败', { code: res.code, msg: res.msg })
        message.error(res.msg || '获取出库申请失败')
      }
    } catch (error) {
      outboundTestTrace('GET /outbound 异常', error?.response?.data || error?.message)
      logger.error('获取出库申请失败:', error)
      message.error('获取出库申请失败')
    } finally {
      setLoading(false)
    }
  }

  const handleTableChange = (newPagination) => {
    setPagination({
      ...pagination,
      current: newPagination.current,
      pageSize: newPagination.pageSize,
    })
  }

  const handleReset = () => {
    setFilters({
      keyword: '',
      applicantId: undefined,
      approverId: undefined,
      department: '',
      status: undefined,
      startDate: undefined,
      endDate: undefined,
    })
    setPagination(prev => ({ ...prev, current: 1 }))
  }

  // 检查申请是否包含特殊药品
  const checkSpecialDrugs = async (applyId) => {
    try {
      const res = await request.get(`/outbound/${applyId}/items`)
      if (res.code === 200) {
        const items = res.data || []
        setApproveItems(items)
        
        // 检查是否包含特殊药品（优先用接口返回的 isSpecial，避免仅依赖本地 drugs 缓存导致代录/混单误判）
        let hasSpecial = false
        for (const item of items) {
          const fromApi = item.isSpecial != null && Number(item.isSpecial) === 1
          const drug = drugs.find((d) => Number(d.id) === Number(item.drugId))
          const fromCache = drug != null && Number(drug.isSpecial) === 1
          if (fromApi || fromCache) {
            hasSpecial = true
            break
          }
        }
        setHasSpecialDrug(hasSpecial)
        return hasSpecial
      }
    } catch (error) {
      logger.error('获取申请明细失败:', error)
    }
    return false
  }

  // 审批前拉取库存校验结果（用于弹窗内友好提示，不足时禁止通过）
  const fetchStockCheckForApply = async (applyId) => {
    try {
      const res = await request.get(`/outbound/${applyId}/stock-check`)
      if (res.code === 200 && res.data) {
        setStockCheckResult(res.data)
        outboundTestTrace(`GET /outbound/${applyId}/stock-check`, {
          sufficient: res.data.sufficient,
          message: res.data.message,
        })
      } else {
        setStockCheckResult({ sufficient: true, message: '', details: [] })
        outboundTestTrace(`GET /outbound/${applyId}/stock-check 无有效 data，按充足处理`)
      }
    } catch (e) {
      outboundTestTrace(`GET /outbound/${applyId}/stock-check 异常`, e?.response?.data || e?.message)
      logger.error('库存校验请求失败:', e)
      setStockCheckResult({ sufficient: true, message: '', details: [] })
    }
  }

  // 打开查看详情弹窗（拉取申请明细）
  const openDetailModal = async (record) => {
    outboundTestTrace('打开详情', {
      id: record.id,
      applyNumber: record.applyNumber,
      status: record.status,
    })
    setCurrentRecord(record)
    setDetailItems([])
    setDetailModalVisible(true)
    try {
      const res = await request.get(`/outbound/${record.id}/items`)
      if (res.code === 200) {
        setDetailItems(res.data || [])
        outboundTestTrace(`GET /outbound/${record.id}/items 详情明细行数`, (res.data || []).length)
      }
    } catch (e) {
      logger.error('获取申请明细失败:', e)
      message.error('获取申请明细失败')
    }
  }

  // 申请人撤回出库申请（仅待审批状态）
  const handleWithdraw = async (id) => {
    outboundTestTrace(`POST /outbound/${id}/withdraw 请求`)
    try {
      const res = await request.post(`/outbound/${id}/withdraw`)
      if (res.code === 200) {
        outboundTestTrace(`POST /outbound/${id}/withdraw 成功`)
        message.success('已撤回')
        setDetailModalVisible(false)
        fetchOutboundApplies()
      } else {
        outboundTestTrace(`POST /outbound/${id}/withdraw 失败`, res.msg)
        message.error(res.msg || '撤回失败')
      }
    } catch (error) {
      outboundTestTrace(`POST /outbound/${id}/withdraw 异常`, error.response?.data || error.message)
      message.error(error.response?.data?.msg || error.message || '撤回失败')
    }
  }

  const handleApprove = async (values) => {
    try {
      // 如果包含特殊药品，必须填写第二审批人
      if (hasSpecialDrug && !values.secondApproverId) {
        message.error('申请包含特殊药品，必须指定第二审批人')
        return
      }

      const currentUser = getUser()
      // 验证：第一审批人（当前用户）与第二审批人不能是同一人
      if (hasSpecialDrug && values.secondApproverId && currentUser && values.secondApproverId === currentUser.id) {
        message.error('第一审批人和第二审批人不能为同一人')
        return
      }
      // 验证：申请人和审批人不能是同一人（后端也会验证，这里提前提示）
      if (currentRecord && currentRecord.applicantId) {
        if (currentUser && currentUser.id === currentRecord.applicantId) {
          message.error('申请人和审批人不能是同一人')
          return
        }
        if (hasSpecialDrug && values.secondApproverId && values.secondApproverId === currentRecord.applicantId) {
          message.error('特殊药品申请人和第二审批人不能是同一人')
          return
        }
      }

      outboundTestTrace(`POST /outbound/${currentRecord.id}/approve 请求`, {
        applyNumber: currentRecord.applyNumber,
        secondApproverId: values.secondApproverId || null,
        hasSpecialDrug,
      })
      const res = await request.post(`/outbound/${currentRecord.id}/approve`, {
        secondApproverId: values.secondApproverId || null,
      })
      if (res.code === 200) {
        outboundTestTrace(`POST /outbound/${currentRecord.id}/approve 成功 → 状态应为 APPROVED`)
        message.success('审批通过')
        setApproveModalVisible(false)
        setTimeout(() => {
          approveForm.resetFields()
        }, 0)
        setApproveItems([])
        setHasSpecialDrug(false)
        fetchOutboundApplies()
      } else {
        outboundTestTrace(`POST /outbound/${currentRecord.id}/approve 失败`, res.msg)
        message.error(res.msg || '审批失败')
      }
    } catch (error) {
      const errorMsg = error.response?.data?.msg || error.message || '审批失败'
      outboundTestTrace('审批异常', errorMsg)
      message.error(errorMsg)
    }
  }

  const handleReject = async (applyId, reason) => {
    if (!applyId) {
      message.error('无法获取申请信息')
      return
    }
    try {
      outboundTestTrace(`POST /outbound/${applyId}/reject 请求`, { reasonLen: reason?.length ?? 0 })
      const res = await request.post(`/outbound/${applyId}/reject`, {
        rejectReason: reason,
      })
      if (res.code === 200) {
        outboundTestTrace(`POST /outbound/${applyId}/reject 成功 → 状态应为 REJECTED`)
        message.success('已驳回')
        fetchOutboundApplies()
      } else {
        outboundTestTrace(`POST /outbound/${applyId}/reject 失败`, res.msg)
        message.error(res.msg || '驳回失败')
      }
    } catch (error) {
      outboundTestTrace(`POST /outbound/${applyId}/reject 异常`, error?.response?.data || error?.message)
      logger.error('驳回失败:', error)
      message.error('驳回失败')
    }
  }

  /** 特殊药品：第二审批人本人确认通过（PENDING_SECOND → APPROVED） */
  const handleSecondApprove = async (applyId) => {
    try {
      outboundTestTrace(`POST /outbound/${applyId}/second-approve 请求`)
      const res = await request.post(`/outbound/${applyId}/second-approve`, {})
      if (res.code === 200) {
        outboundTestTrace(`POST /outbound/${applyId}/second-approve 成功 → APPROVED`)
        message.success('第二审批已通过，可执行出库')
        fetchOutboundApplies()
      } else {
        outboundTestTrace(`POST /outbound/${applyId}/second-approve 失败`, res.msg)
        message.error(res.msg || '第二审批失败')
      }
    } catch (error) {
      outboundTestTrace('第二审批异常', error?.response?.data || error?.message)
      message.error(error.response?.data?.msg || error.message || '第二审批失败')
    }
  }

  const handleCreateApply = async (values) => {
    try {
      // 验证申请明细
      if (!values.department || values.department.trim() === '') {
        message.error('请输入所属科室')
        return
      }
      if (!values.purpose || values.purpose.trim() === '') {
        message.error('请输入用途说明')
        return
      }
      if (!applyFormItems || applyFormItems.length === 0) {
        message.error('请至少添加一个药品')
        return
      }
      
      // 验证每个明细项
      const validItems = []
      for (const item of applyFormItems) {
        if (!item.drugId) {
          message.error('请选择药品')
          return
        }
        if (!item.quantity || item.quantity <= 0) {
          message.error('请输入有效的申领数量')
          return
        }
        validItems.push({
          drugId: item.drugId,
          batchNumber: item.batchNumber || undefined,
          quantity: item.quantity,
        })
      }

      const basePayload = {
        department: values.department.trim(),
        purpose: values.purpose.trim(),
        remark: values.remark || '',
        items: validItems,
      }

      const currentUid = getUser()?.id
      const useOnBehalf =
        canProxyOutbound &&
        values.applicantId != null &&
        currentUid != null &&
        values.applicantId !== currentUid

      if (canProxyOutbound && !canApplyOutbound && values.applicantId == null) {
        message.error('请选择申领医护人员')
        return
      }

      outboundTestTrace(useOnBehalf ? 'POST /outbound/on-behalf 请求' : 'POST /outbound 请求', {
        itemCount: validItems.length,
        department: basePayload.department,
      })
      const res = useOnBehalf
        ? await request.post('/outbound/on-behalf', {
            ...basePayload,
            applicantId: values.applicantId,
          })
        : await request.post('/outbound', basePayload)

      if (res.code === 200) {
        outboundTestTrace(useOnBehalf ? 'POST /outbound/on-behalf 成功' : 'POST /outbound 成功', {
          id: res.data?.id,
          applyNumber: res.data?.applyNumber,
          status: res.data?.status,
        })
        message.success(useOnBehalf ? '代录出库申请已创建' : '出库申请创建成功')
        setModalVisible(false)
        setTimeout(() => {
          form.resetFields()
          setApplyFormItems([{ drugId: undefined, quantity: undefined, batchNumber: undefined }])
        }, 0)
        fetchOutboundApplies()
      } else {
        outboundTestTrace(useOnBehalf ? '创建代录申请失败' : '创建申请失败', res.msg)
        message.error(res.msg || '创建出库申请失败')
      }
    } catch (error) {
      outboundTestTrace('创建出库申请异常', error.response?.data || error.message)
      logger.error('创建出库申请失败:', error)
      message.error(error.response?.data?.msg || error.message || '创建出库申请失败')
    }
  }

  const handleExecute = async (values) => {
    try {
      // 验证出库明细
      const outboundItems = []
      for (const item of applyItems) {
        const formItem = values[`item_${item.id}`]
        if (!formItem || !formItem.actualQuantity || formItem.actualQuantity <= 0) {
          message.error(`请填写药品ID ${item.drugId} 的实际出库数量`)
          return
        }
        if (formItem.actualQuantity > item.quantity) {
          message.error(`实际出库数量不能超过申请数量（申请：${item.quantity}）`)
          return
        }
        outboundItems.push({
          drugId: item.drugId,
          batchNumber: formItem.batchNumber || undefined,
          actualQuantity: formItem.actualQuantity,
        })
      }

      outboundTestTrace(`POST /outbound/${currentRecord.id}/execute 请求`, {
        applyNumber: currentRecord.applyNumber,
        outboundLines: outboundItems.length,
        outboundItems,
      })
      const res = await request.post(`/outbound/${currentRecord.id}/execute`, {
        outboundItems: outboundItems,
      })
      if (res.code === 200) {
        outboundTestTrace(`POST /outbound/${currentRecord.id}/execute 成功 → 状态应为 OUTBOUND，可与仪表盘/库存对照`)
        message.success('出库执行成功')
        setExecuteModalVisible(false)
        setTimeout(() => {
          executeForm.resetFields()
        }, 0)
        setApplyItems([])
        setInventoryBatches({})
        fetchOutboundApplies()
      } else {
        outboundTestTrace(`POST /outbound/${currentRecord.id}/execute 失败`, res.msg)
        message.error(res.msg || '出库执行失败')
      }
    } catch (error) {
      outboundTestTrace('执行出库异常', error.response?.data || error.message)
      logger.error('出库执行失败:', error)
      message.error(error.response?.data?.msg || error.message || '出库执行失败')
    }
  }

  const addApplyFormItem = () => {
    setApplyFormItems([...applyFormItems, { drugId: undefined, quantity: undefined, batchNumber: undefined }])
  }

  const removeApplyFormItem = (index) => {
    const newItems = applyFormItems.filter((_, i) => i !== index)
    setApplyFormItems(newItems)
  }

  const getStatusTag = (status) => {
    const statusMap = {
      PENDING: { color: 'orange', text: '待审批' },
      PENDING_SECOND: { color: 'gold', text: '待第二审批' },
      APPROVED: { color: 'green', text: '已通过' },
      REJECTED: { color: 'red', text: '已驳回' },
      OUTBOUND: { color: 'blue', text: '已出库' },
      CANCELLED: { color: 'default', text: '已取消' },
    }
    const statusInfo = statusMap[status] || { color: 'default', text: status }
    return <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
  }

  const columns = [
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>ID</span>,
      dataIndex: 'id',
      key: 'id',
      width: 80,
      sorter: (a, b) => a.id - b.id,
      defaultSortOrder: 'ascend',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>申领单号</span>,
      dataIndex: 'applyNumber',
      key: 'applyNumber',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>申请人</span>,
      dataIndex: 'applicantName',
      key: 'applicantName',
      width: 120,
      ellipsis: true,
      render: (name, record) => {
        const main =
          record.applicantRoleName != null && record.applicantRoleName !== ''
            ? `${name || '-'} (${record.applicantRoleName})`
            : (name ?? '-')
        if (!record.proxyRegistrarId) return main
        const proxy =
          record.proxyRegistrarRoleName != null && record.proxyRegistrarRoleName !== ''
            ? `${record.proxyRegistrarName || '-'}（${record.proxyRegistrarRoleName}）`
            : (record.proxyRegistrarName ?? '-')
        return (
          <div>
            <div>{main}</div>
            <div style={{ fontSize: 12, color: 'rgba(0,0,0,0.45)', marginTop: 2 }}>代录：{proxy}</div>
          </div>
        )
      },
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>所属科室</span>,
      dataIndex: 'department',
      key: 'department',
      width: 120,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>用途</span>,
      dataIndex: 'purpose',
      key: 'purpose',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>申请状态</span>,
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => getStatusTag(status),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>审批人</span>,
      dataIndex: 'approverName',
      key: 'approverName',
      width: 140,
      ellipsis: true,
      render: (name, record) => {
        const first =
          record.approverRoleName != null && record.approverRoleName !== ''
            ? `${name || '-'}（${record.approverRoleName}）`
            : (name ?? '-')
        if (!record.secondApproverId && !record.secondApproverName) return first
        const sec =
          record.secondApproverRoleName != null && record.secondApproverRoleName !== ''
            ? `${record.secondApproverName || '-'}（${record.secondApproverRoleName}）`
            : (record.secondApproverName ?? '-')
        return (
          <div style={{ fontSize: 12, lineHeight: 1.45 }}>
            <div>一：{first}</div>
            <div style={{ color: 'rgba(0,0,0,0.55)', marginTop: 2 }}>二：{sec}</div>
          </div>
        )
      },
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>审批时间</span>,
      dataIndex: 'approveTime',
      key: 'approveTime',
      width: 200,
      render: (time, record) => {
        if (record.status === 'PENDING_SECOND' && record.firstApproveTime) {
          return (
            <div style={{ fontSize: 12, lineHeight: 1.45 }}>
              <div>一：{dayjs(record.firstApproveTime).format('YYYY-MM-DD HH:mm:ss')}</div>
              <div style={{ color: 'rgba(0,0,0,0.45)', marginTop: 2 }}>终批：待第二人</div>
            </div>
          )
        }
        return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
      },
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>出库时间</span>,
      dataIndex: 'outboundTime',
      key: 'outboundTime',
      width: 180,
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作</span>,
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => {
        const canApprove = hasPermission([PERMISSIONS.OUTBOUND_APPROVE, PERMISSIONS.OUTBOUND_APPROVE_SPECIAL])
        const canExecute = hasPermission(PERMISSIONS.OUTBOUND_EXECUTE)
        const canViewDetail = hasPermission(PERMISSIONS.OUTBOUND_VIEW) || hasPermission(PERMISSIONS.OUTBOUND_APPLY)
        const currentUser = getUser()
        const isApplicant = currentUser && record.applicantId === currentUser.id
        const canWithdraw = hasPermission(PERMISSIONS.OUTBOUND_APPLY) && record.status === 'PENDING' && isApplicant
        const isSecondApprover =
          currentUser && record.secondApproverId != null && Number(record.secondApproverId) === Number(currentUser.id)
        const isFirstApprover =
          currentUser && record.approverId != null && Number(record.approverId) === Number(currentUser.id)
        const canRejectPendingSecond =
          record.status === 'PENDING_SECOND' && canApprove && (isFirstApprover || isSecondApprover)

        return (
          <Space>
            {canViewDetail && (
              <Tooltip title="查看详情">
                <Button
                  type="link"
                  size="small"
                  icon={<EyeOutlined />}
                  onClick={() => openDetailModal(record)}
                />
              </Tooltip>
            )}
            {canWithdraw && (
              <Tooltip title="撤回申请">
                <Button
                  type="link"
                  size="small"
                  danger
                  icon={<RollbackOutlined />}
                  onClick={() => {
                    Modal.confirm({
                      title: '确认撤回',
                      content: '撤回后该申请将变为已取消，确定要撤回吗？',
                      onOk: () => handleWithdraw(record.id),
                    })
                  }}
                />
              </Tooltip>
            )}
            {record.status === 'PENDING' && canApprove && (
              <>
                <Tooltip title="审批">
                  <Button
                    type="link"
                    size="small"
                    icon={<CheckCircleOutlined />}
                    onClick={async () => {
                      outboundTestTrace('点击「审批」打开弹窗', {
                        id: record.id,
                        applyNumber: record.applyNumber,
                        status: record.status,
                      })
                      setCurrentRecord(record)
                      setStockCheckResult(null)
                      await checkSpecialDrugs(record.id)
                      await fetchStockCheckForApply(record.id)
                      await fetchSecondApproverCandidates()
                      setApproveModalVisible(true)
                      outboundTestTrace('审批弹窗已打开（已请求明细/库存校验/第二审批人候选）', {
                        applyId: record.id,
                      })
                    }}
                  />
                </Tooltip>
                <Tooltip title="驳回">
                  <Button
                    type="link"
                    size="small"
                    danger
                    icon={<CloseCircleOutlined />}
                    onClick={() => {
                      const applyId = record.id
                      Modal.confirm({
                        title: '确认驳回',
                        content: '请输入驳回理由',
                        onOk: (close) => {
                          const reason = prompt('请输入驳回理由:')
                          if (reason) {
                            handleReject(applyId, reason)
                            close()
                          }
                        },
                      })
                    }}
                  />
                </Tooltip>
              </>
            )}
            {record.status === 'PENDING_SECOND' && canApprove && isSecondApprover && (
              <Tooltip title="第二审批人本人确认通过">
                <Button
                  type="link"
                  size="small"
                  icon={<SafetyCertificateOutlined />}
                  onClick={() => {
                    Modal.confirm({
                      title: '第二审批确认',
                      content:
                        '请确认已复核本单药品、用途与数量。通过后申请将变为「已通过」，可由仓库执行出库。',
                      onOk: () => handleSecondApprove(record.id),
                    })
                  }}
                />
              </Tooltip>
            )}
            {canRejectPendingSecond && (
              <Tooltip title="驳回申请">
                <Button
                  type="link"
                  size="small"
                  danger
                  icon={<CloseCircleOutlined />}
                  onClick={() => {
                    const applyId = record.id
                    Modal.confirm({
                      title: '确认驳回',
                      content: '请输入驳回理由',
                      onOk: (close) => {
                        const reason = prompt('请输入驳回理由:')
                        if (reason) {
                          handleReject(applyId, reason)
                          close()
                        }
                      },
                    })
                  }}
                />
              </Tooltip>
            )}
            {record.status === 'APPROVED' && canExecute && (
              <Tooltip title="执行出库">
                <Button
                  type="link"
                  size="small"
                  icon={<PlayCircleOutlined />}
                  onClick={async () => {
                    outboundTestTrace('点击「执行出库」', {
                      id: record.id,
                      applyNumber: record.applyNumber,
                      status: record.status,
                    })
                    setCurrentRecord(record)
                    await fetchApplyItems(record.id)
                    setExecuteModalVisible(true)
                    outboundTestTrace('执行出库弹窗已打开', { applyId: record.id })
                  }}
                />
              </Tooltip>
            )}
          </Space>
        )
      },
    },
  ]

  return (
    <div style={pageRootStyle}>
      <div style={toolbarRowCompactStyle}>
        <h2 style={{ ...toolbarPageTitleStyle, whiteSpace: 'nowrap' }}>出库管理</h2>
        <div style={compactFilterRowStyle}>
          <div style={filterCellFlex('1.15 1 80px', 80, 220)}>
            <Input
              placeholder="搜索申领单号、申请人"
              value={filters.keyword}
              onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
              style={{ width: '100%' }}
              allowClear
            />
          </div>
          <div style={filterCellFlex('0.9 1 56px', 56, 140)}>
            <Input
              placeholder="所属科室"
              value={filters.department}
              onChange={(e) => setFilters({ ...filters, department: e.target.value })}
              style={{ width: '100%' }}
              allowClear
            />
          </div>
          <div style={{ flex: '0 0 auto', width: 120, minWidth: 112 }}>
            <Select
              placeholder="申请状态"
              value={filters.status}
              onChange={(value) => setFilters({ ...filters, status: value })}
              style={{ width: '100%' }}
              allowClear
            >
              <Select.Option value="PENDING">待审批</Select.Option>
              <Select.Option value="PENDING_SECOND">待第二审批</Select.Option>
              <Select.Option value="APPROVED">已通过</Select.Option>
              <Select.Option value="REJECTED">已驳回</Select.Option>
              <Select.Option value="OUTBOUND">已出库</Select.Option>
              <Select.Option value="CANCELLED">已取消</Select.Option>
            </Select>
          </div>
          <div style={filterCellFlex('1.1 1 200px', 180, 320)}>
            <RangePicker
              placeholder={['开始日期', '结束日期']}
              value={filters.startDate && filters.endDate
                ? [filters.startDate, filters.endDate]
                : null}
              onChange={(dates) => {
                setFilters({
                  ...filters,
                  startDate: dates ? dates[0] : undefined,
                  endDate: dates ? dates[1] : undefined,
                })
              }}
              style={{ width: '100%' }}
            />
          </div>
          <Space size={4} style={{ flexShrink: 0 }}>
            <Tooltip title="查询">
              <Button
                type="primary"
                icon={<SearchOutlined />}
                onClick={fetchOutboundApplies}
              />
            </Tooltip>
            <Tooltip title="重置">
              <Button icon={<ReloadOutlined />} onClick={handleReset} />
            </Tooltip>
            {canViewPickSummary && (
              <Tooltip title="按批次与存储位置汇总待出库药品，可打印拣货单">
                <Button
                  icon={<PrinterOutlined />}
                  onClick={() => {
                    setPickSummaryDate(dayjs())
                    setPickSummaryOpen(true)
                  }}
                />
              </Tooltip>
            )}
            {(canApplyOutbound || canProxyOutbound) && (
              <Tooltip
                title={
                  canProxyOutbound && !canApplyOutbound
                    ? '代录出库申请（现场）'
                    : canApplyOutbound && canProxyOutbound
                      ? '新建或代录出库申请'
                      : '新建出库申请'
                }
              >
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={async () => {
                    await fetchDepartmentOptions()
                    if (canProxyOutbound) {
                      setLoadingMedicalApplicants(true)
                      try {
                        const res = await request.get('/outbound/medical-applicants')
                        if (res.code === 200) {
                          setMedicalApplicants(Array.isArray(res.data) ? res.data : [])
                        }
                      } catch (error) {
                        logger.error('获取医护人员列表失败:', error)
                        message.error(error.response?.data?.msg || '获取医护人员列表失败')
                        setMedicalApplicants([])
                      } finally {
                        setLoadingMedicalApplicants(false)
                      }
                    }
                    setApplyInventoryBatches({})
                    setApplyInventoryBatchLoading({})
                    setModalVisible(true)
                    setApplyFormItems([{ drugId: undefined, quantity: undefined, batchNumber: undefined }])
                  }}
                />
              </Tooltip>
            )}
          </Space>
        </div>
      </div>

      <div style={tableAreaStyle}>
        <Table
          columns={columns}
          dataSource={outboundApplies}
          rowKey="id"
          loading={loading}
          size="middle"
          scroll={{ x: 'max-content', y: TABLE_SCROLL_Y }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          onChange={handleTableChange}
        />
      </div>

      {/* 新建 / 代录出库申请模态框 */}
      <Modal
        title={
          canProxyOutbound && !canApplyOutbound
            ? '代录出库申请（现场）'
            : canApplyOutbound && canProxyOutbound
              ? '新建 / 代录出库申请'
              : '新建出库申请'
        }
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false)
          setTimeout(() => {
            form.resetFields()
          }, 0)
          setApplyInventoryBatches({})
          setApplyInventoryBatchLoading({})
          setApplyFormItems([{ drugId: undefined, quantity: undefined, batchNumber: undefined }])
        }}
        afterOpenChange={(open) => {
          if (open) {
            form.resetFields()
          }
        }}
        onOk={() => form.submit()}
        width={900}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreateApply}
        >
          {canProxyOutbound && (
            <Form.Item
              name="applicantId"
              label="申领医护人员"
              rules={
                canApplyOutbound
                  ? []
                  : [{ required: true, message: '请选择到场申领的医护人员' }]
              }
              extra={
                canApplyOutbound
                  ? '不选则以当前账号作为申请人；现场代领时请勾选到场医护人员。'
                  : '请选择到场提出申领的医护人员，作为药品领用责任主体；本单由当前登录的仓库管理员代录。'
              }
            >
              <Select
                allowClear={!!canApplyOutbound}
                placeholder="请选择医护人员"
                loading={loadingMedicalApplicants}
                showSearch
                optionFilterProp="label"
                options={medicalApplicants.map((u) => ({
                  value: u.id,
                  label: u.roleName ? `${u.username}（${u.roleName}）` : u.username,
                }))}
              />
            </Form.Item>
          )}

          <Form.Item
            name="department"
            label="所属科室"
            rules={[
              { required: true, message: '请选择或输入所属科室' },
              { max: 100, message: '所属科室长度不能超过100个字符' },
            ]}
          >
            <AutoComplete
              options={departmentOptions.map((d) => ({ value: d }))}
              placeholder="请选择已有科室或直接输入新科室名称"
              filterOption={(inputValue, option) =>
                (option?.value ?? '').toLowerCase().includes((inputValue || '').toLowerCase())
              }
            />
          </Form.Item>

          <Form.Item
            name="purpose"
            label="用途说明"
            rules={[
              { required: true, message: '请输入用途说明' },
              { max: 200, message: '用途说明长度不能超过200个字符' },
            ]}
          >
            <TextArea
              rows={3}
              placeholder="请输入用途说明（如：门诊用药、手术用药等）"
            />
          </Form.Item>

          <Form.Item label="申领药品">
            {applyFormItems.map((item, index) => (
              <div key={index} style={{ marginBottom: 16, padding: 16, border: '1px solid #d9d9d9', borderRadius: 4 }}>
                <Space direction="vertical" style={{ width: '100%' }} size="small">
                  <Space wrap>
                    <Form.Item
                      label="药品"
                      style={{ width: 300, marginBottom: 0 }}
                      rules={[{ required: true, message: '请选择药品' }]}
                    >
                      <AutoComplete
                        options={drugOptions}
                        placeholder="请输入或选择药品"
                        value={
                          item.drugId
                            ? (() => {
                                const d = drugs.find((x) => x.id === item.drugId)
                                return d ? `${d.drugName}${d.specification ? ` (${d.specification})` : ''}` : ''
                              })()
                            : undefined
                        }
                        filterOption={(inputValue, option) =>
                          (option?.label ?? '').toString().toUpperCase().indexOf((inputValue || '').toUpperCase()) !== -1
                        }
                        onSelect={(value) => {
                          const newItems = [...applyFormItems]
                          const drugId = value != null && String(value) !== '' ? Number(value) : undefined
                          newItems[index].drugId = Number.isFinite(drugId) ? drugId : undefined
                          newItems[index].batchNumber = undefined
                          setApplyFormItems(newItems)
                          loadBatchesForApplyDrug(newItems[index].drugId)
                        }}
                        onChange={(value) => {
                          if (value == null || String(value).trim() === '') {
                            const newItems = [...applyFormItems]
                            newItems[index].drugId = undefined
                            newItems[index].batchNumber = undefined
                            setApplyFormItems(newItems)
                          }
                        }}
                        style={{ width: 300 }}
                      />
                    </Form.Item>

                    <Form.Item
                      label="申领数量"
                      style={{ width: 150, marginBottom: 0 }}
                      rules={[
                        { required: true, message: '请输入申领数量' },
                        { type: 'number', min: 1, message: '申领数量必须大于0' },
                      ]}
                    >
                      <InputNumber
                        style={{ width: 150 }}
                        placeholder="数量"
                        min={1}
                        precision={0}
                        onChange={(value) => {
                          const newItems = [...applyFormItems]
                          newItems[index].quantity = value
                          setApplyFormItems(newItems)
                        }}
                      />
                    </Form.Item>

                    <Form.Item
                      label="指定批次（可选）"
                      style={{ width: 320, marginBottom: 0 }}
                    >
                      {(() => {
                        const raw = item.drugId != null ? (applyInventoryBatches[item.drugId] || []) : []
                        const batches = raw.slice().sort((a, b) => {
                          if (!a?.expiryDate && !b?.expiryDate) return 0
                          if (!a?.expiryDate) return 1
                          if (!b?.expiryDate) return -1
                          return dayjs(a.expiryDate).valueOf() - dayjs(b.expiryDate).valueOf()
                        })
                        const loading = !!applyInventoryBatchLoading[item.drugId]
                        return (
                          <Select
                            allowClear
                            showSearch
                            optionFilterProp="label"
                            placeholder={
                              item.drugId
                                ? loading
                                  ? '正在加载批次…'
                                  : batches.length
                                    ? '不选则出库时按 FIFO'
                                    : '当前无库存批次记录'
                                : '请先选择药品'
                            }
                            disabled={!item.drugId}
                            loading={loading}
                            value={item.batchNumber}
                            onChange={(v) => {
                              const newItems = [...applyFormItems]
                              newItems[index].batchNumber = v
                              setApplyFormItems(newItems)
                            }}
                            style={{ width: 320 }}
                            options={batches.map((b) => ({
                              value: b.batchNumber,
                              label: `${b.batchNumber}（库存 ${b.quantity ?? 0}${b.expiryDate ? `，效期 ${dayjs(b.expiryDate).format('YYYY-MM-DD')}` : ''}）`,
                            }))}
                          />
                        )
                      })()}
                    </Form.Item>

                    {applyFormItems.length > 1 && (
                      <Tooltip title="删除">
                        <Button
                          type="link"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={() => removeApplyFormItem(index)}
                        />
                      </Tooltip>
                    )}
                  </Space>
                </Space>
              </div>
            ))}
            <Button
              type="dashed"
              onClick={addApplyFormItem}
              style={{ width: '100%' }}
            >
              + 添加药品
            </Button>
          </Form.Item>

          <Form.Item
            name="remark"
            label="备注"
            rules={[
              { max: 500, message: '备注长度不能超过500个字符' },
            ]}
          >
            <TextArea
              rows={3}
              placeholder="请输入备注（可选）"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 审批模态框 */}
      <Modal
        title={hasSpecialDrug ? '第一审批（出库申请 · 含特殊药品）' : '审批出库申请'}
        open={approveModalVisible}
        onCancel={() => {
          setApproveModalVisible(false)
          setTimeout(() => {
            approveForm.resetFields()
          }, 0)
          setApproveItems([])
          setHasSpecialDrug(false)
          setStockCheckResult(null)
        }}
        onOk={() => approveForm.submit()}
        okButtonProps={{ disabled: stockCheckResult && stockCheckResult.sufficient === false }}
        width={600}
      >
        {stockCheckResult && stockCheckResult.sufficient === false && (
          <Alert
            message="库存不足，无法审批通过"
            description={
              <div>
                <p style={{ marginBottom: 8 }}>{stockCheckResult.message}</p>
                {Array.isArray(stockCheckResult.details) && stockCheckResult.details.length > 0 && (
                  <ul style={{ marginBottom: 0 }}>
                    {stockCheckResult.details.filter(d => !d.sufficient).map((d, i) => (
                      <li key={i}>
                        {d.drugName}：需要 {d.required}，可用 {d.available}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            }
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}
        {stockCheckResult && stockCheckResult.sufficient === true && (
          <Alert message="当前库存充足，可审批通过。" type="success" showIcon style={{ marginBottom: 16 }} />
        )}
        {currentRecord && (
          <div style={{ marginBottom: 16 }}>
            <p><strong>申领单号：</strong>{currentRecord.applyNumber}</p>
            <p><strong>申请人：</strong>
              {currentRecord.applicantRoleName
                ? `${currentRecord.applicantName || '-'}（${currentRecord.applicantRoleName}）`
                : (currentRecord.applicantName ?? '-')}
            </p>
            {currentRecord.proxyRegistrarId ? (
              <p><strong>代录人：</strong>
                {currentRecord.proxyRegistrarRoleName
                  ? `${currentRecord.proxyRegistrarName || '-'}（${currentRecord.proxyRegistrarRoleName}）`
                  : (currentRecord.proxyRegistrarName ?? '-')}
              </p>
            ) : null}
            <p><strong>所属科室：</strong>{currentRecord.department}</p>
            <p><strong>用途：</strong>{currentRecord.purpose}</p>
          </div>
        )}

        {hasSpecialDrug && (
          <Alert
            message="特殊药品双人审批说明"
            description="此申请含特殊药品：您完成第一审批并指定第二审批人后，申请将进入「待第二审批」，须由第二审批人本人登录系统并点击「第二审批通过」后，本单才变为「已通过」并可执行出库。"
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}

        {approveItems.length > 0 && (
          <div style={{ marginBottom: 16 }}>
            <strong>申请明细：</strong>
            <ul style={{ marginTop: 8, marginBottom: 0 }}>
              {approveItems.map((item, index) => {
                const drug = drugs.find((d) => Number(d.id) === Number(item.drugId))
                const special =
                  Number(item.isSpecial) === 1 || (drug != null && Number(drug.isSpecial) === 1) // 接口 isSpecial 优先
                return (
                  <li key={index}>
                    {formatOutboundApplyItemDrugLabel(item, drugs)}
                    {' '}× {item.quantity}
                    {item.batchNumber ? <>，批次 <strong>{item.batchNumber}</strong></> : null}
                    {special ? (
                      <Tag color="red" style={{ marginLeft: 8 }}>特殊药品</Tag>
                    ) : null}
                  </li>
                )
              })}
            </ul>
          </div>
        )}

        <Form
          form={approveForm}
          layout="vertical"
          onFinish={handleApprove}
        >
          <Form.Item
            name="secondApproverId"
            label={hasSpecialDrug ? "第二审批人（特殊药品必填）" : "第二审批人（可选）"}
            rules={hasSpecialDrug ? [
              { required: true, message: '特殊药品必须指定第二审批人' },
            ] : []}
          >
            <Select
              placeholder={hasSpecialDrug ? "请选择第二审批人（必填）" : "请选择第二审批人（可选）"}
              loading={loadingUsers}
              showSearch
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
                  options={secondApproverCandidates
                .filter((user) => {
                  const currentUser = getUser()
                  if (currentUser && user.id === currentUser.id) return false // 第二审批人不能为第一审批人（当前用户）
                  if (currentRecord && user.id === currentRecord.applicantId) return false
                  return true
                })
                .map((user) => ({
                  value: user.id,
                  label: `${user.username} (${user.phone || '无手机号'})`,
                }))}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 查看详情模态框（只读，申请人可在此查看自己申请的内容） */}
      <Modal
        title="出库申请详情"
        open={detailModalVisible}
        onCancel={() => {
          setDetailModalVisible(false)
          setDetailItems([])
        }}
        footer={[
          currentRecord?.status === 'PENDING' && currentRecord?.applicantId === getUser()?.id && hasPermission(PERMISSIONS.OUTBOUND_APPLY) ? (
            <Button
              key="withdraw"
              danger
              icon={<RollbackOutlined />}
              onClick={() => {
                Modal.confirm({
                  title: '确认撤回',
                  content: '撤回后该申请将变为已取消，确定要撤回吗？',
                  onOk: () => handleWithdraw(currentRecord.id),
                })
              }}
            >
              撤回申请
            </Button>
          ) : null,
          <Button key="close" type="primary" onClick={() => { setDetailModalVisible(false); setDetailItems([]) }}>
            关闭
          </Button>,
        ].filter(Boolean)}
        width={560}
      >
        {currentRecord && (
          <div>
            <p><strong>申领单号：</strong>{currentRecord.applyNumber}</p>
            <p><strong>申请人：</strong>
              {currentRecord.applicantRoleName
                ? `${currentRecord.applicantName || '‑'}（${currentRecord.applicantRoleName}）`
                : (currentRecord.applicantName ?? '‑')}
            </p>
            {currentRecord.proxyRegistrarId ? (
              <p><strong>代录人：</strong>
                {currentRecord.proxyRegistrarRoleName
                  ? `${currentRecord.proxyRegistrarName || '‑'}（${currentRecord.proxyRegistrarRoleName}）`
                  : (currentRecord.proxyRegistrarName ?? '‑')}
              </p>
            ) : null}
            <p><strong>所属科室：</strong>{currentRecord.department}</p>
            <p><strong>用途：</strong>{currentRecord.purpose}</p>
            {currentRecord.remark ? <p><strong>申请备注：</strong>{currentRecord.remark}</p> : null}
            {currentRecord.rejectReason ? <p><strong>审核备注：</strong>{currentRecord.rejectReason}</p> : null}
            <p><strong>申请状态：</strong>{getStatusTag(currentRecord.status)}</p>
            {currentRecord.status === 'PENDING_SECOND' && (
              <Alert
                type="info"
                showIcon
                style={{ marginBottom: 12 }}
                message="等待第二审批人确认"
                description="第一审批已完成。请第二审批人本人登录后在列表中点击盾牌图标「第二审批通过」，或通过筛选「待第二审批」查找本单。"
              />
            )}
            {currentRecord.firstApproveTime ? (
              <p>
                <strong>第一审批时间：</strong>
                {dayjs(currentRecord.firstApproveTime).format('YYYY-MM-DD HH:mm:ss')}
              </p>
            ) : null}
            {currentRecord.secondApproverName ? (
              <p>
                <strong>第二审批人：</strong>
                {currentRecord.secondApproverRoleName
                  ? `${currentRecord.secondApproverName}（${currentRecord.secondApproverRoleName}）`
                  : currentRecord.secondApproverName}
              </p>
            ) : null}
            {detailItems.length > 0 && (
              <div style={{ marginTop: 12 }}>
                <strong>申请明细：</strong>
                <ul style={{ marginTop: 8, marginBottom: 0 }}>
                  {detailItems.map((item, index) => {
                    const drug = drugs.find((d) => Number(d.id) === Number(item.drugId))
                    const special =
                      Number(item.isSpecial) === 1 || (drug != null && Number(drug.isSpecial) === 1)
                    return (
                      <li key={index}>
                        {formatOutboundApplyItemDrugLabel(item, drugs)}
                        {' '}× {item.quantity}
                        {item.batchNumber ? <>，指定批次：<strong>{item.batchNumber}</strong></> : null}
                        {item.remark ? <>（备注：{item.remark}）</> : null}
                        {special ? (
                          <Tag color="red" style={{ marginLeft: 8 }}>特殊药品</Tag>
                        ) : null}
                      </li>
                    )
                  })}
                </ul>
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* 执行出库模态框 */}
      <Modal
        title="执行出库"
        open={executeModalVisible}
        onCancel={() => {
          setExecuteModalVisible(false)
          setTimeout(() => {
            executeForm.resetFields()
          }, 0)
          setApplyItems([])
          setInventoryBatches({})
        }}
        afterOpenChange={(open) => {
          if (!open || applyItems.length === 0) return
          executeForm.resetFields()
          const executeFields = {}
          for (const row of applyItems) {
            const bn = row.batchNumber != null && String(row.batchNumber).trim() !== '' ? String(row.batchNumber).trim() : undefined
            executeFields[`item_${row.id}`] = {
              actualQuantity: row.quantity,
              batchNumber: bn,
            }
          }
          executeForm.setFieldsValue(executeFields)
        }}
        onOk={() => executeForm.submit()}
        width={900}
      >
        {currentRecord && (
          <div style={{ marginBottom: 16 }}>
            <p><strong>申领单号：</strong>{currentRecord.applyNumber}</p>
            <p><strong>申请人：</strong>
              {currentRecord.applicantRoleName
                ? `${currentRecord.applicantName || '-'}（${currentRecord.applicantRoleName}）`
                : (currentRecord.applicantName ?? '-')}
            </p>
            {currentRecord.proxyRegistrarId ? (
              <p><strong>代录人：</strong>
                {currentRecord.proxyRegistrarRoleName
                  ? `${currentRecord.proxyRegistrarName || '-'}（${currentRecord.proxyRegistrarRoleName}）`
                  : (currentRecord.proxyRegistrarName ?? '-')}
              </p>
            ) : null}
            <p><strong>所属科室：</strong>{currentRecord.department}</p>
            <p><strong>用途：</strong>{currentRecord.purpose}</p>
          </div>
        )}

        <Form
          form={executeForm}
          layout="vertical"
          onFinish={handleExecute}
        >
          {applyItems.map((item) => {
            const batches = inventoryBatches[item.drugId] || []
            return (
              <div key={item.id} style={{ marginBottom: 16, padding: 16, border: '1px solid #d9d9d9', borderRadius: 4 }}>
                <Space direction="vertical" style={{ width: '100%' }} size="small">
                  <div>
                    <strong>药品：</strong>
                    {formatOutboundApplyItemDrugLabel(item, drugs)}
                  </div>
                  <div>
                    <strong>申请数量：</strong>{item.quantity}
                  </div>
                  {item.batchNumber ? (
                    <div>
                      <strong>申请指定批次：</strong>{item.batchNumber}
                    </div>
                  ) : null}
                  <Space wrap>
                    <Form.Item
                      name={[`item_${item.id}`, 'batchNumber']}
                      label="批次号（未选时沿用申请时的指定批次，再无则 FIFO）"
                      style={{ width: 280, marginBottom: 0 }}
                    >
                      <Select
                        placeholder="选择批次或清空（见说明）"
                        showSearch
                        allowClear
                      >
                        {batches.map(batch => (
                          <Select.Option key={batch.batchNumber} value={batch.batchNumber}>
                            {batch.batchNumber} (库存: {batch.quantity}, 有效期: {batch.expiryDate ? dayjs(batch.expiryDate).format('YYYY-MM-DD') : '-'})
                          </Select.Option>
                        ))}
                      </Select>
                    </Form.Item>

                    <Form.Item
                      name={[`item_${item.id}`, 'actualQuantity']}
                      label="实际出库数量"
                      rules={[
                        { required: true, message: '请输入实际出库数量' },
                        { type: 'number', min: 1, message: '实际出库数量必须大于0' },
                      ]}
                      style={{ width: 150, marginBottom: 0 }}
                      initialValue={item.quantity}
                    >
                      <InputNumber
                        style={{ width: 150 }}
                        placeholder="实际数量"
                        min={1}
                        max={item.quantity}
                        precision={0}
                      />
                    </Form.Item>
                  </Space>
                </Space>
              </div>
            )
          })}
        </Form>
      </Modal>

      <Modal
        title="出库拣货汇总（现场打印）"
        open={pickSummaryOpen}
        onCancel={() => setPickSummaryOpen(false)}
        width={960}
        footer={[
          <Button key="close" onClick={() => setPickSummaryOpen(false)}>关闭</Button>,
          <Button key="print" type="primary" icon={<PrinterOutlined />} onClick={handlePrintPickSummary} disabled={!pickSummaryData}>
            打印
          </Button>,
        ]}
      >
        <Space wrap style={{ marginBottom: 12 }}>
          <Select
            value={pickSummaryScope}
            style={{ width: 200 }}
            onChange={(v) => setPickSummaryScope(v)}
            options={[
              { value: 'approve_day', label: '按审批通过日（默认当天）' },
              { value: 'all_pending', label: '全部待执行申领单' },
            ]}
          />
          {pickSummaryScope === 'approve_day' && (
            <DatePicker
              value={pickSummaryDate}
              onChange={(d) => d && setPickSummaryDate(d)}
              allowClear={false}
            />
          )}
          <Button type="primary" loading={pickSummaryLoading} onClick={loadPickSummary}>
            刷新
          </Button>
        </Space>
        <p style={{ fontSize: 12, color: 'rgba(0,0,0,0.55)', marginBottom: 12 }}>
          汇总已通过审批、尚未执行出库的申领单；未指定批次时按先到期先出（FIFO）与库存货位模拟分配，便于到库位拣货；打印后可按单执行出库并交接医护人员。
        </p>
        {pickSummaryData?.warnings?.length > 0 && (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 12 }}
            message="库存或分配提示"
            description={
              <ul style={{ margin: 0, paddingLeft: 18 }}>
                {pickSummaryData.warnings.map((t, i) => (
                  <li key={i}>{t}</li>
                ))}
              </ul>
            }
          />
        )}
        <h4 style={{ marginBottom: 8 }}>按货位汇总（拣货主表）</h4>
        <Table
          size="small"
          loading={pickSummaryLoading}
          rowKey={(r) =>
            [r.drugId, r.batchNumber ?? '', r.storageLocation ?? ''].join('\u0000')
          }
          dataSource={pickSummaryData?.summary || []}
          pagination={false}
          scroll={{ x: 'max-content' }}
          columns={[
            { title: '存储位置', dataIndex: 'storageLocation', width: 140, ellipsis: true, render: (v) => v || '—' },
            { title: '药品名称', dataIndex: 'drugName', width: 160, ellipsis: true },
            { title: '规格', dataIndex: 'specification', width: 120, ellipsis: true, render: (v) => v || '—' },
            { title: '批次号', dataIndex: 'batchNumber', width: 120, ellipsis: true, render: (v) => v || '—' },
            {
              title: '效期',
              dataIndex: 'expiryDate',
              width: 110,
              render: (d) => (d ? dayjs(d).format('YYYY-MM-DD') : '—'),
            },
            { title: '数量', dataIndex: 'quantity', width: 72 },
          ]}
        />
        <h4 style={{ margin: '16px 0 8px' }}>按申领单明细（核对交接）</h4>
        <Table
          size="small"
          loading={pickSummaryLoading}
          rowKey={(r) =>
            [
              r.applyNumber,
              r.drugId,
              r.batchNumber ?? '',
              r.storageLocation ?? '',
              r.expiryDate ?? '',
              r.quantity,
              r.approveTime ?? '',
            ].join('\u0000')
          }
          dataSource={pickSummaryData?.pickLines || []}
          pagination={false}
          scroll={{ x: 'max-content', y: 280 }}
          columns={[
            { title: '申领单号', dataIndex: 'applyNumber', width: 130, ellipsis: true },
            { title: '科室', dataIndex: 'department', width: 100, ellipsis: true },
            {
              title: '审批时间',
              dataIndex: 'approveTime',
              width: 160,
              render: (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '—'),
            },
            { title: '药品名称', dataIndex: 'drugName', width: 140, ellipsis: true },
            { title: '规格', dataIndex: 'specification', width: 100, ellipsis: true, render: (v) => v || '—' },
            { title: '批次号', dataIndex: 'batchNumber', width: 120, ellipsis: true, render: (v) => v || '—' },
            { title: '存储位置', dataIndex: 'storageLocation', width: 120, ellipsis: true, render: (v) => v || '—' },
            {
              title: '效期',
              dataIndex: 'expiryDate',
              width: 100,
              render: (d) => (d ? dayjs(d).format('YYYY-MM-DD') : '—'),
            },
            { title: '数量', dataIndex: 'quantity', width: 64 },
          ]}
        />
      </Modal>
    </div>
  )
}

export default OutboundManagement


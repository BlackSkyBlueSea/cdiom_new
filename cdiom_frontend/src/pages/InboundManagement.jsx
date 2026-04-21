import { useState, useEffect, useMemo, useCallback } from 'react'
import { Table, Button, Space, Input, Select, DatePicker, Tag, Modal, Form, message, AutoComplete, InputNumber, Tooltip, Alert, Switch, Descriptions, Spin } from 'antd'
import { SearchOutlined, ReloadOutlined, PlusOutlined, CheckCircleOutlined, CloseCircleOutlined, BarcodeOutlined, RollbackOutlined, EyeOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request from '../utils/request'
import logger from '../utils/logger'
import { hasPermission, PERMISSIONS } from '../utils/permission'
import { getUser } from '../utils/auth'
import {
  pageRootStyle,
  tableAreaStyle,
  toolbarRowCompactStyle,
  toolbarPageTitleStyle,
  compactFilterRowStyle,
  TABLE_SCROLL_Y,
} from '../utils/tablePageLayout'

const { RangePicker } = DatePicker
const { TextArea } = Input

/** 入库筛选区 placeholder 文案（宽度按字数对齐） */
const INBOUND_PH_SEARCH = '搜索入库单号、批次号、药品名称'
const INBOUND_PH_STATUS = '验收状态'
const INBOUND_PH_EXPIRY = '效期校验'
const INBOUND_PH_SECOND = '双人确认'
const INBOUND_PH_RANGE = ['开始日期', '结束日期']

/** 默认 14px 字号下，中文约一字宽 14px；extra 含左右 padding、边框、清除/下拉/日历图标区 */
const INBOUND_CHAR_PX = 14
const fitW = (text, extra) => `${[...text].length * INBOUND_CHAR_PX + extra}px`
/**
 * RangePicker 为左右两段独立输入，每段需单独容纳 placeholder，否则会省略为「开始日…」。
 * perSegPad：每段除文字外的内边距与光标区；middlePad：中间分隔与右侧日历图标区。
 */
const fitRangeW = (a, b, perSegPad = 38, middlePad = 38) => {
  const seg = (t) => [...t].length * INBOUND_CHAR_PX + perSegPad
  return `${seg(a) + seg(b) + middlePad}px`
}

const inboundSearchWrap = { flex: '0 0 auto', width: fitW(INBOUND_PH_SEARCH, 48) }
const inboundStatusWrap = { flex: '0 0 auto', width: fitW(INBOUND_PH_STATUS, 52) }
const inboundExpiryWrap = { flex: '0 0 auto', width: fitW(INBOUND_PH_EXPIRY, 52) }
const inboundSecondWrap = { flex: '0 0 auto', width: fitW(INBOUND_PH_SECOND, 52) }
const inboundRangePickerWidth = fitRangeW(INBOUND_PH_RANGE[0], INBOUND_PH_RANGE[1])
const inboundRangeWrap = {
  flex: '0 0 auto',
  flexShrink: 0,
  width: inboundRangePickerWidth,
  minWidth: inboundRangePickerWidth,
}

/** 拆行验收时后端写入备注中的追溯码，避免依赖数据库额外表字段 */
function parseAcceptanceTraceFromRemark(remark) {
  if (!remark || typeof remark !== 'string') return null
  const m = remark.match(/追溯码:\s*([a-f0-9]+)/i)
  return m ? m[1] : null
}

/** 与后端 InboundDispositionCodes 一致，接口失败时用于下拉与列表展示 */
const DISPOSITION_LABELS_FALLBACK = {
  PENDING: '待处理',
  RETURN_SUPPLIER: '拟退回供应商',
  DESTROY: '拟销毁',
  HOLD: '暂存待处理',
  OTHER: '其他',
}

const SECOND_CONFIRM_LABELS = {
  NONE: '不适用',
  CONFIRMED: '已入账',
  PENDING_SECOND: '待第二人确认',
  REJECTED: '第二人已驳回',
  WITHDRAWN: '第一人已撤回',
  TIMEOUT: '超时关闭',
}

const EXPIRY_CHECK_LABELS = {
  PASS: '通过',
  WARNING: '警告（不足180天）',
  FORCE: '强制入库',
}

const InboundManagement = () => {
  const [inboundRecords, setInboundRecords] = useState([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [filters, setFilters] = useState({
    keyword: '',
    orderId: undefined,
    drugId: undefined,
    operatorId: undefined,
    startDate: undefined,
    endDate: undefined,
    status: undefined,
    expiryCheckStatus: undefined,
    secondConfirmStatus: undefined,
  })
  const [modalVisible, setModalVisible] = useState(false)
  const [inboundType, setInboundType] = useState('order') // 'order' 或 'temporary'
  const [form] = Form.useForm()
  const [drugs, setDrugs] = useState([])
  const [orders, setOrders] = useState([])
  const [selectedDrug, setSelectedDrug] = useState(null)
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [orderItems, setOrderItems] = useState([])
  const [needForceReason, setNeedForceReason] = useState(false)
  const [users, setUsers] = useState([])
  const [loadingUsers, setLoadingUsers] = useState(false)
  const [rejectModalVisible, setRejectModalVisible] = useState(false)
  const [rejectingRecordId, setRejectingRecordId] = useState(null)
  const [rejectForm] = Form.useForm()
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailRecord, setDetailRecord] = useState(null)
  /** 采购订单入库：同一车多药品共用的到货批次头 ID（首条入库成功后由接口返回） */
  const [receiptBatchId, setReceiptBatchId] = useState(null)
  const [receiptBatchCode, setReceiptBatchCode] = useState('')
  /** 当前所选订单各药品：订单量 / 已占用 / 剩余可入（与提交校验口径一致） */
  const [orderRemainRows, setOrderRemainRows] = useState([])
  const [orderRemainLoading, setOrderRemainLoading] = useState(false)
  /** 同一批号同时登记合格+不合格（采购订单入库） */
  const [splitAcceptanceMode, setSplitAcceptanceMode] = useState(false)
  /** 不合格处置意向 code -> 展示名（来自 GET /inbound/disposition-options） */
  const [dispositionLabelMap, setDispositionLabelMap] = useState({})

  const watchedStatus = Form.useWatch('status', form)
  const watchedDrugId = Form.useWatch('drugId', form)

  const orderRemainColumns = useMemo(
    () => [
      {
        title: '药品',
        dataIndex: 'drugName',
        key: 'drugName',
        ellipsis: true,
        render: (t, r) => {
          const spec = r.specification ? ` (${r.specification})` : ''
          return `${t || '—'}${spec}`
        },
      },
      {
        title: '订单数量',
        dataIndex: 'orderedQuantity',
        key: 'orderedQuantity',
        width: 96,
        align: 'right',
      },
      {
        title: '已占用',
        dataIndex: 'committedQuantity',
        key: 'committedQuantity',
        width: 100,
        align: 'right',
        render: (v) => v ?? 0,
      },
      {
        title: '剩余可入',
        dataIndex: 'remainingQuantity',
        key: 'remainingQuantity',
        width: 100,
        align: 'right',
        render: (v) => {
          const n = v ?? 0
          if (n <= 0) return <Tag color="default">0</Tag>
          return <Tag color="processing">{n}</Tag>
        },
      },
    ],
    []
  )

  const remainByDrugId = useMemo(() => {
    const m = {}
    for (const r of orderRemainRows) {
      m[Number(r.drugId)] = r
    }
    return m
  }, [orderRemainRows])

  const selectedRemainRow = useMemo(
    () => orderRemainRows.find((r) => Number(r.drugId) === Number(watchedDrugId)),
    [orderRemainRows, watchedDrugId]
  )

  const showSecondOperatorField = useMemo(() => {
    if (splitAcceptanceMode) {
      return !!(selectedDrug && selectedDrug.isSpecial === 1)
    }
    if (watchedStatus !== 'QUALIFIED') return false
    if (!selectedDrug || selectedDrug.isSpecial !== 1) return false
    return true
  }, [splitAcceptanceMode, watchedStatus, selectedDrug])

  const fetchUsers = async () => {
    setLoadingUsers(true)
    try {
      const res = await request.get('/inbound/second-operator-candidates')
      if (res.code === 200) {
        setUsers(Array.isArray(res.data) ? res.data : [])
      }
    } catch (error) {
      logger.error('获取第二操作人候选失败:', error)
    } finally {
      setLoadingUsers(false)
    }
  }

  useEffect(() => {
    if (
      modalVisible &&
      (hasPermission(PERMISSIONS.DRUG_MANAGE) ||
        hasPermission(PERMISSIONS.INBOUND_CREATE) ||
        hasPermission(PERMISSIONS.INBOUND_APPROVE) ||
        hasPermission(PERMISSIONS.INBOUND_EXECUTE))
    ) {
      fetchUsers()
    }
  }, [modalVisible])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const res = await request.get('/inbound/disposition-options')
        if (!cancelled && res.code === 200 && res.data && typeof res.data === 'object') {
          setDispositionLabelMap(res.data)
        }
      } catch (e) {
        logger.error('获取处置意向选项失败:', e)
        if (!cancelled) {
          setDispositionLabelMap({ ...DISPOSITION_LABELS_FALLBACK })
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const dispositionSelectOptions = useMemo(() => {
    const src =
      Object.keys(dispositionLabelMap).length > 0 ? dispositionLabelMap : DISPOSITION_LABELS_FALLBACK
    return Object.entries(src).map(([value, label]) => ({
      value,
      label: `${label}`,
    }))
  }, [dispositionLabelMap])

  useEffect(() => {
    if (!showSecondOperatorField) {
      form.setFieldsValue({ secondOperatorId: undefined })
    }
  }, [showSecondOperatorField, form])

  useEffect(() => {
    fetchInboundRecords()
  }, [pagination.current, pagination.pageSize, filters])

  useEffect(() => {
    fetchDrugs()
    if (inboundType === 'order') {
      fetchOrders()
    }
  }, [inboundType])

  // 药品选项（用于 AutoComplete）：value 须为字符串，避免 combobox 下 number 类型警告
  const drugOptions = useMemo(() => {
    return drugs.map((drug) => ({
      value: String(drug.id),
      label: `${drug.drugName || ''} (${drug.specification || ''})`.replace(/\s*\(\s*\)$/, '').trim() || String(drug.id),
      drug,
    }))
  }, [drugs])

  // 采购订单选项（用于Select）：显示「订单编号 - 供应商名称」，无供应商时只显示订单编号
  const orderOptions = useMemo(() => {
    return orders
      .filter(order => order.status === 'SHIPPED')
      .map(order => ({
        value: order.id,
        label: order.supplierName ? `${order.orderNumber} - ${order.supplierName}` : (order.orderNumber || `订单#${order.id}`),
        order: order
      }))
  }, [orders])

  const fetchInboundRecords = async () => {
    setLoading(true)
    try {
      const params = {
        page: pagination.current,
        size: pagination.pageSize,
        keyword: filters.keyword || undefined,
        orderId: filters.orderId,
        drugId: filters.drugId,
        operatorId: filters.operatorId,
        startDate: filters.startDate ? filters.startDate.format('YYYY-MM-DD') : undefined,
        endDate: filters.endDate ? filters.endDate.format('YYYY-MM-DD') : undefined,
        status: filters.status,
        expiryCheckStatus: filters.expiryCheckStatus,
        secondConfirmStatus: filters.secondConfirmStatus,
      }
      const res = await request.get('/inbound', { params })
      if (res.code === 200) {
        setInboundRecords(res.data.records || [])
        setPagination({
          ...pagination,
          total: res.data.total || 0,
        })
      } else {
        message.error(res.msg || '获取入库记录失败')
      }
    } catch (error) {
      logger.error('获取入库记录失败:', error)
      message.error('获取入库记录失败')
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
      orderId: undefined,
      drugId: undefined,
      operatorId: undefined,
      startDate: undefined,
      endDate: undefined,
      status: undefined,
      expiryCheckStatus: undefined,
      secondConfirmStatus: undefined,
    })
    setPagination({ ...pagination, current: 1 })
  }

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

  const fetchOrders = async () => {
    try {
      const res = await request.get('/purchase-orders', {
        params: { page: 1, size: 1000, status: 'SHIPPED' }
      })
      if (res.code === 200) {
        setOrders(res.data.records || [])
      }
    } catch (error) {
      logger.error('获取采购订单列表失败:', error)
    }
  }

  const fetchOrderInboundRemaining = async (orderId, showLoading = false) => {
    if (!orderId) {
      setOrderRemainRows([])
      return
    }
    if (showLoading) setOrderRemainLoading(true)
    try {
      const res = await request.get(`/inbound/order/${orderId}/inbound-remaining`)
      if (res.code === 200) {
        setOrderRemainRows(res.data || [])
      } else {
        setOrderRemainRows([])
      }
    } catch (error) {
      logger.error('获取订单剩余可入库失败:', error)
      setOrderRemainRows([])
    } finally {
      if (showLoading) setOrderRemainLoading(false)
    }
  }

  const handleOrderChange = async (orderId) => {
    if (!orderId) {
      setSelectedOrder(null)
      setOrderItems([])
      setOrderRemainRows([])
      setOrderRemainLoading(false)
      setReceiptBatchId(null)
      setReceiptBatchCode('')
      setSplitAcceptanceMode(false)
      form.setFieldsValue({ drugId: undefined })
      return
    }

    try {
      let order = orders.find(o => o.id === orderId)
      if (!order) {
        const res = await request.get(`/purchase-orders/${orderId}`)
        if (res.code === 200 && res.data) order = res.data
      }
      if (!order) {
        message.error('未找到该订单')
        return
      }
      setSelectedOrder(order)
      setReceiptBatchId(null)
      setReceiptBatchCode('')
      setSplitAcceptanceMode(false)
      form.setFieldsValue({ drugId: undefined, quantity: undefined })
      setSelectedDrug(null)

      const res = await request.get(`/purchase-orders/${orderId}/items`)
      if (res.code === 200) {
        setOrderItems(res.data || [])
      }
      await fetchOrderInboundRemaining(orderId, true)
    } catch (error) {
      logger.error('获取订单明细失败:', error)
      message.error('获取订单明细失败')
      setOrderRemainLoading(false)
    }
  }

  // 扫描/输入订单编号识别订单（条形码内容为订单编号）
  const [barcodeOrderNumber, setBarcodeOrderNumber] = useState('')
  const [barcodeLoading, setBarcodeLoading] = useState(false)
  const handleBarcodeRecognize = async () => {
    const num = (barcodeOrderNumber || '').trim()
    if (!num) {
      message.warning('请输入或扫描订单编号')
      return
    }
    setBarcodeLoading(true)
    try {
      const res = await request.get(`/purchase-orders/order-number/${encodeURIComponent(num)}`)
      if (res.code !== 200 || !res.data) {
        message.error('未找到该订单编号')
        return
      }
      const order = res.data
      if (order.status !== 'SHIPPED') {
        message.warning('仅支持已发货订单入库，当前订单状态：' + (order.status === 'PENDING' ? '待确认' : order.status === 'CONFIRMED' ? '待发货' : order.status === 'REJECTED' ? '已拒绝' : order.status === 'RECEIVED' ? '已入库' : order.status === 'CANCELLED' ? '已取消' : order.status))
        return
      }
      setOrders(prev => (prev.some(o => o.id === order.id) ? prev : [...prev, order]))
      form.setFieldsValue({ orderId: order.id })
      await handleOrderChange(order.id)
      setBarcodeOrderNumber('')
      message.success('已识别订单：' + order.orderNumber)
    } catch (e) {
      logger.error('根据订单编号查询失败', e)
      message.error(e?.response?.data?.msg || '识别订单失败')
    } finally {
      setBarcodeLoading(false)
    }
  }

  const handleDrugSelect = (value, option) => {
    const drug = option.drug
    setSelectedDrug(drug)
    const drugId = value != null && String(value) !== '' ? Number(value) : undefined
    form.setFieldsValue({
      drugId: Number.isFinite(drugId) ? drugId : undefined,
      manufacturer: drug.manufacturer || '',
      storageLocation: drug.storageLocation || '',
    })
  }

  const handleCreateSplitInbound = async (values) => {
    try {
      if (!values.drugId) {
        message.error('请选择药品')
        return
      }
      if (!values.batchNumber || values.batchNumber.trim() === '') {
        message.error('请输入批次号')
        return
      }
      const qn = Number(values.qualifiedQuantity)
      const unq = Number(values.unqualifiedQuantity)
      if (!Number.isFinite(qn) || qn < 1) {
        message.error('合格数量须为 ≥1 的整数')
        return
      }
      if (!Number.isFinite(unq) || unq < 1) {
        message.error('不合格数量须为 ≥1 的整数')
        return
      }
      if (!values.unqualifiedReason || String(values.unqualifiedReason).trim() === '') {
        message.error('请填写不合格原因（可追溯）')
        return
      }
      if (!values.expiryDate) {
        message.error('请选择有效期至')
        return
      }
      if (!values.productionDate) {
        message.error('请选择生产日期')
        return
      }
      const loc = values.storageLocation != null ? String(values.storageLocation).trim() : ''
      if (!loc) {
        message.error('合格部分须填写存储位置')
        return
      }
      if (!values.orderId) {
        message.error('请选择采购订单')
        return
      }
      if (!receiptBatchId && (!values.deliveryNoteNumber || String(values.deliveryNoteNumber).trim() === '')) {
        message.error('请填写随货同行单编号（按批次留痕；同一车第二味药起将自动沿用上批）')
        return
      }
      if (!receiptBatchId && !values.arrivalAt) {
        message.error('请选择本批到货时间')
        return
      }
      const orderItem = orderItems.find((item) => Number(item.drugId) === Number(values.drugId))
      if (!orderItem) {
        message.error('所选药品不在该采购订单中')
        return
      }
      const committedRes = await request.get(`/inbound/order/${values.orderId}/drug/${values.drugId}/committed-quantity`)
      const committedQty = committedRes.code === 200 ? (committedRes.data || 0) : 0
      const remainingQuantity = orderItem.quantity - committedQty
      if (qn + unq > remainingQuantity) {
        message.error(
          `合格+不合格数量不能超过订单剩余数量（剩余：${remainingQuantity}，含已登记不合格）`
        )
        return
      }
      const drugForSecond = drugs.find((d) => Number(d.id) === Number(values.drugId))
      if (drugForSecond && drugForSecond.isSpecial === 1) {
        if (!values.secondOperatorId) {
          message.error('特殊药品合格部分须指定第二操作人')
          return
        }
        const me = getUser()
        if (me && Number(values.secondOperatorId) === Number(me.id)) {
          message.error('第二操作人不能与当前操作人（第一操作人）相同')
          return
        }
      }

      const data = {
        orderId: values.orderId,
        drugId: values.drugId,
        batchNumber: values.batchNumber,
        qualifiedQuantity: qn,
        unqualifiedQuantity: unq,
        unqualifiedReason: String(values.unqualifiedReason).trim(),
        unqualifiedDispositionCode: values.unqualifiedDispositionCode || 'PENDING',
        unqualifiedDispositionRemark: values.unqualifiedDispositionRemark,
        expiryDate: values.expiryDate ? values.expiryDate.format('YYYY-MM-DD') : undefined,
        productionDate: values.productionDate ? values.productionDate.format('YYYY-MM-DD') : undefined,
        manufacturer: values.manufacturer,
        deliveryNoteNumber: values.deliveryNoteNumber,
        deliveryNoteImage: values.deliveryNoteImage,
        storageLocation: loc,
        expiryCheckReason: values.expiryCheckReason,
        remark: values.remark,
        secondOperatorId: showSecondOperatorField ? values.secondOperatorId : undefined,
      }
      if (receiptBatchId) {
        data.receiptBatchId = receiptBatchId
      } else if (values.arrivalAt) {
        data.arrivalAt = values.arrivalAt.format('YYYY-MM-DD HH:mm:ss')
      }

      if (data.expiryDate) {
        const checkRes = await request.post('/inbound/check-expiry', {
          expiryDate: data.expiryDate,
        })
        if (checkRes.code === 200) {
          data.expiryCheckStatus = checkRes.data.status
          if (checkRes.data.status === 'FORCE' && !data.expiryCheckReason) {
            message.warning('有效期不足90天，需要填写强制入库原因')
            return
          }
        }
      }

      if (data.productionDate && data.expiryDate) {
        const production = dayjs(data.productionDate)
        const expiry = dayjs(data.expiryDate)
        if (production.isAfter(expiry)) {
          message.error('生产日期不能晚于有效期')
          return
        }
      }

      const res = await request.post('/inbound/from-order/split', data)
      if (res.code === 200) {
        message.success(res.msg || '拆行验收已提交')
        if (res.data?.qualifiedRecord?.receiptBatchId) {
          setReceiptBatchId(res.data.qualifiedRecord.receiptBatchId)
          setReceiptBatchCode(res.data.qualifiedRecord.receiptBatchCode || '')
          form.setFieldsValue({
            drugId: undefined,
            qualifiedQuantity: undefined,
            unqualifiedQuantity: undefined,
            unqualifiedReason: undefined,
            unqualifiedDispositionCode: 'PENDING',
            unqualifiedDispositionRemark: undefined,
            batchNumber: undefined,
            expiryDate: undefined,
            productionDate: undefined,
            manufacturer: undefined,
            storageLocation: undefined,
            secondOperatorId: undefined,
            expiryCheckReason: undefined,
          })
          setSelectedDrug(null)
          setNeedForceReason(false)
          if (selectedOrder?.id) {
            fetchOrderInboundRemaining(selectedOrder.id, false)
          }
          message.info('可继续录入本批其它药品；换车请点击「新开一批」或关闭弹窗')
          fetchInboundRecords()
        } else {
          setModalVisible(false)
          form.resetFields()
          setSelectedDrug(null)
          setSelectedOrder(null)
          setOrderItems([])
          setReceiptBatchId(null)
          setReceiptBatchCode('')
          setOrderRemainRows([])
          setSplitAcceptanceMode(false)
          fetchInboundRecords()
        }
      } else {
        message.error(res.msg || '拆行验收失败')
      }
    } catch (error) {
      logger.error('拆行验收失败:', error)
      message.error(error.response?.data?.msg || error.message || '拆行验收失败')
    }
  }

  const handleCreateInbound = async (values) => {
    try {
      if (inboundType === 'order' && splitAcceptanceMode) {
        await handleCreateSplitInbound(values)
        return
      }
      // 验证必填字段
      if (!values.drugId) {
        message.error('请选择药品')
        return
      }
      if (!values.batchNumber || values.batchNumber.trim() === '') {
        message.error('请输入批次号')
        return
      }
      if (!values.quantity || values.quantity <= 0) {
        message.error('请输入有效的入库数量')
        return
      }
      if (!values.expiryDate) {
        message.error('请选择有效期至')
        return
      }
      if (!values.productionDate) {
        message.error('请选择生产日期')
        return
      }
      if (!values.status) {
        message.error('请选择验收状态')
        return
      }
      if (values.status === 'QUALIFIED') {
        const loc = values.storageLocation != null ? String(values.storageLocation).trim() : ''
        if (!loc) {
          message.error('合格入库须填写存储位置')
          return
        }
      }

      // 如果是采购订单入库，验证订单和数量
      if (inboundType === 'order' && values.orderId) {
        if (!receiptBatchId && (!values.deliveryNoteNumber || String(values.deliveryNoteNumber).trim() === '')) {
          message.error('请填写随货同行单编号（按批次留痕；同一车第二味药起将自动沿用上批）')
          return
        }
        if (!receiptBatchId && !values.arrivalAt) {
          message.error('请选择本批到货时间')
          return
        }
        const orderItem = orderItems.find((item) => Number(item.drugId) === Number(values.drugId))
        if (!orderItem) {
          message.error('所选药品不在该采购订单中')
          return
        }
        const committedRes = await request.get(`/inbound/order/${values.orderId}/drug/${values.drugId}/committed-quantity`)
        const committedQty = committedRes.code === 200 ? (committedRes.data || 0) : 0
        const remainingQuantity = orderItem.quantity - committedQty
        if (values.quantity > remainingQuantity) {
          message.error(`入库数量不能超过订单剩余数量（剩余：${remainingQuantity}，含待第二人确认）`)
          return
        }
      }

      const drugForSecond = inboundType === 'order'
        ? drugs.find(d => Number(d.id) === Number(values.drugId))
        : selectedDrug
      if (values.status === 'QUALIFIED' && drugForSecond && drugForSecond.isSpecial === 1) {
        if (!values.secondOperatorId) {
          message.error('特殊药品初验合格须指定第二操作人')
          return
        }
        const me = getUser()
        if (me && Number(values.secondOperatorId) === Number(me.id)) {
          message.error('第二操作人不能与当前操作人（第一操作人）相同')
          return
        }
      }

      const data = {
        orderId: values.orderId,
        drugId: values.drugId,
        batchNumber: values.batchNumber,
        quantity: values.quantity,
        expiryDate: values.expiryDate ? values.expiryDate.format('YYYY-MM-DD') : undefined,
        productionDate: values.productionDate ? values.productionDate.format('YYYY-MM-DD') : undefined,
        manufacturer: values.manufacturer,
        deliveryNoteNumber: values.deliveryNoteNumber,
        deliveryNoteImage: values.deliveryNoteImage,
        status: values.status,
        storageLocation:
          values.storageLocation != null && String(values.storageLocation).trim() !== ''
            ? String(values.storageLocation).trim()
            : undefined,
        expiryCheckReason: values.expiryCheckReason,
        remark: values.remark,
        secondOperatorId: showSecondOperatorField ? values.secondOperatorId : undefined,
      }
      if (values.status === 'UNQUALIFIED') {
        data.dispositionCode = values.dispositionCode || 'PENDING'
        data.dispositionRemark = values.dispositionRemark
      }
      if (inboundType === 'order') {
        if (receiptBatchId) {
          data.receiptBatchId = receiptBatchId
        } else if (values.arrivalAt) {
          data.arrivalAt = values.arrivalAt.format('YYYY-MM-DD HH:mm:ss')
        }
      } else {
        data.arrivalDate = values.arrivalDate ? values.arrivalDate.format('YYYY-MM-DD') : undefined
      }
      
      // 效期校验
      if (data.expiryDate) {
        const checkRes = await request.post('/inbound/check-expiry', {
          expiryDate: data.expiryDate,
        })
        if (checkRes.code === 200) {
          data.expiryCheckStatus = checkRes.data.status
          if (checkRes.data.status === 'FORCE' && !data.expiryCheckReason) {
            message.warning('有效期不足90天，需要填写强制入库原因')
            return
          }
        }
      }

      // 验证生产日期不能晚于有效期
      if (data.productionDate && data.expiryDate) {
        const production = dayjs(data.productionDate)
        const expiry = dayjs(data.expiryDate)
        if (production.isAfter(expiry)) {
          message.error('生产日期不能晚于有效期')
          return
        }
      }

      const url = inboundType === 'order' ? '/inbound/from-order' : '/inbound/temporary'
      const res = await request.post(url, data)
      if (res.code === 200) {
        message.success(res.msg || '入库成功')
        if (inboundType === 'order' && res.data?.receiptBatchId) {
          setReceiptBatchId(res.data.receiptBatchId)
          setReceiptBatchCode(res.data.receiptBatchCode || '')
          form.setFieldsValue({
            drugId: undefined,
            quantity: undefined,
            batchNumber: undefined,
            expiryDate: undefined,
            productionDate: undefined,
            manufacturer: undefined,
            storageLocation: undefined,
            secondOperatorId: undefined,
            expiryCheckReason: undefined,
            dispositionCode: 'PENDING',
            dispositionRemark: undefined,
            status: 'QUALIFIED',
          })
          setSelectedDrug(null)
          setNeedForceReason(false)
          if (selectedOrder?.id) {
            fetchOrderInboundRemaining(selectedOrder.id, false)
          }
          message.info('可继续录入本批其它药品；换车请点击「新开一批」或关闭弹窗')
          fetchInboundRecords()
        } else {
          setModalVisible(false)
          form.resetFields()
          setSelectedDrug(null)
          setSelectedOrder(null)
          setOrderItems([])
          setReceiptBatchId(null)
          setReceiptBatchCode('')
          setOrderRemainRows([])
          fetchInboundRecords()
        }
      } else {
        message.error(res.msg || '入库失败')
      }
    } catch (error) {
      logger.error('入库失败:', error)
      message.error(error.response?.data?.msg || error.message || '入库失败')
    }
  }

  const handleSecondConfirm = (id) => {
    Modal.confirm({
      title: '确认入账',
      content: '请确认已现场核对实物与单据一致，确认后将增加可用库存。',
      onOk: async () => {
        try {
          const res = await request.post(`/inbound/${id}/second-confirm`)
          if (res.code === 200) {
            message.success(res.msg || '已确认')
            fetchInboundRecords()
          } else {
            message.error(res.msg || '操作失败')
          }
        } catch (e) {
          message.error(e?.response?.data?.msg || e.message || '操作失败')
        }
      },
    })
  }

  const handleWithdraw = (id) => {
    Modal.confirm({
      title: '撤回待确认入库',
      content: '撤回后该单不再占用订单数量，可重新发起入库。',
      onOk: async () => {
        try {
          const res = await request.post(`/inbound/${id}/withdraw-pending-second`)
          if (res.code === 200) {
            message.success(res.msg || '已撤回')
            fetchInboundRecords()
          } else {
            message.error(res.msg || '操作失败')
          }
        } catch (e) {
          message.error(e?.response?.data?.msg || e.message || '操作失败')
        }
      },
    })
  }

  const openRejectModal = (id) => {
    setRejectingRecordId(id)
    rejectForm.resetFields()
    setRejectModalVisible(true)
  }

  const submitReject = async () => {
    try {
      const values = await rejectForm.validateFields()
      const res = await request.post(`/inbound/${rejectingRecordId}/second-reject`, { reason: values.reason })
      if (res.code === 200) {
        message.success(res.msg || '已驳回')
        setRejectModalVisible(false)
        fetchInboundRecords()
      } else {
        message.error(res.msg || '操作失败')
      }
    } catch (e) {
      if (e?.errorFields) return
      message.error(e?.response?.data?.msg || e.message || '操作失败')
    }
  }

  const openInboundDetail = useCallback(async (id) => {
    setDetailModalVisible(true)
    setDetailLoading(true)
    setDetailRecord(null)
    try {
      const res = await request.get(`/inbound/${id}`)
      if (res.code === 200) {
        setDetailRecord(res.data)
      } else {
        message.error(res.msg || '加载失败')
        setDetailModalVisible(false)
      }
    } catch (e) {
      message.error(e?.response?.data?.msg || e.message || '加载失败')
      setDetailModalVisible(false)
    } finally {
      setDetailLoading(false)
    }
  }, [])

  const columns = useMemo(() => [
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>ID</span>,
      dataIndex: 'id',
      key: 'id',
      width: 80,
      sorter: (a, b) => a.id - b.id,
      defaultSortOrder: 'ascend',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>入库单号</span>,
      dataIndex: 'recordNumber',
      key: 'recordNumber',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>到货批次</span>,
      dataIndex: 'receiptBatchCode',
      key: 'receiptBatchCode',
      width: 130,
      ellipsis: true,
      render: (v) => v || '—',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>验收追溯码</span>,
      key: 'acceptanceTrace',
      width: 128,
      ellipsis: true,
      render: (_, record) => {
        const v = parseAcceptanceTraceFromRemark(record.remark)
        return v ? (
          <Tooltip title={v}>
            <Tag color="blue">{v.length > 14 ? `${v.slice(0, 12)}…` : v}</Tag>
          </Tooltip>
        ) : (
          '—'
        )
      },
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>药品名称</span>,
      dataIndex: 'drugName',
      key: 'drugName',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>批次号</span>,
      dataIndex: 'batchNumber',
      key: 'batchNumber',
      width: 120,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>入库数量</span>,
      dataIndex: 'quantity',
      key: 'quantity',
      width: 100,
      align: 'right',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>验收状态</span>,
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        if (status === 'QUALIFIED') {
          return <Tag color="green" icon={<CheckCircleOutlined />}>合格</Tag>
        } else if (status === 'UNQUALIFIED') {
          return <Tag color="red" icon={<CloseCircleOutlined />}>不合格</Tag>
        }
        return '-'
      },
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>处置意向</span>,
      dataIndex: 'dispositionCode',
      key: 'dispositionCode',
      width: 130,
      ellipsis: true,
      render: (code, record) => {
        if (record.status !== 'UNQUALIFIED') return '—'
        const label = dispositionLabelMap[code] || code || '—'
        const tip = record.dispositionRemark
        if (tip) {
          return (
            <Tooltip title={tip}>
              <span>{label}</span>
            </Tooltip>
          )
        }
        return label
      },
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>双人确认</span>,
      dataIndex: 'secondConfirmStatus',
      key: 'secondConfirmStatus',
      width: 110,
      render: (v, record) => {
        const map = {
          NONE: { text: '—', color: 'default' },
          CONFIRMED: { text: '已入账', color: 'success' },
          PENDING_SECOND: { text: '待第二人', color: 'orange' },
          REJECTED: { text: '已驳回', color: 'error' },
          WITHDRAWN: { text: '已撤回', color: 'default' },
          TIMEOUT: { text: '超时关闭', color: 'warning' },
        }
        const m = map[v] || { text: v || '-', color: 'default' }
        const tag = <Tag color={m.color}>{m.text}</Tag>
        if (v === 'CONFIRMED' && record.secondConfirmTime) {
          const t = dayjs(record.secondConfirmTime).format('YYYY-MM-DD HH:mm:ss')
          return (
            <Tooltip title={`第二人确认时间：${t}`}>
              {tag}
            </Tooltip>
          )
        }
        return tag
      },
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>第一操作人</span>,
      dataIndex: 'operatorName',
      key: 'operatorName',
      width: 120,
      ellipsis: true,
      render: (v, record) => (v || (record.operatorId != null ? `ID:${record.operatorId}` : '—')),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>第二审核人</span>,
      dataIndex: 'secondOperatorName',
      key: 'secondOperatorName',
      width: 120,
      ellipsis: true,
      render: (v, record) => {
        if (record.secondOperatorId == null) return '—'
        return v || `ID:${record.secondOperatorId}`
      },
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>效期校验</span>,
      dataIndex: 'expiryCheckStatus',
      key: 'expiryCheckStatus',
      width: 100,
      render: (status) => {
        if (status === 'PASS') {
          return <Tag color="green">通过</Tag>
        } else if (status === 'WARNING') {
          return <Tag color="orange">警告</Tag>
        } else if (status === 'FORCE') {
          return <Tag color="red">强制入库</Tag>
        }
        return '-'
      },
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>有效期至</span>,
      dataIndex: 'expiryDate',
      key: 'expiryDate',
      width: 120,
      render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>到货日期</span>,
      dataIndex: 'arrivalDate',
      key: 'arrivalDate',
      width: 120,
      render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>创建时间</span>,
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作</span>,
      key: 'actions',
      fixed: 'right',
      width: 240,
      render: (_, record) => {
        const canViewDetail = hasPermission(PERMISSIONS.DRUG_VIEW) || hasPermission(PERMISSIONS.DRUG_MANAGE)
        const detailBtn = canViewDetail ? (
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => openInboundDetail(record.id)}
          >
            详情
          </Button>
        ) : null
        const me = getUser()
        if (!me || !hasPermission(PERMISSIONS.DRUG_MANAGE)) {
          return detailBtn ? <Space size="small">{detailBtn}</Space> : null
        }
        if (record.secondConfirmStatus !== 'PENDING_SECOND') {
          return detailBtn ? <Space size="small">{detailBtn}</Space> : null
        }
        const isSecond = Number(record.secondOperatorId) === Number(me.id)
        const isFirst = Number(record.operatorId) === Number(me.id)
        return (
          <Space size="small" wrap>
            {detailBtn}
            {isSecond && (
              <>
                <Button type="link" size="small" onClick={() => handleSecondConfirm(record.id)}>确认入账</Button>
                <Button type="link" size="small" danger onClick={() => openRejectModal(record.id)}>驳回</Button>
              </>
            )}
            {isFirst && (
              <Button type="link" size="small" icon={<RollbackOutlined />} onClick={() => handleWithdraw(record.id)}>撤回</Button>
            )}
          </Space>
        )
      },
    },
  ], [dispositionLabelMap, openInboundDetail])

  return (
    <div style={pageRootStyle}>
      <div style={toolbarRowCompactStyle}>
        <h2 style={{ ...toolbarPageTitleStyle, whiteSpace: 'nowrap' }}>入库管理</h2>
        <div style={compactFilterRowStyle}>
          <div style={inboundSearchWrap}>
            <Input
              placeholder={INBOUND_PH_SEARCH}
              value={filters.keyword}
              onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
              style={{ width: '100%' }}
              allowClear
            />
          </div>
          <div style={inboundStatusWrap}>
            <Select
              placeholder={INBOUND_PH_STATUS}
              value={filters.status}
              onChange={(value) => setFilters({ ...filters, status: value })}
              style={{ width: '100%' }}
              allowClear
            >
              <Select.Option value="QUALIFIED">合格</Select.Option>
              <Select.Option value="UNQUALIFIED">不合格</Select.Option>
            </Select>
          </div>
          <div style={inboundExpiryWrap}>
            <Select
              placeholder={INBOUND_PH_EXPIRY}
              value={filters.expiryCheckStatus}
              onChange={(value) => setFilters({ ...filters, expiryCheckStatus: value })}
              style={{ width: '100%' }}
              allowClear
            >
              <Select.Option value="PASS">通过</Select.Option>
              <Select.Option value="WARNING">警告</Select.Option>
              <Select.Option value="FORCE">强制入库</Select.Option>
            </Select>
          </div>
          <div style={inboundSecondWrap}>
            <Select
              placeholder={INBOUND_PH_SECOND}
              value={filters.secondConfirmStatus}
              onChange={(value) => setFilters({ ...filters, secondConfirmStatus: value })}
              style={{ width: '100%' }}
              allowClear
            >
              <Select.Option value="PENDING_SECOND">待第二人</Select.Option>
              <Select.Option value="CONFIRMED">已入账</Select.Option>
              <Select.Option value="NONE">不适用</Select.Option>
              <Select.Option value="REJECTED">已驳回</Select.Option>
              <Select.Option value="WITHDRAWN">已撤回</Select.Option>
              <Select.Option value="TIMEOUT">超时关闭</Select.Option>
            </Select>
          </div>
          <div style={inboundRangeWrap}>
            <RangePicker
              placeholder={INBOUND_PH_RANGE}
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
                onClick={fetchInboundRecords}
              />
            </Tooltip>
            <Tooltip title="重置">
              <Button icon={<ReloadOutlined />} onClick={handleReset} />
            </Tooltip>
            {hasPermission(PERMISSIONS.DRUG_MANAGE) && (
              <Tooltip title="采购订单入库">
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => {
                    setReceiptBatchId(null)
                    setReceiptBatchCode('')
                    setSplitAcceptanceMode(false)
                    setModalVisible(true)
                    setInboundType('order')
                  }}
                />
              </Tooltip>
            )}
            {hasPermission(PERMISSIONS.DRUG_MANAGE) && (
              <Tooltip title="临时入库">
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => {
                    setReceiptBatchId(null)
                    setReceiptBatchCode('')
                    setOrderRemainRows([])
                    setOrderRemainLoading(false)
                    setSplitAcceptanceMode(false)
                    setModalVisible(true)
                    setInboundType('temporary')
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
          dataSource={inboundRecords}
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

      <Modal
        title={inboundType === 'order' ? '采购订单入库' : '临时入库'}
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false)
          form.resetFields()
          setSelectedDrug(null)
          setSelectedOrder(null)
          setOrderItems([])
          setOrderRemainRows([])
          setOrderRemainLoading(false)
          setBarcodeOrderNumber('')
          setReceiptBatchId(null)
          setReceiptBatchCode('')
          setSplitAcceptanceMode(false)
        }}
        onOk={() => form.submit()}
        width={800}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreateInbound}
        >
          {inboundType === 'order' && (
            <>
              <Form.Item label="扫描订单条形码">
                <Space.Compact style={{ width: '100%' }}>
                  <Input
                    prefix={<BarcodeOutlined style={{ color: '#999' }} />}
                    placeholder="将扫码枪对准此处扫描，或输入订单编号后回车"
                    value={barcodeOrderNumber}
                    onChange={(e) => setBarcodeOrderNumber(e.target.value)}
                    onPressEnter={(e) => { e.preventDefault(); handleBarcodeRecognize() }}
                    allowClear
                  />
                  <Tooltip title="根据订单编号识别并带出订单信息">
                    <Button type="primary" loading={barcodeLoading} onClick={handleBarcodeRecognize}>
                      识别
                    </Button>
                  </Tooltip>
                </Space.Compact>
                <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
                  条形码内容为订单编号，扫描后自动带出订单及药品明细
                </div>
              </Form.Item>
              <Form.Item
                name="orderId"
                label="采购订单"
                rules={[
                  { required: true, message: '请选择采购订单' },
                ]}
              >
                <Select
                  placeholder="或从下拉选择采购订单（仅显示已发货订单）"
                  showSearch
                  filterOption={(input, option) =>
                    (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                  }
                  onChange={handleOrderChange}
                  options={orderOptions}
                />
              </Form.Item>
            </>
          )}

          {inboundType === 'order' && selectedOrder && (
            <>
              <div style={{ marginBottom: 12 }}>
                <div style={{ marginBottom: 6, fontWeight: 500, color: 'rgba(0,0,0,0.85)' }}>
                  本订单各药品入库进度
                </div>
                <div style={{ fontSize: 12, color: '#888', marginBottom: 8 }}>
                  已占用含：合格（已入账/待第二人确认）+ 不合格登记数量；剩余可入 = 订单数量 − 已占用。
                </div>
                <Table
                  size="small"
                  pagination={false}
                  loading={orderRemainLoading}
                  columns={orderRemainColumns}
                  dataSource={orderRemainRows}
                  rowKey={(r) => String(r.drugId)}
                  locale={{ emptyText: orderRemainLoading ? '加载中…' : '暂无明细' }}
                />
              </div>
              {receiptBatchId && (
                <Alert
                  type="info"
                  showIcon
                  style={{ marginBottom: 12 }}
                  message={`当前明细将归入到货批次：${receiptBatchCode || `ID ${receiptBatchId}`}（同一车可继续录入其它药品）`}
                />
              )}
              {receiptBatchId && (
                <div style={{ marginBottom: 12 }}>
                  <Button
                    size="small"
                    onClick={() => {
                      setReceiptBatchId(null)
                      setReceiptBatchCode('')
                      form.setFieldsValue({ deliveryNoteNumber: undefined, arrivalAt: dayjs() })
                    }}
                  >
                    新开一批（换车 / 新随货单）
                  </Button>
                </div>
              )}
              <div style={{ marginBottom: 12 }}>
                <Space align="center">
                  <span style={{ fontWeight: 500 }}>分批验收</span>
                  <Switch
                    checked={splitAcceptanceMode}
                    onChange={(v) => {
                      setSplitAcceptanceMode(v)
                      form.setFieldsValue({
                        quantity: undefined,
                        status: 'QUALIFIED',
                        qualifiedQuantity: undefined,
                        unqualifiedQuantity: undefined,
                        unqualifiedReason: undefined,
                        unqualifiedDispositionCode: 'PENDING',
                        unqualifiedDispositionRemark: undefined,
                      })
                    }}
                  />
                  <span style={{ fontSize: 12, color: '#666' }}>
                    同一批号同时登记合格与不合格两条，系统自动关联同一追溯码
                  </span>
                </Space>
              </div>
              <Form.Item
                name="deliveryNoteNumber"
                label="随货同行单编号"
                extra="同一车多品种：首条填写后，后续将自动沿用同一批次；换车请点「新开一批」"
                rules={[
                  () => ({
                    validator(_, value) {
                      if (receiptBatchId) return Promise.resolve()
                      if (!value || String(value).trim() === '') {
                        return Promise.reject(new Error('请填写随货同行单编号'))
                      }
                      return Promise.resolve()
                    },
                  }),
                  { max: 100, message: '随货同行单编号长度不能超过100个字符' },
                ]}
              >
                <Input placeholder="请输入随货同行单编号" disabled={!!receiptBatchId} />
              </Form.Item>
              <Form.Item
                name="arrivalAt"
                label="到货时间（本批）"
                initialValue={dayjs()}
                rules={[
                  () => ({
                    validator(_, value) {
                      if (receiptBatchId) return Promise.resolve()
                      if (!value) return Promise.reject(new Error('请选择本批到货时间'))
                      return Promise.resolve()
                    },
                  }),
                ]}
              >
                <DatePicker
                  showTime
                  style={{ width: '100%' }}
                  disabled={!!receiptBatchId}
                  disabledDate={(current) => current && current > dayjs().endOf('day')}
                />
              </Form.Item>
            </>
          )}
          
          {inboundType === 'order' && selectedOrder && orderItems.length > 0 && (
            <Form.Item
              name="drugId"
              label="订单药品"
              rules={[{ required: true, message: '请选择药品' }]}
            >
              <Select
                placeholder="请选择订单中的药品"
                optionFilterProp="label"
                onChange={(drugId) => {
                  const item = orderItems.find(i => Number(i.drugId) === Number(drugId))
                  const row = remainByDrugId[Number(drugId)]
                  if (row && row.remainingQuantity <= 0) {
                    message.warning('该药品已无剩余可入库数量')
                    form.setFieldsValue({ drugId: undefined, quantity: undefined })
                    return
                  }
                  if (item) {
                    const q = row && row.remainingQuantity > 0 ? row.remainingQuantity : item.quantity
                    form.setFieldsValue({ quantity: q })
                    const drug = drugs.find(d => Number(d.id) === Number(drugId))
                    if (drug) {
                      setSelectedDrug(drug)
                      form.setFieldsValue({
                        manufacturer: drug.manufacturer || '',
                        storageLocation: drug.storageLocation || '',
                      })
                    }
                  }
                }}
              >
                {orderItems.map(item => {
                  const drug = drugs.find(d => Number(d.id) === Number(item.drugId))
                  const rem = remainByDrugId[Number(item.drugId)]
                  const remQty = rem != null ? rem.remainingQuantity : null
                  const base = drug
                    ? `${drug.drugName || '未知'}${drug.specification ? ` (${drug.specification})` : ''}`
                    : `药品ID: ${item.drugId}`
                  const label = remQty != null
                    ? `${base} · 订${item.quantity} · 余${remQty}`
                    : `${base} · 订单数量: ${item.quantity}`
                  const done = remQty != null && remQty <= 0
                  return (
                    <Select.Option key={item.drugId} value={item.drugId} label={label} disabled={done}>
                      {label}{done ? '（已满）' : ''}
                    </Select.Option>
                  )
                })}
              </Select>
            </Form.Item>
          )}

          {inboundType === 'temporary' && (
            <Form.Item
              name="drugId"
              label="药品"
              rules={[
                { required: true, message: '请选择药品' },
              ]}
            >
              <AutoComplete
                options={drugOptions}
                placeholder="请输入或选择药品"
                filterOption={(inputValue, option) =>
                  (option?.label ?? '').toString().toUpperCase().indexOf((inputValue || '').toUpperCase()) !== -1
                }
                onSelect={handleDrugSelect}
                onChange={(value) => {
                  if (!value) {
                    setSelectedDrug(null)
                    form.setFieldsValue({ manufacturer: '', storageLocation: '' })
                  }
                }}
              />
            </Form.Item>
          )}

          <Form.Item
            name="batchNumber"
            label="批次号"
            rules={[
              { required: true, message: '请输入批次号' },
              { pattern: /^[A-Za-z0-9]+$/, message: '批次号只能包含字母和数字' },
              { max: 50, message: '批次号长度不能超过50个字符' },
            ]}
          >
            <Input placeholder="请输入批次号" />
          </Form.Item>

          {inboundType === 'order' && splitAcceptanceMode && (
            <>
              <Alert
                type="warning"
                showIcon
                style={{ marginBottom: 12 }}
                message="将生成两条入库单：一条合格（可入账）、一条不合格（不入账），共享同一追溯码，备注中自动记录拆行说明。"
              />
              <Form.Item
                name="qualifiedQuantity"
                label="合格数量"
                extra={
                  selectedRemainRow != null
                    ? `本单该药品剩余可入：${selectedRemainRow.remainingQuantity}（合格+不合格合计不可超）`
                    : undefined
                }
                rules={[
                  { required: true, message: '请输入合格数量' },
                  { type: 'number', min: 1, message: '须 ≥1' },
                  {
                    validator: (_, value) => {
                      const row = orderRemainRows.find(
                        (r) => Number(r.drugId) === Number(form.getFieldValue('drugId'))
                      )
                      const uq = form.getFieldValue('unqualifiedQuantity')
                      if (!row) return Promise.resolve()
                      const n = Number(value) + (Number.isFinite(Number(uq)) ? Number(uq) : 0)
                      if (Number.isFinite(n) && n > row.remainingQuantity) {
                        return Promise.reject(
                          new Error(`合格+不合格合计不能超过剩余 ${row.remainingQuantity}`)
                        )
                      }
                      return Promise.resolve()
                    },
                  },
                ]}
              >
                <InputNumber style={{ width: '100%' }} min={1} precision={0} placeholder="合格部分数量" />
              </Form.Item>
              <Form.Item
                name="unqualifiedQuantity"
                label="不合格数量"
                rules={[
                  { required: true, message: '请输入不合格数量' },
                  { type: 'number', min: 1, message: '须 ≥1' },
                  {
                    validator: (_, value) => {
                      const row = orderRemainRows.find(
                        (r) => Number(r.drugId) === Number(form.getFieldValue('drugId'))
                      )
                      const qq = form.getFieldValue('qualifiedQuantity')
                      if (!row) return Promise.resolve()
                      const n = Number(value) + (Number.isFinite(Number(qq)) ? Number(qq) : 0)
                      if (Number.isFinite(n) && n > row.remainingQuantity) {
                        return Promise.reject(
                          new Error(`合格+不合格合计不能超过剩余 ${row.remainingQuantity}`)
                        )
                      }
                      return Promise.resolve()
                    },
                  },
                ]}
              >
                <InputNumber style={{ width: '100%' }} min={1} precision={0} placeholder="不合格部分数量" />
              </Form.Item>
              <Form.Item
                name="unqualifiedReason"
                label="不合格原因"
                rules={[
                  { required: true, message: '请填写不合格原因（可追溯）' },
                  { max: 500, message: '不超过500字' },
                ]}
              >
                <TextArea rows={3} placeholder="例如：包装破损、标签不符、近效期等" />
              </Form.Item>
              <Form.Item
                name="unqualifiedDispositionCode"
                label="不合格处置意向"
                initialValue="PENDING"
                rules={[{ required: true, message: '请选择处置意向' }]}
              >
                <Select
                  placeholder="请选择"
                  options={dispositionSelectOptions}
                  showSearch
                  optionFilterProp="label"
                />
              </Form.Item>
              <Form.Item
                name="unqualifiedDispositionRemark"
                label="处置补充说明"
                rules={[{ max: 500, message: '不超过500字' }]}
              >
                <TextArea rows={2} placeholder="可选：与供应商/质控沟通的简要说明" />
              </Form.Item>
            </>
          )}

          {!(inboundType === 'order' && splitAcceptanceMode) && (
            <Form.Item
              name="quantity"
              label="入库数量"
              extra={
                inboundType === 'order' && selectedRemainRow != null
                  ? `本单该药品剩余可入：${selectedRemainRow.remainingQuantity}（已占用含不合格登记）`
                  : undefined
              }
              rules={[
                { required: true, message: '请输入入库数量' },
                { type: 'number', min: 1, message: '入库数量必须大于0' },
                ...(inboundType === 'order'
                  ? [
                      {
                        validator: (_, value) => {
                          const row = orderRemainRows.find(
                            (r) => Number(r.drugId) === Number(form.getFieldValue('drugId'))
                          )
                          if (!row) return Promise.resolve()
                          if (row.remainingQuantity <= 0) {
                            return Promise.reject(new Error('该药品已无剩余可入库数量'))
                          }
                          const n = Number(value)
                          if (Number.isFinite(n) && n > row.remainingQuantity) {
                            return Promise.reject(
                              new Error(`不能超过剩余可入库数量 ${row.remainingQuantity}`)
                            )
                          }
                          return Promise.resolve()
                        },
                      },
                    ]
                  : [{ type: 'number', max: 999999, message: '入库数量不能超过999999' }]),
              ]}
            >
              <InputNumber
                style={{ width: '100%' }}
                placeholder="请输入入库数量"
                min={1}
                max={
                  inboundType === 'order' &&
                  selectedRemainRow != null &&
                  selectedRemainRow.remainingQuantity > 0
                    ? selectedRemainRow.remainingQuantity
                    : 999999
                }
                precision={0}
              />
            </Form.Item>
          )}

          <Form.Item
            name="expiryDate"
            label="有效期至"
            rules={[
              { required: true, message: '请选择有效期至' },
            ]}
          >
            <DatePicker
              style={{ width: '100%' }}
              disabledDate={(current) => current && current < dayjs().startOf('day')}
              onChange={(date) => {
                if (!date) {
                  setNeedForceReason(false)
                  return
                }
                const daysUntilExpiry = date.startOf('day').diff(dayjs().startOf('day'), 'day')
                setNeedForceReason(daysUntilExpiry < 90)
              }}
            />
          </Form.Item>

          {inboundType === 'temporary' && (
            <Form.Item
              name="arrivalDate"
              label="到货日期"
              initialValue={dayjs()}
            >
              <DatePicker
                style={{ width: '100%' }}
                disabledDate={(current) => current && current > dayjs().endOf('day')}
              />
            </Form.Item>
          )}

          <Form.Item
            name="productionDate"
            label="生产日期"
            rules={[
              { required: true, message: '请选择生产日期' },
            ]}
          >
            <DatePicker
              style={{ width: '100%' }}
              disabledDate={(current) => current && current > dayjs().endOf('day')}
            />
          </Form.Item>

          <Form.Item
            name="manufacturer"
            label="生产厂家"
            rules={[
              { max: 200, message: '生产厂家长度不能超过200个字符' },
            ]}
          >
            <Input placeholder="请输入生产厂家" />
          </Form.Item>

          {inboundType === 'temporary' && (
            <Form.Item
              name="deliveryNoteNumber"
              label="随货同行单编号"
              rules={[
                { max: 100, message: '随货同行单编号长度不能超过100个字符' },
              ]}
            >
              <Input placeholder="请输入随货同行单编号（可选）" />
            </Form.Item>
          )}

          {!(inboundType === 'order' && splitAcceptanceMode) && (
            <Form.Item
              name="status"
              label="验收状态"
              rules={[
                { required: true, message: '请选择验收状态' },
              ]}
            >
              <Select placeholder="请选择验收状态">
                <Select.Option value="QUALIFIED">合格</Select.Option>
                <Select.Option value="UNQUALIFIED">不合格</Select.Option>
              </Select>
            </Form.Item>
          )}

          {!(inboundType === 'order' && splitAcceptanceMode) && watchedStatus === 'UNQUALIFIED' && (
            <>
              <Form.Item
                name="dispositionCode"
                label="处置意向"
                initialValue="PENDING"
                rules={[{ required: true, message: '请选择处置意向' }]}
              >
                <Select
                  placeholder="请选择"
                  options={dispositionSelectOptions}
                  showSearch
                  optionFilterProp="label"
                />
              </Form.Item>
              <Form.Item
                name="dispositionRemark"
                label="处置补充说明"
                rules={[{ max: 500, message: '不超过500字' }]}
              >
                <TextArea rows={2} placeholder="可选：与供应商/质控沟通的简要说明" />
              </Form.Item>
            </>
          )}

          <Form.Item
            name="storageLocation"
            label="存储位置"
            extra={
              inboundType === 'order' && splitAcceptanceMode
                ? '仅针对合格部分；不合格不入合格库位（系统不写入可用库存）'
                : watchedStatus === 'QUALIFIED'
                  ? '合格入库必填，入账后写入该批次库存（可与药品档案默认位置不同）'
                  : '不合格入库可不填'
            }
            rules={[
              { max: 200, message: '存储位置长度不能超过200个字符' },
              () => ({
                validator(_, value) {
                  if (inboundType === 'order' && splitAcceptanceMode) {
                    if (value == null || String(value).trim() === '') {
                      return Promise.reject(new Error('合格部分须填写存储位置'))
                    }
                    return Promise.resolve()
                  }
                  if (watchedStatus === 'QUALIFIED') {
                    if (value == null || String(value).trim() === '') {
                      return Promise.reject(new Error('合格入库须填写存储位置'))
                    }
                  }
                  return Promise.resolve()
                },
              }),
            ]}
          >
            <Input placeholder="如：阴凉库A区-3层" allowClear />
          </Form.Item>

          {showSecondOperatorField && (
            <Form.Item
              name="secondOperatorId"
              label="第二操作人"
              rules={[{ required: true, message: '请选择第二操作人' }]}
              extra="须为另一名仓库管理员；提交后请其登录系统确认，邮件仅作提醒"
            >
              <Select
                placeholder="请选择第二操作人"
                loading={loadingUsers}
                showSearch
                optionFilterProp="label"
                options={users
                  .filter((u) => !getUser() || Number(u.id) !== Number(getUser().id))
                  .map((u) => ({
                    value: u.id,
                    label: `${u.username || u.id}${u.realName ? ` (${u.realName})` : ''}`,
                  }))}
              />
            </Form.Item>
          )}

          <Form.Item
            name="expiryCheckReason"
            label="效期校验说明"
            rules={[
              () => ({
                validator(_, value) {
                  if (needForceReason && !value) {
                    return Promise.reject(new Error('有效期不足90天时，需填写强制入库原因'))
                  }
                  return Promise.resolve()
                },
              }),
              { max: 500, message: '效期校验说明长度不能超过500个字符' },
            ]}
          >
            <TextArea
              rows={3}
              placeholder="当效期不足90天时，需填写强制入库原因"
            />
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

      <Modal
        title="驳回入库"
        open={rejectModalVisible}
        onOk={submitReject}
        onCancel={() => setRejectModalVisible(false)}
        destroyOnHidden
      >
        <Form form={rejectForm} layout="vertical">
          <Form.Item
            name="reason"
            label="驳回原因"
            rules={[{ required: true, message: '请填写驳回原因' }, { max: 500, message: '不超过500字' }]}
          >
            <TextArea rows={4} placeholder="请说明驳回原因" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="入库单详情（追溯）"
        open={detailModalVisible}
        onCancel={() => {
          setDetailModalVisible(false)
          setDetailRecord(null)
        }}
        footer={null}
        width={900}
        destroyOnHidden
      >
        <Spin spinning={detailLoading}>
          {detailRecord && (
            <Descriptions
              bordered
              column={2}
              size="small"
              styles={{ label: { width: 132 } }}
            >
              <Descriptions.Item label="入库单号">{detailRecord.recordNumber}</Descriptions.Item>
              <Descriptions.Item label="关联订单ID">{detailRecord.orderId ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="到货批次">{detailRecord.receiptBatchCode || '—'}</Descriptions.Item>
              <Descriptions.Item label="药品名称">{detailRecord.drugName || '—'}</Descriptions.Item>
              <Descriptions.Item label="批次号">{detailRecord.batchNumber}</Descriptions.Item>
              <Descriptions.Item label="入库数量">{detailRecord.quantity}</Descriptions.Item>
              <Descriptions.Item label="验收状态">
                {detailRecord.status === 'QUALIFIED' ? '合格' : detailRecord.status === 'UNQUALIFIED' ? '不合格' : (detailRecord.status || '—')}
              </Descriptions.Item>
              <Descriptions.Item label="双人确认">
                {SECOND_CONFIRM_LABELS[detailRecord.secondConfirmStatus] || detailRecord.secondConfirmStatus || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="第一操作人">
                {detailRecord.operatorName || (detailRecord.operatorId != null ? `用户ID ${detailRecord.operatorId}` : '—')}
              </Descriptions.Item>
              <Descriptions.Item label="第二审核人">
                {detailRecord.secondOperatorId == null
                  ? '—'
                  : (detailRecord.secondOperatorName || `用户ID ${detailRecord.secondOperatorId}`)}
              </Descriptions.Item>
              <Descriptions.Item label="第二人确认时间">
                {detailRecord.secondConfirmTime
                  ? dayjs(detailRecord.secondConfirmTime).format('YYYY-MM-DD HH:mm:ss')
                  : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="第二人确认截止">
                {detailRecord.secondConfirmDeadline
                  ? dayjs(detailRecord.secondConfirmDeadline).format('YYYY-MM-DD HH:mm:ss')
                  : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="第二人驳回原因" span={2}>
                {detailRecord.secondRejectReason || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="效期校验">
                {EXPIRY_CHECK_LABELS[detailRecord.expiryCheckStatus] || detailRecord.expiryCheckStatus || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="效期校验说明">
                {detailRecord.expiryCheckReason || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="有效期至">
                {detailRecord.expiryDate ? dayjs(detailRecord.expiryDate).format('YYYY-MM-DD') : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="到货日期">
                {detailRecord.arrivalDate ? dayjs(detailRecord.arrivalDate).format('YYYY-MM-DD') : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="生产日期">
                {detailRecord.productionDate ? dayjs(detailRecord.productionDate).format('YYYY-MM-DD') : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="生产厂家">{detailRecord.manufacturer || '—'}</Descriptions.Item>
              <Descriptions.Item label="存储位置">{detailRecord.storageLocation || '—'}</Descriptions.Item>
              <Descriptions.Item label="随货同行单号">{detailRecord.deliveryNoteNumber || '—'}</Descriptions.Item>
              <Descriptions.Item label="随货单图片" span={2}>
                {detailRecord.deliveryNoteImage || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="不合格处置意向">
                {detailRecord.status === 'UNQUALIFIED'
                  ? (dispositionLabelMap[detailRecord.dispositionCode] || detailRecord.dispositionCode || '—')
                  : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="处置补充说明">
                {detailRecord.status === 'UNQUALIFIED' ? (detailRecord.dispositionRemark || '—') : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="验收追溯码" span={2}>
                {parseAcceptanceTraceFromRemark(detailRecord.remark) || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="备注" span={2}>
                {detailRecord.remark || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间" span={2}>
                {detailRecord.createTime ? dayjs(detailRecord.createTime).format('YYYY-MM-DD HH:mm:ss') : '—'}
              </Descriptions.Item>
            </Descriptions>
          )}
        </Spin>
      </Modal>
    </div>
  )
}

export default InboundManagement


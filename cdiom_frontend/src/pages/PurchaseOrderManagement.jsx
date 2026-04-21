import { useState, useEffect, useMemo, useCallback } from 'react'
import { Table, Button, Space, Input, Select, Tag, Modal, Form, message, DatePicker, InputNumber, AutoComplete, Popconfirm, Image, Tooltip } from 'antd'
import { SearchOutlined, ReloadOutlined, PlusOutlined, EyeOutlined, DeleteOutlined, CheckOutlined, CloseOutlined, SendOutlined, StopOutlined, EditOutlined, BarcodeOutlined, DownloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request from '../utils/request'
import logger from '../utils/logger'
import { hasPermission, PERMISSIONS } from '../utils/permission'
import { getUserRoleId, getToken } from '../utils/auth'
import {
  pageRootStyle,
  tableAreaStyle,
  toolbarSectionStackedStyle,
  toolbarPageTitleStyle,
  compactFilterRowFullWidthStyle,
  filterCellFlex,
  TABLE_SCROLL_Y_STACKED,
} from '../utils/tablePageLayout'

const PurchaseOrderManagement = () => {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [filters, setFilters] = useState({
    keyword: '',
    supplierId: undefined,
    purchaserId: undefined,
    status: undefined,
  })
  const [modalVisible, setModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [currentOrder, setCurrentOrder] = useState(null)
  const [form] = Form.useForm()
  const [suppliers, setSuppliers] = useState([])
  const [drugs, setDrugs] = useState([])
  const [selectedSupplierId, setSelectedSupplierId] = useState(undefined)
  const [orderFormItems, setOrderFormItems] = useState([{ drugId: undefined, drugNameDisplay: '', quantity: undefined, unitPrice: undefined }])
  const [rejectModalVisible, setRejectModalVisible] = useState(false)
  const [shipModalVisible, setShipModalVisible] = useState(false)
  const [cancelModalVisible, setCancelModalVisible] = useState(false)
  const [logisticsModalVisible, setLogisticsModalVisible] = useState(false)
  const [rejectForm] = Form.useForm()
  const [shipForm] = Form.useForm()
  const [cancelForm] = Form.useForm()
  const [logisticsForm] = Form.useForm()
  const [actionOrderId, setActionOrderId] = useState(null)
  const [barcodeModalVisible, setBarcodeModalVisible] = useState(false)
  const [currentBarcode, setCurrentBarcode] = useState(null)
  const [currentBarcodeOrderId, setCurrentBarcodeOrderId] = useState(null)
  const [exporting, setExporting] = useState(false)

  useEffect(() => {
    fetchOrders()
  }, [pagination.current, pagination.pageSize, filters])

  useEffect(() => {
    fetchSuppliers()
  }, [])

  useEffect(() => {
    if (selectedSupplierId) {
      fetchDrugsBySupplier(selectedSupplierId)
    } else {
      fetchDrugs()
    }
  }, [selectedSupplierId])

  // 供应商选项
  const supplierOptions = useMemo(() => {
    return suppliers
      .filter(s => s.status === 1 && s.auditStatus === 1) // 只显示启用且已审核通过的供应商
      .map(supplier => ({
        value: supplier.id,
        label: supplier.name,
        supplier: supplier
      }))
  }, [suppliers])

  // 药品选项（选中供应商时含 unitPrice，用于自动带出单价）
  // value 使用字符串，避免 AutoComplete 在 combobox 场景下报 number type 警告
  const drugOptions = useMemo(() => {
    return drugs.map(drug => ({
      value: String(drug.id),
      label: `${drug.drugName || ''} (${drug.specification || ''})`.replace(/\s*\(\s*\)$/, '').trim() || String(drug.id),
      unitPrice: drug.unitPrice,
      drug: drug
    }))
  }, [drugs])

  const fetchSuppliers = async () => {
    try {
      const res = await request.get('/suppliers', {
        params: { page: 1, size: 10000, status: 1, auditStatus: 1 }
      })
      if (res.code === 200) {
        setSuppliers(res.data.records || [])
      }
    } catch (error) {
      logger.error('获取供应商列表失败:', error)
    }
  }

  const fetchDrugs = async () => {
    try {
      const res = await request.get('/drugs', {
        params: { page: 1, size: 10000 }
      })
      if (res.code === 200) {
        setDrugs(res.data.records || [])
      }
    } catch (error) {
      logger.error('获取药品列表失败:', error)
    }
  }

  const fetchDrugsBySupplier = async (supplierId) => {
    try {
      const res = await request.get(`/suppliers/${supplierId}/drugs`, {
        params: { page: 1, size: 10000 }
      })
      if (res.code === 200) {
        setDrugs(res.data.records || [])
      }
    } catch (error) {
      logger.error('获取供应商药品列表失败:', error)
      message.error('获取供应商药品列表失败')
    }
  }

  const fetchOrders = async () => {
    setLoading(true)
    try {
      const params = {
        page: pagination.current,
        size: pagination.pageSize,
        keyword: filters.keyword || undefined,
        supplierId: filters.supplierId,
        purchaserId: filters.purchaserId,
        status: filters.status,
      }
      const res = await request.get('/purchase-orders', { params })
      if (res.code === 200) {
        setOrders(res.data.records || [])
        setPagination({
          ...pagination,
          total: res.data.total || 0,
        })
      } else {
        message.error(res.msg || '获取采购订单失败')
      }
    } catch (error) {
      logger.error('获取采购订单失败:', error)
      message.error('获取采购订单失败')
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
      supplierId: undefined,
      purchaserId: undefined,
      status: undefined,
    })
    setPagination({ ...pagination, current: 1 })
  }

  const handleExport = async () => {
    setExporting(true)
    try {
      const token = getToken()
      if (!token) {
        message.error('未登录，请重新登录')
        return
      }
      const params = new URLSearchParams()
      if (filters.keyword) {
        params.append('keyword', filters.keyword)
      }
      if (filters.supplierId) {
        params.append('supplierId', filters.supplierId)
      }
      if (filters.purchaserId) {
        params.append('purchaserId', filters.purchaserId)
      }
      if (filters.status) {
        params.append('status', filters.status)
      }
      
      const url = `/api/v1/purchase-orders/export?${params.toString()}`
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
        credentials: 'include',
      })
      
      if (!response.ok) {
        throw new Error('导出失败')
      }
      
      const blob = await response.blob()
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = `采购订单列表_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(downloadUrl)
      
      message.success('导出成功')
    } catch (error) {
      logger.error('导出失败:', error)
      message.error('导出失败: ' + (error.message || '未知错误'))
    } finally {
      setExporting(false)
    }
  }

  const handleCreateOrder = async (values) => {
    try {
      // 验证必填字段
      if (!values.supplierId) {
        message.error('请选择供应商')
        return
      }
      if (!values.expectedDeliveryDate) {
        message.error('请选择预计交货日期')
        return
      }
      if (!orderFormItems || orderFormItems.length === 0) {
        message.error('请至少添加一个药品')
        return
      }

      // 验证每个明细项
      const validItems = []
      let totalAmount = 0
      for (const item of orderFormItems) {
        if (!item.drugId) {
          message.error('请选择药品')
          return
        }
        if (!item.quantity || item.quantity <= 0) {
          message.error('请输入有效的数量')
          return
        }
        if (!item.unitPrice || item.unitPrice <= 0) {
          message.error('请输入有效的单价')
          return
        }
        const totalPrice = item.quantity * item.unitPrice
        totalAmount += totalPrice
        validItems.push({
          drugId: item.drugId,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          totalPrice: totalPrice,
        })
      }

      const data = {
        supplierId: values.supplierId,
        expectedDeliveryDate: values.expectedDeliveryDate.format('YYYY-MM-DD'),
        remark: values.remark || '',
        items: validItems,
      }

      const res = await request.post('/purchase-orders', data)
      if (res.code === 200) {
        message.success('采购订单创建成功')
        setModalVisible(false)
        form.resetFields()
        setOrderFormItems([{ drugId: undefined, drugNameDisplay: '', quantity: undefined, unitPrice: undefined }])
        fetchOrders()
      } else {
        message.error(res.msg || '创建采购订单失败')
      }
    } catch (error) {
      logger.error('创建采购订单失败:', error)
      message.error(error.response?.data?.msg || error.message || '创建采购订单失败')
    }
  }

  const addOrderFormItem = () => {
    setOrderFormItems([...orderFormItems, { drugId: undefined, drugNameDisplay: '', quantity: undefined, unitPrice: undefined }])
  }

  const removeOrderFormItem = (index) => {
    const newItems = orderFormItems.filter((_, i) => i !== index)
    setOrderFormItems(newItems)
  }

  const getStatusTag = (status) => {
    const statusMap = {
      PENDING: { color: 'orange', text: '待确认' },
      REJECTED: { color: 'red', text: '已拒绝' },
      CONFIRMED: { color: 'blue', text: '待发货' },
      SHIPPED: { color: 'cyan', text: '已发货' },
      RECEIVED: { color: 'green', text: '已入库' },
      CANCELLED: { color: 'default', text: '已取消' },
    }
    const statusInfo = statusMap[status] || { color: 'default', text: status }
    return <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
  }

  // 确认订单
  const handleConfirmOrder = async (orderId) => {
    try {
      const res = await request.post(`/purchase-orders/${orderId}/confirm`)
      if (res.code === 200) {
        message.success('订单确认成功')
        fetchOrders()
      } else {
        message.error(res.msg || '确认订单失败')
      }
    } catch (error) {
      message.error(error.response?.data?.msg || error.message || '确认订单失败')
    }
  }

  // 拒绝订单
  const handleRejectOrder = async (values) => {
    try {
      const res = await request.post(`/purchase-orders/${actionOrderId}/reject`, {
        reason: values.reason
      })
      if (res.code === 200) {
        message.success('订单已拒绝')
        setRejectModalVisible(false)
        rejectForm.resetFields()
        setActionOrderId(null)
        fetchOrders()
      } else {
        message.error(res.msg || '拒绝订单失败')
      }
    } catch (error) {
      message.error(error.response?.data?.msg || error.message || '拒绝订单失败')
    }
  }

  // 发货
  const handleShipOrder = async (values) => {
    try {
      const res = await request.post(`/purchase-orders/${actionOrderId}/ship`, {
        logisticsNumber: values.logisticsNumber
      })
      if (res.code === 200) {
        message.success('订单发货成功')
        setShipModalVisible(false)
        shipForm.resetFields()
        setActionOrderId(null)
        fetchOrders()
      } else {
        message.error(res.msg || '发货失败')
      }
    } catch (error) {
      message.error(error.response?.data?.msg || error.message || '发货失败')
    }
  }

  // 取消订单
  const handleCancelOrder = async (values) => {
    try {
      const res = await request.post(`/purchase-orders/${actionOrderId}/cancel`, {
        reason: values.reason
      })
      if (res.code === 200) {
        message.success('订单已取消')
        setCancelModalVisible(false)
        cancelForm.resetFields()
        setActionOrderId(null)
        fetchOrders()
      } else {
        message.error(res.msg || '取消订单失败')
      }
    } catch (error) {
      message.error(error.response?.data?.msg || error.message || '取消订单失败')
    }
  }

  // 更新物流单号
  const handleUpdateLogistics = async (values) => {
    try {
      const res = await request.put(`/purchase-orders/${actionOrderId}/logistics`, {
        logisticsNumber: values.logisticsNumber
      })
      if (res.code === 200) {
        message.success('物流单号更新成功')
        setLogisticsModalVisible(false)
        logisticsForm.resetFields()
        setActionOrderId(null)
        fetchOrders()
      } else {
        message.error(res.msg || '更新物流单号失败')
      }
    } catch (error) {
      message.error(error.response?.data?.msg || error.message || '更新物流单号失败')
    }
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
      title: <span style={{ whiteSpace: 'nowrap' }}>订单编号</span>,
      dataIndex: 'orderNumber',
      key: 'orderNumber',
      width: 150,
      ellipsis: true,
      render: (text, record) => (
        <Space>
          <span>{text}</span>
          <Tooltip title="条形码">
            <Button
              type="link"
              size="small"
              icon={<BarcodeOutlined />}
              onClick={async () => {
                try {
                  const res = await request.get(`/purchase-orders/${record.id}/barcode`)
                  if (res.code === 200) {
                    setCurrentBarcode(res.data)
                    setCurrentBarcodeOrderId(record.id)
                    setBarcodeModalVisible(true)
                  } else {
                    message.error(res.msg || '获取条形码失败')
                  }
                } catch (error) {
                  message.error('获取条形码失败')
                }
              }}
            />
          </Tooltip>
        </Space>
      ),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>供应商</span>,
      dataIndex: 'supplierName',
      key: 'supplierName',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>订单状态</span>,
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => getStatusTag(status),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>预计交货日期</span>,
      dataIndex: 'expectedDeliveryDate',
      key: 'expectedDeliveryDate',
      width: 120,
      render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>物流单号</span>,
      dataIndex: 'logisticsNumber',
      key: 'logisticsNumber',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>订单总金额</span>,
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      align: 'right',
      render: (amount) => amount ? `¥${amount.toFixed(2)}` : '-',
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
      key: 'action',
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space wrap>
          <Tooltip title="查看明细">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={async () => {
                try {
                  const res = await request.get(`/purchase-orders/${record.id}/items`)
                  if (res.code === 200) {
                    setCurrentOrder({ ...record, items: res.data })
                    setDetailModalVisible(true)
                  }
                } catch (error) {
                  message.error('获取订单明细失败')
                }
              }}
            />
          </Tooltip>
          {/* 采购专员：仅可取消订单申请，不可确认/拒绝/发货/填物流（由供应商操作） */}
          {record.status !== 'RECEIVED' && record.status !== 'CANCELLED' && record.status !== 'REJECTED' && hasPermission(PERMISSIONS.DRUG_MANAGE) && getUserRoleId() !== 5 && (
            <Tooltip title="取消">
              <Button
                type="link"
                size="small"
                icon={<StopOutlined />}
                danger
                onClick={() => {
                  setActionOrderId(record.id)
                  setCancelModalVisible(true)
                }}
              />
            </Tooltip>
          )}
          {/* 供应商：确认/拒绝/发货/物流（仅对自己的订单） */}
          {getUserRoleId() === 5 && record.status === 'PENDING' && (
            <>
              <Popconfirm
                title="确认订单"
                description="确定要确认此订单吗？"
                onConfirm={() => handleConfirmOrder(record.id)}
                okText="确认"
                cancelText="取消"
              >
                <Tooltip title="确认">
                  <Button type="link" size="small" icon={<CheckOutlined />} style={{ color: '#52c41a' }} />
                </Tooltip>
              </Popconfirm>
              <Tooltip title="拒绝">
                <Button
                  type="link"
                  size="small"
                  icon={<CloseOutlined />}
                  danger
                  onClick={() => {
                    setActionOrderId(record.id)
                    setRejectModalVisible(true)
                  }}
                />
              </Tooltip>
            </>
          )}
          {getUserRoleId() === 5 && record.status === 'CONFIRMED' && (
            <Tooltip title="发货">
              <Button
                type="link"
                size="small"
                icon={<SendOutlined />}
                onClick={() => {
                  setActionOrderId(record.id)
                  shipForm.setFieldsValue({ logisticsNumber: record.logisticsNumber || '' })
                  setShipModalVisible(true)
                }}
              />
            </Tooltip>
          )}
          {getUserRoleId() === 5 && (record.status === 'SHIPPED' || record.status === 'CONFIRMED') && (
            <Tooltip title="物流">
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => {
                  setActionOrderId(record.id)
                  logisticsForm.setFieldsValue({ logisticsNumber: record.logisticsNumber || '' })
                  setLogisticsModalVisible(true)
                }}
              />
            </Tooltip>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div style={pageRootStyle}>
      <div style={toolbarSectionStackedStyle}>
        <h2 style={toolbarPageTitleStyle}>采购订单管理</h2>
        <div style={compactFilterRowFullWidthStyle}>
          <div style={filterCellFlex('1.25 1 96px', 96, 280)}>
            <Input
              placeholder="搜索订单编号、供应商"
              value={filters.keyword}
              onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
              style={{ width: '100%' }}
              allowClear
            />
          </div>
          <div style={{ flex: '0 0 auto', width: 124, minWidth: 116 }}>
            <Select
              placeholder="订单状态"
              value={filters.status}
              onChange={(value) => setFilters({ ...filters, status: value })}
              style={{ width: '100%' }}
              allowClear
            >
              <Select.Option value="PENDING">待确认</Select.Option>
              <Select.Option value="REJECTED">已拒绝</Select.Option>
              <Select.Option value="CONFIRMED">待发货</Select.Option>
              <Select.Option value="SHIPPED">已发货</Select.Option>
              <Select.Option value="RECEIVED">已入库</Select.Option>
              <Select.Option value="CANCELLED">已取消</Select.Option>
            </Select>
          </div>
          <Space size={4} style={{ flexShrink: 0 }}>
            <Tooltip title="查询">
              <Button
                type="primary"
                icon={<SearchOutlined />}
                onClick={fetchOrders}
              />
            </Tooltip>
            <Tooltip title="重置">
              <Button icon={<ReloadOutlined />} onClick={handleReset} />
            </Tooltip>
            <Tooltip title="导出Excel">
              <Button
                icon={<DownloadOutlined />}
                onClick={handleExport}
                loading={exporting}
              />
            </Tooltip>
            {hasPermission(PERMISSIONS.DRUG_MANAGE) && (
              <Tooltip title="新建采购订单">
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => {
                    setModalVisible(true)
                    form.resetFields()
                    setSelectedSupplierId(undefined)
                    setOrderFormItems([{ drugId: undefined, drugNameDisplay: '', quantity: undefined, unitPrice: undefined }])
                    fetchDrugs()
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
          dataSource={orders}
          rowKey="id"
          loading={loading}
          size="middle"
          scroll={{ x: 'max-content', y: TABLE_SCROLL_Y_STACKED }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          onChange={handleTableChange}
        />
      </div>

      {/* 新建采购订单模态框 */}
      <Modal
        title="新建采购订单"
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false)
          form.resetFields()
          setSelectedSupplierId(undefined)
          setOrderFormItems([{ drugId: undefined, drugNameDisplay: '', quantity: undefined, unitPrice: undefined }])
          fetchDrugs()
        }}
        onOk={() => form.submit()}
        width={900}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreateOrder}
        >
          <Form.Item
            name="supplierId"
            label="供应商"
            rules={[
              { required: true, message: '请选择供应商' },
            ]}
          >
            <Select
              placeholder="请选择供应商（仅显示已审核通过的供应商）"
              showSearch
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={supplierOptions}
              onChange={(value) => {
                setSelectedSupplierId(value)
                setOrderFormItems([{ drugId: undefined, drugNameDisplay: '', quantity: undefined, unitPrice: undefined }])
              }}
            />
          </Form.Item>

          <Form.Item
            name="expectedDeliveryDate"
            label="预计交货日期"
            rules={[
              { required: true, message: '请选择预计交货日期' },
            ]}
          >
            <DatePicker
              style={{ width: '100%' }}
              disabledDate={(current) => current && current < dayjs().startOf('day')}
            />
          </Form.Item>

          <Form.Item label="订单明细">
            {orderFormItems.map((item, index) => (
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
                        placeholder="请输入或选择药品（选中后自动带出协议价）"
                        value={orderFormItems[index].drugNameDisplay ?? ''}
                        filterOption={(inputValue, option) =>
                          (option?.label ?? '').toUpperCase().indexOf((inputValue || '').toUpperCase()) !== -1
                        }
                        onSelect={(value, option) => {
                          const newItems = [...orderFormItems]
                          newItems[index].drugId = value ? Number(value) : undefined
                          newItems[index].drugNameDisplay = option?.label ?? ''
                          if (option?.unitPrice != null) newItems[index].unitPrice = Number(option.unitPrice)
                          setOrderFormItems(newItems)
                        }}
                        onChange={(e) => {
                          const v = e?.target?.value ?? ''
                          const newItems = [...orderFormItems]
                          newItems[index].drugNameDisplay = v
                          if (!v) newItems[index].drugId = undefined
                          setOrderFormItems(newItems)
                        }}
                        style={{ width: 300 }}
                      />
                    </Form.Item>

                    <Form.Item
                      label="数量"
                      style={{ width: 120, marginBottom: 0 }}
                      rules={[
                        { required: true, message: '请输入数量' },
                        { type: 'number', min: 1, message: '数量必须大于0' },
                      ]}
                    >
                      <InputNumber
                        style={{ width: 120 }}
                        placeholder="数量"
                        min={1}
                        precision={0}
                        value={orderFormItems[index].quantity}
                        onChange={(value) => {
                          const newItems = [...orderFormItems]
                          newItems[index].quantity = value
                          setOrderFormItems(newItems)
                        }}
                      />
                    </Form.Item>

                    <Form.Item
                      label="单价（元）"
                      style={{ width: 150, marginBottom: 0 }}
                      rules={[
                        { required: true, message: '请输入单价' },
                        { type: 'number', min: 0.01, message: '单价必须大于0' },
                      ]}
                    >
                      <InputNumber
                        style={{ width: 150 }}
                        placeholder="单价"
                        min={0.01}
                        precision={2}
                        value={orderFormItems[index].unitPrice}
                        onChange={(value) => {
                          const newItems = [...orderFormItems]
                          newItems[index].unitPrice = value
                          setOrderFormItems(newItems)
                        }}
                      />
                    </Form.Item>

                    {orderFormItems.length > 1 && (
                      <Button
                        type="link"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => removeOrderFormItem(index)}
                      >
                        删除
                      </Button>
                    )}
                  </Space>
                  {item.quantity && item.unitPrice && (
                    <div style={{ color: '#666', fontSize: '12px' }}>
                      小计：¥{(item.quantity * item.unitPrice).toFixed(2)}
                    </div>
                  )}
                </Space>
              </div>
            ))}
            <Button
              type="dashed"
              onClick={addOrderFormItem}
              style={{ width: '100%' }}
            >
              + 添加药品
            </Button>
            {orderFormItems.length > 0 && (
              <div style={{ marginTop: 16, textAlign: 'right', fontSize: '16px', fontWeight: 'bold' }}>
                订单总金额：¥{orderFormItems.reduce((sum, item) => {
                  return sum + (item.quantity && item.unitPrice ? item.quantity * item.unitPrice : 0)
                }, 0).toFixed(2)}
              </div>
            )}
          </Form.Item>

          <Form.Item
            name="remark"
            label="备注"
            rules={[
              { max: 500, message: '备注长度不能超过500个字符' },
            ]}
          >
            <Input.TextArea
              rows={3}
              placeholder="请输入备注（可选）"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 订单明细模态框 */}
      <Modal
        title="订单明细"
        open={detailModalVisible}
        onCancel={() => {
          setDetailModalVisible(false)
          setCurrentOrder(null)
        }}
        footer={[
          <Tooltip key="close" title="关闭">
            <Button icon={<CloseOutlined />} onClick={() => setDetailModalVisible(false)} />
          </Tooltip>,
        ]}
        width={800}
      >
        {currentOrder && (
          <div>
            <div style={{ marginBottom: 16 }}>
              <p><strong>订单编号：</strong>{currentOrder.orderNumber}</p>
              <p><strong>订单状态：</strong>{getStatusTag(currentOrder.status)}</p>
              {currentOrder.supplierName && (
                <p><strong>供应商：</strong>{currentOrder.supplierName}</p>
              )}
              {currentOrder.logisticsNumber && (
                <p><strong>物流单号：</strong>{currentOrder.logisticsNumber}</p>
              )}
              {currentOrder.shipDate && (
                <p><strong>发货日期：</strong>{dayjs(currentOrder.shipDate).format('YYYY-MM-DD HH:mm:ss')}</p>
              )}
              {currentOrder.rejectReason && (
                <p><strong>拒绝理由：</strong><span style={{ color: '#ff4d4f' }}>{currentOrder.rejectReason}</span></p>
              )}
              <p><strong>订单备注：</strong>{currentOrder.remark ? currentOrder.remark : '无'}</p>
            </div>
            <Table
              columns={[
                { 
                  title: '药品名称', 
                  dataIndex: 'drugName', 
                  key: 'drugName',
                  render: (text, record) => {
                    const name = text || '未知';
                    const spec = record.specification;
                    return spec ? `${name}（${spec}）` : name;
                  }
                },
                { title: '数量', dataIndex: 'quantity', key: 'quantity', align: 'right' },
                { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', align: 'right', render: (price) => price ? `¥${price.toFixed(2)}` : '-' },
                { title: '总价', dataIndex: 'totalPrice', key: 'totalPrice', align: 'right', render: (price) => price ? `¥${price.toFixed(2)}` : '-' },
              ]}
              dataSource={currentOrder.items || []}
              rowKey="id"
              pagination={false}
            />
          </div>
        )}
      </Modal>

      {/* 拒绝订单模态框 */}
      <Modal
        title="拒绝订单"
        open={rejectModalVisible}
        onCancel={() => {
          setRejectModalVisible(false)
          rejectForm.resetFields()
          setActionOrderId(null)
        }}
        onOk={() => rejectForm.submit()}
        okText="确认拒绝"
        cancelText="取消"
      >
        <Form
          form={rejectForm}
          layout="vertical"
          onFinish={handleRejectOrder}
        >
          <Form.Item
            name="reason"
            label="拒绝理由"
            rules={[
              { required: true, message: '请输入拒绝理由' },
              { max: 500, message: '拒绝理由长度不能超过500个字符' },
            ]}
          >
            <Input.TextArea
              rows={4}
              placeholder="请输入拒绝理由"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 发货模态框 */}
      <Modal
        title="订单发货"
        open={shipModalVisible}
        onCancel={() => {
          setShipModalVisible(false)
          shipForm.resetFields()
          setActionOrderId(null)
        }}
        onOk={() => shipForm.submit()}
        okText="确认发货"
        cancelText="取消"
      >
        <Form
          form={shipForm}
          layout="vertical"
          onFinish={handleShipOrder}
        >
          <Form.Item
            name="logisticsNumber"
            label="物流单号"
            rules={[
              { required: true, message: '请输入物流单号' },
              { max: 100, message: '物流单号长度不能超过100个字符' },
            ]}
          >
            <Input
              placeholder="请输入物流单号"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 取消订单模态框 */}
      <Modal
        title="取消订单"
        open={cancelModalVisible}
        onCancel={() => {
          setCancelModalVisible(false)
          cancelForm.resetFields()
          setActionOrderId(null)
        }}
        onOk={() => cancelForm.submit()}
        okText="确认取消"
        cancelText="取消"
      >
        <Form
          form={cancelForm}
          layout="vertical"
          onFinish={handleCancelOrder}
        >
          <Form.Item
            name="reason"
            label="取消原因"
            rules={[
              { max: 500, message: '取消原因长度不能超过500个字符' },
            ]}
          >
            <Input.TextArea
              rows={4}
              placeholder="请输入取消原因（可选）"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 更新物流单号模态框 */}
      <Modal
        title="更新物流单号"
        open={logisticsModalVisible}
        onCancel={() => {
          setLogisticsModalVisible(false)
          logisticsForm.resetFields()
          setActionOrderId(null)
        }}
        onOk={() => logisticsForm.submit()}
        okText="确认更新"
        cancelText="取消"
      >
        <Form
          form={logisticsForm}
          layout="vertical"
          onFinish={handleUpdateLogistics}
        >
          <Form.Item
            name="logisticsNumber"
            label="物流单号"
            rules={[
              { required: true, message: '请输入物流单号' },
              { max: 100, message: '物流单号长度不能超过100个字符' },
            ]}
          >
            <Input
              placeholder="请输入物流单号"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 条形码显示模态框 */}
      <Modal
        title="订单条形码"
        open={barcodeModalVisible}
        onCancel={() => {
          setBarcodeModalVisible(false)
          setCurrentBarcode(null)
          setCurrentBarcodeOrderId(null)
        }}
        footer={[
          <Tooltip key="download" title="下载条形码">
            <Button type="primary" icon={<DownloadOutlined />} onClick={async () => {
              if (currentBarcodeOrderId && currentBarcode && currentBarcode.orderNumber) {
                try {
                  const response = await fetch(`/api/v1/purchase-orders/${currentBarcodeOrderId}/barcode/download`, {
                    method: 'GET',
                    credentials: 'include',
                  })
                  if (response.ok) {
                    const blob = await response.blob()
                    const url = window.URL.createObjectURL(blob)
                    const a = document.createElement('a')
                    a.href = url
                    a.download = `barcode_${currentBarcode.orderNumber}.png`
                    document.body.appendChild(a)
                    a.click()
                    window.URL.revokeObjectURL(url)
                    document.body.removeChild(a)
                    message.success('条形码下载成功')
                  } else {
                    message.error('下载条形码失败')
                  }
                } catch (error) {
                  message.error('下载条形码失败')
                }
              }
            }} />
          </Tooltip>,
          <Tooltip key="close" title="关闭">
            <Button icon={<CloseOutlined />} onClick={() => {
              setBarcodeModalVisible(false)
              setCurrentBarcode(null)
              setCurrentBarcodeOrderId(null)
            }} />
          </Tooltip>,
        ]}
        width={500}
      >
        {currentBarcode && (
          <div style={{ textAlign: 'center', padding: '20px' }}>
            <p style={{ marginBottom: '20px', fontSize: '16px', fontWeight: 'bold' }}>
              订单编号：{currentBarcode.orderNumber}
            </p>
            {currentBarcode.barcodeBase64 && (
              <Image
                src={currentBarcode.barcodeBase64}
                alt="订单条形码"
                style={{ maxWidth: '100%' }}
              />
            )}
            <p style={{ marginTop: '20px', fontSize: '12px', color: '#666' }}>
              符合GSP规范的Code128条形码，可用于扫码枪扫描
            </p>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default PurchaseOrderManagement


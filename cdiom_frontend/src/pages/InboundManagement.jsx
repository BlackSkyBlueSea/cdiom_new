import { useState, useEffect, useMemo } from 'react'
import { Table, Button, Space, Input, Select, DatePicker, Tag, Modal, Form, message, AutoComplete, InputNumber, Tooltip } from 'antd'
import { SearchOutlined, ReloadOutlined, PlusOutlined, CheckCircleOutlined, CloseCircleOutlined, BarcodeOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request from '../utils/request'
import logger from '../utils/logger'
import { hasPermission, PERMISSIONS } from '../utils/permission'

const { RangePicker } = DatePicker
const { TextArea } = Input

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
    batchNumber: '',
    operatorId: undefined,
    startDate: undefined,
    endDate: undefined,
    status: undefined,
    expiryCheckStatus: undefined,
  })
  const [modalVisible, setModalVisible] = useState(false)
  const [inboundType, setInboundType] = useState('order') // 'order' 或 'temporary'
  const [form] = Form.useForm()
  const [drugs, setDrugs] = useState([])
  const [orders, setOrders] = useState([])
  const [selectedDrug, setSelectedDrug] = useState(null)
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [orderItems, setOrderItems] = useState([])

  useEffect(() => {
    fetchInboundRecords()
  }, [pagination.current, pagination.pageSize, filters])

  useEffect(() => {
    fetchDrugs()
    if (inboundType === 'order') {
      fetchOrders()
    }
  }, [inboundType])

  // 药品选项（用于AutoComplete）
  const drugOptions = useMemo(() => {
    return drugs.map(drug => ({
      value: drug.id,
      label: `${drug.drugName} (${drug.specification || ''})`,
      drug: drug
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
        batchNumber: filters.batchNumber || undefined,
        operatorId: filters.operatorId,
        startDate: filters.startDate ? filters.startDate.format('YYYY-MM-DD') : undefined,
        endDate: filters.endDate ? filters.endDate.format('YYYY-MM-DD') : undefined,
        status: filters.status,
        expiryCheckStatus: filters.expiryCheckStatus,
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
      batchNumber: '',
      operatorId: undefined,
      startDate: undefined,
      endDate: undefined,
      status: undefined,
      expiryCheckStatus: undefined,
    })
    setPagination({ ...pagination, current: 1 })
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

  const handleOrderChange = async (orderId) => {
    if (!orderId) {
      setSelectedOrder(null)
      setOrderItems([])
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
      form.setFieldsValue({ drugId: undefined, quantity: undefined })
      setSelectedDrug(null)

      const res = await request.get(`/purchase-orders/${orderId}/items`)
      if (res.code === 200) {
        setOrderItems(res.data || [])
      }
    } catch (error) {
      logger.error('获取订单明细失败:', error)
      message.error('获取订单明细失败')
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
    form.setFieldsValue({
      manufacturer: drug.manufacturer || '',
    })
  }

  const handleCreateInbound = async (values) => {
    try {
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

      // 如果是采购订单入库，验证订单和数量
      if (inboundType === 'order' && values.orderId) {
        const orderItem = orderItems.find(item => item.drugId === values.drugId)
        if (!orderItem) {
          message.error('所选药品不在该采购订单中')
          return
        }
        // 检查已入库数量
        const inboundRes = await request.get(`/inbound/order/${values.orderId}/drug/${values.drugId}/quantity`)
        const inboundQuantity = inboundRes.code === 200 ? (inboundRes.data || 0) : 0
        const remainingQuantity = orderItem.quantity - inboundQuantity
        if (values.quantity > remainingQuantity) {
          message.error(`入库数量不能超过订单剩余数量（剩余：${remainingQuantity}）`)
          return
        }
      }

      const data = {
        ...values,
        expiryDate: values.expiryDate ? values.expiryDate.format('YYYY-MM-DD') : undefined,
        arrivalDate: values.arrivalDate ? values.arrivalDate.format('YYYY-MM-DD') : undefined,
        productionDate: values.productionDate ? values.productionDate.format('YYYY-MM-DD') : undefined,
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
        message.success('入库成功')
        setModalVisible(false)
        form.resetFields()
        setSelectedDrug(null)
        setSelectedOrder(null)
        setOrderItems([])
        fetchInboundRecords()
      } else {
        message.error(res.msg || '入库失败')
      }
    } catch (error) {
      logger.error('入库失败:', error)
      message.error(error.response?.data?.msg || error.message || '入库失败')
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
      title: <span style={{ whiteSpace: 'nowrap' }}>入库单号</span>,
      dataIndex: 'recordNumber',
      key: 'recordNumber',
      width: 150,
      ellipsis: true,
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
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>入库管理</h2>
        <Space wrap>
          <Input
            placeholder="搜索入库单号、药品名称"
            value={filters.keyword}
            onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
            style={{ width: 200 }}
            allowClear
          />
          <Input
            placeholder="批次号"
            value={filters.batchNumber}
            onChange={(e) => setFilters({ ...filters, batchNumber: e.target.value })}
            style={{ width: 150 }}
            allowClear
          />
          <Select
            placeholder="验收状态"
            value={filters.status}
            onChange={(value) => setFilters({ ...filters, status: value })}
            style={{ width: 120 }}
            allowClear
          >
            <Select.Option value="QUALIFIED">合格</Select.Option>
            <Select.Option value="UNQUALIFIED">不合格</Select.Option>
          </Select>
          <Select
            placeholder="效期校验"
            value={filters.expiryCheckStatus}
            onChange={(value) => setFilters({ ...filters, expiryCheckStatus: value })}
            style={{ width: 120 }}
            allowClear
          >
            <Select.Option value="PASS">通过</Select.Option>
            <Select.Option value="WARNING">警告</Select.Option>
            <Select.Option value="FORCE">强制入库</Select.Option>
          </Select>
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
          />
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
                  setModalVisible(true)
                  setInboundType('temporary')
                }}
              />
            </Tooltip>
          )}
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={inboundRecords}
        rowKey="id"
        loading={loading}
        size="middle"
        scroll={{ x: 'max-content' }}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
        onChange={handleTableChange}
      />

      <Modal
        title={inboundType === 'order' ? '采购订单入库' : '临时入库'}
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false)
          form.resetFields()
          setSelectedDrug(null)
          setSelectedOrder(null)
          setOrderItems([])
          setBarcodeOrderNumber('')
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
                  if (item) {
                    form.setFieldsValue({ quantity: item.quantity })
                    const drug = drugs.find(d => Number(d.id) === Number(drugId))
                    if (drug) {
                      setSelectedDrug(drug)
                      form.setFieldsValue({ manufacturer: drug.manufacturer || '' })
                    }
                  }
                }}
              >
                {orderItems.map(item => {
                  const drug = drugs.find(d => Number(d.id) === Number(item.drugId))
                  const label = drug
                    ? `${drug.drugName || '未知'}${drug.specification ? ` (${drug.specification})` : ''} - 订单数量: ${item.quantity}`
                    : `药品ID: ${item.drugId} - 数量: ${item.quantity}`
                  return (
                    <Select.Option key={item.drugId} value={item.drugId} label={label}>
                      {label}
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
                  option.label.toUpperCase().indexOf(inputValue.toUpperCase()) !== -1
                }
                onSelect={handleDrugSelect}
                onChange={(value) => {
                  if (!value) {
                    setSelectedDrug(null)
                    form.setFieldsValue({ manufacturer: '' })
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

          <Form.Item
            name="quantity"
            label="入库数量"
            rules={[
              { required: true, message: '请输入入库数量' },
              { type: 'number', min: 1, message: '入库数量必须大于0' },
              { type: 'number', max: 999999, message: '入库数量不能超过999999' },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="请输入入库数量"
              min={1}
              max={999999}
              precision={0}
            />
          </Form.Item>

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
            />
          </Form.Item>

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

          <Form.Item
            name="deliveryNoteNumber"
            label="随货同行单编号"
            rules={[
              { max: 100, message: '随货同行单编号长度不能超过100个字符' },
            ]}
          >
            <Input placeholder="请输入随货同行单编号（可选）" />
          </Form.Item>

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

          <Form.Item
            name="expiryCheckReason"
            label="效期校验说明"
            rules={[
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
    </div>
  )
}

export default InboundManagement


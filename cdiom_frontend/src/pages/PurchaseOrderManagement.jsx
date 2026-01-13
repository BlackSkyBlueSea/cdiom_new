import { useState, useEffect, useMemo } from 'react'
import { Table, Button, Space, Input, Select, Card, Tag, Modal, Form, message, DatePicker, InputNumber, AutoComplete } from 'antd'
import { SearchOutlined, ReloadOutlined, PlusOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request from '../utils/request'
import { hasPermission, PERMISSIONS } from '../utils/permission'

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
  const [orderFormItems, setOrderFormItems] = useState([{ drugId: undefined, quantity: undefined, unitPrice: undefined }])

  useEffect(() => {
    fetchOrders()
  }, [pagination.current, pagination.pageSize, filters])

  useEffect(() => {
    fetchSuppliers()
    fetchDrugs()
  }, [])

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

  // 药品选项
  const drugOptions = useMemo(() => {
    return drugs.map(drug => ({
      value: drug.id,
      label: `${drug.drugName} (${drug.specification || ''})`,
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
      console.error('获取供应商列表失败:', error)
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
      console.error('获取药品列表失败:', error)
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
      console.error('获取采购订单失败:', error)
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
        setOrderFormItems([{ drugId: undefined, quantity: undefined, unitPrice: undefined }])
        fetchOrders()
      } else {
        message.error(res.msg || '创建采购订单失败')
      }
    } catch (error) {
      console.error('创建采购订单失败:', error)
      message.error(error.response?.data?.msg || error.message || '创建采购订单失败')
    }
  }

  const addOrderFormItem = () => {
    setOrderFormItems([...orderFormItems, { drugId: undefined, quantity: undefined, unitPrice: undefined }])
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

  const columns = [
    {
      title: '订单编号',
      dataIndex: 'orderNumber',
      key: 'orderNumber',
      width: 150,
    },
    {
      title: '供应商',
      dataIndex: 'supplierName',
      key: 'supplierName',
      width: 150,
    },
    {
      title: '订单状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => getStatusTag(status),
    },
    {
      title: '预计交货日期',
      dataIndex: 'expectedDeliveryDate',
      key: 'expectedDeliveryDate',
      width: 120,
      render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-',
    },
    {
      title: '物流单号',
      dataIndex: 'logisticsNumber',
      key: 'logisticsNumber',
      width: 150,
    },
    {
      title: '订单总金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      align: 'right',
      render: (amount) => amount ? `¥${amount.toFixed(2)}` : '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space>
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
          >
            查看明细
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <Space wrap>
            <Input
              placeholder="搜索订单编号、供应商"
              value={filters.keyword}
              onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
              style={{ width: 200 }}
              allowClear
            />
            <Select
              placeholder="订单状态"
              value={filters.status}
              onChange={(value) => setFilters({ ...filters, status: value })}
              style={{ width: 120 }}
              allowClear
            >
              <Select.Option value="PENDING">待确认</Select.Option>
              <Select.Option value="REJECTED">已拒绝</Select.Option>
              <Select.Option value="CONFIRMED">待发货</Select.Option>
              <Select.Option value="SHIPPED">已发货</Select.Option>
              <Select.Option value="RECEIVED">已入库</Select.Option>
              <Select.Option value="CANCELLED">已取消</Select.Option>
            </Select>
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={fetchOrders}
            >
              查询
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>
              重置
            </Button>
            {hasPermission(PERMISSIONS.DRUG_MANAGE) && (
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => {
                  setModalVisible(true)
                  form.resetFields()
                  setOrderFormItems([{ drugId: undefined, quantity: undefined, unitPrice: undefined }])
                }}
              >
                新建采购订单
              </Button>
            )}
          </Space>

          <Table
            columns={columns}
            dataSource={orders}
            rowKey="id"
            loading={loading}
            pagination={{
              ...pagination,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 条`,
            }}
            onChange={handleTableChange}
          />
        </Space>
      </Card>

      {/* 新建采购订单模态框 */}
      <Modal
        title="新建采购订单"
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false)
          form.resetFields()
          setOrderFormItems([{ drugId: undefined, quantity: undefined, unitPrice: undefined }])
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
                        placeholder="请输入或选择药品"
                        filterOption={(inputValue, option) =>
                          option.label.toUpperCase().indexOf(inputValue.toUpperCase()) !== -1
                        }
                        onSelect={(value) => {
                          const newItems = [...orderFormItems]
                          newItems[index].drugId = value
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
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>,
        ]}
        width={800}
      >
        {currentOrder && (
          <div>
            <p><strong>订单编号：</strong>{currentOrder.orderNumber}</p>
            <p><strong>订单状态：</strong>{getStatusTag(currentOrder.status)}</p>
            <Table
              columns={[
                { title: '药品名称', dataIndex: 'drugName', key: 'drugName' },
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
    </div>
  )
}

export default PurchaseOrderManagement


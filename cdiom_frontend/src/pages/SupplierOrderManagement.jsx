import { useState, useEffect } from 'react'
import { Table, Button, Space, Input, Select, Tag, Modal, Form, message, DatePicker } from 'antd'
import { SearchOutlined, ReloadOutlined, EyeOutlined, CheckOutlined, CloseOutlined, SendOutlined, EditOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request from '../utils/request'
import { useNavigate, useSearchParams } from 'react-router-dom'

const SupplierOrderManagement = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [filters, setFilters] = useState({
    keyword: '',
    status: searchParams.get('status') || undefined,
  })
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [currentOrder, setCurrentOrder] = useState(null)
  const [confirmModalVisible, setConfirmModalVisible] = useState(false)
  const [rejectModalVisible, setRejectModalVisible] = useState(false)
  const [shipModalVisible, setShipModalVisible] = useState(false)
  const [logisticsModalVisible, setLogisticsModalVisible] = useState(false)
  const [confirmForm] = Form.useForm()
  const [rejectForm] = Form.useForm()
  const [shipForm] = Form.useForm()
  const [logisticsForm] = Form.useForm()
  const [actionOrderId, setActionOrderId] = useState(null)

  useEffect(() => {
    fetchOrders()
  }, [pagination.current, pagination.pageSize, filters])

  const fetchOrders = async () => {
    setLoading(true)
    try {
      const params = {
        page: pagination.current,
        size: pagination.pageSize,
        keyword: filters.keyword || undefined,
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
        message.error(res.msg || '获取订单列表失败')
      }
    } catch (error) {
      console.error('获取订单列表失败:', error)
      message.error('获取订单列表失败')
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
      status: undefined,
    })
    setPagination({ ...pagination, current: 1 })
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
  const handleConfirmOrder = async () => {
    try {
      const res = await request.post(`/purchase-orders/${actionOrderId}/confirm`)
      if (res.code === 200) {
        message.success('订单确认成功')
        setConfirmModalVisible(false)
        confirmForm.resetFields()
        setActionOrderId(null)
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
      title: '订单编号',
      dataIndex: 'orderNumber',
      key: 'orderNumber',
      width: 150,
      ellipsis: true,
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
      ellipsis: true,
    },
    {
      title: '发货日期',
      dataIndex: 'shipDate',
      key: 'shipDate',
      width: 180,
      render: (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '订单金额',
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
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space wrap>
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
          {record.status === 'PENDING' && (
            <>
              <Button
                type="link"
                size="small"
                icon={<CheckOutlined />}
                style={{ color: '#52c41a' }}
                onClick={() => {
                  setActionOrderId(record.id)
                  setConfirmModalVisible(true)
                }}
              >
                确认
              </Button>
              <Button
                type="link"
                size="small"
                icon={<CloseOutlined />}
                danger
                onClick={() => {
                  setActionOrderId(record.id)
                  setRejectModalVisible(true)
                }}
              >
                拒绝
              </Button>
            </>
          )}
          {record.status === 'CONFIRMED' && (
            <Button
              type="link"
              size="small"
              icon={<SendOutlined />}
              onClick={() => {
                setActionOrderId(record.id)
                shipForm.resetFields()
                setShipModalVisible(true)
              }}
            >
              发货
            </Button>
          )}
          {(record.status === 'SHIPPED' || record.status === 'CONFIRMED') && (
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => {
                setActionOrderId(record.id)
                logisticsForm.setFieldsValue({ logisticsNumber: record.logisticsNumber || '' })
                setLogisticsModalVisible(true)
              }}
            >
              物流
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>订单管理</h2>
        <Space wrap>
          <Input
            placeholder="搜索订单编号"
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
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={orders}
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
            <div style={{ marginBottom: 16 }}>
              <p><strong>订单编号：</strong>{currentOrder.orderNumber}</p>
              <p><strong>订单状态：</strong>{getStatusTag(currentOrder.status)}</p>
              {currentOrder.logisticsNumber && (
                <p><strong>物流单号：</strong>{currentOrder.logisticsNumber}</p>
              )}
              {currentOrder.shipDate && (
                <p><strong>发货日期：</strong>{dayjs(currentOrder.shipDate).format('YYYY-MM-DD HH:mm:ss')}</p>
              )}
              {currentOrder.rejectReason && (
                <p><strong>拒绝理由：</strong><span style={{ color: '#ff4d4f' }}>{currentOrder.rejectReason}</span></p>
              )}
              {currentOrder.remark && (
                <p><strong>备注：</strong>{currentOrder.remark}</p>
              )}
            </div>
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

      {/* 确认订单模态框 */}
      <Modal
        title="确认订单"
        open={confirmModalVisible}
        onCancel={() => {
          setConfirmModalVisible(false)
          confirmForm.resetFields()
          setActionOrderId(null)
        }}
        onOk={handleConfirmOrder}
        okText="确认订单"
        cancelText="取消"
      >
        <p>确定要确认此订单吗？确认后订单状态将变为"待发货"。</p>
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
    </div>
  )
}

export default SupplierOrderManagement




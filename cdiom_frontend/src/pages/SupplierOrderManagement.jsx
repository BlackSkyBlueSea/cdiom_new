import { useState, useEffect } from 'react'
import { Table, Button, Space, Input, Select, Tag, Modal, Form, message, DatePicker, Tooltip, Image } from 'antd'
import { SearchOutlined, ReloadOutlined, EyeOutlined, CheckOutlined, CloseOutlined, SendOutlined, EditOutlined, BarcodeOutlined, DownloadOutlined } from '@ant-design/icons'
import Cookies from 'js-cookie'
import dayjs from 'dayjs'
import request from '../utils/request'
import logger from '../utils/logger'
import { useSearchParams } from 'react-router-dom'
import {
  pageRootStyle,
  tableAreaStyle,
  toolbarSectionStackedStyle,
  toolbarPageTitleStyle,
  compactFilterRowFullWidthStyle,
  filterCellFlex,
  TABLE_SCROLL_Y_STACKED,
} from '../utils/tablePageLayout'

const SupplierOrderManagement = () => {
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
  const [rejectForm] = Form.useForm()
  const [shipForm] = Form.useForm()
  const [logisticsForm] = Form.useForm()
  const [actionOrderId, setActionOrderId] = useState(null)
  const [barcodeModalVisible, setBarcodeModalVisible] = useState(false)
  const [currentBarcode, setCurrentBarcode] = useState(null)
  const [currentBarcodeOrderId, setCurrentBarcodeOrderId] = useState(null)

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
      logger.error('获取订单列表失败:', error)
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

  const openOrderBarcode = async (record) => {
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
  }

  const columns = [
    {
      title: '订单编号',
      dataIndex: 'orderNumber',
      key: 'orderNumber',
      width: 180,
      ellipsis: true,
      render: (text, record) => (
        <Space>
          <span>{text}</span>
          <Tooltip title="条形码（与采购端一致，可下载）">
            <Button
              type="link"
              size="small"
              icon={<BarcodeOutlined />}
              onClick={() => openOrderBarcode(record)}
            />
          </Tooltip>
        </Space>
      ),
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
          {record.status === 'PENDING' && (
            <>
              <Tooltip title="确认">
                <Button
                  type="link"
                  size="small"
                  icon={<CheckOutlined />}
                  style={{ color: '#52c41a' }}
                  onClick={() => {
                    setActionOrderId(record.id)
                    setConfirmModalVisible(true)
                  }}
                />
              </Tooltip>
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
          {record.status === 'CONFIRMED' && (
            <Tooltip title="发货">
              <Button
                type="link"
                size="small"
                icon={<SendOutlined />}
                onClick={() => {
                  setActionOrderId(record.id)
                  shipForm.resetFields()
                  setShipModalVisible(true)
                }}
              />
            </Tooltip>
          )}
          {(record.status === 'SHIPPED' || record.status === 'CONFIRMED') && (
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
        <h2 style={toolbarPageTitleStyle}>订单管理</h2>
        <div style={compactFilterRowFullWidthStyle}>
          <div style={filterCellFlex('1.2 1 100px', 100, 280)}>
            <Input
              placeholder="搜索订单编号"
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
              <p>
                <strong>订单编号：</strong>
                {currentOrder.orderNumber}
                <Tooltip title="条形码">
                  <Button
                    type="link"
                    size="small"
                    icon={<BarcodeOutlined />}
                    onClick={() => openOrderBarcode(currentOrder)}
                  />
                </Tooltip>
              </p>
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

      {/* 订单条形码（与采购订单管理相同接口） */}
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
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              onClick={async () => {
                if (!currentBarcodeOrderId || !currentBarcode?.orderNumber) return
                try {
                  let token = sessionStorage.getItem('cdiom_token')
                  if (!token) token = Cookies.get('cdiom_token')
                  const headers = {}
                  if (token) headers.Authorization = `Bearer ${token}`
                  const response = await fetch(
                    `/api/v1/purchase-orders/${currentBarcodeOrderId}/barcode/download`,
                    { method: 'GET', credentials: 'include', headers }
                  )
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
              }}
            />
          </Tooltip>,
          <Tooltip key="close" title="关闭">
            <Button
              icon={<CloseOutlined />}
              onClick={() => {
                setBarcodeModalVisible(false)
                setCurrentBarcode(null)
                setCurrentBarcodeOrderId(null)
              }}
            />
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
              符合 GSP 规范的 Code128 条形码，可用于扫码枪扫描
            </p>
          </div>
        )}
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




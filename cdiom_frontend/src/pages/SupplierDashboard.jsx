import { useState, useEffect } from 'react'
import { Card, Row, Col, Statistic, Table, Tag, Spin, Empty, Button, Alert } from 'antd'
import { 
  ShoppingCartOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  SendOutlined,
  EyeOutlined,
} from '@ant-design/icons'
import { Column } from '@ant-design/charts'
import request from '../utils/request'
import { useNavigate } from 'react-router-dom'
import dayjs from 'dayjs'

const SupplierDashboard = () => {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [dashboardData, setDashboardData] = useState({
    totalOrders: 0,
    statusStats: {},
    totalAmount: 0,
    pendingAmount: 0,
    confirmedAmount: 0,
    pendingOrders: 0,
    dates: [],
    orderCounts: []
  })
  const [shippedOrders, setShippedOrders] = useState([])
  const [shippedOrdersLoading, setShippedOrdersLoading] = useState(false)

  useEffect(() => {
    fetchDashboardData()
    fetchShippedOrders()
  }, [])

  const fetchDashboardData = async () => {
    setLoading(true)
    try {
      const res = await request.get('/dashboard/supplier')
      if (res.code === 200) {
        setDashboardData(res.data)
      }
    } catch (error) {
      console.error('获取供应商仪表盘数据失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const fetchShippedOrders = async () => {
    setShippedOrdersLoading(true)
    try {
      const res = await request.get('/purchase-orders/shipped-orders', {
        params: { page: 1, size: 5 }
      })
      if (res.code === 200) {
        setShippedOrders(res.data.records || [])
      }
    } catch (error) {
      console.error('获取已发货订单失败:', error)
    } finally {
      setShippedOrdersLoading(false)
    }
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

  const chartConfig = {
    data: dashboardData.dates.map((date, index) => ({
      date,
      count: dashboardData.orderCounts[index] || 0
    })),
    xField: 'date',
    yField: 'count',
    columnWidthRatio: 0.6,
    color: '#1890ff',
    label: {
      position: 'top',
      style: {
        fill: '#666',
        fontSize: 12,
      },
    },
    xAxis: {
      label: {
        autoRotate: false,
      },
    },
  }

  const shippedOrdersColumns = [
    {
      title: '订单编号',
      dataIndex: 'orderNumber',
      key: 'orderNumber',
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
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => navigate(`/supplier-orders?orderId=${record.id}`)}
        >
          查看详情
        </Button>
      ),
    },
  ]

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
      </div>
    )
  }

  return (
    <div>
      <h2 style={{ marginBottom: '24px', margin: 0 }}>供应商工作台</h2>

      {/* 待处理订单提醒卡片 */}
      {dashboardData.pendingOrders > 0 && (
        <Alert
          message={`您有 ${dashboardData.pendingOrders} 个待处理订单，请及时处理`}
          type="warning"
          showIcon
          icon={<WarningOutlined />}
          style={{ marginBottom: '24px' }}
          action={
            <Button
              type="primary"
              size="small"
              onClick={() => navigate('/supplier-orders?status=PENDING')}
            >
              立即处理
            </Button>
          }
        />
      )}

      {/* 统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="订单总数"
              value={dashboardData.totalOrders || 0}
              prefix={<ShoppingCartOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="待处理订单"
              value={dashboardData.pendingOrders || 0}
              prefix={<WarningOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="订单总金额"
              value={dashboardData.totalAmount || 0}
              precision={2}
              valueStyle={{ color: '#52c41a' }}
              suffix="元"
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="待确认金额"
              value={dashboardData.pendingAmount || 0}
              precision={2}
              valueStyle={{ color: '#ff4d4f' }}
              suffix="元"
            />
          </Card>
        </Col>
      </Row>

      {/* 近30天订单统计图表 */}
      <Card title="近30天订单统计" style={{ marginBottom: '24px' }}>
        {dashboardData.dates && dashboardData.dates.length > 0 ? (
          <Column {...chartConfig} height={300} />
        ) : (
          <Empty description="暂无数据" />
        )}
      </Card>

      {/* 已发货订单跟踪列表 */}
      <Card 
        title="已发货订单跟踪" 
        extra={
          <Button type="link" onClick={() => navigate('/supplier-orders?status=SHIPPED')}>
            查看全部
          </Button>
        }
      >
        <Table
          columns={shippedOrdersColumns}
          dataSource={shippedOrders}
          rowKey="id"
          loading={shippedOrdersLoading}
          pagination={false}
          size="middle"
          locale={{
            emptyText: '暂无已发货订单'
          }}
        />
      </Card>
    </div>
  )
}

export default SupplierDashboard


import { useState, useEffect } from 'react'
import { Card, Row, Col, Statistic, Table, Tag, Spin, Empty, List, Button, Alert } from 'antd'
import { 
  UserOutlined, 
  TeamOutlined, 
  SettingOutlined, 
  BellOutlined,
  MedicineBoxOutlined,
  LoginOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  InboxOutlined,
  ExportOutlined,
  ShoppingCartOutlined,
  CheckCircleFilled,
  CloseCircleFilled,
  DollarOutlined,
  ShopOutlined,
  FileDoneOutlined
} from '@ant-design/icons'
import { Column, Line } from '@ant-design/charts'
import request from '../utils/request'
import { hasPermission, PERMISSIONS } from '../utils/permission'
import { getUserRoleId } from '../utils/auth'
import { useNavigate } from 'react-router-dom'
import dayjs from 'dayjs'
import './Dashboard.css'

const Dashboard = () => {
  const navigate = useNavigate()
  const roleId = getUserRoleId()
  const [loading, setLoading] = useState(true)
  const [statistics, setStatistics] = useState({})
  const [loginTrend, setLoginTrend] = useState({})
  const [operationStats, setOperationStats] = useState({})
  const [recentLogs, setRecentLogs] = useState([])
  // 仓库管理员专用数据
  const [warehouseStats, setWarehouseStats] = useState({
    nearExpiryWarning: { yellow: 0, red: 0 },
    pendingTasks: { pendingInbound: 0, pendingOutbound: 0 },
    todayStats: { inbound: 0, outbound: 0 },
    totalInventory: 0,
    dates: [],
    inboundCounts: [],
    outboundCounts: []
  })
  // 采购专员专用数据
  const [purchaserStats, setPurchaserStats] = useState({
    totalOrders: 0,
    statusStats: {},
    totalAmount: 0,
    dates: [],
    orderCounts: [],
    uniqueSuppliers: 0
  })
  // 医护人员专用数据
  const [medicalStaffStats, setMedicalStaffStats] = useState({
    totalApplies: 0,
    statusStats: {},
    dates: [],
    applyCounts: []
  })
  // 供应商专用数据
  const [supplierStats, setSupplierStats] = useState({
    totalOrders: 0,
    statusStats: {},
    totalAmount: 0,
    pendingAmount: 0,
    confirmedAmount: 0,
    dates: [],
    orderCounts: []
  })

  useEffect(() => {
    fetchDashboardData()
  }, [])

  const fetchDashboardData = async () => {
    setLoading(true)
    try {
      // 获取统计数据
      const statsRes = await request.get('/dashboard/statistics')
      if (statsRes.code === 200) {
        setStatistics(statsRes.data)
      }

      // 仓库管理员专用数据
      if (roleId === 2) {
        try {
          const warehouseRes = await request.get('/dashboard/warehouse')
          if (warehouseRes.code === 200 && warehouseRes.data) {
            setWarehouseStats(warehouseRes.data)
          } else {
            setWarehouseStats({
              nearExpiryWarning: { yellow: 0, red: 0 },
              pendingTasks: { pendingInbound: 0, pendingOutbound: 0 },
              todayStats: { inbound: 0, outbound: 0 },
              totalInventory: 0,
              dates: [],
              inboundCounts: [],
              outboundCounts: []
            })
          }
        } catch (error) {
          console.warn('获取仓库管理员数据失败，使用默认数据:', error.message)
          setWarehouseStats({
            nearExpiryWarning: { yellow: 0, red: 0 },
            pendingTasks: { pendingInbound: 0, pendingOutbound: 0 },
            todayStats: { inbound: 0, outbound: 0 },
            totalInventory: 0
          })
        }
      }

      // 采购专员专用数据
      if (roleId === 3) {
        try {
          const purchaserRes = await request.get('/dashboard/purchaser')
          if (purchaserRes.code === 200 && purchaserRes.data) {
            setPurchaserStats(purchaserRes.data)
          }
        } catch (error) {
          console.warn('获取采购专员数据失败:', error.message)
        }
      }

      // 医护人员专用数据
      if (roleId === 4) {
        try {
          const medicalRes = await request.get('/dashboard/medical-staff')
          if (medicalRes.code === 200 && medicalRes.data) {
            setMedicalStaffStats(medicalRes.data)
          }
        } catch (error) {
          console.warn('获取医护人员数据失败:', error.message)
        }
      }

      // 供应商专用数据
      if (roleId === 5) {
        try {
          const supplierRes = await request.get('/dashboard/supplier')
          if (supplierRes.code === 200 && supplierRes.data) {
            setSupplierStats(supplierRes.data)
          }
        } catch (error) {
          console.warn('获取供应商数据失败:', error.message)
        }
      }

      // 获取登录趋势（仅系统管理员）
      if (hasPermission(PERMISSIONS.LOG_LOGIN_VIEW)) {
        try {
          const trendRes = await request.get('/dashboard/login-trend')
          if (trendRes.code === 200) {
            setLoginTrend(trendRes.data)
          }
        } catch (error) {
          console.warn('获取登录趋势失败:', error)
        }
      }

      // 获取操作日志统计（仅系统管理员）
      if (hasPermission(PERMISSIONS.LOG_OPERATION_VIEW)) {
        try {
          const operationRes = await request.get('/dashboard/operation-statistics')
          if (operationRes.code === 200) {
            setOperationStats(operationRes.data)
          }
        } catch (error) {
          console.warn('获取操作日志统计失败:', error)
        }

        // 获取最近操作日志
        try {
          const logsRes = await request.get('/operation-logs', {
            params: {
              page: 1,
              size: 10,
            }
          })
          if (logsRes.code === 200) {
            setRecentLogs(logsRes.data?.records || [])
          }
        } catch (error) {
          console.warn('获取最近操作日志失败:', error)
        }
      }
    } catch (error) {
      console.error('获取仪表盘数据失败:', error)
    } finally {
      setLoading(false)
    }
  }

  // 登录趋势图表配置
  const loginTrendData = loginTrend.dates?.map((date, index) => [
    { date, type: '成功', value: loginTrend.successCounts?.[index] || 0 },
    { date, type: '失败', value: loginTrend.failCounts?.[index] || 0 },
  ]).flat() || []
  
  const loginTrendConfig = {
    data: loginTrendData,
    xField: 'date',
    yField: 'value',
    seriesField: 'type',
    smooth: true,
    point: {
      size: 4,
      shape: 'circle',
    },
    legend: {
      position: 'top',
    },
    color: ['#52c41a', '#ff4d4f'],
  }

  // 操作日志趋势图表配置
  const operationTrendConfig = {
    data: operationStats.dates?.map((date, index) => ({
      date,
      操作次数: operationStats.counts?.[index] || 0,
    })) || [],
    xField: 'date',
    yField: '操作次数',
    smooth: true,
    point: {
      size: 4,
      shape: 'circle',
    },
    color: '#1890ff',
  }

  // 模块统计图表配置
  const moduleStatsConfig = {
    data: Object.entries(operationStats.moduleStats || {}).map(([module, count]) => ({
      module,
      count,
    })).sort((a, b) => b.count - a.count).slice(0, 5),
    xField: 'module',
    yField: 'count',
    columnWidthRatio: 0.6,
    color: '#1890ff',
  }

  // 操作类型统计图表配置
  const typeStatsConfig = {
    data: Object.entries(operationStats.typeStats || {}).map(([type, count]) => ({
      type,
      count,
    })).sort((a, b) => b.count - a.count),
    xField: 'type',
    yField: 'count',
    columnWidthRatio: 0.6,
    color: '#52c41a',
  }

  // 最近操作日志表格列
  const logColumns = [
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>ID</span>,
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作时间</span>,
      dataIndex: 'operationTime',
      key: 'operationTime',
      width: 180,
      render: (text) => text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-',
      sorter: (a, b) => {
        const timeA = a.operationTime ? dayjs(a.operationTime).valueOf() : 0
        const timeB = b.operationTime ? dayjs(b.operationTime).valueOf() : 0
        return timeB - timeA // 降序：最新的在上
      },
      defaultSortOrder: 'descend',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作人</span>,
      dataIndex: 'username',
      key: 'username',
      width: 120,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作模块</span>,
      dataIndex: 'module',
      key: 'module',
      width: 120,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作类型</span>,
      dataIndex: 'operationType',
      key: 'operationType',
      width: 100,
      render: (type) => {
        const typeMap = {
          'INSERT': { color: 'green', text: '新增' },
          'UPDATE': { color: 'blue', text: '修改' },
          'DELETE': { color: 'red', text: '删除' },
          'SELECT': { color: 'default', text: '查询' },
        }
        const config = typeMap[type] || { color: 'default', text: type }
        return <Tag color={config.color}>{config.text}</Tag>
      },
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作内容</span>,
      dataIndex: 'operationContent',
      key: 'operationContent',
      width: 200,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>状态</span>,
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) => status === 1 ? (
        <Tag icon={<CheckCircleOutlined />} color="success">成功</Tag>
      ) : (
        <Tag icon={<CloseCircleOutlined />} color="error">失败</Tag>
      ),
    },
  ]

  // 今日出入库统计图表配置（仓库管理员）
  const todayInOutConfig = {
    data: [
      { type: '入库', value: warehouseStats.todayStats?.inbound ?? 0 },
      { type: '出库', value: warehouseStats.todayStats?.outbound ?? 0 },
    ],
    xField: 'type',
    yField: 'value',
    columnWidthRatio: 0.6,
    color: ({ type }) => {
      return type === '入库' ? '#52c41a' : '#1890ff'
    },
  }

  // 出入库趋势图表配置（仓库管理员）
  const inOutTrendData = warehouseStats.dates?.map((date, index) => [
    { date, type: '入库', value: warehouseStats.inboundCounts?.[index] || 0 },
    { date, type: '出库', value: warehouseStats.outboundCounts?.[index] || 0 },
  ]).flat() || []
  
  const inOutTrendConfig = {
    data: inOutTrendData,
    xField: 'date',
    yField: 'value',
    seriesField: 'type',
    smooth: true,
    point: { size: 4, shape: 'circle' },
    legend: { position: 'top' },
    color: ['#52c41a', '#1890ff'],
  }

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
      </div>
    )
  }

  // 仓库管理员专用仪表盘
  if (roleId === 2) {
    return (
      <div>
        {/* 统计卡片 - 一行显示5个 */}
        <Row 
          gutter={[16, 16]} 
          style={{ marginBottom: '24px' }}
          className="warehouse-stats-row"
        >
          <Col xs={24} sm={12} md={24} lg={24} xl={24} style={{ flex: '1 1 0', minWidth: '180px' }}>
            <Card>
              <Statistic
                title="近效期预警（90-180天）"
                value={warehouseStats.nearExpiryWarning?.yellow ?? 0}
                prefix={<WarningOutlined />}
                valueStyle={{ color: '#faad14' }}
              />
              <div style={{ marginTop: '8px', fontSize: '12px', color: '#999' }}>
                黄色预警
              </div>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={24} lg={24} xl={24} style={{ flex: '1 1 0', minWidth: '180px' }}>
            <Card>
              <Statistic
                title="近效期预警（≤90天）"
                value={warehouseStats.nearExpiryWarning?.red ?? 0}
                prefix={<WarningOutlined />}
                valueStyle={{ color: '#ff4d4f' }}
              />
              <div style={{ marginTop: '8px', fontSize: '12px', color: '#999' }}>
                红色预警
              </div>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={24} lg={24} xl={24} style={{ flex: '1 1 0', minWidth: '180px' }}>
            <Card>
              <Statistic
                title="库存总量"
                value={warehouseStats.totalInventory ?? 0}
                prefix={<MedicineBoxOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
              <div style={{ marginTop: '8px', fontSize: '12px', color: '#999' }}>
                库存批次总数
              </div>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={24} lg={24} xl={24} style={{ flex: '1 1 0', minWidth: '180px' }}>
            <Card>
              <Statistic
                title="药品总数"
                value={statistics.totalDrugs || 0}
                prefix={<MedicineBoxOutlined />}
                valueStyle={{ color: '#13c2c2' }}
              />
              <div style={{ marginTop: '8px', fontSize: '12px', color: '#999' }}>
                普通: {statistics.normalDrugs || 0} | 特殊: {statistics.specialDrugs || 0}
              </div>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={24} lg={24} xl={24} style={{ flex: '1 1 0', minWidth: '180px' }}>
            <Card>
              <Statistic
                title="通知公告"
                value={statistics.totalNotices || 0}
                prefix={<BellOutlined />}
                valueStyle={{ color: '#fa8c16' }}
              />
              <div style={{ marginTop: '8px', fontSize: '12px', color: '#999' }}>
                启用: {statistics.activeNotices || 0}
              </div>
            </Card>
          </Col>
        </Row>

        {/* 待办任务和今日统计 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
          <Col xs={24} lg={12}>
            <Card title="待办任务" extra={<Button type="link" onClick={() => navigate('/drugs')}>查看详情</Button>}>
              <List
                size="small"
                dataSource={[
                  { 
                    title: '待入库订单', 
                    count: warehouseStats.pendingTasks?.pendingInbound ?? 0,
                    icon: <InboxOutlined style={{ color: '#1890ff' }} />,
                    path: '/drugs' // 待实现：入库管理页面
                  },
                  { 
                    title: '待审批出库', 
                    count: warehouseStats.pendingTasks?.pendingOutbound ?? 0,
                    icon: <ExportOutlined style={{ color: '#52c41a' }} />,
                    path: '/drugs' // 待实现：出库管理页面
                  },
                ]}
                renderItem={(item) => (
                  <List.Item
                    style={{ cursor: 'pointer' }}
                    onClick={() => navigate(item.path)}
                  >
                    <List.Item.Meta
                      avatar={item.icon}
                      title={
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span>{item.title}</span>
                          <Tag color={item.count > 0 ? 'red' : 'default'}>{item.count} 项</Tag>
                        </div>
                      }
                    />
                  </List.Item>
                )}
              />
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="今日出入库统计" style={{ height: '100%' }}>
              {(warehouseStats.todayStats?.inbound ?? 0) > 0 || (warehouseStats.todayStats?.outbound ?? 0) > 0 ? (
                <Column {...todayInOutConfig} height={200} />
              ) : (
                <Empty description="今日暂无出入库记录" style={{ marginTop: '50px' }} />
              )}
              <div style={{ marginTop: '16px', display: 'flex', justifyContent: 'space-around' }}>
                <Statistic
                  title="今日入库"
                  value={warehouseStats.todayStats?.inbound ?? 0}
                  prefix={<InboxOutlined />}
                  valueStyle={{ color: '#52c41a' }}
                />
                <Statistic
                  title="今日出库"
                  value={warehouseStats.todayStats?.outbound ?? 0}
                  prefix={<ExportOutlined />}
                  valueStyle={{ color: '#1890ff' }}
                />
              </div>
            </Card>
          </Col>
        </Row>

        {/* 出入库趋势图 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
          <Col xs={24}>
            <Card title="最近7天出入库趋势">
              {warehouseStats.dates && warehouseStats.dates.length > 0 ? (
                <Line {...inOutTrendConfig} height={300} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: '50px' }} />
              )}
            </Card>
          </Col>
        </Row>

        {/* 近效期预警提示 */}
        {((warehouseStats.nearExpiryWarning?.yellow ?? 0) > 0 || (warehouseStats.nearExpiryWarning?.red ?? 0) > 0) && (
          <Alert
            message="近效期药品预警"
            description={
              <div>
                {(warehouseStats.nearExpiryWarning?.red ?? 0) > 0 && (
                  <div style={{ marginBottom: '8px' }}>
                    <Tag color="red">紧急：{warehouseStats.nearExpiryWarning.red} 个批次有效期≤90天，请立即处理！</Tag>
                  </div>
                )}
                {(warehouseStats.nearExpiryWarning?.yellow ?? 0) > 0 && (
                  <div>
                    <Tag color="orange">注意：{warehouseStats.nearExpiryWarning.yellow} 个批次有效期90-180天，请关注处理。</Tag>
                  </div>
                )}
              </div>
            }
            type="warning"
            showIcon
            icon={<WarningOutlined />}
            style={{ marginBottom: '24px' }}
            action={
              <Button size="small" onClick={() => navigate('/drugs')}>
                查看详情
              </Button>
            }
          />
        )}

      </div>
    )
  }

  // 采购专员专用仪表盘
  if (roleId === 3) {
    const statusMap = {
      'PENDING': { text: '待确认', color: 'orange' },
      'REJECTED': { text: '已拒绝', color: 'red' },
      'CONFIRMED': { text: '待发货', color: 'blue' },
      'SHIPPED': { text: '已发货', color: 'cyan' },
      'RECEIVED': { text: '已入库', color: 'green' },
      'CANCELLED': { text: '已取消', color: 'default' }
    }

    const orderTrendConfig = {
      data: purchaserStats.dates?.map((date, index) => ({
        date,
        订单数: purchaserStats.orderCounts?.[index] || 0,
      })) || [],
      xField: 'date',
      yField: '订单数',
      smooth: true,
      point: { size: 4, shape: 'circle' },
      color: '#1890ff',
    }

    const statusStatsData = Object.entries(purchaserStats.statusStats || {}).map(([status, count]) => ({
      status: statusMap[status]?.text || status,
      count,
    })).filter(item => item.count > 0)

    const statusStatsConfig = {
      data: statusStatsData,
      xField: 'status',
      yField: 'count',
      columnWidthRatio: 0.6,
      color: '#52c41a',
    }

    return (
      <div>
        <h2 style={{ marginBottom: '24px', margin: 0 }}>采购专员仪表盘</h2>

        {/* 统计卡片 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="订单总数"
                value={purchaserStats.totalOrders || 0}
                prefix={<ShoppingCartOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="订单总金额"
                value={purchaserStats.totalAmount || 0}
                prefix={<DollarOutlined />}
                precision={2}
                valueStyle={{ color: '#52c41a' }}
                suffix="元"
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="合作供应商"
                value={purchaserStats.uniqueSuppliers || 0}
                prefix={<ShopOutlined />}
                valueStyle={{ color: '#13c2c2' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="待确认订单"
                value={purchaserStats.statusStats?.PENDING || 0}
                prefix={<FileDoneOutlined />}
                valueStyle={{ color: '#faad14' }}
              />
            </Card>
          </Col>
        </Row>

        {/* 图表区域 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
          <Col xs={24} lg={12}>
            <Card title="最近7天订单趋势" style={{ height: '350px' }}>
              {purchaserStats.dates && purchaserStats.dates.length > 0 ? (
                <Line {...orderTrendConfig} height={280} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: '50px' }} />
              )}
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="订单状态分布" style={{ height: '350px' }}>
              {statusStatsData.length > 0 ? (
                <Column {...statusStatsConfig} height={280} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: '50px' }} />
              )}
            </Card>
          </Col>
        </Row>

        {/* 订单状态详情 */}
        <Card title="订单状态统计">
          <Row gutter={[16, 16]}>
            {Object.entries(purchaserStats.statusStats || {}).map(([status, count]) => (
              <Col xs={12} sm={8} md={4} key={status}>
                <Card size="small">
                  <Statistic
                    title={statusMap[status]?.text || status}
                    value={count}
                    valueStyle={{ color: statusMap[status]?.color || '#1890ff' }}
                  />
                </Card>
              </Col>
            ))}
          </Row>
        </Card>
      </div>
    )
  }

  // 医护人员专用仪表盘
  if (roleId === 4) {
    const statusMap = {
      'PENDING': { text: '待审批', color: 'orange' },
      'APPROVED': { text: '已通过', color: 'green' },
      'REJECTED': { text: '已驳回', color: 'red' },
      'OUTBOUND': { text: '已出库', color: 'blue' },
      'CANCELLED': { text: '已取消', color: 'default' }
    }

    const applyTrendConfig = {
      data: medicalStaffStats.dates?.map((date, index) => ({
        date,
        申请数: medicalStaffStats.applyCounts?.[index] || 0,
      })) || [],
      xField: 'date',
      yField: '申请数',
      smooth: true,
      point: { size: 4, shape: 'circle' },
      color: '#1890ff',
    }

    const statusStatsData = Object.entries(medicalStaffStats.statusStats || {}).map(([status, count]) => ({
      status: statusMap[status]?.text || status,
      count,
    })).filter(item => item.count > 0)

    const statusStatsConfig = {
      data: statusStatsData,
      xField: 'status',
      yField: 'count',
      columnWidthRatio: 0.6,
      color: '#52c41a',
    }

    return (
      <div>
        <h2 style={{ marginBottom: '24px', margin: 0 }}>医护人员仪表盘</h2>

        {/* 统计卡片 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
          <Col xs={24} sm={12} md={8}>
            <Card>
              <Statistic
                title="申请总数"
                value={medicalStaffStats.totalApplies || 0}
                prefix={<FileDoneOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8}>
            <Card>
              <Statistic
                title="待审批"
                value={medicalStaffStats.statusStats?.PENDING || 0}
                prefix={<WarningOutlined />}
                valueStyle={{ color: '#faad14' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8}>
            <Card>
              <Statistic
                title="已出库"
                value={medicalStaffStats.statusStats?.OUTBOUND || 0}
                prefix={<CheckCircleOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
        </Row>

        {/* 图表区域 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
          <Col xs={24} lg={12}>
            <Card title="最近7天申请趋势" style={{ height: '350px' }}>
              {medicalStaffStats.dates && medicalStaffStats.dates.length > 0 ? (
                <Line {...applyTrendConfig} height={280} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: '50px' }} />
              )}
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="申请状态分布" style={{ height: '350px' }}>
              {statusStatsData.length > 0 ? (
                <Column {...statusStatsConfig} height={280} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: '50px' }} />
              )}
            </Card>
          </Col>
        </Row>

        {/* 申请状态详情 */}
        <Card title="申请状态统计">
          <Row gutter={[16, 16]}>
            {Object.entries(medicalStaffStats.statusStats || {}).map(([status, count]) => (
              <Col xs={12} sm={8} md={4} key={status}>
                <Card size="small">
                  <Statistic
                    title={statusMap[status]?.text || status}
                    value={count}
                    valueStyle={{ color: statusMap[status]?.color || '#1890ff' }}
                  />
                </Card>
              </Col>
            ))}
          </Row>
        </Card>
      </div>
    )
  }

  // 供应商专用仪表盘
  if (roleId === 5) {
    const statusMap = {
      'PENDING': { text: '待确认', color: 'orange' },
      'REJECTED': { text: '已拒绝', color: 'red' },
      'CONFIRMED': { text: '待发货', color: 'blue' },
      'SHIPPED': { text: '已发货', color: 'cyan' },
      'RECEIVED': { text: '已入库', color: 'green' },
      'CANCELLED': { text: '已取消', color: 'default' }
    }

    const orderTrendConfig = {
      data: supplierStats.dates?.map((date, index) => ({
        date,
        订单数: supplierStats.orderCounts?.[index] || 0,
      })) || [],
      xField: 'date',
      yField: '订单数',
      smooth: true,
      point: { size: 4, shape: 'circle' },
      color: '#1890ff',
    }

    const statusStatsData = Object.entries(supplierStats.statusStats || {}).map(([status, count]) => ({
      status: statusMap[status]?.text || status,
      count,
    })).filter(item => item.count > 0)

    const statusStatsConfig = {
      data: statusStatsData,
      xField: 'status',
      yField: 'count',
      columnWidthRatio: 0.6,
      color: '#52c41a',
    }

    return (
      <div>
        <h2 style={{ marginBottom: '24px', margin: 0 }}>供应商仪表盘</h2>

        {/* 统计卡片 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="订单总数"
                value={supplierStats.totalOrders || 0}
                prefix={<ShoppingCartOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="订单总金额"
                value={supplierStats.totalAmount || 0}
                prefix={<DollarOutlined />}
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
                value={supplierStats.pendingAmount || 0}
                prefix={<WarningOutlined />}
                precision={2}
                valueStyle={{ color: '#faad14' }}
                suffix="元"
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="已确认金额"
                value={supplierStats.confirmedAmount || 0}
                prefix={<CheckCircleOutlined />}
                precision={2}
                valueStyle={{ color: '#13c2c2' }}
                suffix="元"
              />
            </Card>
          </Col>
        </Row>

        {/* 图表区域 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
          <Col xs={24} lg={12}>
            <Card title="最近7天订单趋势" style={{ height: '350px' }}>
              {supplierStats.dates && supplierStats.dates.length > 0 ? (
                <Line {...orderTrendConfig} height={280} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: '50px' }} />
              )}
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="订单状态分布" style={{ height: '350px' }}>
              {statusStatsData.length > 0 ? (
                <Column {...statusStatsConfig} height={280} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: '50px' }} />
              )}
            </Card>
          </Col>
        </Row>

        {/* 订单状态详情 */}
        <Card title="订单状态统计">
          <Row gutter={[16, 16]}>
            {Object.entries(supplierStats.statusStats || {}).map(([status, count]) => (
              <Col xs={12} sm={8} md={4} key={status}>
                <Card size="small">
                  <Statistic
                    title={statusMap[status]?.text || status}
                    value={count}
                    valueStyle={{ color: statusMap[status]?.color || '#1890ff' }}
                  />
                </Card>
              </Col>
            ))}
          </Row>
        </Card>
      </div>
    )
  }

  // 系统管理员仪表盘
  return (
    <div>
      <h2 style={{ marginBottom: '24px', margin: 0 }}>系统仪表盘</h2>

      {/* 统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="用户总数"
              value={statistics.totalUsers || 0}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
            <div style={{ marginTop: '8px', fontSize: '12px', color: '#999' }}>
              活跃: {statistics.activeUsers || 0} | 禁用: {statistics.disabledUsers || 0}
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="角色总数"
              value={statistics.totalRoles || 0}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="系统配置"
              value={statistics.totalConfigs || 0}
              prefix={<SettingOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="通知公告"
              value={statistics.totalNotices || 0}
              prefix={<BellOutlined />}
              valueStyle={{ color: '#fa8c16' }}
            />
            <div style={{ marginTop: '8px', fontSize: '12px', color: '#999' }}>
              启用: {statistics.activeNotices || 0}
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="药品总数"
              value={statistics.totalDrugs || 0}
              prefix={<MedicineBoxOutlined />}
              valueStyle={{ color: '#13c2c2' }}
            />
            <div style={{ marginTop: '8px', fontSize: '12px', color: '#999' }}>
              普通: {statistics.normalDrugs || 0} | 特殊: {statistics.specialDrugs || 0}
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="今日登录"
              value={statistics.todayLogins || 0}
              prefix={<LoginOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="今日操作"
              value={statistics.todayOperations || 0}
              prefix={<FileTextOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="系统状态"
              value="正常"
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 图表区域 */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        {/* 登录趋势（仅系统管理员可见） */}
        {hasPermission(PERMISSIONS.LOG_LOGIN_VIEW) && (
          <Col xs={24} lg={12}>
            <Card title="最近7天登录趋势" style={{ height: '350px' }}>
              {loginTrend.dates && loginTrend.dates.length > 0 ? (
                <Line {...loginTrendConfig} height={280} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: '50px' }} />
              )}
            </Card>
          </Col>
        )}
        {/* 操作趋势（仅系统管理员可见） */}
        {hasPermission(PERMISSIONS.LOG_OPERATION_VIEW) && (
          <Col xs={24} lg={hasPermission(PERMISSIONS.LOG_LOGIN_VIEW) ? 12 : 24}>
            <Card title="最近7天操作趋势" style={{ height: '350px' }}>
              {operationStats.dates && operationStats.dates.length > 0 ? (
                <Line {...operationTrendConfig} height={280} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: '50px' }} />
              )}
            </Card>
          </Col>
        )}
      </Row>

      {/* 操作统计图表（仅系统管理员可见） */}
      {hasPermission(PERMISSIONS.LOG_OPERATION_VIEW) && (
        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
          <Col xs={24} lg={12}>
            <Card title="操作模块统计（Top 5）" style={{ height: '350px' }}>
              {operationStats.moduleStats && Object.keys(operationStats.moduleStats).length > 0 ? (
                <Column {...moduleStatsConfig} height={280} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: '50px' }} />
              )}
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="操作类型统计" style={{ height: '350px' }}>
              {operationStats.typeStats && Object.keys(operationStats.typeStats).length > 0 ? (
                <Column {...typeStatsConfig} height={280} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: '50px' }} />
              )}
            </Card>
          </Col>
        </Row>
      )}

      {/* 最近操作日志（仅系统管理员可见） */}
      {hasPermission(PERMISSIONS.LOG_OPERATION_VIEW) && (
        <Card title="最近操作日志">
          <Table
            columns={logColumns}
            dataSource={recentLogs}
            rowKey="id"
            pagination={false}
            size="middle"
            scroll={{ x: 'max-content', y: 400 }}
          />
        </Card>
      )}
    </div>
  )
}

export default Dashboard

import { useState, useEffect } from 'react'
import { Table, Button, Space, Input, Select, DatePicker, Tag, message } from 'antd'
import { SearchOutlined, ReloadOutlined, WarningOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request from '../utils/request'
import { hasPermission, PERMISSIONS } from '../utils/permission'

const { RangePicker } = DatePicker

const InventoryManagement = () => {
  const [inventory, setInventory] = useState([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [filters, setFilters] = useState({
    keyword: '',
    drugId: undefined,
    batchNumber: '',
    storageLocation: '',
    expiryDateStart: undefined,
    expiryDateEnd: undefined,
    isSpecial: undefined,
  })

  useEffect(() => {
    fetchInventory()
  }, [pagination.current, pagination.pageSize, filters])

  const fetchInventory = async () => {
    setLoading(true)
    try {
      const params = {
        page: pagination.current,
        size: pagination.pageSize,
        keyword: filters.keyword || undefined,
        drugId: filters.drugId,
        batchNumber: filters.batchNumber || undefined,
        storageLocation: filters.storageLocation || undefined,
        expiryDateStart: filters.expiryDateStart ? filters.expiryDateStart.format('YYYY-MM-DD') : undefined,
        expiryDateEnd: filters.expiryDateEnd ? filters.expiryDateEnd.format('YYYY-MM-DD') : undefined,
        isSpecial: filters.isSpecial,
      }
      const res = await request.get('/inventory', { params })
      if (res.code === 200) {
        setInventory(res.data.records || [])
        setPagination({
          ...pagination,
          total: res.data.total || 0,
        })
      } else {
        message.error(res.msg || '获取库存列表失败')
      }
    } catch (error) {
      console.error('获取库存列表失败:', error)
      message.error('获取库存列表失败')
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
      drugId: undefined,
      batchNumber: '',
      storageLocation: '',
      expiryDateStart: undefined,
      expiryDateEnd: undefined,
      isSpecial: undefined,
    })
    setPagination({ ...pagination, current: 1 })
  }

  const getExpiryWarning = (expiryDate) => {
    if (!expiryDate) return null
    const days = dayjs(expiryDate).diff(dayjs(), 'day')
    if (days <= 90) {
      return <Tag color="red" icon={<WarningOutlined />}>红色预警（≤90天）</Tag>
    } else if (days <= 180) {
      return <Tag color="orange" icon={<WarningOutlined />}>黄色预警（90-180天）</Tag>
    }
    return null
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
      title: <span style={{ whiteSpace: 'nowrap' }}>库存数量</span>,
      dataIndex: 'quantity',
      key: 'quantity',
      width: 100,
      align: 'right',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>有效期至</span>,
      dataIndex: 'expiryDate',
      key: 'expiryDate',
      width: 120,
      render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>预警状态</span>,
      key: 'warning',
      width: 150,
      render: (_, record) => getExpiryWarning(record.expiryDate),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>存储位置</span>,
      dataIndex: 'storageLocation',
      key: 'storageLocation',
      width: 120,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>生产日期</span>,
      dataIndex: 'productionDate',
      key: 'productionDate',
      width: 120,
      render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>生产厂家</span>,
      dataIndex: 'manufacturer',
      key: 'manufacturer',
      width: 150,
      ellipsis: true,
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>库存管理</h2>
        <Space wrap>
          <Input
            placeholder="搜索药品名称、批次号"
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
          <Input
            placeholder="存储位置"
            value={filters.storageLocation}
            onChange={(e) => setFilters({ ...filters, storageLocation: e.target.value })}
            style={{ width: 150 }}
            allowClear
          />
          <Select
            placeholder="特殊药品"
            value={filters.isSpecial}
            onChange={(value) => setFilters({ ...filters, isSpecial: value })}
            style={{ width: 120 }}
            allowClear
          >
            <Select.Option value={1}>是</Select.Option>
            <Select.Option value={0}>否</Select.Option>
          </Select>
          <RangePicker
            placeholder={['有效期开始', '有效期结束']}
            value={filters.expiryDateStart && filters.expiryDateEnd 
              ? [filters.expiryDateStart, filters.expiryDateEnd] 
              : null}
            onChange={(dates) => {
              setFilters({
                ...filters,
                expiryDateStart: dates ? dates[0] : undefined,
                expiryDateEnd: dates ? dates[1] : undefined,
              })
            }}
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={fetchInventory}
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
        dataSource={inventory}
        rowKey="id"
        loading={loading}
        size="middle"
        scroll={{ x: 'max-content', y: 'calc(100vh - 250px)' }}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
        onChange={handleTableChange}
      />
    </div>
  )
}

export default InventoryManagement


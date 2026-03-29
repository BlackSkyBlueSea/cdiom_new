import { useState, useEffect } from 'react'
import { Table, Input, Select, Space, Button, message, Tooltip } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import request from '../utils/request'
import logger from '../utils/logger'
import { hasPermission, PERMISSIONS } from '../utils/permission'
import { Navigate } from 'react-router-dom'
import {
  pageRootStyle,
  tableAreaStyle,
  toolbarSectionStackedStyle,
  toolbarPageTitleStyle,
  compactFilterRowFullWidthStyle,
  filterCellFlex,
  TABLE_SCROLL_Y_STACKED,
} from '../utils/tablePageLayout'

const OperationLog = () => {
  // 权限检查：只有系统管理员可以查看操作日志
  if (!hasPermission(PERMISSIONS.LOG_OPERATION_VIEW)) {
    message.error('您没有权限访问此页面')
    return <Navigate to="/dashboard" replace />
  }

  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [filters, setFilters] = useState({
    keyword: '',
    module: '',
    operationType: '',
    status: undefined,
  })

  useEffect(() => {
    fetchLogs()
  }, [pagination.current, pagination.pageSize, filters])

  const fetchLogs = async () => {
    setLoading(true)
    try {
      const res = await request.get('/operation-logs', {
        params: {
          page: pagination.current,
          size: pagination.pageSize,
          ...filters,
        },
      })
      if (res.code === 200) {
        setLogs(res.data.records)
        setPagination({
          ...pagination,
          total: res.data.total,
        })
      }
    } catch (error) {
      logger.error('获取操作日志失败', error)
    } finally {
      setLoading(false)
    }
  }

  const handleReset = () => {
    setFilters({
      keyword: '',
      module: '',
      operationType: '',
      status: undefined,
    })
  }

  const columns = [
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>ID</span>,
      dataIndex: 'id',
      key: 'id',
      width: 80,
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
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作内容</span>,
      dataIndex: 'operationContent',
      key: 'operationContent',
      width: 200,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>IP地址</span>,
      dataIndex: 'ip',
      key: 'ip',
      width: 120,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>状态</span>,
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) => (status === 1 ? '成功' : '失败'),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作时间</span>,
      dataIndex: 'operationTime',
      key: 'operationTime',
      width: 180,
      sorter: (a, b) => {
        const timeA = a.operationTime ? new Date(a.operationTime).getTime() : 0
        const timeB = b.operationTime ? new Date(b.operationTime).getTime() : 0
        return timeB - timeA // 降序：最新的在上
      },
      defaultSortOrder: 'descend',
    },
  ]

  return (
    <div style={pageRootStyle}>
      <div style={toolbarSectionStackedStyle}>
        <h2 style={toolbarPageTitleStyle}>操作日志</h2>
        <div style={compactFilterRowFullWidthStyle}>
          <div style={filterCellFlex('1.2 1 88px', 88, 240)}>
            <Input
              placeholder="搜索关键词"
              value={filters.keyword}
              onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
              style={{ width: '100%' }}
              allowClear
            />
          </div>
          <div style={filterCellFlex('1 1 72px', 72, 180)}>
            <Input
              placeholder="操作模块"
              value={filters.module}
              onChange={(e) => setFilters({ ...filters, module: e.target.value })}
              style={{ width: '100%' }}
              allowClear
            />
          </div>
          <div style={{ flex: '0 0 auto', width: 132, minWidth: 120 }}>
            <Select
              placeholder="操作类型"
              value={filters.operationType}
              onChange={(value) => setFilters({ ...filters, operationType: value })}
              style={{ width: '100%' }}
              allowClear
            >
              <Select.Option value="INSERT">新增</Select.Option>
              <Select.Option value="UPDATE">更新</Select.Option>
              <Select.Option value="DELETE">删除</Select.Option>
              <Select.Option value="SELECT">查询</Select.Option>
            </Select>
          </div>
          <div style={{ flex: '0 0 auto', width: 108, minWidth: 100 }}>
            <Select
              placeholder="状态"
              value={filters.status}
              onChange={(value) => setFilters({ ...filters, status: value })}
              style={{ width: '100%' }}
              allowClear
            >
              <Select.Option value={1}>成功</Select.Option>
              <Select.Option value={0}>失败</Select.Option>
            </Select>
          </div>
          <Space size={4} style={{ flexShrink: 0 }}>
            <Tooltip title="重置"><Button icon={<ReloadOutlined />} onClick={handleReset} /></Tooltip>
          </Space>
        </div>
      </div>
      <div style={tableAreaStyle}>
        <Table
          columns={columns}
          dataSource={logs}
          loading={loading}
          rowKey="id"
          size="middle"
          scroll={{ x: 'max-content', y: TABLE_SCROLL_Y_STACKED }}
          pagination={{
            ...pagination,
            onChange: (page, pageSize) => {
              setPagination({ ...pagination, current: page, pageSize })
            },
          }}
        />
      </div>
    </div>
  )
}

export default OperationLog





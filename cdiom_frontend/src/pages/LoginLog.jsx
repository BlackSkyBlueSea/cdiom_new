import { useState, useEffect } from 'react'
import { Table, Input, Select, Space, Button, Tag, Tooltip } from 'antd'
import { EnvironmentOutlined, ReloadOutlined } from '@ant-design/icons'
import request from '../utils/request'
import logger from '../utils/logger'
import dayjs from 'dayjs'
import {
  pageRootStyle,
  tableAreaStyle,
  toolbarRowCompactStyle,
  toolbarPageTitleStyle,
  compactFilterRowStyle,
  filterCellFlex,
  TABLE_SCROLL_Y,
} from '../utils/tablePageLayout'

const LoginLog = () => {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [filters, setFilters] = useState({
    keyword: '',
    status: undefined,
  })

  useEffect(() => {
    fetchLogs()
  }, [pagination.current, pagination.pageSize, filters])

  const fetchLogs = async () => {
    setLoading(true)
    try {
      const res = await request.get('/login-logs', {
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
      logger.error('获取登录日志失败', error)
    } finally {
      setLoading(false)
    }
  }

  const handleReset = () => {
    setFilters({
      keyword: '',
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
      title: <span style={{ whiteSpace: 'nowrap' }}>用户名</span>,
      dataIndex: 'username',
      key: 'username',
      width: 120,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>IP地址</span>,
      dataIndex: 'ip',
      key: 'ip',
      width: 120,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>登录地点</span>,
      dataIndex: 'location',
      key: 'location',
      width: 150,
      ellipsis: true,
      render: (location) => (
        location ? (
          <Space>
            <EnvironmentOutlined style={{ color: '#1890ff' }} />
            <span>{location}</span>
          </Space>
        ) : (
          <span style={{ color: '#999' }}>未知</span>
        )
      ),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>浏览器</span>,
      dataIndex: 'browser',
      key: 'browser',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作系统</span>,
      dataIndex: 'os',
      key: 'os',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>状态</span>,
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) => (
        <Tag color={status === 1 ? 'success' : 'error'}>
          {status === 1 ? '成功' : '失败'}
        </Tag>
      ),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>消息</span>,
      dataIndex: 'msg',
      key: 'msg',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>登录时间</span>,
      dataIndex: 'loginTime',
      key: 'loginTime',
      width: 180,
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
  ]

  return (
    <div style={pageRootStyle}>
      <div style={toolbarRowCompactStyle}>
        <h2 style={{ ...toolbarPageTitleStyle, whiteSpace: 'nowrap' }}>登录日志</h2>
        <div style={compactFilterRowStyle}>
          <div style={filterCellFlex('1.2 1 100px', 100, 280)}>
            <Input
              placeholder="搜索关键词"
              value={filters.keyword}
              onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
              style={{ width: '100%' }}
              allowClear
            />
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
          scroll={{ x: 'max-content', y: TABLE_SCROLL_Y }}
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

export default LoginLog







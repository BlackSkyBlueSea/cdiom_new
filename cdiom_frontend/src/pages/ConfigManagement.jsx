import { useState, useEffect, useCallback } from 'react'
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tooltip, Spin } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import request from '../utils/request'
import {
  pageRootStyle,
  tableAreaStyle,
  toolbarRowCompactStyle,
  toolbarPageTitleStyle,
  TABLE_SCROLL_Y,
} from '../utils/tablePageLayout'

/** 按 configKey 映射 runtime-effective 接口字段，与后端统一 */
function formatRuntimeEffectiveForKey(configKey, r, loading) {
  if (loading && !r) return <Spin size="small" />
  if (!r) return '—'
  switch (configKey) {
    case 'expiry_warning_days':
      return r.expiryWarningDays != null ? `${r.expiryWarningDays} 天` : '—'
    case 'expiry_critical_days':
      return r.expiryCriticalDays != null ? `${r.expiryCriticalDays} 天` : '—'
    case 'log_retention_years':
      return r.logRetentionYears != null ? `${r.logRetentionYears} 年` : '—'
    case 'jwt_expiration': {
      const ms = r.jwtExpirationMs != null ? Number(r.jwtExpirationMs) : NaN
      if (!Number.isFinite(ms)) return '—'
      return `${ms} ms（约 ${(ms / 3600000).toFixed(2)} 小时）`
    }
    case 'login.fail.threshold':
      return r.loginFailThreshold != null ? `${r.loginFailThreshold} 次` : '—'
    case 'login.fail.time.window':
      return r.loginFailTimeWindowMinutes != null ? `${r.loginFailTimeWindowMinutes} 分钟` : '—'
    case 'login.lock.duration':
      return r.loginLockDurationHours != null ? `${r.loginLockDurationHours} 小时` : '—'
    default:
      return '—'
  }
}

const ConfigManagement = () => {
  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingConfig, setEditingConfig] = useState(null)
  const [form] = Form.useForm()
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  /** 与后端各模块运行时一致（表保存后刷新；JWT 等对非法值有回退） */
  const [runtimeEffective, setRuntimeEffective] = useState(null)
  const [runtimeLoading, setRuntimeLoading] = useState(false)

  const fetchRuntimeEffective = useCallback(async () => {
    setRuntimeLoading(true)
    try {
      const res = await request.get('/configs/runtime-effective')
      if (res.code === 200) {
        setRuntimeEffective(res.data || null)
      }
    } catch {
      setRuntimeEffective(null)
    } finally {
      setRuntimeLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchConfigs()
  }, [pagination.current, pagination.pageSize])

  useEffect(() => {
    fetchRuntimeEffective()
  }, [fetchRuntimeEffective])

  const fetchConfigs = async () => {
    setLoading(true)
    try {
      const res = await request.get('/configs', {
        params: {
          page: pagination.current,
          size: pagination.pageSize,
        },
      })
      if (res.code === 200) {
        setConfigs(res.data.records)
        setPagination({
          ...pagination,
          total: res.data.total,
        })
      }
    } catch (error) {
      message.error('获取配置列表失败')
    } finally {
      setLoading(false)
    }
  }

  const handleAdd = () => {
    setEditingConfig(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record) => {
    setEditingConfig(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id) => {
    try {
      await request.delete(`/configs/${id}`)
      message.success('删除成功')
      fetchConfigs()
      fetchRuntimeEffective()
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingConfig) {
        await request.put(`/configs/${editingConfig.id}`, values)
        message.success('更新成功')
      } else {
        await request.post('/configs', values)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchConfigs()
      fetchRuntimeEffective()
    } catch (error) {
      if (error.errorFields) {
        return
      }
      message.error(error.message || '操作失败')
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
      title: <span style={{ whiteSpace: 'nowrap' }}>参数名称</span>,
      dataIndex: 'configName',
      key: 'configName',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>参数键名</span>,
      dataIndex: 'configKey',
      key: 'configKey',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>参数值</span>,
      dataIndex: 'configValue',
      key: 'configValue',
      width: 200,
      ellipsis: true,
    },
    {
      title: (
        <Tooltip title="后端各模块实际使用的值；与「参数值」不一致时多为 JWT 非法回退、日志保留年上下限等规则导致">
          <span style={{ whiteSpace: 'nowrap' }}>当前业务生效</span>
        </Tooltip>
      ),
      key: 'runtimeEffective',
      width: 220,
      ellipsis: true,
      render: (_, record) => (
        <span style={{ fontSize: 13 }}>
          {formatRuntimeEffectiveForKey(record.configKey, runtimeEffective, runtimeLoading)}
        </span>
      ),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>参数类型</span>,
      dataIndex: 'configType',
      key: 'configType',
      width: 100,
      render: (type) => (type === 1 ? '系统参数' : '业务参数'),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>备注</span>,
      dataIndex: 'remark',
      key: 'remark',
      width: 200,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作</span>,
      key: 'action',
      width: 120,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Tooltip title="编辑">
            <Button
              type="link"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确定要删除吗？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Tooltip title="删除">
              <Button type="link" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={pageRootStyle}>
      <div style={{ ...toolbarRowCompactStyle, alignItems: 'flex-start' }}>
        <div style={{ minWidth: 0, flex: '1 1 auto' }}>
          <h2 style={toolbarPageTitleStyle}>参数配置</h2>
          <div style={{ marginTop: 6, fontSize: 13, color: '#666' }}>
            「当前业务生效」列与后端运行时一致；未映射的键名显示「—」。JWT 表值超范围时将回退为配置文件默认值。
          </div>
        </div>
        <Space style={{ flexShrink: 0 }}>
          <Tooltip title="重新拉取各键运行时生效值">
            <Button onClick={fetchRuntimeEffective} loading={runtimeLoading}>
              刷新生效参数
            </Button>
          </Tooltip>
          <Tooltip title="新增配置">
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} />
          </Tooltip>
        </Space>
      </div>

      <div style={tableAreaStyle}>
        <Table
          columns={columns}
          dataSource={configs}
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
      <Modal
        title={editingConfig ? '编辑配置' : '新增配置'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="configName"
            label="参数名称"
            rules={[{ required: true, message: '请输入参数名称' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="configKey"
            label="参数键名"
            rules={[{ required: true, message: '请输入参数键名' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="configValue"
            label="参数值"
            rules={[{ required: true, message: '请输入参数值' }]}
          >
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item
            name="configType"
            label="参数类型"
            initialValue={1}
          >
            <Select>
              <Select.Option value={1}>系统参数</Select.Option>
              <Select.Option value={2}>业务参数</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="remark"
            label="备注"
          >
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ConfigManagement



import { useState, useEffect } from 'react'
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tooltip } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import request from '../utils/request'

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

  useEffect(() => {
    fetchConfigs()
  }, [pagination.current, pagination.pageSize])

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
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16 }}>
        <h2 style={{ margin: 0 }}>参数配置</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          新增配置
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={configs}
        loading={loading}
        rowKey="id"
        size="middle"
        scroll={{ x: 'max-content' }}
        pagination={{
          ...pagination,
          onChange: (page, pageSize) => {
            setPagination({ ...pagination, current: page, pageSize })
          },
        }}
      />
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



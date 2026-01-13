import { useState, useEffect } from 'react'
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tooltip } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined, CloseCircleOutlined, SettingOutlined } from '@ant-design/icons'
import request from '../utils/request'
import SuperAdminModal from '../components/SuperAdminModal'

const RoleManagement = () => {
  const [roles, setRoles] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRole, setEditingRole] = useState(null)
  const [form] = Form.useForm()
  const [superAdminModalVisible, setSuperAdminModalVisible] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  useEffect(() => {
    fetchRoles()
  }, [pagination.current, pagination.pageSize])

  const fetchRoles = async () => {
    setLoading(true)
    try {
      const res = await request.get('/roles', {
        params: {
          page: pagination.current,
          size: pagination.pageSize,
        },
      })
      if (res.code === 200) {
        setRoles(res.data.records)
        setPagination({
          ...pagination,
          total: res.data.total,
        })
      }
    } catch (error) {
      message.error('获取角色列表失败')
    } finally {
      setLoading(false)
    }
  }

  const handleAdd = () => {
    setEditingRole(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record) => {
    setEditingRole(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id) => {
    try {
      await request.delete(`/roles/${id}`)
      message.success('删除成功')
      fetchRoles()
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handleStatusChange = async (id, status) => {
    // 超级管理员（roleId=6）需要通过邮箱验证码启用/停用
    if (id === 6) {
      setSuperAdminModalVisible(true)
      return
    }
    
    try {
      await request.put(`/roles/${id}/status`, { status })
      message.success('状态更新成功')
      fetchRoles()
    } catch (error) {
      message.error('状态更新失败')
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingRole) {
        await request.put(`/roles/${editingRole.id}`, values)
        message.success('更新成功')
      } else {
        await request.post('/roles', values)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchRoles()
    } catch (error) {
      if (error.errorFields) {
        return
      }
      message.error(error.message || '操作失败')
    }
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      sorter: (a, b) => a.id - b.id,
      defaultSortOrder: 'ascend',
    },
    {
      title: '角色名称',
      dataIndex: 'roleName',
      key: 'roleName',
    },
    {
      title: '角色代码',
      dataIndex: 'roleCode',
      key: 'roleCode',
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (status === 1 ? '正常' : '禁用'),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Tooltip title="编辑">
            <Button
              type="link"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
              disabled={record.id === 6} // 超级管理员角色不允许编辑
            />
          </Tooltip>
          {record.id === 6 ? (
            <Tooltip title="管理超级管理员">
              <Button
                type="link"
                icon={<SettingOutlined />}
                onClick={() => setSuperAdminModalVisible(true)}
              >
                管理
              </Button>
            </Tooltip>
          ) : (
            <Tooltip title={record.status === 1 ? '禁用' : '启用'}>
              <Button
                type="link"
                danger={record.status === 1}
                icon={record.status === 1 ? <CloseCircleOutlined /> : <CheckCircleOutlined />}
                onClick={() => handleStatusChange(record.id, record.status === 1 ? 0 : 1)}
              />
            </Tooltip>
          )}
          <Popconfirm
            title="确定要删除吗？"
            onConfirm={() => handleDelete(record.id)}
            disabled={record.id === 6} // 超级管理员角色不允许删除
          >
            <Tooltip title="删除">
              <Button 
                type="link" 
                danger 
                icon={<DeleteOutlined />}
                disabled={record.id === 6} // 超级管理员角色不允许删除
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h2>角色管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          新增角色
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={roles}
        loading={loading}
        rowKey="id"
        pagination={{
          ...pagination,
          onChange: (page, pageSize) => {
            setPagination({ ...pagination, current: page, pageSize })
          },
        }}
      />
      <Modal
        title={editingRole ? '编辑角色' : '新增角色'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="roleName"
            label="角色名称"
            rules={[{ required: true, message: '请输入角色名称' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="roleCode"
            label="角色代码"
            rules={[{ required: true, message: '请输入角色代码' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea />
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
            initialValue={1}
          >
            <Select>
              <Select.Option value={1}>正常</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
      <SuperAdminModal
        open={superAdminModalVisible}
        onCancel={() => setSuperAdminModalVisible(false)}
        onSuccess={() => {
          setSuperAdminModalVisible(false)
          fetchRoles()
        }}
      />
    </div>
  )
}

export default RoleManagement



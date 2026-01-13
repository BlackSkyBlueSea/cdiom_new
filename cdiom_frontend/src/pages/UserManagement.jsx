import { useState, useEffect } from 'react'
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tooltip } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, UnlockOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import request from '../utils/request'
import { hasPermission, PERMISSIONS, PermissionWrapper } from '../utils/permission'
import SuperAdminModal from '../components/SuperAdminModal'

const UserManagement = () => {
  const [users, setUsers] = useState([])
  const [roles, setRoles] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingUser, setEditingUser] = useState(null)
  const [form] = Form.useForm()
  const [superAdminModalVisible, setSuperAdminModalVisible] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  useEffect(() => {
    fetchUsers()
    fetchRoles()
  }, [pagination.current, pagination.pageSize])

  const fetchUsers = async () => {
    setLoading(true)
    try {
      const res = await request.get('/users', {
        params: {
          page: pagination.current,
          size: pagination.pageSize,
        },
      })
      if (res.code === 200) {
        setUsers(res.data.records)
        setPagination({
          ...pagination,
          total: res.data.total,
        })
      }
    } catch (error) {
      message.error('获取用户列表失败')
    } finally {
      setLoading(false)
    }
  }

  const fetchRoles = async () => {
    try {
      const res = await request.get('/roles', {
        params: { page: 1, size: 100 },
      })
      if (res.code === 200) {
        setRoles(res.data.records)
      }
    } catch (error) {
      console.error('获取角色列表失败', error)
    }
  }

  const handleAdd = () => {
    setEditingUser(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record) => {
    setEditingUser(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id) => {
    try {
      await request.delete(`/users/${id}`)
      message.success('删除成功')
      fetchUsers()
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handleUnlock = async (id) => {
    try {
      await request.put(`/users/${id}/unlock`)
      message.success('解锁成功')
      fetchUsers()
    } catch (error) {
      message.error('解锁失败')
    }
  }

  const handleStatusChange = async (id, status) => {
    // 检查是否是超级管理员用户（username=super_admin 或 roleId=6）
    const user = users.find(u => u.id === id)
    if (user && (user.username === 'super_admin' || user.roleId === 6)) {
      // 超级管理员需要通过验证码验证
      setSuperAdminModalVisible(true)
      return
    }
    
    try {
      await request.put(`/users/${id}/status`, { status })
      message.success('状态更新成功')
      fetchUsers()
    } catch (error) {
      const errorMsg = error.response?.data?.msg || error.message || '状态更新失败'
      message.error(errorMsg)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingUser) {
        await request.put(`/users/${editingUser.id}`, values)
        message.success('更新成功')
      } else {
        await request.post('/users', values)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchUsers()
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
      title: <span style={{ whiteSpace: 'nowrap' }}>用户名</span>,
      dataIndex: 'username',
      key: 'username',
      width: 120,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>手机号</span>,
      dataIndex: 'phone',
      key: 'phone',
      width: 120,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>邮箱</span>,
      dataIndex: 'email',
      key: 'email',
      width: 180,
      ellipsis: true,
      render: (email) => email || '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>角色</span>,
      dataIndex: 'roleId',
      key: 'roleId',
      width: 120,
      render: (roleId) => {
        const role = roles.find((r) => r.id === roleId)
        return role ? role.roleName : '-'
      },
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>状态</span>,
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) => (status === 1 ? '正常' : '禁用'),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作</span>,
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          {hasPermission(PERMISSIONS.USER_UPDATE) && (
            <Tooltip title="编辑">
              <Button
                type="link"
                icon={<EditOutlined />}
                onClick={() => handleEdit(record)}
              />
            </Tooltip>
          )}
          {hasPermission(PERMISSIONS.USER_UPDATE) && (
            <Tooltip title={record.status === 1 ? '禁用' : '启用'}>
              <Button
                type="link"
                danger={record.status === 1}
                icon={record.status === 1 ? <CloseCircleOutlined /> : <CheckCircleOutlined />}
                onClick={() => handleStatusChange(record.id, record.status === 1 ? 0 : 1)}
              />
            </Tooltip>
          )}
          {record.lockTime && hasPermission(PERMISSIONS.USER_UPDATE) && (
            <Tooltip title="解锁">
              <Button
                type="link"
                icon={<UnlockOutlined />}
                onClick={() => handleUnlock(record.id)}
              />
            </Tooltip>
          )}
          {hasPermission(PERMISSIONS.USER_DELETE) && (
            <Popconfirm
              title="确定要删除吗？"
              onConfirm={() => handleDelete(record.id)}
            >
              <Tooltip title="删除">
                <Button type="link" danger icon={<DeleteOutlined />} />
              </Tooltip>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16 }}>
        <h2 style={{ margin: 0 }}>用户管理</h2>
        <PermissionWrapper permission={PERMISSIONS.USER_CREATE}>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增用户
          </Button>
        </PermissionWrapper>
      </div>
      <Table
        columns={columns}
        dataSource={users}
        loading={loading}
        rowKey="id"
        size="middle"
        scroll={{ x: 'max-content', y: 'calc(100vh - 200px)' }}
        pagination={{
          ...pagination,
          onChange: (page, pageSize) => {
            setPagination({ ...pagination, current: page, pageSize })
          },
        }}
      />
      <Modal
        title={editingUser ? '编辑用户' : '新增用户'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="phone"
            label="手机号"
            rules={[{ required: true, message: '请输入手机号' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="email"
            label="邮箱"
            rules={[
              { type: 'email', message: '请输入有效的邮箱地址' },
            ]}
          >
            <Input placeholder="请输入邮箱地址（可选，但超级管理员必须填写）" />
          </Form.Item>
          <Form.Item
            name="password"
            label="密码"
            rules={[{ required: !editingUser, message: '请输入密码' }]}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item
            name="roleId"
            label="角色"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select>
              {roles.map((role) => (
                <Select.Option key={role.id} value={role.id}>
                  {role.roleName}
                </Select.Option>
              ))}
            </Select>
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
          fetchUsers()
        }}
      />
    </div>
  )
}

export default UserManagement



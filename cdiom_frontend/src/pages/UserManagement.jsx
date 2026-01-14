import { useState, useEffect, useMemo } from 'react'
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tooltip, InputNumber, Tag, Checkbox, Divider, Card, Tabs } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, UnlockOutlined, CheckCircleOutlined, CloseCircleOutlined, UndoOutlined, ExclamationCircleOutlined, SafetyOutlined, CloseOutlined } from '@ant-design/icons'
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
  const [recycleBinModalVisible, setRecycleBinModalVisible] = useState(false)
  const [deletedUsers, setDeletedUsers] = useState([])
  const [deletedUsersLoading, setDeletedUsersLoading] = useState(false)
  const [deletedUsersPagination, setDeletedUsersPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [permanentDeleteModalVisible, setPermanentDeleteModalVisible] = useState(false)
  const [permanentDeleteForm] = Form.useForm()
  const [permanentDeleteUserId, setPermanentDeleteUserId] = useState(null)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [permissionModalVisible, setPermissionModalVisible] = useState(false)
  const [currentUserPermissions, setCurrentUserPermissions] = useState(null)
  const [allPermissions, setAllPermissions] = useState([])
  const [rolePermissions, setRolePermissions] = useState([])
  const [userDirectPermissions, setUserDirectPermissions] = useState([])
  const [selectedPermissionIds, setSelectedPermissionIds] = useState([])
  const [activeTabKey, setActiveTabKey] = useState('')
  const [visibleTabs, setVisibleTabs] = useState([])

  useEffect(() => {
    fetchUsers()
    fetchRoles()
    fetchAllPermissions()
  }, [pagination.current, pagination.pageSize])

  useEffect(() => {
    if (recycleBinModalVisible) {
      fetchDeletedUsers()
    }
  }, [deletedUsersPagination.current, deletedUsersPagination.pageSize, recycleBinModalVisible])

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

  const fetchAllPermissions = async () => {
    try {
      const res = await request.get('/users/permissions/all')
      if (res.code === 200) {
        setAllPermissions(res.data || [])
      }
    } catch (error) {
      console.error('获取权限列表失败', error)
    }
  }

  // 权限分类映射
  const getPermissionCategory = (permissionCode) => {
    if (permissionCode.startsWith('user:')) return '用户管理'
    if (permissionCode.startsWith('role:')) return '角色管理'
    if (permissionCode.startsWith('drug:')) return '药品管理'
    if (permissionCode.startsWith('supplier:')) return '供应商管理'
    if (permissionCode.startsWith('config:')) return '系统配置'
    if (permissionCode.startsWith('notice:')) return '通知公告'
    if (permissionCode.startsWith('log:')) return '日志查看'
    if (permissionCode.startsWith('outbound:')) return '出库管理'
    if (permissionCode.startsWith('inbound:')) return '入库管理'
    if (permissionCode.startsWith('inventory:')) return '库存管理'
    return '其他'
  }

  // 按分类分组权限
  const groupedPermissions = useMemo(() => {
    const groups = {}
    allPermissions.forEach(permission => {
      const category = getPermissionCategory(permission.permissionCode)
      if (!groups[category]) {
        groups[category] = []
      }
      groups[category].push(permission)
    })
    return groups
  }, [allPermissions])

  const handleEditPermissions = async (record) => {
    try {
      setCurrentUserPermissions(record)
      const res = await request.get(`/users/${record.id}/permissions`)
      if (res.code === 200) {
        const data = res.data
        setRolePermissions(data.rolePermissions || [])
        setUserDirectPermissions(data.userPermissions || [])
        // 设置选中的权限ID（用户直接权限 + 角色权限，因为用户可以移除角色权限）
        const allUserPermissionIds = [
          ...(data.rolePermissions || []).map(p => p.id),
          ...(data.userPermissions || []).map(p => p.id)
        ]
        setSelectedPermissionIds(allUserPermissionIds)
        
        // 初始化可见标签（基于用户拥有的权限分类）
        const userPermissionCodes = new Set([
          ...(data.rolePermissions || []).map(p => p.permissionCode),
          ...(data.userPermissions || []).map(p => p.permissionCode)
        ])
        const categories = new Set()
        allPermissions.forEach(p => {
          if (userPermissionCodes.has(p.permissionCode)) {
            categories.add(getPermissionCategory(p.permissionCode))
          }
        })
        const visibleCategories = Array.from(categories).sort()
        // 如果用户没有任何权限分类，显示所有分类
        const tabsToShow = visibleCategories.length > 0 ? visibleCategories : Object.keys(groupedPermissions).sort()
        setVisibleTabs(tabsToShow)
        setActiveTabKey(tabsToShow.length > 0 ? tabsToShow[0] : '')
      }
      setPermissionModalVisible(true)
    } catch (error) {
      message.error('获取用户权限失败')
    }
  }

  const handleSavePermissions = async () => {
    try {
      await request.put(`/users/${currentUserPermissions.id}/permissions`, {
        permissionIds: selectedPermissionIds
      })
      message.success('权限更新成功')
      setPermissionModalVisible(false)
      setCurrentUserPermissions(null)
      setSelectedPermissionIds([])
      setVisibleTabs([])
      setActiveTabKey('')
      fetchUsers()
    } catch (error) {
      message.error(error.response?.data?.msg || error.message || '权限更新失败')
    }
  }

  // 关闭标签
  const handleCloseTab = (targetKey, e) => {
    e?.stopPropagation()
    const newVisibleTabs = visibleTabs.filter(key => key !== targetKey)
    setVisibleTabs(newVisibleTabs)
    
    // 如果关闭的是当前激活的标签，切换到其他标签
    if (targetKey === activeTabKey) {
      const currentIndex = visibleTabs.indexOf(targetKey)
      if (newVisibleTabs.length > 0) {
        // 优先切换到下一个，如果没有则切换到上一个
        const nextIndex = currentIndex < newVisibleTabs.length ? currentIndex : currentIndex - 1
        setActiveTabKey(newVisibleTabs[nextIndex] || newVisibleTabs[0])
      } else {
        setActiveTabKey('')
      }
    }
    
    // 移除该分类下的所有权限
    const categoryPermissions = groupedPermissions[targetKey] || []
    const categoryPermissionIds = categoryPermissions.map(p => p.id)
    setSelectedPermissionIds(prev => prev.filter(id => !categoryPermissionIds.includes(id)))
  }

  // 添加标签
  const handleAddTab = (category) => {
    if (!visibleTabs.includes(category)) {
      setVisibleTabs([...visibleTabs, category].sort())
      setActiveTabKey(category)
    }
  }

  // 获取权限显示标签
  const getPermissionTags = (record) => {
    // 在表格中显示一个可点击的标签，点击后打开权限管理模态框
    return (
      <Tooltip title="点击查看和编辑权限">
        <Tag 
          icon={<SafetyOutlined />} 
          color="blue" 
          style={{ cursor: 'pointer' }}
          onClick={() => handleEditPermissions(record)}
        >
          管理权限
        </Tag>
      </Tooltip>
    )
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

  const fetchDeletedUsers = async () => {
    setDeletedUsersLoading(true)
    try {
      const res = await request.get('/users/deleted', {
        params: {
          page: deletedUsersPagination.current,
          size: deletedUsersPagination.pageSize,
        },
      })
      if (res.code === 200) {
        setDeletedUsers(res.data.records)
        setDeletedUsersPagination({
          ...deletedUsersPagination,
          total: res.data.total,
        })
      }
    } catch (error) {
      message.error('获取已删除用户列表失败')
    } finally {
      setDeletedUsersLoading(false)
    }
  }

  const handleOpenRecycleBin = () => {
    setRecycleBinModalVisible(true)
    setDeletedUsersPagination({ ...deletedUsersPagination, current: 1 })
  }

  const handleRestoreUser = async (id) => {
    try {
      await request.put(`/users/${id}/restore`)
      message.success('恢复成功')
      fetchDeletedUsers()
      fetchUsers() // 刷新主列表
    } catch (error) {
      message.error(error.response?.data?.msg || error.message || '恢复失败')
    }
  }

  const handlePermanentDelete = async (id) => {
    setPermanentDeleteUserId(id)
    setPermanentDeleteModalVisible(true)
    permanentDeleteForm.resetFields()
  }

  const handleConfirmPermanentDelete = async () => {
    try {
      const values = await permanentDeleteForm.validateFields()
      if (values.confirmText !== 'DELETE') {
        message.error('确认文本不正确，请输入 DELETE 进行确认')
        return
      }
      
      await request.delete(`/users/${permanentDeleteUserId}/permanent`, {
        data: { confirmText: values.confirmText }
      })
      message.success('用户已永久删除')
      setPermanentDeleteModalVisible(false)
      permanentDeleteForm.resetFields()
      fetchDeletedUsers()
    } catch (error) {
      message.error(error.response?.data?.msg || error.message || '永久删除失败')
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
      title: <span style={{ whiteSpace: 'nowrap' }}>权限</span>,
      key: 'permissions',
      width: 150,
      render: (_, record) => getPermissionTags(record),
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
            <Tooltip title="编辑权限">
              <Button
                type="link"
                icon={<SafetyOutlined />}
                onClick={() => handleEditPermissions(record)}
              />
            </Tooltip>
          )}
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
        <Space>
          <Tooltip title="查看和恢复已删除的用户">
            <Button 
              icon={<UndoOutlined />} 
              onClick={handleOpenRecycleBin}
            >
              回收站
            </Button>
          </Tooltip>
          <PermissionWrapper permission={PERMISSIONS.USER_CREATE}>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增用户
            </Button>
          </PermissionWrapper>
        </Space>
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
            rules={[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号（11位数字，以1开头）' },
            ]}
          >
            <Input placeholder="请输入11位手机号" maxLength={11} />
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

      {/* 权限管理模态框 */}
      <Modal
        title={`权限管理 - ${currentUserPermissions?.username || ''}`}
        open={permissionModalVisible}
        onOk={handleSavePermissions}
        onCancel={() => {
          setPermissionModalVisible(false)
          setCurrentUserPermissions(null)
          setSelectedPermissionIds([])
          setVisibleTabs([])
          setActiveTabKey('')
        }}
        width={900}
        okText="保存"
        cancelText="取消"
      >
        {currentUserPermissions && (
          <div>
            <Card size="small" style={{ marginBottom: 16 }}>
              <div><strong>用户：</strong>{currentUserPermissions.username}</div>
              <div><strong>角色：</strong>{roles.find(r => r.id === currentUserPermissions.roleId)?.roleName || '-'}</div>
            </Card>
            
            <Divider orientation="left">权限分类</Divider>
            
            {visibleTabs.length > 0 ? (
              <Tabs
                activeKey={activeTabKey}
                onChange={setActiveTabKey}
                type="card"
                items={visibleTabs.map(category => {
                  const categoryPermissions = groupedPermissions[category] || []
                  const isActive = activeTabKey === category
                  
                  return {
                    key: category,
                    label: (
                      <span style={{ 
                        display: 'inline-flex', 
                        alignItems: 'center',
                        gap: 4,
                        color: isActive ? '#1890ff' : undefined,
                        position: 'relative',
                        paddingRight: 4
                      }}>
                        {category}
                        <span
                          onClick={(e) => handleCloseTab(category, e)}
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: 16,
                            height: 16,
                            marginLeft: 4,
                            borderRadius: '50%',
                            color: isActive ? '#1890ff' : '#8c8c8c',
                            transition: 'all 0.2s',
                            cursor: 'pointer',
                            fontSize: 12,
                            lineHeight: 1
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.color = '#ff4d4f'
                            e.currentTarget.style.backgroundColor = '#fff1f0'
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.color = isActive ? '#1890ff' : '#8c8c8c'
                            e.currentTarget.style.backgroundColor = 'transparent'
                          }}
                        >
                          <CloseOutlined />
                        </span>
                      </span>
                    ),
                    children: (
                      <div style={{ maxHeight: 400, overflowY: 'auto', padding: '8px 0' }}>
                        <Checkbox.Group
                          value={selectedPermissionIds}
                          onChange={(checkedValues) => setSelectedPermissionIds(checkedValues)}
                          style={{ width: '100%' }}
                        >
                          <Space direction="vertical" style={{ width: '100%' }}>
                            {categoryPermissions.map(permission => {
                              const isRolePermission = rolePermissions.some(rp => rp.id === permission.id)
                              const isSelected = selectedPermissionIds.includes(permission.id)
                              return (
                                <Checkbox
                                  key={permission.id}
                                  value={permission.id}
                                  style={{ width: '100%' }}
                                >
                                  <span style={{ marginRight: 8 }}>{permission.permissionName}</span>
                                  <Tag size="small" color="default">{permission.permissionCode}</Tag>
                                  {isRolePermission && (
                                    <Tag size="small" color="green">来自角色</Tag>
                                  )}
                                  {isSelected && !isRolePermission && (
                                    <Tag size="small" color="blue">用户权限</Tag>
                                  )}
                                </Checkbox>
                              )
                            })}
                          </Space>
                        </Checkbox.Group>
                      </div>
                    )
                  }
                })}
              />
            ) : (
              <div style={{ 
                textAlign: 'center', 
                padding: '40px 20px', 
                color: '#999',
                border: '1px dashed #d9d9d9',
                borderRadius: 4
              }}>
                <p style={{ margin: 0, fontSize: 14 }}>暂无可选权限分类</p>
                <p style={{ margin: '8px 0 0 0', fontSize: 12 }}>请从下方添加权限分类</p>
              </div>
            )}

            {/* 添加权限分类 */}
            <div style={{ marginTop: 16, padding: 12, backgroundColor: '#fafafa', borderRadius: 4 }}>
              <div style={{ marginBottom: 8, fontSize: 12, color: '#666' }}>添加权限分类：</div>
              <Space wrap>
                {Object.keys(groupedPermissions).sort().map(category => (
                  <Button
                    key={category}
                    size="small"
                    type={visibleTabs.includes(category) ? 'default' : 'dashed'}
                    disabled={visibleTabs.includes(category)}
                    onClick={() => handleAddTab(category)}
                  >
                    {category}
                  </Button>
                ))}
              </Space>
            </div>

            <div style={{ marginTop: 16, color: '#999', fontSize: 12 }}>
              <p>说明：</p>
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                <li>点击标签右侧的 × 可关闭该分类，关闭后该分类下的所有权限将被移除</li>
                <li>绿色标签表示来自角色的权限，取消勾选后将从用户权限中移除</li>
                <li>蓝色标签表示用户直接拥有的权限</li>
                <li>用户最终拥有的权限 = 角色权限 + 用户直接权限（去重）</li>
                <li>用户不需要拥有所属角色的所有权限，可以移除不需要的角色权限</li>
              </ul>
            </div>
          </div>
        )}
      </Modal>

      <SuperAdminModal
        open={superAdminModalVisible}
        onCancel={() => setSuperAdminModalVisible(false)}
        onSuccess={() => {
          setSuperAdminModalVisible(false)
          fetchUsers()
        }}
      />

      {/* 回收站模态框 */}
      <Modal
        title="回收站 - 已删除的用户"
        open={recycleBinModalVisible}
        onCancel={() => setRecycleBinModalVisible(false)}
        footer={null}
        width={1000}
      >
        <Table
          columns={[
            {
              title: 'ID',
              dataIndex: 'id',
              key: 'id',
              width: 80,
            },
            {
              title: '用户名',
              dataIndex: 'username',
              key: 'username',
              width: 120,
            },
            {
              title: '手机号',
              dataIndex: 'phone',
              key: 'phone',
              width: 120,
            },
            {
              title: '邮箱',
              dataIndex: 'email',
              key: 'email',
              width: 180,
              render: (email) => email || '-',
            },
            {
              title: '角色',
              dataIndex: 'roleId',
              key: 'roleId',
              width: 120,
              render: (roleId) => {
                const role = roles.find((r) => r.id === roleId)
                return role ? role.roleName : '-'
              },
            },
            {
              title: '操作',
              key: 'action',
              width: 200,
              render: (_, record) => (
                <Space>
                  <Button
                    type="link"
                    icon={<UndoOutlined />}
                    onClick={() => handleRestoreUser(record.id)}
                  >
                    恢复
                  </Button>
                  <Popconfirm
                    title="确定要永久删除吗？"
                    description="此操作不可恢复，将真正从数据库删除该用户"
                    onConfirm={() => handlePermanentDelete(record.id)}
                    okText="确定"
                    cancelText="取消"
                    okButtonProps={{ danger: true }}
                  >
                    <Button
                      type="link"
                      danger
                      icon={<ExclamationCircleOutlined />}
                    >
                      永久删除
                    </Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
          dataSource={deletedUsers}
          loading={deletedUsersLoading}
          rowKey="id"
          pagination={{
            ...deletedUsersPagination,
            onChange: (page, pageSize) => {
              setDeletedUsersPagination({ ...deletedUsersPagination, current: page, pageSize })
            },
          }}
        />
      </Modal>

      {/* 永久删除确认模态框 */}
      <Modal
        title="确认永久删除"
        open={permanentDeleteModalVisible}
        onOk={handleConfirmPermanentDelete}
        onCancel={() => {
          setPermanentDeleteModalVisible(false)
          permanentDeleteForm.resetFields()
        }}
        okText="确认删除"
        cancelText="取消"
        okButtonProps={{ danger: true }}
      >
        <div style={{ marginBottom: 16 }}>
          <p style={{ color: '#ff4d4f', marginBottom: 16 }}>
            <strong>警告：此操作不可恢复！</strong>
          </p>
          <p>用户将被真正从数据库中删除，所有相关数据将无法恢复。</p>
          <p>请输入 <strong style={{ color: '#ff4d4f' }}>DELETE</strong> 以确认此操作：</p>
        </div>
        <Form form={permanentDeleteForm} layout="vertical">
          <Form.Item
            name="confirmText"
            label="确认文本"
            rules={[
              { required: true, message: '请输入确认文本' },
              { 
                validator: (_, value) => {
                  if (value && value !== 'DELETE') {
                    return Promise.reject(new Error('请输入 DELETE 进行确认'))
                  }
                  return Promise.resolve()
                }
              },
            ]}
          >
            <Input placeholder="请输入 DELETE" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default UserManagement
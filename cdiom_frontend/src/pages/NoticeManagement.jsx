import { useState, useEffect } from 'react'
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tooltip } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import request from '../utils/request'
import { getUser, getUserRoleId } from '../utils/auth'
import { hasPermission, PERMISSIONS } from '../utils/permission'

const NoticeManagement = () => {
  const [notices, setNotices] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingNotice, setEditingNotice] = useState(null)
  const [form] = Form.useForm()
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [currentUser, setCurrentUser] = useState(null)

  // 获取当前用户信息
  useEffect(() => {
    const user = getUser()
    setCurrentUser(user)
  }, [])

  useEffect(() => {
    fetchNotices()
  }, [pagination.current, pagination.pageSize])

  const fetchNotices = async () => {
    setLoading(true)
    try {
      const res = await request.get('/notices', {
        params: {
          page: pagination.current,
          size: pagination.pageSize,
        },
      })
      if (res.code === 200) {
        setNotices(res.data.records)
        setPagination({
          ...pagination,
          total: res.data.total,
        })
      }
    } catch (error) {
      message.error('获取通知公告列表失败')
    } finally {
      setLoading(false)
    }
  }

  const handleAdd = () => {
    setEditingNotice(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record) => {
    setEditingNotice(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id) => {
    try {
      await request.delete(`/notices/${id}`)
      message.success('删除成功')
      fetchNotices()
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handleStatusChange = async (id, status) => {
    try {
      await request.put(`/notices/${id}/status`, { status })
      message.success('状态更新成功')
      fetchNotices()
    } catch (error) {
      message.error('状态更新失败')
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingNotice) {
        await request.put(`/notices/${editingNotice.id}`, values)
        message.success('更新成功')
      } else {
        await request.post('/notices', values)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchNotices()
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
      title: <span style={{ whiteSpace: 'nowrap' }}>标题</span>,
      dataIndex: 'noticeTitle',
      key: 'noticeTitle',
      width: 200,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>类型</span>,
      dataIndex: 'noticeType',
      key: 'noticeType',
      width: 100,
      render: (type) => (type === 1 ? '通知' : '公告'),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>状态</span>,
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) => (status === 1 ? '正常' : '关闭'),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>创建时间</span>,
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作</span>,
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_, record) => {
        // 检查是否有权限操作：系统管理员（roleId=1）和超级管理员（roleId=6）可以操作所有，其他用户只能操作自己创建的
        const roleId = getUserRoleId()
        const currentUserId = currentUser?.id
        const isAdmin = roleId === 1 || roleId === 6
        const canOperate = isAdmin || (currentUserId && record.createBy === currentUserId)
        
        if (!canOperate) {
          return <span style={{ color: '#999' }}>仅查看</span>
        }
        
        return (
          <Space>
            <Tooltip title="编辑">
              <Button
                type="link"
                icon={<EditOutlined />}
                onClick={() => handleEdit(record)}
              />
            </Tooltip>
            <Tooltip title={record.status === 1 ? '关闭' : '开启'}>
              <Button
                type="link"
                danger={record.status === 1}
                icon={record.status === 1 ? <CloseCircleOutlined /> : <CheckCircleOutlined />}
                onClick={() => handleStatusChange(record.id, record.status === 1 ? 0 : 1)}
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
        )
      },
    },
  ]

  // 检查是否有创建权限（所有有查看权限的用户都可以创建）
  const canCreate = hasPermission([PERMISSIONS.NOTICE_VIEW, PERMISSIONS.NOTICE_MANAGE])

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16 }}>
        <h2 style={{ margin: 0 }}>通知公告</h2>
        {canCreate && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增公告
          </Button>
        )}
      </div>
      <Table
        columns={columns}
        dataSource={notices}
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
        title={editingNotice ? '编辑公告' : '新增公告'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={800}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="noticeTitle"
            label="标题"
            rules={[{ required: true, message: '请输入标题' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="noticeType"
            label="类型"
            initialValue={1}
          >
            <Select>
              <Select.Option value={1}>通知</Select.Option>
              <Select.Option value={2}>公告</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="noticeContent"
            label="内容"
            rules={[{ required: true, message: '请输入内容' }]}
          >
            <Input.TextArea rows={6} />
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
            initialValue={1}
          >
            <Select>
              <Select.Option value={1}>正常</Select.Option>
              <Select.Option value={0}>关闭</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default NoticeManagement



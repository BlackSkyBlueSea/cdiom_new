import { useState, useEffect } from 'react'
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tooltip } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import request from '../utils/request'

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
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: '标题',
      dataIndex: 'noticeTitle',
      key: 'noticeTitle',
    },
    {
      title: '类型',
      dataIndex: 'noticeType',
      key: 'noticeType',
      render: (type) => (type === 1 ? '通知' : '公告'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (status === 1 ? '正常' : '关闭'),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
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
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h2>通知公告</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          新增公告
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={notices}
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



import { useState, useEffect } from 'react'
import { Table, Button, Space, Input, Select, Card, Tag, Modal, Form, message } from 'antd'
import { SearchOutlined, ReloadOutlined, PlusOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request from '../utils/request'
import { hasPermission, PERMISSIONS } from '../utils/permission'

const SupplierManagement = () => {
  const [suppliers, setSuppliers] = useState([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [filters, setFilters] = useState({
    keyword: '',
    status: undefined,
    auditStatus: undefined,
  })
  const [modalVisible, setModalVisible] = useState(false)
  const [editingSupplier, setEditingSupplier] = useState(null)
  const [form] = Form.useForm()

  useEffect(() => {
    fetchSuppliers()
  }, [pagination.current, pagination.pageSize, filters])

  const fetchSuppliers = async () => {
    setLoading(true)
    try {
      const params = {
        page: pagination.current,
        size: pagination.pageSize,
        keyword: filters.keyword || undefined,
        status: filters.status,
        auditStatus: filters.auditStatus,
      }
      const res = await request.get('/suppliers', { params })
      if (res.code === 200) {
        setSuppliers(res.data.records || [])
        setPagination({
          ...pagination,
          total: res.data.total || 0,
        })
      } else {
        message.error(res.msg || '获取供应商列表失败')
      }
    } catch (error) {
      console.error('获取供应商列表失败:', error)
      message.error('获取供应商列表失败')
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
      status: undefined,
      auditStatus: undefined,
    })
    setPagination({ ...pagination, current: 1 })
  }

  const handleSubmit = async (values) => {
    try {
      const url = editingSupplier 
        ? `/suppliers/${editingSupplier.id}` 
        : '/suppliers'
      const method = editingSupplier ? 'put' : 'post'
      const res = await request[method](url, values)
      if (res.code === 200) {
        message.success(editingSupplier ? '更新成功' : '创建成功')
        setModalVisible(false)
        form.resetFields()
        setEditingSupplier(null)
        fetchSuppliers()
      } else {
        message.error(res.msg || '操作失败')
      }
    } catch (error) {
      console.error('操作失败:', error)
      message.error('操作失败')
    }
  }

  const handleDelete = async (id) => {
    try {
      const res = await request.delete(`/suppliers/${id}`)
      if (res.code === 200) {
        message.success('删除成功')
        fetchSuppliers()
      } else {
        message.error(res.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除失败:', error)
      message.error('删除失败')
    }
  }

  const handleAudit = async (id, auditStatus, auditReason) => {
    try {
      const res = await request.post(`/suppliers/${id}/audit`, {
        auditStatus,
        auditReason,
      })
      if (res.code === 200) {
        message.success('审核完成')
        fetchSuppliers()
      } else {
        message.error(res.msg || '审核失败')
      }
    } catch (error) {
      console.error('审核失败:', error)
      message.error('审核失败')
    }
  }

  const getStatusTag = (status) => {
    const statusMap = {
      0: { color: 'default', text: '禁用' },
      1: { color: 'green', text: '启用' },
      2: { color: 'orange', text: '待审核' },
    }
    const statusInfo = statusMap[status] || { color: 'default', text: status }
    return <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
  }

  const getAuditStatusTag = (auditStatus) => {
    const statusMap = {
      0: { color: 'orange', text: '待审核' },
      1: { color: 'green', text: '已通过', icon: <CheckCircleOutlined /> },
      2: { color: 'red', text: '已驳回', icon: <CloseCircleOutlined /> },
    }
    const statusInfo = statusMap[auditStatus] || { color: 'default', text: auditStatus }
    return <Tag color={statusInfo.color} icon={statusInfo.icon}>{statusInfo.text}</Tag>
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
      sorter: (a, b) => a.id - b.id,
      defaultSortOrder: 'ascend',
    },
    {
      title: '供应商名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '联系人',
      dataIndex: 'contactPerson',
      key: 'contactPerson',
      width: 120,
    },
    {
      title: '联系电话',
      dataIndex: 'phone',
      key: 'phone',
      width: 150,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => getStatusTag(status),
    },
    {
      title: '审核状态',
      dataIndex: 'auditStatus',
      key: 'auditStatus',
      width: 100,
      render: (auditStatus) => getAuditStatusTag(auditStatus),
    },
    {
      title: '统一社会信用代码',
      dataIndex: 'creditCode',
      key: 'creditCode',
      width: 180,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 250,
      render: (_, record) => (
        <Space>
          {hasPermission(PERMISSIONS.DRUG_MANAGE) && (
            <>
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => {
                  setEditingSupplier(record)
                  form.setFieldsValue(record)
                  setModalVisible(true)
                }}
              >
                编辑
              </Button>
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={() => {
                  Modal.confirm({
                    title: '确认删除',
                    content: '确定要删除该供应商吗？',
                    onOk: () => handleDelete(record.id),
                  })
                }}
              >
                删除
              </Button>
              {record.auditStatus === 0 && (
                <>
                  <Button
                    type="link"
                    size="small"
                    onClick={() => {
                      Modal.confirm({
                        title: '确认审核',
                        content: '确定要通过该供应商的审核吗？',
                        onOk: () => handleAudit(record.id, 1, '审核通过'),
                      })
                    }}
                  >
                    通过
                  </Button>
                  <Button
                    type="link"
                    size="small"
                    danger
                    onClick={() => {
                      const reason = prompt('请输入驳回理由:')
                      if (reason) {
                        handleAudit(record.id, 2, reason)
                      }
                    }}
                  >
                    驳回
                  </Button>
                </>
              )}
            </>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <Space wrap>
            <Input
              placeholder="搜索供应商名称、联系人"
              value={filters.keyword}
              onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
              style={{ width: 200 }}
              allowClear
            />
            <Select
              placeholder="状态"
              value={filters.status}
              onChange={(value) => setFilters({ ...filters, status: value })}
              style={{ width: 120 }}
              allowClear
            >
              <Select.Option value={0}>禁用</Select.Option>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={2}>待审核</Select.Option>
            </Select>
            <Select
              placeholder="审核状态"
              value={filters.auditStatus}
              onChange={(value) => setFilters({ ...filters, auditStatus: value })}
              style={{ width: 120 }}
              allowClear
            >
              <Select.Option value={0}>待审核</Select.Option>
              <Select.Option value={1}>已通过</Select.Option>
              <Select.Option value={2}>已驳回</Select.Option>
            </Select>
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={fetchSuppliers}
            >
              查询
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>
              重置
            </Button>
            {hasPermission(PERMISSIONS.DRUG_MANAGE) && (
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => {
                  setEditingSupplier(null)
                  form.resetFields()
                  setModalVisible(true)
                }}
              >
                新建供应商
              </Button>
            )}
          </Space>

          <Table
            columns={columns}
            dataSource={suppliers}
            rowKey="id"
            loading={loading}
            pagination={{
              ...pagination,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 条`,
            }}
            onChange={handleTableChange}
          />
        </Space>
      </Card>

      <Modal
        title={editingSupplier ? '编辑供应商' : '新建供应商'}
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false)
          form.resetFields()
          setEditingSupplier(null)
        }}
        onOk={() => form.submit()}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            name="name"
            label="供应商名称"
            rules={[
              { required: true, message: '请输入供应商名称' },
              { max: 200, message: '供应商名称长度不能超过200个字符' },
            ]}
          >
            <Input placeholder="请输入供应商名称" />
          </Form.Item>
          <Form.Item
            name="contactPerson"
            label="联系人"
            rules={[
              { required: true, message: '请输入联系人' },
              { max: 50, message: '联系人长度不能超过50个字符' },
            ]}
          >
            <Input placeholder="请输入联系人" />
          </Form.Item>
          <Form.Item
            name="phone"
            label="联系电话"
            rules={[
              { required: true, message: '请输入联系电话' },
              { pattern: /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$/, message: '请输入有效的联系电话格式（手机号或固定电话）' },
            ]}
          >
            <Input placeholder="请输入联系电话（手机号或固定电话）" />
          </Form.Item>
          <Form.Item
            name="address"
            label="地址"
            rules={[
              { max: 500, message: '地址长度不能超过500个字符' },
            ]}
          >
            <Input placeholder="请输入地址" />
          </Form.Item>
          <Form.Item
            name="creditCode"
            label="统一社会信用代码"
            rules={[
              { pattern: /^[0-9A-HJ-NPQRTUWXY]{2}\d{6}[0-9A-HJ-NPQRTUWXY]{10}$/, message: '请输入有效的统一社会信用代码（18位）' },
            ]}
          >
            <Input placeholder="请输入统一社会信用代码（18位）" maxLength={18} />
          </Form.Item>
          {editingSupplier && (
            <Form.Item
              name="status"
              label="状态"
            >
              <Select>
                <Select.Option value={0}>禁用</Select.Option>
                <Select.Option value={1}>启用</Select.Option>
              </Select>
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  )
}

export default SupplierManagement


import { useState, useEffect } from 'react'
import { Table, Button, Space, Input, Select, Tag, Modal, Form, message, DatePicker, Upload, Image } from 'antd'
import { SearchOutlined, ReloadOutlined, PlusOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined, CloseCircleOutlined, UploadOutlined } from '@ant-design/icons'
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
  const [fileList, setFileList] = useState([])
  const [uploading, setUploading] = useState(false)

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
      // 处理日期格式
      const submitData = {
        ...values,
        licenseExpiryDate: values.licenseExpiryDate 
          ? dayjs(values.licenseExpiryDate).format('YYYY-MM-DD') 
          : undefined,
        licenseImage: values.licenseImage || undefined,
      }
      const url = editingSupplier 
        ? `/suppliers/${editingSupplier.id}` 
        : '/suppliers'
      const method = editingSupplier ? 'put' : 'post'
      const res = await request[method](url, submitData)
      if (res.code === 200) {
        message.success(editingSupplier ? '更新成功' : '创建成功')
        setModalVisible(false)
        form.resetFields()
        setFileList([])
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

  const handleUpload = async (file) => {
    setUploading(true)
    const formData = new FormData()
    formData.append('file', file)
    
    try {
      const res = await request.post('/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })
      if (res.code === 200) {
        const fileUrl = res.data
        form.setFieldsValue({ licenseImage: fileUrl })
        setFileList([{
          uid: '-1',
          name: file.name,
          status: 'done',
          url: fileUrl,
        }])
        message.success('文件上传成功')
        return false // 阻止默认上传行为
      } else {
        message.error(res.msg || '文件上传失败')
        return false
      }
    } catch (error) {
      console.error('文件上传失败:', error)
      message.error('文件上传失败')
      return false
    } finally {
      setUploading(false)
    }
  }

  const handleRemove = async () => {
    const licenseImage = form.getFieldValue('licenseImage')
    if (licenseImage) {
      try {
        await request.delete('/upload', {
          params: { url: licenseImage },
        })
      } catch (error) {
        console.error('删除文件失败:', error)
      }
    }
    form.setFieldsValue({ licenseImage: undefined })
    setFileList([])
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

  const [auditModalVisible, setAuditModalVisible] = useState(false)
  const [auditForm] = Form.useForm()
  const [currentAuditSupplier, setCurrentAuditSupplier] = useState(null)

  const handleAudit = async (id, auditStatus, auditReason) => {
    try {
      const res = await request.post(`/suppliers/${id}/audit`, {
        auditStatus,
        auditReason,
      })
      if (res.code === 200) {
        message.success('审核完成')
        setAuditModalVisible(false)
        auditForm.resetFields()
        setCurrentAuditSupplier(null)
        fetchSuppliers()
      } else {
        message.error(res.msg || '审核失败')
      }
    } catch (error) {
      console.error('审核失败:', error)
      message.error('审核失败')
    }
  }

  const handleAuditSubmit = (values) => {
    if (currentAuditSupplier) {
      handleAudit(currentAuditSupplier.id, values.auditStatus, values.auditReason || '')
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
      title: <span style={{ whiteSpace: 'nowrap' }}>ID</span>,
      dataIndex: 'id',
      key: 'id',
      width: 80,
      sorter: (a, b) => a.id - b.id,
      defaultSortOrder: 'ascend',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>供应商名称</span>,
      dataIndex: 'name',
      key: 'name',
      width: 200,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>联系人</span>,
      dataIndex: 'contactPerson',
      key: 'contactPerson',
      width: 120,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>联系电话</span>,
      dataIndex: 'phone',
      key: 'phone',
      width: 150,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>状态</span>,
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => getStatusTag(status),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>审核状态</span>,
      dataIndex: 'auditStatus',
      key: 'auditStatus',
      width: 100,
      render: (auditStatus) => getAuditStatusTag(auditStatus),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>统一社会信用代码</span>,
      dataIndex: 'creditCode',
      key: 'creditCode',
      width: 180,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>许可证到期日期</span>,
      dataIndex: 'licenseExpiryDate',
      key: 'licenseExpiryDate',
      width: 150,
      render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>许可证图片</span>,
      dataIndex: 'licenseImage',
      key: 'licenseImage',
      width: 120,
      render: (url) => url ? (
        <Image
          src={url}
          alt="许可证"
          width={60}
          height={60}
          style={{ objectFit: 'cover' }}
          preview={{
            mask: '查看',
          }}
        />
      ) : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>备注</span>,
      dataIndex: 'remark',
      key: 'remark',
      width: 200,
      ellipsis: true,
      render: (text) => text || '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>审核理由</span>,
      dataIndex: 'auditReason',
      key: 'auditReason',
      width: 200,
      ellipsis: true,
      render: (reason) => reason || '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>审核时间</span>,
      dataIndex: 'auditTime',
      key: 'auditTime',
      width: 180,
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>创建时间</span>,
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作</span>,
      key: 'action',
      width: 250,
      fixed: 'right',
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
                  const formValues = {
                    ...record,
                    licenseExpiryDate: record.licenseExpiryDate ? dayjs(record.licenseExpiryDate) : undefined,
                  }
                  form.setFieldsValue(formValues)
                  // 设置文件列表
                  if (record.licenseImage) {
                    setFileList([{
                      uid: '-1',
                      name: '许可证图片',
                      status: 'done',
                      url: record.licenseImage,
                    }])
                  } else {
                    setFileList([])
                  }
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
              {record.auditStatus === 0 && hasPermission(PERMISSIONS.SUPPLIER_AUDIT) && (
                <>
                  <Button
                    type="link"
                    size="small"
                    onClick={() => {
                      setCurrentAuditSupplier(record)
                      auditForm.setFieldsValue({ auditStatus: 1, auditReason: '审核通过' })
                      setAuditModalVisible(true)
                    }}
                  >
                    通过
                  </Button>
                  <Button
                    type="link"
                    size="small"
                    danger
                    onClick={() => {
                      setCurrentAuditSupplier(record)
                      auditForm.setFieldsValue({ auditStatus: 2, auditReason: '' })
                      setAuditModalVisible(true)
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
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>供应商管理</h2>
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
                setFileList([])
                setModalVisible(true)
              }}
            >
              新建供应商
            </Button>
          )}
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={suppliers}
        rowKey="id"
        loading={loading}
        size="middle"
        scroll={{ x: 'max-content' }}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
        onChange={handleTableChange}
      />

      <Modal
        title={editingSupplier ? '编辑供应商' : '新建供应商'}
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false)
          form.resetFields()
          setFileList([])
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
          <Form.Item
            name="licenseImage"
            label="许可证图片"
          >
            <Upload
              name="file"
              listType="picture-card"
              fileList={fileList}
              beforeUpload={handleUpload}
              onRemove={handleRemove}
              accept="image/*"
              maxCount={1}
            >
              {fileList.length >= 1 ? null : (
                <div>
                  <UploadOutlined />
                  <div style={{ marginTop: 8 }}>上传图片</div>
                </div>
              )}
            </Upload>
            <div style={{ marginTop: 8, color: '#999', fontSize: '12px' }}>
              支持格式：JPG、PNG、GIF等，最大10MB
            </div>
            {fileList.length > 0 && fileList[0].url && (
              <div style={{ marginTop: 8 }}>
                <Image
                  src={fileList[0].url}
                  alt="许可证图片"
                  style={{ maxWidth: '100%', maxHeight: '200px' }}
                  preview={{
                    mask: '预览',
                  }}
                />
              </div>
            )}
          </Form.Item>
          <Form.Item
            name="licenseExpiryDate"
            label="许可证到期日期"
          >
            <DatePicker 
              style={{ width: '100%' }}
              placeholder="请选择许可证到期日期"
              format="YYYY-MM-DD"
            />
          </Form.Item>
          <Form.Item
            name="remark"
            label="备注/描述"
            rules={[
              { max: 2000, message: '备注长度不能超过2000个字符' },
            ]}
          >
            <Input.TextArea 
              rows={4}
              placeholder="请输入备注或描述信息"
              showCount
              maxLength={2000}
            />
          </Form.Item>
          {editingSupplier && (
            <Form.Item
              name="status"
              label="状态"
            >
              <Select>
                <Select.Option value={0}>禁用</Select.Option>
                <Select.Option value={1}>启用</Select.Option>
                <Select.Option value={2}>待审核</Select.Option>
              </Select>
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal
        title="审核供应商"
        open={auditModalVisible}
        onCancel={() => {
          setAuditModalVisible(false)
          auditForm.resetFields()
          setCurrentAuditSupplier(null)
        }}
        onOk={() => auditForm.submit()}
        width={500}
      >
        <Form
          form={auditForm}
          layout="vertical"
          onFinish={handleAuditSubmit}
        >
          <Form.Item
            name="auditStatus"
            label="审核结果"
            rules={[{ required: true, message: '请选择审核结果' }]}
          >
            <Select placeholder="请选择审核结果">
              <Select.Option value={1}>通过</Select.Option>
              <Select.Option value={2}>驳回</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="auditReason"
            label="审核理由"
            rules={[
              { required: true, message: '请输入审核理由' },
              { max: 500, message: '审核理由长度不能超过500个字符' },
            ]}
          >
            <Input.TextArea 
              rows={4}
              placeholder="请输入审核理由"
              showCount
              maxLength={500}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default SupplierManagement


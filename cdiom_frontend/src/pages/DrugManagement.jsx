import { useState, useEffect } from 'react'
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tooltip, DatePicker } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request from '../utils/request'

const { TextArea } = Input

const DrugManagement = () => {
  const [drugs, setDrugs] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingDrug, setEditingDrug] = useState(null)
  const [form] = Form.useForm()
  const [filters, setFilters] = useState({
    keyword: '',
    isSpecial: undefined,
  })

  useEffect(() => {
    fetchDrugs()
  }, [filters])

  const fetchDrugs = async () => {
    setLoading(true)
    try {
      const res = await request.get('/drugs', {
        params: {
          page: 1,
          size: 10000, // 设置一个很大的值以获取所有数据
          keyword: filters.keyword || undefined,
          isSpecial: filters.isSpecial,
        },
      })
      if (res.code === 200) {
        setDrugs(res.data.records || [])
      } else {
        message.error(res.msg || '获取药品列表失败')
      }
    } catch (error) {
      console.error('获取药品列表失败:', error)
      const errorMsg = error.response?.data?.msg || error.message || '获取药品列表失败'
      message.error(errorMsg)
      // 如果是401错误，可能是token过期
      if (error.response?.status === 401) {
        message.warning('登录已过期，请重新登录')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleAdd = () => {
    setEditingDrug(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record) => {
    setEditingDrug(record)
    const formData = {
      ...record,
      expiryDate: record.expiryDate ? dayjs(record.expiryDate) : null,
    }
    form.setFieldsValue(formData)
    setModalVisible(true)
  }

  const handleDelete = async (id) => {
    try {
      await request.delete(`/drugs/${id}`)
      message.success('删除成功')
      fetchDrugs()
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      const submitData = {
        ...values,
        expiryDate: values.expiryDate ? values.expiryDate.format('YYYY-MM-DD') : null,
      }
      
      if (editingDrug) {
        await request.put(`/drugs/${editingDrug.id}`, submitData)
        message.success('更新成功')
      } else {
        await request.post('/drugs', submitData)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchDrugs()
    } catch (error) {
      if (error.errorFields) {
        return
      }
      message.error(error.message || '操作失败')
    }
  }

  const handleSearch = (value) => {
    setFilters({ ...filters, keyword: value })
  }

  const handleFilterChange = (key, value) => {
    setFilters({ ...filters, [key]: value })
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
      title: <span style={{ whiteSpace: 'nowrap' }}>药品名称</span>,
      dataIndex: 'drugName',
      key: 'drugName',
      ellipsis: true,
      width: 150,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>国家本位码</span>,
      dataIndex: 'nationalCode',
      key: 'nationalCode',
      ellipsis: true,
      width: 150,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>剂型</span>,
      dataIndex: 'dosageForm',
      key: 'dosageForm',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>规格</span>,
      dataIndex: 'specification',
      key: 'specification',
      ellipsis: true,
      width: 100,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>生产厂家</span>,
      dataIndex: 'manufacturer',
      key: 'manufacturer',
      ellipsis: true,
      width: 250,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>批准文号</span>,
      dataIndex: 'approvalNumber',
      key: 'approvalNumber',
      ellipsis: true,
      width: 150,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>是否特殊</span>,
      dataIndex: 'isSpecial',
      key: 'isSpecial',
      render: (isSpecial) => (isSpecial === 1 ? '是' : '否'),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>单位</span>,
      dataIndex: 'unit',
      key: 'unit',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作</span>,
      key: 'action',
      width: 100,
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
        <h2 style={{ margin: 0 }}>药品信息管理</h2>
        <div style={{ display: 'flex', gap: 16, alignItems: 'center', flex: 1, justifyContent: 'flex-end' }}>
          <Input.Search
            placeholder="搜索药品名称、国家本位码、批准文号、生产厂家"
            allowClear
            style={{ width: 300 }}
            onSearch={handleSearch}
            enterButton
          />
          <Select
            placeholder="筛选特殊药品"
            allowClear
            style={{ width: 150 }}
            onChange={(value) => handleFilterChange('isSpecial', value)}
          >
            <Select.Option value={0}>普通药品</Select.Option>
            <Select.Option value={1}>特殊药品</Select.Option>
          </Select>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增药品
          </Button>
        </div>
      </div>

      <Table
        columns={columns}
        dataSource={drugs}
        loading={loading}
        rowKey="id"
        size="middle"
        scroll={{ x: 1200, y: 'calc(100vh - 200px)' }}
        pagination={false}
      />
      
      <Modal
        title={editingDrug ? '编辑药品信息' : '新增药品信息'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={800}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="drugName"
            label="药品名称"
            rules={[{ required: true, message: '请输入药品名称' }]}
          >
            <Input />
          </Form.Item>
          
          <Form.Item
            name="nationalCode"
            label="国家本位码"
            rules={[{ required: true, message: '请输入国家本位码' }]}
          >
            <Input />
          </Form.Item>
          
          <Form.Item
            name="traceCode"
            label="药品追溯码"
          >
            <Input />
          </Form.Item>
          
          <Form.Item
            name="productCode"
            label="商品码"
          >
            <Input />
          </Form.Item>
          
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="dosageForm"
              label="剂型"
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
            
            <Form.Item
              name="specification"
              label="规格"
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
          </div>
          
          <Form.Item
            name="approvalNumber"
            label="批准文号"
          >
            <Input />
          </Form.Item>
          
          <Form.Item
            name="manufacturer"
            label="生产厂家"
          >
            <Input />
          </Form.Item>
          
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="isSpecial"
              label="是否特殊药品"
              style={{ flex: 1 }}
              initialValue={0}
            >
              <Select>
                <Select.Option value={0}>普通药品</Select.Option>
                <Select.Option value={1}>特殊药品</Select.Option>
              </Select>
            </Form.Item>
            
            <Form.Item
              name="unit"
              label="单位"
              style={{ flex: 1 }}
              initialValue="盒"
            >
              <Select>
                <Select.Option value="盒">盒</Select.Option>
                <Select.Option value="片">片</Select.Option>
                <Select.Option value="粒">粒</Select.Option>
                <Select.Option value="袋">袋</Select.Option>
                <Select.Option value="支">支</Select.Option>
                <Select.Option value="瓶">瓶</Select.Option>
              </Select>
            </Form.Item>
          </div>
          
          <Form.Item
            name="expiryDate"
            label="有效期"
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          
          <Form.Item
            name="storageRequirement"
            label="存储要求"
          >
            <Input />
          </Form.Item>
          
          <Form.Item
            name="storageLocation"
            label="存储位置"
          >
            <Input />
          </Form.Item>
          
          <Form.Item
            name="description"
            label="描述"
          >
            <TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default DrugManagement


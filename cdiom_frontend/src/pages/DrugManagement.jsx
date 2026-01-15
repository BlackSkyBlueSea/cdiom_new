import { useState, useEffect, useMemo } from 'react'
import { Table, Button, Space, Modal, Form, Input, Select, AutoComplete, message, Popconfirm, Tooltip, DatePicker } from 'antd'

const { Compact } = Space
import { PlusOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined, CloseCircleOutlined, ScanOutlined, SearchOutlined, DownloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request from '../utils/request'
import { hasPermission, PERMISSIONS, PermissionWrapper } from '../utils/permission'

const { TextArea } = Input

// 常见剂型选项
const DOSAGE_FORM_OPTIONS = [
  '片剂', '胶囊剂', '颗粒剂', '散剂', '丸剂', '软膏剂', '乳膏剂', '凝胶剂',
  '注射剂', '输液剂', '滴眼剂', '滴耳剂', '滴鼻剂', '喷雾剂', '气雾剂',
  '栓剂', '贴剂', '膜剂', '糖浆剂', '口服液', '混悬剂', '乳剂', '溶液剂',
  '酊剂', '搽剂', '洗剂', '灌肠剂', '其他'
]

// 常见规格选项
const SPECIFICATION_OPTIONS = [
  '10mg', '20mg', '25mg', '50mg', '100mg', '200mg', '250mg', '500mg',
  '0.1g', '0.25g', '0.5g', '1g', '2g', '5g',
  '10ml', '20ml', '50ml', '100ml', '250ml', '500ml',
  '10片', '12片', '14片', '16片', '20片', '24片', '28片', '30片',
  '10粒', '12粒', '14粒', '16粒', '20粒', '24粒', '28粒', '30粒',
  '1袋', '2袋', '5袋', '10袋', '12袋', '14袋', '20袋',
  '1支', '2支', '5支', '10支', '20支',
  '其他'
]

// 常见存储要求选项
const STORAGE_REQUIREMENT_OPTIONS = [
  '常温保存', '阴凉处保存', '冷藏保存（2-8℃）', '冷冻保存（-20℃以下）',
  '避光保存', '密封保存', '干燥处保存', '防潮保存',
  '常温、避光保存', '阴凉、避光保存', '冷藏、避光保存',
  '其他'
]

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
  const [scanning, setScanning] = useState(false)
  const [searchingByName, setSearchingByName] = useState(false)
  const [searchingByApproval, setSearchingByApproval] = useState(false)
  const [exporting, setExporting] = useState(false)

  // 从已有药品数据中提取生产厂家选项
  const manufacturerOptions = useMemo(() => {
    const manufacturers = new Set()
    drugs.forEach(drug => {
      if (drug.manufacturer && drug.manufacturer.trim()) {
        manufacturers.add(drug.manufacturer.trim())
      }
    })
    return Array.from(manufacturers).sort().map(manufacturer => ({
      value: manufacturer,
      label: manufacturer
    }))
  }, [drugs])

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

  // 填充表单数据的通用方法
  const fillFormData = (drugInfo, code = '') => {
    const formData = {
      drugName: drugInfo.drugName || '',
      nationalCode: drugInfo.nationalCode || code,
      productCode: drugInfo.productCode || code,
      traceCode: drugInfo.traceCode || '',
      dosageForm: drugInfo.dosageForm || '',
      specification: drugInfo.specification || '',
      approvalNumber: drugInfo.approvalNumber || '',
      manufacturer: drugInfo.manufacturer || '',
      isSpecial: drugInfo.isSpecial !== undefined ? drugInfo.isSpecial : 0,
      unit: drugInfo.unit || '盒',
      expiryDate: drugInfo.expiryDate ? dayjs(drugInfo.expiryDate) : null,
      storageRequirement: drugInfo.storageRequirement || '',
      storageLocation: drugInfo.storageLocation || '',
      description: drugInfo.description || '',
    }
    form.setFieldsValue(formData)
    message.success('药品信息已自动填充，请核对并修改')
  }

  // 方法2：扫描商品码或本位码（本地+极速数据API）
  const handleScan = async (code) => {
    if (!code || code.trim() === '') {
      message.warning('请输入商品码或本位码')
      return
    }

    setScanning(true)
    try {
      const res = await request.get('/drugs/search', {
        params: { code: code.trim() }
      })
      
      if (res.code === 200 && res.data) {
        fillFormData(res.data, code.trim())
      } else {
        message.warning(res.msg || '未找到药品信息')
      }
    } catch (error) {
      console.error('扫描失败:', error)
      const errorMsg = error.response?.data?.msg || error.message || '扫描失败'
      message.error(errorMsg)
    } finally {
      setScanning(false)
    }
  }

  // 方法3：按药品名称搜索（万维易源API）
  const handleSearchByName = async () => {
    const drugName = form.getFieldValue('drugName')
    if (!drugName || drugName.trim() === '') {
      message.warning('请输入药品名称')
      return
    }

    setSearchingByName(true)
    try {
      const res = await request.get('/drugs/search/name', {
        params: { drugName: drugName.trim() }
      })
      
      if (res.code === 200 && res.data) {
        fillFormData(res.data)
      } else {
        message.warning(res.msg || '未找到药品信息')
      }
    } catch (error) {
      console.error('搜索失败:', error)
      const errorMsg = error.response?.data?.msg || error.message || '搜索失败'
      message.error(errorMsg)
    } finally {
      setSearchingByName(false)
    }
  }

  // 方法4：按批准文号搜索（万维易源API）
  const handleSearchByApproval = async () => {
    const approvalNumber = form.getFieldValue('approvalNumber')
    if (!approvalNumber || approvalNumber.trim() === '') {
      message.warning('请输入批准文号')
      return
    }

    setSearchingByApproval(true)
    try {
      const res = await request.get('/drugs/search/approval', {
        params: { approvalNumber: approvalNumber.trim() }
      })
      
      if (res.code === 200 && res.data) {
        fillFormData(res.data)
      } else {
        message.warning(res.msg || '未找到药品信息')
      }
    } catch (error) {
      console.error('搜索失败:', error)
      const errorMsg = error.response?.data?.msg || error.message || '搜索失败'
      message.error(errorMsg)
    } finally {
      setSearchingByApproval(false)
    }
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

  const handleExport = async () => {
    setExporting(true)
    try {
      const params = new URLSearchParams()
      if (filters.keyword) {
        params.append('keyword', filters.keyword)
      }
      if (filters.isSpecial !== undefined) {
        params.append('isSpecial', filters.isSpecial)
      }
      
      const url = `/api/v1/drugs/export?${params.toString()}`
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        credentials: 'include',
      })
      
      if (!response.ok) {
        throw new Error('导出失败')
      }
      
      const blob = await response.blob()
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = `药品列表_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(downloadUrl)
      
      message.success('导出成功')
    } catch (error) {
      console.error('导出失败:', error)
      message.error('导出失败: ' + (error.message || '未知错误'))
    } finally {
      setExporting(false)
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
          {hasPermission(PERMISSIONS.DRUG_UPDATE) && (
            <Tooltip title="编辑">
              <Button
                type="link"
                icon={<EditOutlined />}
                onClick={() => handleEdit(record)}
              />
            </Tooltip>
          )}
          {hasPermission(PERMISSIONS.DRUG_DELETE) && (
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
          <Button 
            icon={<DownloadOutlined />} 
            onClick={handleExport}
            loading={exporting}
          >
            导出Excel
          </Button>
          <PermissionWrapper permission={PERMISSIONS.DRUG_CREATE}>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增药品
            </Button>
          </PermissionWrapper>
        </div>
      </div>

      <Table
        columns={columns}
        dataSource={drugs}
        loading={loading}
        rowKey="id"
        size="middle"
        scroll={{ x: 1200 }}
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
          {/* 扫描输入框 */}
          <Form.Item label="扫描商品码/本位码">
            <Space.Compact style={{ width: '100%' }}>
              <Input
                style={{ flex: 1 }}
                placeholder="请输入或扫描商品码、本位码、追溯码"
                onPressEnter={(e) => {
                  const value = e.target.value
                  if (value) {
                    handleScan(value)
                  }
                }}
                allowClear
              />
              <Button
                type="primary"
                icon={<ScanOutlined />}
                loading={scanning}
                onClick={() => {
                  const scanInput = document.querySelector('input[placeholder*="扫描商品码"]')
                  if (scanInput && scanInput.value) {
                    handleScan(scanInput.value)
                  } else {
                    message.warning('请输入商品码或本位码')
                  }
                }}
              >
                扫描识别
              </Button>
            </Space.Compact>
          </Form.Item>

          {/* 第一行：药品名称和国家本位码 */}
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="drugName"
              label="药品名称"
              rules={[{ required: true, message: '请输入药品名称' }]}
              style={{ flex: 1 }}
            >
              <Input 
                addonAfter={
                  <Button
                    type="text"
                    icon={<SearchOutlined />}
                    loading={searchingByName}
                    onClick={handleSearchByName}
                    title="搜索药品信息"
                  />
                }
                placeholder="请输入药品名称，点击右侧搜索"
              />
            </Form.Item>
            
            <Form.Item
              name="nationalCode"
              label="国家本位码"
              rules={[{ required: true, message: '请输入国家本位码' }]}
              style={{ flex: 1 }}
            >
              <Input 
                onPressEnter={(e) => {
                  const value = e.target.value
                  if (value) {
                    handleScan(value)
                  }
                }}
              />
            </Form.Item>
          </div>

          {/* 第二行：药品追溯码和商品码 */}
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="traceCode"
              label="药品追溯码"
              style={{ flex: 1 }}
            >
              <Input 
                onPressEnter={(e) => {
                  const value = e.target.value
                  if (value) {
                    handleScan(value)
                  }
                }}
              />
            </Form.Item>
            
            <Form.Item
              name="productCode"
              label="商品码"
              style={{ flex: 1 }}
            >
              <Input 
                onPressEnter={(e) => {
                  const value = e.target.value
                  if (value) {
                    handleScan(value)
                  }
                }}
              />
            </Form.Item>
          </div>
          
          {/* 批准文号和生产厂家共占一行 */}
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="approvalNumber"
              label="批准文号"
              style={{ flex: 1 }}
            >
              <Input 
                placeholder="请输入批准文号，点击右侧搜索"
                addonAfter={
                  <Button
                    type="text"
                    icon={<SearchOutlined />}
                    loading={searchingByApproval}
                    onClick={handleSearchByApproval}
                    title="搜索药品信息"
                  />
                }
              />
            </Form.Item>
            
            <Form.Item
              name="manufacturer"
              label="生产厂家"
              style={{ flex: 1 }}
            >
              <AutoComplete
                options={manufacturerOptions}
                placeholder="请选择或输入生产厂家"
                filterOption={(inputValue, option) =>
                  option.value.toUpperCase().indexOf(inputValue.toUpperCase()) !== -1
                }
                allowClear
              />
            </Form.Item>
          </div>

          {/* 剂型、规格、有效期共占一行 */}
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="dosageForm"
              label="剂型"
              style={{ flex: 1 }}
            >
              <AutoComplete
                options={DOSAGE_FORM_OPTIONS.map(form => ({ value: form, label: form }))}
                placeholder="请选择或输入剂型"
                filterOption={(inputValue, option) =>
                  option.value.toUpperCase().indexOf(inputValue.toUpperCase()) !== -1
                }
                allowClear
              />
            </Form.Item>
            
            <Form.Item
              name="specification"
              label="规格"
              style={{ flex: 1 }}
            >
              <AutoComplete
                options={SPECIFICATION_OPTIONS.map(spec => ({ value: spec, label: spec }))}
                placeholder="请选择或输入规格"
                filterOption={(inputValue, option) =>
                  option.value.toUpperCase().indexOf(inputValue.toUpperCase()) !== -1
                }
                allowClear
              />
            </Form.Item>

            <Form.Item
              name="expiryDate"
              label="有效期"
              style={{ flex: 1 }}
            >
              <DatePicker style={{ width: '100%' }} placeholder="请选择有效期" />
            </Form.Item>
          </div>
          
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="isSpecial"
              label="是否特殊药品"
              style={{ flex: 1 }}
              initialValue={0}
              tooltip="请根据药品说明书/包装判断：1. 说明书首页或'警示语'栏明确标注'麻醉药品'、'第一类精神药品'、'第二类精神药品'等字样；2. 包装上有专用标识图案（如麻醉药品为'蓝白相间的麻字标识'，精神药品为'绿白相间的精字标识'）；3. 说明书'注意事项'或'特殊人群用药'栏注明'凭麻醉药品专用处方购买'、'仅限二级以上医院使用'等管控要求。"
            >
              <Select placeholder="请根据药品说明书/包装手动选择">
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
              <AutoComplete
                options={[
                  { value: '盒', label: '盒' },
                  { value: '片', label: '片' },
                  { value: '粒', label: '粒' },
                  { value: '袋', label: '袋' },
                  { value: '支', label: '支' },
                  { value: '瓶', label: '瓶' },
                  { value: '包', label: '包' },
                  { value: '条', label: '条' },
                  { value: '板', label: '板' },
                  { value: '盒装', label: '盒装' }
                ]}
                placeholder="请选择或输入单位"
                filterOption={(inputValue, option) =>
                  option.value.toUpperCase().indexOf(inputValue.toUpperCase()) !== -1
                }
                allowClear
              />
            </Form.Item>
          </div>
          
          {/* 存储要求和存储位置共占一行 */}
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="storageRequirement"
              label="存储要求"
              style={{ flex: 1 }}
            >
              <AutoComplete
                options={STORAGE_REQUIREMENT_OPTIONS.map(req => ({ value: req, label: req }))}
                placeholder="请选择或输入存储要求"
                filterOption={(inputValue, option) =>
                  option.value.toUpperCase().indexOf(inputValue.toUpperCase()) !== -1
                }
                allowClear
              />
            </Form.Item>
            
            <Form.Item
              name="storageLocation"
              label="存储位置"
              style={{ flex: 1 }}
            >
              <Input placeholder="请输入存储位置" />
            </Form.Item>
          </div>
          
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


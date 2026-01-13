import { useState, useEffect, useMemo } from 'react'
import { Table, Button, Space, Input, Select, DatePicker, Card, Tag, Modal, Form, message, AutoComplete, InputNumber } from 'antd'
import { SearchOutlined, ReloadOutlined, PlusOutlined, CheckCircleOutlined, CloseCircleOutlined, PlayCircleOutlined, DeleteOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request from '../utils/request'
import { hasPermission, PERMISSIONS } from '../utils/permission'

const { RangePicker } = DatePicker
const { TextArea } = Input

const OutboundManagement = () => {
  const [outboundApplies, setOutboundApplies] = useState([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [filters, setFilters] = useState({
    keyword: '',
    applicantId: undefined,
    approverId: undefined,
    department: '',
    status: undefined,
    startDate: undefined,
    endDate: undefined,
  })
  const [modalVisible, setModalVisible] = useState(false)
  const [approveModalVisible, setApproveModalVisible] = useState(false)
  const [executeModalVisible, setExecuteModalVisible] = useState(false)
  const [currentRecord, setCurrentRecord] = useState(null)
  const [applyItems, setApplyItems] = useState([])
  const [inventoryBatches, setInventoryBatches] = useState({}) // {drugId: [batches]}
  const [form] = Form.useForm()
  const [approveForm] = Form.useForm()
  const [executeForm] = Form.useForm()
  const [drugs, setDrugs] = useState([])
  const [applyFormItems, setApplyFormItems] = useState([{ drugId: undefined, quantity: undefined, batchNumber: undefined }])

  useEffect(() => {
    fetchOutboundApplies()
  }, [pagination.current, pagination.pageSize, filters])

  useEffect(() => {
    fetchDrugs()
  }, [])

  // 药品选项（用于AutoComplete）
  const drugOptions = useMemo(() => {
    return drugs.map(drug => ({
      value: drug.id,
      label: `${drug.drugName} (${drug.specification || ''})`,
      drug: drug
    }))
  }, [drugs])

  const fetchDrugs = async () => {
    try {
      const res = await request.get('/drugs', {
        params: { page: 1, size: 10000 }
      })
      if (res.code === 200) {
        setDrugs(res.data.records || [])
      }
    } catch (error) {
      console.error('获取药品列表失败:', error)
    }
  }

  const fetchApplyItems = async (applyId) => {
    try {
      const res = await request.get(`/outbound/${applyId}/items`)
      if (res.code === 200) {
        setApplyItems(res.data || [])
        // 获取每个药品的可用批次
        const batchesMap = {}
        for (const item of res.data) {
          try {
            const inventoryRes = await request.get('/inventory', {
              params: {
                page: 1,
                size: 100,
                drugId: item.drugId,
              }
            })
            if (inventoryRes.code === 200) {
              batchesMap[item.drugId] = inventoryRes.data.records || []
            }
          } catch (error) {
            console.error(`获取药品${item.drugId}的库存批次失败:`, error)
          }
        }
        setInventoryBatches(batchesMap)
      }
    } catch (error) {
      console.error('获取申请明细失败:', error)
      message.error('获取申请明细失败')
    }
  }

  const fetchOutboundApplies = async () => {
    setLoading(true)
    try {
      const params = {
        page: pagination.current,
        size: pagination.pageSize,
        keyword: filters.keyword || undefined,
        applicantId: filters.applicantId,
        approverId: filters.approverId,
        department: filters.department || undefined,
        status: filters.status,
        startDate: filters.startDate ? filters.startDate.format('YYYY-MM-DD') : undefined,
        endDate: filters.endDate ? filters.endDate.format('YYYY-MM-DD') : undefined,
      }
      const res = await request.get('/outbound', { params })
      if (res.code === 200) {
        setOutboundApplies(res.data.records || [])
        setPagination({
          ...pagination,
          total: res.data.total || 0,
        })
      } else {
        message.error(res.msg || '获取出库申请失败')
      }
    } catch (error) {
      console.error('获取出库申请失败:', error)
      message.error('获取出库申请失败')
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
      applicantId: undefined,
      approverId: undefined,
      department: '',
      status: undefined,
      startDate: undefined,
      endDate: undefined,
    })
    setPagination({ ...pagination, current: 1 })
  }

  const handleApprove = async (values) => {
    try {
      const res = await request.post(`/outbound/${currentRecord.id}/approve`, {
        secondApproverId: values.secondApproverId,
      })
      if (res.code === 200) {
        message.success('审批通过')
        setApproveModalVisible(false)
        approveForm.resetFields()
        fetchOutboundApplies()
      } else {
        message.error(res.msg || '审批失败')
      }
    } catch (error) {
      console.error('审批失败:', error)
      message.error('审批失败')
    }
  }

  const handleReject = async (reason) => {
    try {
      const res = await request.post(`/outbound/${currentRecord.id}/reject`, {
        rejectReason: reason,
      })
      if (res.code === 200) {
        message.success('已驳回')
        fetchOutboundApplies()
      } else {
        message.error(res.msg || '驳回失败')
      }
    } catch (error) {
      console.error('驳回失败:', error)
      message.error('驳回失败')
    }
  }

  const handleCreateApply = async (values) => {
    try {
      // 验证申请明细
      if (!values.department || values.department.trim() === '') {
        message.error('请输入所属科室')
        return
      }
      if (!values.purpose || values.purpose.trim() === '') {
        message.error('请输入用途说明')
        return
      }
      if (!applyFormItems || applyFormItems.length === 0) {
        message.error('请至少添加一个药品')
        return
      }
      
      // 验证每个明细项
      const validItems = []
      for (const item of applyFormItems) {
        if (!item.drugId) {
          message.error('请选择药品')
          return
        }
        if (!item.quantity || item.quantity <= 0) {
          message.error('请输入有效的申领数量')
          return
        }
        validItems.push({
          drugId: item.drugId,
          batchNumber: item.batchNumber || undefined,
          quantity: item.quantity,
        })
      }

      const data = {
        department: values.department.trim(),
        purpose: values.purpose.trim(),
        remark: values.remark || '',
        items: validItems,
      }

      const res = await request.post('/outbound', data)
      if (res.code === 200) {
        message.success('出库申请创建成功')
        setModalVisible(false)
        form.resetFields()
        setApplyFormItems([{ drugId: undefined, quantity: undefined, batchNumber: undefined }])
        fetchOutboundApplies()
      } else {
        message.error(res.msg || '创建出库申请失败')
      }
    } catch (error) {
      console.error('创建出库申请失败:', error)
      message.error(error.response?.data?.msg || error.message || '创建出库申请失败')
    }
  }

  const handleExecute = async (values) => {
    try {
      // 验证出库明细
      const outboundItems = []
      for (const item of applyItems) {
        const formItem = values[`item_${item.id}`]
        if (!formItem || !formItem.actualQuantity || formItem.actualQuantity <= 0) {
          message.error(`请填写药品ID ${item.drugId} 的实际出库数量`)
          return
        }
        if (formItem.actualQuantity > item.quantity) {
          message.error(`实际出库数量不能超过申请数量（申请：${item.quantity}）`)
          return
        }
        outboundItems.push({
          drugId: item.drugId,
          batchNumber: formItem.batchNumber || undefined,
          actualQuantity: formItem.actualQuantity,
        })
      }

      const res = await request.post(`/outbound/${currentRecord.id}/execute`, {
        outboundItems: outboundItems,
      })
      if (res.code === 200) {
        message.success('出库执行成功')
        setExecuteModalVisible(false)
        executeForm.resetFields()
        setApplyItems([])
        setInventoryBatches({})
        fetchOutboundApplies()
      } else {
        message.error(res.msg || '出库执行失败')
      }
    } catch (error) {
      console.error('出库执行失败:', error)
      message.error(error.response?.data?.msg || error.message || '出库执行失败')
    }
  }

  const addApplyFormItem = () => {
    setApplyFormItems([...applyFormItems, { drugId: undefined, quantity: undefined, batchNumber: undefined }])
  }

  const removeApplyFormItem = (index) => {
    const newItems = applyFormItems.filter((_, i) => i !== index)
    setApplyFormItems(newItems)
  }

  const getStatusTag = (status) => {
    const statusMap = {
      PENDING: { color: 'orange', text: '待审批' },
      APPROVED: { color: 'green', text: '已通过' },
      REJECTED: { color: 'red', text: '已驳回' },
      OUTBOUND: { color: 'blue', text: '已出库' },
      CANCELLED: { color: 'default', text: '已取消' },
    }
    const statusInfo = statusMap[status] || { color: 'default', text: status }
    return <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
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
      title: '申领单号',
      dataIndex: 'applyNumber',
      key: 'applyNumber',
      width: 150,
    },
    {
      title: '申请人',
      dataIndex: 'applicantName',
      key: 'applicantName',
      width: 100,
    },
    {
      title: '所属科室',
      dataIndex: 'department',
      key: 'department',
      width: 120,
    },
    {
      title: '用途',
      dataIndex: 'purpose',
      key: 'purpose',
      width: 150,
    },
    {
      title: '申请状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => getStatusTag(status),
    },
    {
      title: '审批人',
      dataIndex: 'approverName',
      key: 'approverName',
      width: 100,
    },
    {
      title: '审批时间',
      dataIndex: 'approveTime',
      key: 'approveTime',
      width: 180,
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '出库时间',
      dataIndex: 'outboundTime',
      key: 'outboundTime',
      width: 180,
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_, record) => (
        <Space>
          {record.status === 'PENDING' && hasPermission(PERMISSIONS.DRUG_MANAGE) && (
            <>
              <Button
                type="link"
                size="small"
                onClick={() => {
                  setCurrentRecord(record)
                  setApproveModalVisible(true)
                }}
              >
                审批
              </Button>
              <Button
                type="link"
                size="small"
                danger
                onClick={() => {
                  Modal.confirm({
                    title: '确认驳回',
                    content: '请输入驳回理由',
                    onOk: (close) => {
                      const reason = prompt('请输入驳回理由:')
                      if (reason) {
                        handleReject(reason)
                        close()
                      }
                    },
                  })
                }}
              >
                驳回
              </Button>
            </>
          )}
          {record.status === 'APPROVED' && hasPermission(PERMISSIONS.DRUG_MANAGE) && (
            <Button
              type="link"
              size="small"
              icon={<PlayCircleOutlined />}
              onClick={async () => {
                setCurrentRecord(record)
                await fetchApplyItems(record.id)
                setExecuteModalVisible(true)
              }}
            >
              执行出库
            </Button>
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
              placeholder="搜索申领单号、申请人"
              value={filters.keyword}
              onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
              style={{ width: 200 }}
              allowClear
            />
            <Input
              placeholder="所属科室"
              value={filters.department}
              onChange={(e) => setFilters({ ...filters, department: e.target.value })}
              style={{ width: 150 }}
              allowClear
            />
            <Select
              placeholder="申请状态"
              value={filters.status}
              onChange={(value) => setFilters({ ...filters, status: value })}
              style={{ width: 120 }}
              allowClear
            >
              <Select.Option value="PENDING">待审批</Select.Option>
              <Select.Option value="APPROVED">已通过</Select.Option>
              <Select.Option value="REJECTED">已驳回</Select.Option>
              <Select.Option value="OUTBOUND">已出库</Select.Option>
              <Select.Option value="CANCELLED">已取消</Select.Option>
            </Select>
            <RangePicker
              placeholder={['开始日期', '结束日期']}
              value={filters.startDate && filters.endDate 
                ? [filters.startDate, filters.endDate] 
                : null}
              onChange={(dates) => {
                setFilters({
                  ...filters,
                  startDate: dates ? dates[0] : undefined,
                  endDate: dates ? dates[1] : undefined,
                })
              }}
            />
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={fetchOutboundApplies}
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
                  setModalVisible(true)
                  form.resetFields()
                  setApplyFormItems([{ drugId: undefined, quantity: undefined, batchNumber: undefined }])
                }}
              >
                新建出库申请
              </Button>
            )}
          </Space>

          <Table
            columns={columns}
            dataSource={outboundApplies}
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

      {/* 新建出库申请模态框 */}
      <Modal
        title="新建出库申请"
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false)
          form.resetFields()
          setApplyFormItems([{ drugId: undefined, quantity: undefined, batchNumber: undefined }])
        }}
        onOk={() => form.submit()}
        width={900}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreateApply}
        >
          <Form.Item
            name="department"
            label="所属科室"
            rules={[
              { required: true, message: '请输入所属科室' },
              { max: 100, message: '所属科室长度不能超过100个字符' },
            ]}
          >
            <Input placeholder="请输入所属科室" />
          </Form.Item>

          <Form.Item
            name="purpose"
            label="用途说明"
            rules={[
              { required: true, message: '请输入用途说明' },
              { max: 200, message: '用途说明长度不能超过200个字符' },
            ]}
          >
            <TextArea
              rows={3}
              placeholder="请输入用途说明（如：门诊用药、手术用药等）"
            />
          </Form.Item>

          <Form.Item label="申领药品">
            {applyFormItems.map((item, index) => (
              <div key={index} style={{ marginBottom: 16, padding: 16, border: '1px solid #d9d9d9', borderRadius: 4 }}>
                <Space direction="vertical" style={{ width: '100%' }} size="small">
                  <Space wrap>
                    <Form.Item
                      label="药品"
                      style={{ width: 300, marginBottom: 0 }}
                      rules={[{ required: true, message: '请选择药品' }]}
                    >
                      <AutoComplete
                        options={drugOptions}
                        placeholder="请输入或选择药品"
                        filterOption={(inputValue, option) =>
                          option.label.toUpperCase().indexOf(inputValue.toUpperCase()) !== -1
                        }
                        onSelect={(value, option) => {
                          const newItems = [...applyFormItems]
                          newItems[index].drugId = value
                          setApplyFormItems(newItems)
                        }}
                        style={{ width: 300 }}
                      />
                    </Form.Item>

                    <Form.Item
                      label="申领数量"
                      style={{ width: 150, marginBottom: 0 }}
                      rules={[
                        { required: true, message: '请输入申领数量' },
                        { type: 'number', min: 1, message: '申领数量必须大于0' },
                      ]}
                    >
                      <InputNumber
                        style={{ width: 150 }}
                        placeholder="数量"
                        min={1}
                        precision={0}
                        onChange={(value) => {
                          const newItems = [...applyFormItems]
                          newItems[index].quantity = value
                          setApplyFormItems(newItems)
                        }}
                      />
                    </Form.Item>

                    <Form.Item
                      label="指定批次（可选）"
                      style={{ width: 200, marginBottom: 0 }}
                    >
                      <Input
                        placeholder="不指定则按FIFO"
                        onChange={(e) => {
                          const newItems = [...applyFormItems]
                          newItems[index].batchNumber = e.target.value
                          setApplyFormItems(newItems)
                        }}
                      />
                    </Form.Item>

                    {applyFormItems.length > 1 && (
                      <Button
                        type="link"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => removeApplyFormItem(index)}
                      >
                        删除
                      </Button>
                    )}
                  </Space>
                </Space>
              </div>
            ))}
            <Button
              type="dashed"
              onClick={addApplyFormItem}
              style={{ width: '100%' }}
            >
              + 添加药品
            </Button>
          </Form.Item>

          <Form.Item
            name="remark"
            label="备注"
            rules={[
              { max: 500, message: '备注长度不能超过500个字符' },
            ]}
          >
            <TextArea
              rows={3}
              placeholder="请输入备注（可选）"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 审批模态框 */}
      <Modal
        title="审批出库申请"
        open={approveModalVisible}
        onCancel={() => {
          setApproveModalVisible(false)
          approveForm.resetFields()
        }}
        onOk={() => approveForm.submit()}
      >
        <Form
          form={approveForm}
          layout="vertical"
          onFinish={handleApprove}
        >
          <Form.Item
            name="secondApproverId"
            label="第二审批人ID（特殊药品需填写）"
            rules={[
              { pattern: /^\d+$/, message: '请输入有效的用户ID' },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="请输入第二审批人ID（特殊药品必填）"
              min={1}
              precision={0}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 执行出库模态框 */}
      <Modal
        title="执行出库"
        open={executeModalVisible}
        onCancel={() => {
          setExecuteModalVisible(false)
          executeForm.resetFields()
          setApplyItems([])
          setInventoryBatches({})
        }}
        onOk={() => executeForm.submit()}
        width={900}
      >
        {currentRecord && (
          <div style={{ marginBottom: 16 }}>
            <p><strong>申领单号：</strong>{currentRecord.applyNumber}</p>
            <p><strong>所属科室：</strong>{currentRecord.department}</p>
            <p><strong>用途：</strong>{currentRecord.purpose}</p>
          </div>
        )}
        
        <Form
          form={executeForm}
          layout="vertical"
          onFinish={handleExecute}
        >
          {applyItems.map((item) => {
            const drug = drugs.find(d => d.id === item.drugId)
            const batches = inventoryBatches[item.drugId] || []
            return (
              <div key={item.id} style={{ marginBottom: 16, padding: 16, border: '1px solid #d9d9d9', borderRadius: 4 }}>
                <Space direction="vertical" style={{ width: '100%' }} size="small">
                  <div>
                    <strong>药品：</strong>{drug ? `${drug.drugName} (${drug.specification || ''})` : `药品ID: ${item.drugId}`}
                  </div>
                  <div>
                    <strong>申请数量：</strong>{item.quantity}
                  </div>
                  <Space wrap>
                    <Form.Item
                      name={[`item_${item.id}`, 'batchNumber']}
                      label="批次号（可选，不指定则按FIFO）"
                      style={{ width: 200, marginBottom: 0 }}
                    >
                      <Select
                        placeholder="选择批次或留空（FIFO）"
                        showSearch
                        allowClear
                      >
                        {batches.map(batch => (
                          <Select.Option key={batch.batchNumber} value={batch.batchNumber}>
                            {batch.batchNumber} (库存: {batch.quantity}, 有效期: {batch.expiryDate ? dayjs(batch.expiryDate).format('YYYY-MM-DD') : '-'})
                          </Select.Option>
                        ))}
                      </Select>
                    </Form.Item>

                    <Form.Item
                      name={[`item_${item.id}`, 'actualQuantity']}
                      label="实际出库数量"
                      rules={[
                        { required: true, message: '请输入实际出库数量' },
                        { type: 'number', min: 1, message: '实际出库数量必须大于0' },
                      ]}
                      style={{ width: 150, marginBottom: 0 }}
                      initialValue={item.quantity}
                    >
                      <InputNumber
                        style={{ width: 150 }}
                        placeholder="实际数量"
                        min={1}
                        max={item.quantity}
                        precision={0}
                      />
                    </Form.Item>
                  </Space>
                </Space>
              </div>
            )
          })}
        </Form>
      </Modal>
    </div>
  )
}

export default OutboundManagement


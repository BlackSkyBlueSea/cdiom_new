import { useState, useEffect, useMemo, useCallback } from 'react'
import { Table, Button, Space, Input, Select, DatePicker, Tag, Modal, Form, message, AutoComplete, InputNumber, Alert, Tooltip } from 'antd'
import { SearchOutlined, ReloadOutlined, PlusOutlined, CheckCircleOutlined, CloseCircleOutlined, PlayCircleOutlined, DeleteOutlined, EyeOutlined, RollbackOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request from '../utils/request'
import logger from '../utils/logger'
import { hasPermission, PERMISSIONS, fetchUserPermissions } from '../utils/permission'
import { getUserRoleId, getUser } from '../utils/auth'

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
  const [approveItems, setApproveItems] = useState([]) // 审批时查看的申请明细
  const [hasSpecialDrug, setHasSpecialDrug] = useState(false) // 是否包含特殊药品
  const [users, setUsers] = useState([]) // 用户列表（用于选择第二审批人）
  const [loadingUsers, setLoadingUsers] = useState(false)
  const [departmentOptions, setDepartmentOptions] = useState([]) // 已有科室列表（新建出库申请时下拉选择）
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [detailItems, setDetailItems] = useState([]) // 查看详情时的申请明细
  const [stockCheckResult, setStockCheckResult] = useState(null) // 审批前库存校验结果 { sufficient, message, details }

  useEffect(() => {
    fetchOutboundApplies()
  }, [pagination.current, pagination.pageSize, filters])

  useEffect(() => {
    fetchDrugs()
    // 如果是仓库管理员，预加载用户列表（用于选择第二审批人）
    const roleId = getUserRoleId()
    if (roleId === 2) {
      fetchUsers()
    }
  }, [])

  // 获取用户列表（用于选择第二审批人）
  const fetchUsers = async () => {
    setLoadingUsers(true)
    try {
      const res = await request.get('/users', {
        params: { page: 1, size: 1000, status: 1 }
      })
      if (res.code === 200) {
        setUsers(res.data.records || [])
      }
    } catch (error) {
      logger.error('获取用户列表失败:', error)
    } finally {
      setLoadingUsers(false)
    }
  }

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
      logger.error('获取药品列表失败:', error)
    }
  }

  // 获取已有科室列表（供新建出库申请时下拉选择，无则仍可手动输入）
  const fetchDepartmentOptions = async () => {
    try {
      const res = await request.get('/outbound/departments')
      if (res.code === 200 && Array.isArray(res.data)) {
        setDepartmentOptions(res.data.filter(Boolean))
      }
    } catch (error) {
      logger.error('获取科室列表失败:', error)
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
            logger.error(`获取药品${item.drugId}的库存批次失败:`, error)
          }
        }
        setInventoryBatches(batchesMap)
      }
    } catch (error) {
      logger.error('获取申请明细失败:', error)
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
      logger.error('获取出库申请失败:', error)
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
    setPagination(prev => ({ ...prev, current: 1 }))
  }

  // 检查申请是否包含特殊药品
  const checkSpecialDrugs = async (applyId) => {
    try {
      const res = await request.get(`/outbound/${applyId}/items`)
      if (res.code === 200) {
        const items = res.data || []
        setApproveItems(items)
        
        // 检查是否包含特殊药品
        let hasSpecial = false
        for (const item of items) {
          const drug = drugs.find(d => d.id === item.drugId)
          if (drug && drug.isSpecial === 1) {
            hasSpecial = true
            break
          }
        }
        setHasSpecialDrug(hasSpecial)
        return hasSpecial
      }
    } catch (error) {
      logger.error('获取申请明细失败:', error)
    }
    return false
  }

  // 审批前拉取库存校验结果（用于弹窗内友好提示，不足时禁止通过）
  const fetchStockCheckForApply = async (applyId) => {
    try {
      const res = await request.get(`/outbound/${applyId}/stock-check`)
      if (res.code === 200 && res.data) {
        setStockCheckResult(res.data)
      } else {
        setStockCheckResult({ sufficient: true, message: '', details: [] })
      }
    } catch (e) {
      logger.error('库存校验请求失败:', e)
      setStockCheckResult({ sufficient: true, message: '', details: [] })
    }
  }

  // 打开查看详情弹窗（拉取申请明细）
  const openDetailModal = async (record) => {
    setCurrentRecord(record)
    setDetailItems([])
    setDetailModalVisible(true)
    try {
      const res = await request.get(`/outbound/${record.id}/items`)
      if (res.code === 200) {
        setDetailItems(res.data || [])
      }
    } catch (e) {
      logger.error('获取申请明细失败:', e)
      message.error('获取申请明细失败')
    }
  }

  // 申请人撤回出库申请（仅待审批状态）
  const handleWithdraw = async (id) => {
    try {
      const res = await request.post(`/outbound/${id}/withdraw`)
      if (res.code === 200) {
        message.success('已撤回')
        setDetailModalVisible(false)
        fetchOutboundApplies()
      } else {
        message.error(res.msg || '撤回失败')
      }
    } catch (error) {
      message.error(error.response?.data?.msg || error.message || '撤回失败')
    }
  }

  const handleApprove = async (values) => {
    try {
      // 如果包含特殊药品，必须填写第二审批人
      if (hasSpecialDrug && !values.secondApproverId) {
        message.error('申请包含特殊药品，必须指定第二审批人')
        return
      }

      const currentUser = getUser()
      // 验证：第一审批人（当前用户）与第二审批人不能是同一人
      if (hasSpecialDrug && values.secondApproverId && currentUser && values.secondApproverId === currentUser.id) {
        message.error('第一审批人和第二审批人不能为同一人')
        return
      }
      // 验证：申请人和审批人不能是同一人（后端也会验证，这里提前提示）
      if (currentRecord && currentRecord.applicantId) {
        if (currentUser && currentUser.id === currentRecord.applicantId) {
          message.error('申请人和审批人不能是同一人')
          return
        }
        if (hasSpecialDrug && values.secondApproverId && values.secondApproverId === currentRecord.applicantId) {
          message.error('特殊药品申请人和第二审批人不能是同一人')
          return
        }
      }

      const res = await request.post(`/outbound/${currentRecord.id}/approve`, {
        secondApproverId: values.secondApproverId || null,
      })
      if (res.code === 200) {
        message.success('审批通过')
        setApproveModalVisible(false)
        approveForm.resetFields()
        setApproveItems([])
        setHasSpecialDrug(false)
        fetchOutboundApplies()
      } else {
        message.error(res.msg || '审批失败')
      }
    } catch (error) {
      const errorMsg = error.response?.data?.msg || error.message || '审批失败'
      message.error(errorMsg)
    }
  }

  const handleReject = async (applyId, reason) => {
    if (!applyId) {
      message.error('无法获取申请信息')
      return
    }
    try {
      const res = await request.post(`/outbound/${applyId}/reject`, {
        rejectReason: reason,
      })
      if (res.code === 200) {
        message.success('已驳回')
        fetchOutboundApplies()
      } else {
        message.error(res.msg || '驳回失败')
      }
    } catch (error) {
      logger.error('驳回失败:', error)
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
      logger.error('创建出库申请失败:', error)
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
      logger.error('出库执行失败:', error)
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
      title: <span style={{ whiteSpace: 'nowrap' }}>ID</span>,
      dataIndex: 'id',
      key: 'id',
      width: 80,
      sorter: (a, b) => a.id - b.id,
      defaultSortOrder: 'ascend',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>申领单号</span>,
      dataIndex: 'applyNumber',
      key: 'applyNumber',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>申请人</span>,
      dataIndex: 'applicantName',
      key: 'applicantName',
      width: 120,
      ellipsis: true,
      render: (name, record) => {
        if (record.applicantRoleName) return `${name || '-'} (${record.applicantRoleName})`
        return name ?? '-'
      },
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>所属科室</span>,
      dataIndex: 'department',
      key: 'department',
      width: 120,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>用途</span>,
      dataIndex: 'purpose',
      key: 'purpose',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>申请状态</span>,
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => getStatusTag(status),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>审批人</span>,
      dataIndex: 'approverName',
      key: 'approverName',
      width: 100,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>审批时间</span>,
      dataIndex: 'approveTime',
      key: 'approveTime',
      width: 180,
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>出库时间</span>,
      dataIndex: 'outboundTime',
      key: 'outboundTime',
      width: 180,
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作</span>,
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => {
        const canApprove = hasPermission([PERMISSIONS.OUTBOUND_APPROVE, PERMISSIONS.OUTBOUND_APPROVE_SPECIAL])
        const canExecute = hasPermission(PERMISSIONS.OUTBOUND_EXECUTE)
        const canViewDetail = hasPermission(PERMISSIONS.OUTBOUND_VIEW) || hasPermission(PERMISSIONS.OUTBOUND_APPLY)
        const currentUser = getUser()
        const isApplicant = currentUser && record.applicantId === currentUser.id
        const canWithdraw = hasPermission(PERMISSIONS.OUTBOUND_APPLY) && record.status === 'PENDING' && isApplicant

        return (
          <Space>
            {canViewDetail && (
              <Tooltip title="查看详情">
                <Button
                  type="link"
                  size="small"
                  icon={<EyeOutlined />}
                  onClick={() => openDetailModal(record)}
                />
              </Tooltip>
            )}
            {canWithdraw && (
              <Tooltip title="撤回申请">
                <Button
                  type="link"
                  size="small"
                  danger
                  icon={<RollbackOutlined />}
                  onClick={() => {
                    Modal.confirm({
                      title: '确认撤回',
                      content: '撤回后该申请将变为已取消，确定要撤回吗？',
                      onOk: () => handleWithdraw(record.id),
                    })
                  }}
                />
              </Tooltip>
            )}
            {record.status === 'PENDING' && canApprove && (
              <>
                <Tooltip title="审批">
                  <Button
                    type="link"
                    size="small"
                    icon={<CheckCircleOutlined />}
                    onClick={async () => {
                      setCurrentRecord(record)
                      setStockCheckResult(null)
                      await checkSpecialDrugs(record.id)
                      await fetchStockCheckForApply(record.id)
                      setApproveModalVisible(true)
                    }}
                  />
                </Tooltip>
                <Tooltip title="驳回">
                  <Button
                    type="link"
                    size="small"
                    danger
                    icon={<CloseCircleOutlined />}
                    onClick={() => {
                      const applyId = record.id
                      Modal.confirm({
                        title: '确认驳回',
                        content: '请输入驳回理由',
                        onOk: (close) => {
                          const reason = prompt('请输入驳回理由:')
                          if (reason) {
                            handleReject(applyId, reason)
                            close()
                          }
                        },
                      })
                    }}
                  />
                </Tooltip>
              </>
            )}
            {record.status === 'APPROVED' && canExecute && (
              <Tooltip title="执行出库">
                <Button
                  type="link"
                  size="small"
                  icon={<PlayCircleOutlined />}
                  onClick={async () => {
                    setCurrentRecord(record)
                    await fetchApplyItems(record.id)
                    setExecuteModalVisible(true)
                  }}
                />
              </Tooltip>
            )}
          </Space>
        )
      },
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0 }}>出库管理</h2>
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
          <Tooltip title="查询">
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={fetchOutboundApplies}
            />
          </Tooltip>
          <Tooltip title="重置">
            <Button icon={<ReloadOutlined />} onClick={handleReset} />
          </Tooltip>
          {hasPermission(PERMISSIONS.OUTBOUND_APPLY) && (
            <Tooltip title="新建出库申请">
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={async () => {
                  await fetchDepartmentOptions()
                  setModalVisible(true)
                  form.resetFields()
                  setApplyFormItems([{ drugId: undefined, quantity: undefined, batchNumber: undefined }])
                }}
              />
            </Tooltip>
          )}
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={outboundApplies}
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
              { required: true, message: '请选择或输入所属科室' },
              { max: 100, message: '所属科室长度不能超过100个字符' },
            ]}
          >
            <AutoComplete
              options={departmentOptions.map((d) => ({ value: d }))}
              placeholder="请选择已有科室或直接输入新科室名称"
              filterOption={(inputValue, option) =>
                (option?.value ?? '').toLowerCase().includes((inputValue || '').toLowerCase())
              }
            />
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
                        value={
                          item.drugId
                            ? (() => {
                                const d = drugs.find((x) => x.id === item.drugId)
                                return d ? `${d.drugName}${d.specification ? ` (${d.specification})` : ''}` : ''
                              })()
                            : undefined
                        }
                        filterOption={(inputValue, option) =>
                          (option?.label ?? '').toString().toUpperCase().indexOf((inputValue || '').toUpperCase()) !== -1
                        }
                        onSelect={(value, option) => {
                          const newItems = [...applyFormItems]
                          newItems[index].drugId = value
                          setApplyFormItems(newItems)
                        }}
                        onChange={(value) => {
                          if (value == null || String(value).trim() === '') {
                            const newItems = [...applyFormItems]
                            newItems[index].drugId = undefined
                            setApplyFormItems(newItems)
                          }
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
                      <Tooltip title="删除">
                        <Button
                          type="link"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={() => removeApplyFormItem(index)}
                        />
                      </Tooltip>
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
          setApproveItems([])
          setHasSpecialDrug(false)
          setStockCheckResult(null)
        }}
        onOk={() => approveForm.submit()}
        okButtonProps={{ disabled: stockCheckResult && stockCheckResult.sufficient === false }}
        width={600}
      >
        {stockCheckResult && stockCheckResult.sufficient === false && (
          <Alert
            message="库存不足，无法审批通过"
            description={
              <div>
                <p style={{ marginBottom: 8 }}>{stockCheckResult.message}</p>
                {Array.isArray(stockCheckResult.details) && stockCheckResult.details.length > 0 && (
                  <ul style={{ marginBottom: 0 }}>
                    {stockCheckResult.details.filter(d => !d.sufficient).map((d, i) => (
                      <li key={i}>
                        {d.drugName}：需要 {d.required}，可用 {d.available}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            }
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}
        {stockCheckResult && stockCheckResult.sufficient === true && (
          <Alert message="当前库存充足，可审批通过。" type="success" showIcon style={{ marginBottom: 16 }} />
        )}
        {currentRecord && (
          <div style={{ marginBottom: 16 }}>
            <p><strong>申领单号：</strong>{currentRecord.applyNumber}</p>
            <p><strong>申请人：</strong>
              {currentRecord.applicantRoleName
                ? `${currentRecord.applicantName || '-'}（${currentRecord.applicantRoleName}）`
                : (currentRecord.applicantName ?? '-')}
            </p>
            <p><strong>所属科室：</strong>{currentRecord.department}</p>
            <p><strong>用途：</strong>{currentRecord.purpose}</p>
          </div>
        )}

        {hasSpecialDrug && (
          <Alert
            message="特殊药品提醒"
            description="此申请包含特殊药品，必须指定第二审批人进行双人审批确认。"
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}

        {approveItems.length > 0 && (
          <div style={{ marginBottom: 16 }}>
            <strong>申请明细：</strong>
            <ul style={{ marginTop: 8, marginBottom: 0 }}>
              {approveItems.map((item, index) => {
                const drug = drugs.find(d => d.id === item.drugId)
                return (
                  <li key={index}>
                    {drug ? `${drug.drugName} (${drug.specification || ''})` : `药品ID: ${item.drugId}`}
                    {' '}× {item.quantity}
                    {drug && drug.isSpecial === 1 && (
                      <Tag color="red" style={{ marginLeft: 8 }}>特殊药品</Tag>
                    )}
                  </li>
                )
              })}
            </ul>
          </div>
        )}

        <Form
          form={approveForm}
          layout="vertical"
          onFinish={handleApprove}
        >
          <Form.Item
            name="secondApproverId"
            label={hasSpecialDrug ? "第二审批人（特殊药品必填）" : "第二审批人（可选）"}
            rules={hasSpecialDrug ? [
              { required: true, message: '特殊药品必须指定第二审批人' },
            ] : []}
          >
            <Select
              placeholder={hasSpecialDrug ? "请选择第二审批人（必填）" : "请选择第二审批人（可选）"}
              loading={loadingUsers}
              showSearch
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
                  options={users
                .filter(user => {
                  const currentUser = getUser()
                  if (currentUser && user.id === currentUser.id) return false // 第二审批人不能选自己（第一审批人）
                  return user.roleId === 2 || user.roleId === 4 // 仓库管理员或医护人员（可能拥有特殊药品审核权限）
                })
                .map(user => ({
                  value: user.id,
                  label: `${user.username} (${user.phone || '无手机号'})`,
                }))}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 查看详情模态框（只读，申请人可在此查看自己申请的内容） */}
      <Modal
        title="出库申请详情"
        open={detailModalVisible}
        onCancel={() => {
          setDetailModalVisible(false)
          setDetailItems([])
        }}
        footer={[
          currentRecord?.status === 'PENDING' && currentRecord?.applicantId === getUser()?.id && hasPermission(PERMISSIONS.OUTBOUND_APPLY) ? (
            <Button
              key="withdraw"
              danger
              icon={<RollbackOutlined />}
              onClick={() => {
                Modal.confirm({
                  title: '确认撤回',
                  content: '撤回后该申请将变为已取消，确定要撤回吗？',
                  onOk: () => handleWithdraw(currentRecord.id),
                })
              }}
            >
              撤回申请
            </Button>
          ) : null,
          <Button key="close" type="primary" onClick={() => { setDetailModalVisible(false); setDetailItems([]) }}>
            关闭
          </Button>,
        ].filter(Boolean)}
        width={560}
      >
        {currentRecord && (
          <div>
            <p><strong>申领单号：</strong>{currentRecord.applyNumber}</p>
            <p><strong>申请人：</strong>
              {currentRecord.applicantRoleName
                ? `${currentRecord.applicantName || '‑'}（${currentRecord.applicantRoleName}）`
                : (currentRecord.applicantName ?? '‑')}
            </p>
            <p><strong>所属科室：</strong>{currentRecord.department}</p>
            <p><strong>用途：</strong>{currentRecord.purpose}</p>
            {currentRecord.remark ? <p><strong>申请备注：</strong>{currentRecord.remark}</p> : null}
            {currentRecord.rejectReason ? <p><strong>审核备注：</strong>{currentRecord.rejectReason}</p> : null}
            <p><strong>申请状态：</strong>{getStatusTag(currentRecord.status)}</p>
            {detailItems.length > 0 && (
              <div style={{ marginTop: 12 }}>
                <strong>申请明细：</strong>
                <ul style={{ marginTop: 8, marginBottom: 0 }}>
                  {detailItems.map((item, index) => {
                    const drug = drugs.find(d => d.id === item.drugId)
                    return (
                      <li key={index}>
                        {drug ? `${drug.drugName} (${drug.specification || ''})` : `药品ID: ${item.drugId}`}
                        {' '}× {item.quantity}
                        {item.remark ? <>（备注：{item.remark}）</> : null}
                        {drug && drug.isSpecial === 1 && (
                          <Tag color="red" style={{ marginLeft: 8 }}>特殊药品</Tag>
                        )}
                      </li>
                    )
                  })}
                </ul>
              </div>
            )}
          </div>
        )}
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


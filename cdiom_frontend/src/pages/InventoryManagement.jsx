import { useState, useEffect, useRef } from 'react'
import { Table, Button, Space, Input, Select, DatePicker, Tag, message, Modal, Form, InputNumber, Alert, Tooltip } from 'antd'
import { SearchOutlined, ReloadOutlined, WarningOutlined, EditOutlined, DownloadOutlined, EnvironmentOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import request, { isPermissionForbiddenError } from '../utils/request'
import { getToken } from '../utils/auth'
import logger from '../utils/logger'
import { hasPermission, PERMISSIONS } from '../utils/permission'
import {
  pageRootStyle,
  tableAreaStyle,
  toolbarRowCompactStyle,
  toolbarPageTitleStyle,
  compactFilterRowStyle,
  filterCellFlex,
  TABLE_SCROLL_Y,
} from '../utils/tablePageLayout'

const { RangePicker } = DatePicker
const { TextArea } = Input

const InventoryManagement = () => {
  const [inventory, setInventory] = useState([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  const [filters, setFilters] = useState({
    keyword: '',
    drugId: undefined,
    expiryDateStart: undefined,
    expiryDateEnd: undefined,
    isSpecial: undefined,
  })
  const [adjustModalVisible, setAdjustModalVisible] = useState(false)
  const [adjustForm] = Form.useForm()
  const [locationModalVisible, setLocationModalVisible] = useState(false)
  const [locationForm] = Form.useForm()
  const [locationRecord, setLocationRecord] = useState(null)
  const [locationSubmitting, setLocationSubmitting] = useState(false)
  const [currentRecord, setCurrentRecord] = useState(null)
  const [drugInfo, setDrugInfo] = useState(null)
  const [users, setUsers] = useState([])
  const [loadingUsers, setLoadingUsers] = useState(false)
  const [adjustmentQuantity, setAdjustmentQuantity] = useState(0)
  const [exporting, setExporting] = useState(false)
  /** 与系统参数一致，供「效期状态」标签展示（仓库角色无 config:manage 时从库存接口拉取） */
  const [expiryThresholds, setExpiryThresholds] = useState({ warning: 180, critical: 90 })
  const fetchInventoryRef = useRef(null)
  const fetchExpiryThresholdsRef = useRef(null)

  const fetchInventory = async () => {
    setLoading(true)
    try {
      const params = {
        page: pagination.current,
        size: pagination.pageSize,
        keyword: filters.keyword || undefined,
        drugId: filters.drugId,
        expiryDateStart: filters.expiryDateStart ? filters.expiryDateStart.format('YYYY-MM-DD') : undefined,
        expiryDateEnd: filters.expiryDateEnd ? filters.expiryDateEnd.format('YYYY-MM-DD') : undefined,
        isSpecial: filters.isSpecial,
      }
      const res = await request.get('/inventory', { params })
      if (res.code === 200) {
        setInventory(res.data.records || [])
        setPagination({
          ...pagination,
          total: res.data.total || 0,
        })
      } else {
        message.error(res.msg || '获取库存列表失败')
      }
    } catch (error) {
      if (isPermissionForbiddenError(error)) {
        setInventory([])
        setPagination((prev) => ({ ...prev, total: 0 }))
        return
      }
      logger.error('获取库存列表失败:', error)
      message.error('获取库存列表失败')
    } finally {
      setLoading(false)
    }
  }
  fetchInventoryRef.current = fetchInventory

  const fetchExpiryThresholds = async () => {
    try {
      const res = await request.get('/inventory/expiry-thresholds')
      if (res.code === 200 && res.data) {
        setExpiryThresholds({
          warning: Number(res.data.expiryWarningDays ?? 180),
          critical: Number(res.data.expiryCriticalDays ?? 90),
        })
      }
    } catch (error) {
      if (!isPermissionForbiddenError(error)) {
        logger.warn('获取效期阈值失败，沿用上次或默认 180/90:', error)
      }
    }
  }
  fetchExpiryThresholdsRef.current = fetchExpiryThresholds

  useEffect(() => {
    fetchExpiryThresholds()
  }, [])

  useEffect(() => {
    fetchInventory()
  }, [pagination.current, pagination.pageSize, filters])

  // 切回本页/本标签时刷新列表，以便执行出库后看到最新库存
  useEffect(() => {
    const onVisibilityChange = () => {
      if (document.visibilityState !== 'visible') return
      if (fetchInventoryRef.current) fetchInventoryRef.current()
      if (fetchExpiryThresholdsRef.current) fetchExpiryThresholdsRef.current()
    }
    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => document.removeEventListener('visibilitychange', onVisibilityChange)
  }, [])

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
      drugId: undefined,
      expiryDateStart: undefined,
      expiryDateEnd: undefined,
      isSpecial: undefined,
    })
    setPagination({ ...pagination, current: 1 })
  }

  const handleExport = async () => {
    setExporting(true)
    try {
      const token = getToken()
      if (!token) {
        message.error('未登录，请重新登录')
        return
      }
      const params = new URLSearchParams()
      if (filters.keyword) {
        params.append('keyword', filters.keyword)
      }
      if (filters.drugId) {
        params.append('drugId', filters.drugId)
      }
      if (filters.expiryDateStart) {
        params.append('expiryDateStart', filters.expiryDateStart.format('YYYY-MM-DD'))
      }
      if (filters.expiryDateEnd) {
        params.append('expiryDateEnd', filters.expiryDateEnd.format('YYYY-MM-DD'))
      }
      if (filters.isSpecial !== undefined) {
        params.append('isSpecial', filters.isSpecial)
      }
      
      const url = `/api/v1/inventory/export?${params.toString()}`
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
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
      link.download = `库存列表_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(downloadUrl)
      
      message.success('导出成功')
    } catch (error) {
      logger.error('导出失败:', error)
      message.error('导出失败: ' + (error.message || '未知错误'))
    } finally {
      setExporting(false)
    }
  }

  const getExpiryWarning = (expiryDate) => {
    if (!expiryDate) return null
    const warn = expiryThresholds.warning
    const crit = expiryThresholds.critical
    const end = dayjs(expiryDate).startOf('day')
    const today = dayjs().startOf('day')
    const days = end.diff(today, 'day')
    if (days < 0) {
      const overdue = -days
      return (
        <Tag color="magenta" icon={<WarningOutlined />}>
          已过期（超期 {overdue} 天）
        </Tag>
      )
    }
    if (days <= crit) {
      return (
        <Tag color="red" icon={<WarningOutlined />}>
          红色预警（≤{crit} 天）
        </Tag>
      )
    }
    if (warn > crit && days <= warn) {
      return (
        <Tag color="orange" icon={<WarningOutlined />}>
          黄色预警（{crit + 1}～{warn} 天）
        </Tag>
      )
    }
    return null
  }

  const handleSearchRefresh = () => {
    fetchExpiryThresholds()
    fetchInventory()
  }

  // 获取用户列表（用于选择第二操作人）
  const fetchUsers = async () => {
    setLoadingUsers(true)
    try {
      const res = await request.get('/inventory/second-operator-candidates')
      if (res.code === 200) {
        setUsers(Array.isArray(res.data) ? res.data : [])
      }
    } catch (error) {
      logger.error('获取第二操作人候选失败:', error)
    } finally {
      setLoadingUsers(false)
    }
  }

  // 获取药品信息
  const fetchDrugInfo = async (drugId) => {
    try {
      const res = await request.get(`/drugs/${drugId}`)
      if (res.code === 200) {
        setDrugInfo(res.data)
        return res.data
      }
    } catch (error) {
      logger.error('获取药品信息失败:', error)
      message.error('获取药品信息失败')
    }
    return null
  }

  const openLocationModal = (record) => {
    setLocationRecord(record)
    locationForm.setFieldsValue({ storageLocation: record.storageLocation || '' })
    setLocationModalVisible(true)
  }

  const handleLocationSubmit = async () => {
    if (!locationRecord) return
    try {
      const values = await locationForm.validateFields()
      const loc = String(values.storageLocation || '').trim()
      if (!loc) {
        message.warning('请输入存储位置')
        return
      }
      setLocationSubmitting(true)
      const res = await request.patch(`/inventory/${locationRecord.id}/storage-location`, {
        storageLocation: loc,
      })
      if (res.code === 200) {
        message.success('存储位置已更新')
        setLocationModalVisible(false)
        locationForm.resetFields()
        setLocationRecord(null)
        fetchInventory()
      } else {
        message.error(res.msg || '更新失败')
      }
    } catch (error) {
      if (error.errorFields) return
      message.error(error.response?.data?.msg || error.message || '更新失败')
    } finally {
      setLocationSubmitting(false)
    }
  }

  // 打开调整弹窗
  const handleAdjust = async (record) => {
    setCurrentRecord(record)
    const drug = await fetchDrugInfo(record.drugId)
    if (!drug) return
    
    adjustForm.setFieldsValue({
      quantityBefore: record.quantity,
      quantityAfter: record.quantity,
      adjustmentReason: '',
      secondOperatorId: undefined,
      remark: '',
    })
    setAdjustmentQuantity(0)
    setAdjustModalVisible(true)
    if (users.length === 0) {
      fetchUsers()
    }
  }

  // 提交调整
  const handleAdjustSubmit = async () => {
    try {
      const values = await adjustForm.validateFields()
      const qtyDiff = values.quantityAfter - values.quantityBefore
      
      // 判断调整类型
      const adjustmentType = qtyDiff > 0 ? 'PROFIT' : qtyDiff < 0 ? 'LOSS' : null
      if (!adjustmentType) {
        message.warning('调整后数量与调整前数量相同，无需调整')
        return
      }

      // 检查特殊药品是否需要第二操作人
      if (drugInfo && drugInfo.isSpecial === 1 && !values.secondOperatorId) {
        message.error('特殊药品库存调整需要第二操作人确认')
        return
      }

      const requestData = {
        drugId: currentRecord.drugId,
        batchNumber: currentRecord.batchNumber,
        adjustmentType: adjustmentType,
        quantityBefore: values.quantityBefore,
        quantityAfter: values.quantityAfter,
        adjustmentReason: values.adjustmentReason,
        secondOperatorId: values.secondOperatorId || null,
        adjustmentImage: null, // 图片上传功能暂不实现
        remark: values.remark || null,
      }

      const res = await request.post('/inventory-adjustments', requestData)
      if (res.code === 200) {
        message.success('库存调整成功')
        setAdjustModalVisible(false)
        adjustForm.resetFields()
        setCurrentRecord(null)
        setDrugInfo(null)
        setAdjustmentQuantity(0)
        fetchInventory()
      } else {
        message.error(res.msg || '库存调整失败')
      }
    } catch (error) {
      if (error.errorFields) {
        return
      }
      const errorMsg = error.response?.data?.msg || error.message || '库存调整失败'
      message.error(errorMsg)
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
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>批次号</span>,
      dataIndex: 'batchNumber',
      key: 'batchNumber',
      width: 120,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>库存数量</span>,
      dataIndex: 'quantity',
      key: 'quantity',
      width: 100,
      align: 'right',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>有效期至</span>,
      dataIndex: 'expiryDate',
      key: 'expiryDate',
      width: 120,
      render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>效期状态</span>,
      key: 'warning',
      width: 190,
      render: (_, record) => getExpiryWarning(record.expiryDate),
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>存储位置</span>,
      dataIndex: 'storageLocation',
      key: 'storageLocation',
      width: 120,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>生产日期</span>,
      dataIndex: 'productionDate',
      key: 'productionDate',
      width: 120,
      render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-',
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>生产厂家</span>,
      dataIndex: 'manufacturer',
      key: 'manufacturer',
      width: 150,
      ellipsis: true,
    },
    {
      title: <span style={{ whiteSpace: 'nowrap' }}>操作</span>,
      key: 'action',
      width: 132,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          {hasPermission(PERMISSIONS.DRUG_MANAGE) && (
            <>
              <Tooltip title="存储位置">
                <Button
                  type="link"
                  size="small"
                  icon={<EnvironmentOutlined />}
                  onClick={() => openLocationModal(record)}
                />
              </Tooltip>
              <Tooltip title="调整数量">
                <Button
                  type="link"
                  size="small"
                  icon={<EditOutlined />}
                  onClick={() => handleAdjust(record)}
                />
              </Tooltip>
            </>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div style={pageRootStyle}>
      <div style={toolbarRowCompactStyle}>
        <h2 style={{ ...toolbarPageTitleStyle, whiteSpace: 'nowrap' }}>库存管理</h2>
        <div style={compactFilterRowStyle}>
          <div style={filterCellFlex('2.2 1 300px', 300, 450)}>
            <Input
              placeholder="搜索药品名称、本位码、批准文号、批次号、存储位置"
              value={filters.keyword}
              onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
              style={{ width: '100%' }}
              allowClear
            />
          </div>
          <div style={{ flex: '0 0 auto', width: 120, minWidth: 80 }}>
            <Select
              placeholder="特殊药品"
              value={filters.isSpecial}
              onChange={(value) => setFilters({ ...filters, isSpecial: value })}
              style={{ width: '100%' }}
              allowClear
            >
              <Select.Option value={1}>是</Select.Option>
              <Select.Option value={0}>否</Select.Option>
            </Select>
          </div>
          <div style={filterCellFlex('1.15 1 100px', 100, 250)}>
            <RangePicker
              placeholder={['有效期开始', '有效期结束']}
              value={filters.expiryDateStart && filters.expiryDateEnd
                ? [filters.expiryDateStart, filters.expiryDateEnd]
                : null}
              onChange={(dates) => {
                setFilters({
                  ...filters,
                  expiryDateStart: dates ? dates[0] : undefined,
                  expiryDateEnd: dates ? dates[1] : undefined,
                })
              }}
              style={{ width: '100%' }}
            />
          </div>
          <Space size={4} style={{ flexShrink: 0 }}>
            <Tooltip title="查询">
              <Button
                type="primary"
                icon={<SearchOutlined />}
                onClick={handleSearchRefresh}
              />
            </Tooltip>
            <Tooltip title="重置">
              <Button icon={<ReloadOutlined />} onClick={handleReset} />
            </Tooltip>
            <Tooltip title="导出Excel">
              <Button
                icon={<DownloadOutlined />}
                onClick={handleExport}
                loading={exporting}
              />
            </Tooltip>
          </Space>
        </div>
      </div>

      <div style={tableAreaStyle}>
        <Table
          columns={columns}
          dataSource={inventory}
          rowKey="id"
          loading={loading}
          size="middle"
          scroll={{ x: 'max-content', y: TABLE_SCROLL_Y }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          onChange={handleTableChange}
        />
      </div>

      {/* 库存调整弹窗 */}
      <Modal
        title="库存调整"
        open={adjustModalVisible}
        onCancel={() => {
          setAdjustModalVisible(false)
          adjustForm.resetFields()
          setCurrentRecord(null)
          setDrugInfo(null)
          setAdjustmentQuantity(0)
        }}
        onOk={handleAdjustSubmit}
        width={600}
        okText="确认调整"
        cancelText="取消"
      >
        {currentRecord && (
          <Form
            form={adjustForm}
            layout="vertical"
            onValuesChange={(changedValues, allValues) => {
              // 实时更新调整数量显示
              if (changedValues.quantityAfter !== undefined || changedValues.quantityBefore !== undefined) {
                const qtyBefore = allValues.quantityBefore || 0
                const qtyAfter = allValues.quantityAfter || 0
                setAdjustmentQuantity(qtyAfter - qtyBefore)
              }
            }}
          >
            <Alert
              message="调整说明"
              description="盘盈：调整后数量 > 调整前数量；盘亏：调整后数量 < 调整前数量"
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />

            {/* 药品信息 */}
            <Form.Item label="药品名称">
              <Input value={currentRecord.drugName} disabled />
            </Form.Item>

            <Form.Item label="批次号">
              <Input value={currentRecord.batchNumber} disabled />
            </Form.Item>

            {/* 调整前数量 */}
            <Form.Item
              name="quantityBefore"
              label="调整前数量"
            >
              <InputNumber
                style={{ width: '100%' }}
                min={0}
                precision={0}
                disabled
              />
            </Form.Item>

            {/* 调整后数量 */}
            <Form.Item
              name="quantityAfter"
              label="调整后数量"
              rules={[
                { required: true, message: '请输入调整后数量' },
                { type: 'number', min: 0, message: '调整后数量不能为负数' },
              ]}
            >
              <InputNumber
                style={{ width: '100%' }}
                min={0}
                precision={0}
                placeholder="请输入调整后数量"
              />
            </Form.Item>

            {/* 调整类型显示 */}
            <Form.Item label="调整类型">
              <Input
                value={
                  adjustmentQuantity > 0
                    ? `盘盈 ${adjustmentQuantity}`
                    : adjustmentQuantity < 0
                    ? `盘亏 ${Math.abs(adjustmentQuantity)}`
                    : '无调整'
                }
                disabled
                style={{
                  color:
                    adjustmentQuantity > 0
                      ? '#52c41a'
                      : adjustmentQuantity < 0
                      ? '#ff4d4f'
                      : '#999',
                }}
              />
            </Form.Item>

            {/* 调整原因 */}
            <Form.Item
              name="adjustmentReason"
              label="调整原因"
              rules={[
                { required: true, message: '请输入调整原因' },
                { max: 500, message: '调整原因长度不能超过500个字符' },
              ]}
            >
              <TextArea
                rows={3}
                placeholder="请输入调整原因（必填）"
                showCount
                maxLength={500}
              />
            </Form.Item>

            {/* 第二操作人（特殊药品必填） */}
            {drugInfo && drugInfo.isSpecial === 1 && (
              <Form.Item
                name="secondOperatorId"
                label="第二操作人（特殊药品必填）"
                rules={[
                  { required: true, message: '特殊药品库存调整需要第二操作人确认' },
                ]}
              >
                <Select
                  placeholder="请选择第二操作人"
                  loading={loadingUsers}
                  showSearch
                  filterOption={(input, option) =>
                    (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                  }
                  options={users.map(user => ({
                    value: user.id,
                    label: `${user.username} (${user.phone || '无手机号'})`,
                  }))}
                />
              </Form.Item>
            )}

            {/* 备注 */}
            <Form.Item
              name="remark"
              label="备注"
            >
              <TextArea
                rows={2}
                placeholder="请输入备注（可选）"
                showCount
                maxLength={500}
              />
            </Form.Item>
          </Form>
        )}
      </Modal>

      <Modal
        title="修改存储位置"
        open={locationModalVisible}
        onCancel={() => {
          setLocationModalVisible(false)
          locationForm.resetFields()
          setLocationRecord(null)
        }}
        onOk={handleLocationSubmit}
        confirmLoading={locationSubmitting}
        okText="保存"
        cancelText="取消"
        destroyOnHidden
      >
        {locationRecord && (
          <Form form={locationForm} layout="vertical">
            <Form.Item label="药品 / 批次">
              <Input value={`${locationRecord.drugName || ''} · ${locationRecord.batchNumber || ''}`} disabled />
            </Form.Item>
            <Form.Item
              name="storageLocation"
              label="存储位置"
              rules={[
                { required: true, message: '请输入存储位置' },
                { max: 200, message: '长度不能超过200个字符' },
              ]}
            >
              <Input placeholder="如：阴凉库A区-3层" allowClear />
            </Form.Item>
          </Form>
        )}
      </Modal>
    </div>
  )
}

export default InventoryManagement


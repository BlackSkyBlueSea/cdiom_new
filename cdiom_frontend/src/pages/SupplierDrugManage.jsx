import { useState, useEffect } from 'react'
import { Table, Button, Space, Input, Modal, Form, message, InputNumber, Popconfirm, Tooltip } from 'antd'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import request from '../utils/request'
import logger from '../utils/logger'
import {
  pageRootStyle,
  tableAreaStyle,
  toolbarRowCompactStyle,
  toolbarPageTitleStyle,
  TABLE_SCROLL_Y,
} from '../utils/tablePageLayout'

/**
 * 供应商可供应药品维护（供应商角色使用）
 * 维护本司可提供的药品及协议价，写入 supplier_drug 表
 */
const SupplierDrugManage = () => {
  const [mySupplier, setMySupplier] = useState(null)
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(false)
  const [addModalVisible, setAddModalVisible] = useState(false)
  const [priceModalVisible, setPriceModalVisible] = useState(false)
  const [addForm] = Form.useForm()
  const [priceForm] = Form.useForm()
  const [editingRecord, setEditingRecord] = useState(null)
  const [allDrugs, setAllDrugs] = useState([])
  const [drugKeyword, setDrugKeyword] = useState('')
  const [addSelectedDrugId, setAddSelectedDrugId] = useState(null)

  useEffect(() => {
    fetchMySupplier()
  }, [])

  useEffect(() => {
    if (mySupplier?.id) {
      fetchMyDrugs()
    }
  }, [mySupplier?.id])

  const fetchMySupplier = async () => {
    try {
      const res = await request.get('/suppliers/me')
      if (res.code === 200 && res.data) {
        setMySupplier(res.data)
      } else {
        setMySupplier(null)
        message.warning('未找到关联的供应商信息，请使用创建该供应商的账号登录')
      }
    } catch (e) {
      logger.error('获取当前供应商失败', e)
      setMySupplier(null)
      message.error('获取当前供应商失败')
    }
  }

  const fetchMyDrugs = async () => {
    if (!mySupplier?.id) return
    setLoading(true)
    try {
      const res = await request.get(`/suppliers/${mySupplier.id}/drugs`, {
        params: { page: 1, size: 10000 }
      })
      if (res.code === 200) {
        setList(res.data?.records || [])
      } else {
        message.error(res.msg || '获取可供应药品列表失败')
      }
    } catch (e) {
      logger.error('获取可供应药品列表失败', e)
      message.error('获取可供应药品列表失败')
    } finally {
      setLoading(false)
    }
  }

  const fetchAllDrugs = async (keyword) => {
    try {
      const res = await request.get('/drugs', { params: { page: 1, size: 5000, keyword: keyword || undefined } })
      if (res.code === 200) {
        setAllDrugs(res.data?.records || [])
      }
    } catch (e) {
      logger.error('获取药品列表失败', e)
    }
  }

  const openAddModal = () => {
    setDrugKeyword('')
    setAllDrugs([])
    setAddSelectedDrugId(null)
    addForm.resetFields()
    setAddModalVisible(true)
    fetchAllDrugs('')
  }

  const handleAddSubmit = async (values) => {
    if (!mySupplier?.id) return
    if (!addSelectedDrugId) {
      message.warning('请从列表中选择一个药品')
      return
    }
    try {
      const res = await request.post('/supplier-drugs', {
        supplierId: mySupplier.id,
        drugId: addSelectedDrugId,
        unitPrice: values.unitPrice
      })
      if (res.code === 200) {
        message.success('添加成功')
        setAddModalVisible(false)
        fetchMyDrugs()
      } else {
        message.error(res.msg || '添加失败')
      }
    } catch (e) {
      logger.error('添加可供应药品失败', e)
      message.error(e?.message || '添加失败')
    }
  }

  const openPriceModal = (record) => {
    setEditingRecord(record)
    priceForm.setFieldsValue({ unitPrice: record.unitPrice })
    setPriceModalVisible(true)
  }

  const handlePriceSubmit = async (values) => {
    if (!mySupplier?.id || !editingRecord?.id) return
    try {
      const res = await request.put('/supplier-drugs/price', {
        supplierId: mySupplier.id,
        drugId: editingRecord.id,
        unitPrice: values.unitPrice
      })
      if (res.code === 200) {
        message.success('协议价已更新')
        setPriceModalVisible(false)
        setEditingRecord(null)
        fetchMyDrugs()
      } else {
        message.error(res.msg || '更新失败')
      }
    } catch (e) {
      logger.error('更新协议价失败', e)
      message.error(e?.message || '更新失败')
    }
  }

  const handleRemove = async (record) => {
    if (!mySupplier?.id) return
    try {
      const res = await request.delete('/supplier-drugs', {
        params: { supplierId: mySupplier.id, drugId: record.id }
      })
      if (res.code === 200) {
        message.success('已移除')
        fetchMyDrugs()
      } else {
        message.error(res.msg || '移除失败')
      }
    } catch (e) {
      logger.error('移除失败', e)
      message.error(e?.message || '移除失败')
    }
  }

  const columns = [
    { title: '药品名称', dataIndex: 'drugName', key: 'drugName', ellipsis: true },
    { title: '规格', dataIndex: 'specification', key: 'specification', width: 120 },
    { title: '国家本位码', dataIndex: 'nationalCode', key: 'nationalCode', width: 140 },
    {
      title: '协议价（元）',
      dataIndex: 'unitPrice',
      key: 'unitPrice',
      width: 120,
      render: (v) => v != null ? Number(v).toFixed(2) : '-'
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_, record) => (
        <Space>
          <Tooltip title="改价">
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openPriceModal(record)} />
          </Tooltip>
          <Popconfirm
            title="确定从可供应列表中移除此药品？"
            onConfirm={() => handleRemove(record)}
          >
            <Tooltip title="移除">
              <Button type="link" size="small" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      )
    }
  ]

  if (mySupplier === null) {
    return (
      <div style={{ padding: 24 }}>
        <p>未找到关联的供应商信息。请使用创建该供应商的账号登录，或联系管理员配置。</p>
        <Tooltip title="重新获取">
          <Button icon={<ReloadOutlined />} onClick={fetchMySupplier} />
        </Tooltip>
      </div>
    )
  }

  return (
    <div style={pageRootStyle}>
      <div style={toolbarRowCompactStyle}>
        <h2 style={{ ...toolbarPageTitleStyle, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          可供应药品维护
          {mySupplier?.name && <span style={{ fontWeight: 'normal', color: '#666', marginLeft: 8 }}>（{mySupplier.name}）</span>}
        </h2>
        <Space style={{ flexShrink: 0 }}>
          <Tooltip title="刷新">
            <Button icon={<ReloadOutlined />} onClick={fetchMyDrugs} />
          </Tooltip>
          <Tooltip title="添加药品">
            <Button type="primary" icon={<PlusOutlined />} onClick={openAddModal} />
          </Tooltip>
        </Space>
      </div>
      <p style={{ color: '#666', marginBottom: 16, flexShrink: 0 }}>
        在此维护本司可提供的药品及协议价。采购专员新建采购订单时，将仅能选择您在此维护的药品，并自动带出协议价（可修改）。
      </p>
      <div style={tableAreaStyle}>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={list}
          loading={loading}
          scroll={{ x: 'max-content', y: TABLE_SCROLL_Y }}
          pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}
        />
      </div>

      <Modal
        title="添加可供应药品"
        open={addModalVisible}
        onCancel={() => setAddModalVisible(false)}
        footer={null}
        width={520}
      >
        <Form form={addForm} layout="vertical" onFinish={handleAddSubmit}>
          <Form.Item label="搜索药品">
            <Input.Search
              placeholder="输入药品名称或本位码后点击搜索"
              allowClear
              onSearch={(v) => { setDrugKeyword(v); fetchAllDrugs(v) }}
            />
          </Form.Item>
          {allDrugs.length > 0 && (
            <>
              <div style={{ marginBottom: 8, color: '#666' }}>从下方点击选择药品{addSelectedDrugId && '（已选）'}</div>
              <div style={{ maxHeight: 280, overflow: 'auto', border: '1px solid #d9d9d9', borderRadius: 4, padding: 8, marginBottom: 16 }}>
                {allDrugs.map((d) => (
                  <div
                    key={d.id}
                    role="button"
                    tabIndex={0}
                    style={{
                      padding: '8px 12px',
                      marginBottom: 4,
                      background: addSelectedDrugId === d.id ? '#e6f7ff' : '#fafafa',
                      borderRadius: 4,
                      cursor: 'pointer',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}
                    onClick={() => setAddSelectedDrugId(d.id)}
                  >
                    <span>{d.drugName} {d.specification ? `（${d.specification}）` : ''}</span>
                    <span style={{ color: '#999', fontSize: 12 }}>{d.nationalCode}</span>
                  </div>
                ))}
              </div>
            </>
          )}
          <Form.Item
            name="unitPrice"
            label="协议价（元）"
            rules={[{ required: true, message: '请输入协议价' }, { type: 'number', min: 0.01, message: '单价须大于 0' }]}
          >
            <InputNumber placeholder="协议价" min={0.01} precision={2} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">确定添加</Button>
              <Button onClick={() => setAddModalVisible(false)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="修改协议价"
        open={priceModalVisible}
        onCancel={() => { setPriceModalVisible(false); setEditingRecord(null) }}
        footer={null}
        width={400}
      >
        {editingRecord && (
          <p style={{ marginBottom: 16 }}>药品：{editingRecord.drugName} {editingRecord.specification ? `（${editingRecord.specification}）` : ''}</p>
        )}
        <Form form={priceForm} layout="vertical" onFinish={handlePriceSubmit}>
          <Form.Item
            name="unitPrice"
            label="协议价（元）"
            rules={[{ required: true, message: '请输入协议价' }, { type: 'number', min: 0.01, message: '单价须大于 0' }]}
          >
            <InputNumber placeholder="协议价" min={0.01} precision={2} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">保存</Button>
              <Button onClick={() => { setPriceModalVisible(false); setEditingRecord(null) }}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default SupplierDrugManage

import { useState, useEffect } from 'react'
import { Modal, Form, Input, Button, message, Space, Tag, Alert } from 'antd'
import { MailOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import request from '../utils/request'
import logger from '../utils/logger'
import { getUser } from '../utils/auth'
import dayjs from 'dayjs'

const SuperAdminModal = ({ open, onCancel, onSuccess }) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [sendingCode, setSendingCode] = useState(false)
  const [status, setStatus] = useState(null)
  const [countdown, setCountdown] = useState(0)
  const isLoggedInSuperAdmin = () => {
    const u = getUser()
    return !!(u && (u.username === 'super_admin' || u.roleId === 6))
  }

  // 隐藏邮箱中间部分，只显示前3个字符和@后面的部分
  const maskEmail = (email) => {
    if (!email) return ''
    const [localPart, domain] = email.split('@')
    if (!domain) return email // 如果格式不正确，返回原邮箱
    
    // 显示前3个字符（如果少于3个字符，显示所有字符）
    const visiblePart = localPart.substring(0, Math.min(3, localPart.length))
    const maskedPart = '***'
    
    return `${visiblePart}${maskedPart}@${domain}`
  }

  useEffect(() => {
    if (open) {
      fetchStatus()
      // 自动填充当前登录用户的邮箱
      const currentUser = getUser()
      if (currentUser && currentUser.email) {
        form.setFieldsValue({ email: currentUser.email })
      } else {
        form.resetFields()
      }
      setCountdown(0)
    }
  }, [open])

  // 倒计时效果
  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000)
      return () => clearTimeout(timer)
    }
  }, [countdown])

  const fetchStatus = async () => {
    try {
      const res = await request.get('/super-admin/status')
      if (res.code === 200) {
        setStatus(res.data)
      }
    } catch (error) {
      logger.error('获取超级管理员状态失败:', error)
    }
  }

  const handleSendCode = async () => {
    try {
      const email = form.getFieldValue('email')
      if (!email) {
        message.warning('请输入邮箱地址')
        return
      }

      // 验证邮箱格式
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(email)) {
        message.error('请输入有效的邮箱地址')
        return
      }

      setSendingCode(true)
      const res = await request.post('/super-admin/send-verification-code', { email })
      if (res.code === 200) {
        message.success('验证码已发送到邮箱，请查收')
        setCountdown(60) // 60秒倒计时
      } else {
        message.error(res.msg || '发送验证码失败')
      }
    } catch (error) {
      logger.error('发送验证码失败:', error)
      message.error(error.response?.data?.msg || error.message || '发送验证码失败')
    } finally {
      setSendingCode(false)
    }
  }

  const handleEnable = async (values) => {
    try {
      setLoading(true)
      const res = await request.post('/super-admin/enable', {
        email: values.email,
        code: values.code,
      })
      if (res.code === 200) {
        message.success('超级管理员已启用')
        form.resetFields()
        setCountdown(0)
        fetchStatus()
        onSuccess?.()
      } else {
        message.error(res.msg || '启用失败')
      }
    } catch (error) {
      logger.error('启用超级管理员失败:', error)
      message.error(error.response?.data?.msg || error.message || '启用失败')
    } finally {
      setLoading(false)
    }
  }

  const handleDisable = async (values) => {
    try {
      setLoading(true)
      const res = await request.post('/super-admin/disable', {
        email: values.email,
        code: values.code,
      })
      if (res.code === 200) {
        message.success('超级管理员已停用')
        form.resetFields()
        setCountdown(0)
        fetchStatus()
        onSuccess?.()
      } else {
        message.error(res.msg || '停用失败')
      }
    } catch (error) {
      logger.error('停用超级管理员失败:', error)
      message.error(error.response?.data?.msg || error.message || '停用失败')
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = () => {
    form.resetFields()
    setCountdown(0)
    onCancel?.()
  }

  return (
    <Modal
      title="超级管理员管理"
      open={open}
      onCancel={handleCancel}
      footer={null}
      width={600}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 状态显示 */}
        {status && (
          <Alert
            message={
              <Space>
                <span>超级管理员状态：</span>
                {status.status === 1 ? (
                  <Tag color="green" icon={<CheckCircleOutlined />}>
                    {status.statusText}
                  </Tag>
                ) : (
                  <Tag color="red" icon={<CloseCircleOutlined />}>
                    {status.statusText}
                  </Tag>
                )}
                <span style={{ color: '#999', fontSize: '12px' }}>
                  （用户名：{status.username}
                  {status.email && `，绑定邮箱：${maskEmail(status.email)}`}
                  ，创建时间：{status.createTime ? dayjs(status.createTime).format('YYYY-MM-DD HH:mm:ss') : '-'}）
                </span>
              </Space>
            }
            type={status.status === 1 ? 'success' : 'warning'}
            showIcon
          />
        )}

        {/* 说明信息 */}
        <Alert
          message="操作说明"
          description={
            <div>
              <p>• 超级管理员（super_admin）拥有所有系统功能和业务功能的权限，主要用于系统测试和维护。</p>
              <p>• 系统投入使用后应停用该用户，后期维护更新测试时可以重新启用。</p>
              <p>• 停用超级管理员须由<strong>其他</strong>具备用户管理权限的账号操作；当前超级管理员账号不能停用自己。</p>
              <p>• 启用/停用操作需要通过邮箱验证码验证，<strong>必须使用当前登录、执行该操作的用户的邮箱</strong>。</p>
              <p>• 验证码将发送到您输入的邮箱地址，验证码有效期为5分钟，请及时使用。</p>
            </div>
          }
          type="info"
          showIcon
        />

        {status?.status === 1 && isLoggedInSuperAdmin() && (
          <Alert
            message="无法停用自己"
            description="您正以超级管理员身份登录，不能在此处停用自己的账号。请使用其他具备用户管理权限的管理员账号登录后操作。"
            type="warning"
            showIcon
          />
        )}

        {/* 操作表单 */}
        <Form
          form={form}
          layout="vertical"
          onFinish={status?.status === 1 ? handleDisable : handleEnable}
        >
          <Form.Item
            name="email"
            label="邮箱地址"
            rules={[
              { required: true, message: '请输入邮箱地址' },
              { type: 'email', message: '请输入有效的邮箱地址' },
            ]}
            help={(() => {
              const currentUser = getUser()
              if (currentUser && currentUser.email) {
                return `请填写当前登录用户的个人完整邮箱：${maskEmail(currentUser.email)}`
              }
              return '请填写当前登录用户的个人完整邮箱'
            })()}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="请填写当前登录用户的个人完整邮箱"
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button
                type="primary"
                icon={<MailOutlined />}
                onClick={handleSendCode}
                loading={sendingCode}
                disabled={countdown > 0}
              >
                {countdown > 0 ? `重新发送(${countdown}秒)` : '发送验证码'}
              </Button>
              {countdown > 0 && (
                <span style={{ color: '#999', fontSize: '12px' }}>
                  验证码已发送，请查收邮箱
                </span>
              )}
            </Space>
          </Form.Item>

          <Form.Item
            name="code"
            label="验证码"
            rules={[
              { required: true, message: '请输入验证码' },
              { pattern: /^\d{6}$/, message: '验证码为6位数字' },
            ]}
          >
            <Input
              placeholder="请输入6位数字验证码"
              maxLength={6}
              style={{ width: '200px' }}
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                danger={status?.status === 1}
                disabled={status?.status === 1 && isLoggedInSuperAdmin()}
              >
                {status?.status === 1 ? '停用超级管理员' : '启用超级管理员'}
              </Button>
              <Button onClick={handleCancel}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Space>
    </Modal>
  )
}

export default SuperAdminModal


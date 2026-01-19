import { useState, useEffect } from 'react'
import { Modal, Form, Input, Button, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import request from '../../utils/request'
import { setToken, setUser } from '../../utils/auth'
import { fetchUserPermissions } from '../../utils/permission'

const LoginModal = ({ open, onClose, onSuccess, multiLogin = false }) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    if (open) {
      form.resetFields()
    }
  }, [open, form])

  const onFinish = async (values) => {
    setLoading(true)
    try {
      const res = await request.post('/auth/login', {
        username: values.username,
        password: values.password,
      })
      if (res.code === 200) {
        // 如果是多用户登录模式，使用sessionStorage
        setToken(res.data.token, multiLogin)
        if (res.data.user) {
          setUser(res.data.user, multiLogin)
        }
        // 获取用户权限
        await fetchUserPermissions()
        message.success('登录成功')
        form.resetFields()
        onClose()
        if (onSuccess) {
          onSuccess(res.data.user)
        } else {
          navigate('/app')
        }
      }
    } catch (error) {
      const errorMsg = error.message || '登录失败'
      message.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      title={multiLogin ? "登录其他账号 - CDIOM 医药管理系统" : "登录 CDIOM 医药管理系统"}
      open={open}
      onCancel={onClose}
      footer={null}
      width={400}
      destroyOnHidden
    >
      <Form
        form={form}
        name="login"
        onFinish={onFinish}
        autoComplete="off"
        size="large"
        layout="vertical"
      >
        <Form.Item
          name="username"
          label="用户名或手机号"
          rules={[{ required: true, message: '请输入用户名或手机号' }]}
        >
          <Input
            prefix={<UserOutlined />}
            placeholder="请输入用户名或手机号"
          />
        </Form.Item>

        <Form.Item
          name="password"
          label="密码"
          rules={[{ required: true, message: '请输入密码' }]}
        >
          <Input.Password
            prefix={<LockOutlined />}
            placeholder="请输入密码"
          />
        </Form.Item>

        <Form.Item>
          <Button
            type="primary"
            htmlType="submit"
            loading={loading}
            block
          >
            登录
          </Button>
        </Form.Item>
      </Form>
      <div style={{ textAlign: 'center', marginTop: 16, color: '#999', fontSize: 12 }}>
        <p>默认账号：admin / admin123</p>
      </div>
    </Modal>
  )
}

export default LoginModal


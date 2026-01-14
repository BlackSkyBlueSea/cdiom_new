import { useState } from 'react'
import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import request from '../utils/request'
import { setToken, setUser } from '../utils/auth'
import { fetchUserPermissions } from '../utils/permission'
import './Login.css'

const Login = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const onFinish = async (values) => {
    setLoading(true)
    try {
      const res = await request.post('/auth/login', {
        username: values.username,
        password: values.password,
      })
      if (res.code === 200) {
        setToken(res.data.token)
        if (res.data.user) {
          setUser(res.data.user)
        }
        // 获取用户权限
        await fetchUserPermissions()
        message.success('登录成功')
        navigate('/dashboard')
      }
    } catch (error) {
      const errorMsg = error.message || '登录失败'
      message.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-container">
      <Card className="login-card" title="CDIOM 临床药品出入库管理系统">
        <Form
          name="login"
          onFinish={onFinish}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名或手机号' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名或手机号"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
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
        <div className="login-tip">
          <p>默认账号：admin / admin123</p>
        </div>
      </Card>
    </div>
  )
}

export default Login



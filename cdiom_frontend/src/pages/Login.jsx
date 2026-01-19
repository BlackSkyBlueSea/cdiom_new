import { useState, useEffect } from 'react'
import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate, useSearchParams } from 'react-router-dom'
import request from '../utils/request'
import { setToken, setUser } from '../utils/auth'
import { fetchUserPermissions } from '../utils/permission'
import './Login.css'

const Login = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const isMultiLogin = searchParams.get('multiLogin') === 'true'

  const onFinish = async (values) => {
    setLoading(true)
    try {
      const res = await request.post('/auth/login', {
        username: values.username,
        password: values.password,
      })
      if (res.code === 200) {
        // 如果是多用户登录模式，使用sessionStorage
        setToken(res.data.token, isMultiLogin)
        if (res.data.user) {
          setUser(res.data.user, isMultiLogin)
        }
        // 获取用户权限
        await fetchUserPermissions()
        message.success('登录成功')
        // 跳转到主应用（如果是多用户登录，URL参数会在跳转时自动清除）
        navigate('/app', { replace: true })
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
      <Card className="login-card" title={isMultiLogin ? "登录其他账号 - CDIOM 临床药品出入库管理系统" : "CDIOM 临床药品出入库管理系统"}>
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



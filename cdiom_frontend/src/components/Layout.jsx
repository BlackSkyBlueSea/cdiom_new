import { useState, useEffect } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout as AntLayout, Menu, Avatar, Dropdown, message, Button } from 'antd'
import {
  DashboardOutlined,
  UserOutlined,
  TeamOutlined,
  SettingOutlined,
  BellOutlined,
  FileTextOutlined,
  LoginOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MedicineBoxOutlined,
} from '@ant-design/icons'
import request from '../utils/request'
import { removeToken, getUser, getUserRoleId } from '../utils/auth'
import './Layout.css'

const { Header, Sider, Content } = AntLayout

const Layout = () => {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const [user, setUserState] = useState(null)
  const [menuItems, setMenuItems] = useState([])

  useEffect(() => {
    const currentUser = getUser()
    setUserState(currentUser)
    const roleId = getUserRoleId()
    
    // 根据角色显示不同菜单
    // 角色ID: 1-系统管理员, 2-仓库管理员, 3-采购专员, 4-医护人员, 5-供应商
    const allMenuItems = [
      {
        key: '/dashboard',
        icon: <DashboardOutlined />,
        label: '仪表盘',
        roles: [1, 2, 3, 4, 5], // 所有角色可见
      },
      {
        key: '/drugs',
        icon: <MedicineBoxOutlined />,
        label: '药品信息管理',
        roles: [1, 2], // 系统管理员和仓库管理员可见
      },
      {
        key: '/users',
        icon: <UserOutlined />,
        label: '用户管理',
        roles: [1], // 仅系统管理员可见
      },
      {
        key: '/roles',
        icon: <TeamOutlined />,
        label: '角色管理',
        roles: [1], // 仅系统管理员可见
      },
      {
        key: '/configs',
        icon: <SettingOutlined />,
        label: '参数配置',
        roles: [1], // 仅系统管理员可见
      },
      {
        key: '/notices',
        icon: <BellOutlined />,
        label: '通知公告',
        roles: [1, 2, 3, 4, 5], // 所有角色可见
      },
      {
        key: '/operation-logs',
        icon: <FileTextOutlined />,
        label: '操作日志',
        roles: [1], // 仅系统管理员可见
      },
      {
        key: '/login-logs',
        icon: <LoginOutlined />,
        label: '登录日志',
        roles: [1], // 仅系统管理员可见
      },
    ]
    
    // 根据角色过滤菜单
    const filteredMenuItems = allMenuItems.filter(item => 
      item.roles.includes(roleId) || roleId === 1 // 系统管理员可以看到所有菜单
    )
    
    setMenuItems(filteredMenuItems)
  }, [])

  const handleMenuClick = ({ key }) => {
    navigate(key)
  }

  const handleLogout = async () => {
    try {
      await request.post('/auth/logout')
      removeToken()
      message.success('登出成功')
      navigate('/login')
    } catch (error) {
      removeToken()
      navigate('/login')
    }
  }

  const userMenuItems = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ]

  return (
    <AntLayout className="layout-container">
      <Sider
        collapsed={collapsed}
        width={150}
        collapsedWidth={64}
        theme="light"
        trigger={null}
      >
        <div className={`logo ${collapsed ? 'logo-collapsed' : ''}`}>
          {collapsed ? 'C' : 'CDIOM系统'}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <AntLayout>
        <Header className="header">
          <div className="header-left">
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              className="trigger-btn"
            />
          </div>
          <div className="header-right">
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <div className="user-info">
                <Avatar icon={<UserOutlined />} />
                <span className="username">{user?.username || '用户'}</span>
              </div>
            </Dropdown>
          </div>
        </Header>
        <Content className="content">
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  )
}

export default Layout



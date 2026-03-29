import { useState, useEffect } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  Layout as AntLayout,
  Menu,
  Avatar,
  Dropdown,
  message,
  Button,
  Modal,
  Descriptions,
  Tooltip,
  Space,
  Tag,
  Spin,
  Typography,
} from 'antd'
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
  InboxOutlined,
  ExportOutlined,
  ShoppingCartOutlined,
  ShopOutlined,
  DatabaseOutlined,
  UserSwitchOutlined,
  DownOutlined,
  InfoCircleOutlined,
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
  const [profileOpen, setProfileOpen] = useState(false)
  const [profileLoading, setProfileLoading] = useState(false)
  const [profileDetail, setProfileDetail] = useState(null)
  const [profilePermissions, setProfilePermissions] = useState([])
  const [adminContact, setAdminContact] = useState(null)

  useEffect(() => {
    const currentUser = getUser()
    setUserState(currentUser)
    const roleId = getUserRoleId()
    
    // 根据角色显示不同菜单
    // 角色ID: 1-系统管理员, 2-仓库管理员, 3-采购专员, 4-医护人员, 5-供应商, 6-超级管理员
    // 系统管理员只负责系统功能，不涉及业务功能
    // 超级管理员拥有所有功能，主要用于系统测试和维护
    // 供应商使用专用菜单（轻量化操作风格）
    const allMenuItems = roleId === 5 ? [
      {
        key: 'supplier-dashboard',
        icon: <DashboardOutlined />,
        label: '工作台',
        roles: [5],
      },
      {
        key: 'supplier-drugs',
        icon: <MedicineBoxOutlined />,
        label: '可供应药品',
        roles: [5],
      },
      {
        key: 'supplier-orders',
        icon: <ShoppingCartOutlined />,
        label: '订单管理',
        roles: [5],
      },
      {
        key: 'notices',
        icon: <BellOutlined />,
        label: '通知公告',
        roles: [5],
      },
    ] : [
      {
        key: 'dashboard',
        icon: <DashboardOutlined />,
        label: '仪表盘',
        roles: [1, 2, 3, 4, 6], // 所有角色可见（除供应商）
      },
      {
        key: 'drugs',
        icon: <MedicineBoxOutlined />,
        label: '药品信息管理',
        roles: [2, 6], // 仓库管理员、超级管理员可见
      },
      {
        key: 'inventory',
        icon: <DatabaseOutlined />,
        label: '库存管理',
        roles: [2, 6], // 仓库管理员、超级管理员可见
      },
      {
        key: 'inbound',
        icon: <InboxOutlined />,
        label: '入库管理',
        roles: [2, 6], // 仓库管理员、超级管理员可见
      },
      {
        key: 'outbound',
        icon: <ExportOutlined />,
        label: '出库管理',
        roles: [2, 4, 6], // 仓库管理员、医护人员、超级管理员可见
      },
      {
        key: 'purchase-orders',
        icon: <ShoppingCartOutlined />,
        label: '采购订单',
        roles: [3, 6], // 采购专员、超级管理员可见
      },
      {
        key: 'suppliers',
        icon: <ShopOutlined />,
        label: '供应商管理',
        roles: [3, 6], // 采购专员、超级管理员可见
      },
      {
        key: 'users',
        icon: <UserOutlined />,
        label: '用户管理',
        roles: [1, 6], // 系统管理员、超级管理员可见
      },
      {
        key: 'roles',
        icon: <TeamOutlined />,
        label: '角色管理',
        roles: [1, 6], // 系统管理员、超级管理员可见
      },
      {
        key: 'configs',
        icon: <SettingOutlined />,
        label: '参数配置',
        roles: [1, 6], // 系统管理员、超级管理员可见
      },
      {
        key: 'notices',
        icon: <BellOutlined />,
        label: '通知公告',
        roles: [1, 2, 3, 4, 5], // 所有角色可见
      },
      {
        key: 'operation-logs',
        icon: <FileTextOutlined />,
        label: '操作日志',
        roles: [1, 6], // 系统管理员、超级管理员可见
      },
      {
        key: 'login-logs',
        icon: <LoginOutlined />,
        label: '登录日志',
        roles: [1, 6], // 系统管理员、超级管理员可见
      },
    ]
    
    // 根据角色过滤菜单
    const filteredMenuItems = allMenuItems.filter(item => 
      item.roles.includes(roleId)
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
      navigate('/')
    } catch (error) {
      removeToken()
      navigate('/')
    }
  }

  const handleLoginOtherUser = () => {
    // 在新标签页中打开登录页面，带multiLogin参数
    const loginUrl = `${window.location.origin}/login?multiLogin=true`
    window.open(loginUrl, '_blank')
    message.info('已在新标签页中打开登录页面，请在新标签页中登录')
  }

  const loadProfileData = async () => {
    setProfileLoading(true)
    try {
      const [curRes, permRes, adminRes] = await Promise.all([
        request.get('/auth/current'),
        request.get('/auth/permissions'),
        request.get('/auth/admin-contact'),
      ])
      if (curRes.code === 200) {
        setProfileDetail(curRes.data)
      } else {
        setProfileDetail(null)
      }
      if (permRes.code === 200) {
        const raw = permRes.data
        setProfilePermissions(Array.isArray(raw) ? raw : [])
      } else {
        setProfilePermissions([])
      }
      if (adminRes.code === 200) {
        setAdminContact(adminRes.data)
      } else {
        setAdminContact(null)
      }
    } catch (e) {
      message.error(e.message || '加载个人信息失败')
      setProfileDetail(null)
      setProfilePermissions([])
      setAdminContact(null)
    } finally {
      setProfileLoading(false)
    }
  }

  const openProfileModal = () => {
    setProfileDetail(null)
    setProfilePermissions([])
    setAdminContact(null)
    setProfileOpen(true)
    loadProfileData()
  }

  const userMenuItems = [
    {
      key: 'loginOther',
      icon: <UserSwitchOutlined />,
      label: '登录其他用户',
      onClick: handleLoginOtherUser,
    },
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
        style={{ overflow: 'auto', height: '100%', WebkitOverflowScrolling: 'touch' }}
      >
        <div className={`logo ${collapsed ? 'logo-collapsed' : ''}`}>
          {collapsed ? 'C' : 'CDIOM系统'}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname.replace('/app/', '')]}
          items={menuItems}
          onClick={handleMenuClick}
          style={{ borderRight: 0 }}
        />
      </Sider>
      <AntLayout style={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0 }}>
        <Header className="header">
          <div className="header-left">
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              className="trigger-btn"
              style={{ WebkitTapHighlightColor: 'transparent' }}
            />
          </div>
          <div className="header-right">
            <div className="header-user-block">
              <div
                className="user-info"
                role="button"
                tabIndex={0}
                onClick={openProfileModal}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault()
                    openProfileModal()
                  }
                }}
              >
                <Avatar icon={<UserOutlined />} />
                <span className="username">{user?.username || '用户'}</span>
              </div>
              <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" trigger={['click']}>
                <Button
                  type="text"
                  size="small"
                  className="user-menu-trigger"
                  icon={<DownOutlined />}
                  aria-label="账号菜单"
                />
              </Dropdown>
            </div>
          </div>
        </Header>
        <Modal
          title={
            <Space>
              <span>个人信息</span>
              <Tooltip
                placement="bottomLeft"
                title={
                  profileLoading ? (
                    <span>正在加载管理员联系方式…</span>
                  ) : !adminContact?.adminUsername && !adminContact?.phone && !adminContact?.email ? (
                    <div style={{ maxWidth: 280 }}>
                      未查询到系统管理员联系方式。如需修改个人信息，请联系本单位信息化或系统运维负责人。
                    </div>
                  ) : (
                    <div style={{ maxWidth: 280 }}>
                      <div>如需修改用户名、手机、邮箱、角色或权限等信息，请联系系统管理员申请。</div>
                      <div style={{ marginTop: 8 }}>
                        管理员账号：{adminContact?.adminUsername || '—'}
                      </div>
                      <div>手机：{adminContact?.phone || '—'}</div>
                      <div>邮箱：{adminContact?.email || '—'}</div>
                    </div>
                  )
                }
              >
                <InfoCircleOutlined style={{ color: 'rgba(0,0,0,0.45)', cursor: 'help' }} />
              </Tooltip>
            </Space>
          }
          open={profileOpen}
          onCancel={() => setProfileOpen(false)}
          footer={null}
          width={560}
          destroyOnClose
        >
          <Spin spinning={profileLoading}>
            {profileDetail ? (
              <>
                <Typography.Paragraph type="secondary" style={{ marginBottom: 16 }}>
                  以下信息仅供查看；修改需由管理员在用户管理中操作。
                </Typography.Paragraph>
                <Descriptions column={1} size="small" bordered>
                  <Descriptions.Item label="用户名">{profileDetail.username || '—'}</Descriptions.Item>
                  <Descriptions.Item label="手机号">{profileDetail.phone || '—'}</Descriptions.Item>
                  <Descriptions.Item label="邮箱">{profileDetail.email || '—'}</Descriptions.Item>
                  <Descriptions.Item label="角色">
                    {profileDetail.roleName || '—'}
                  </Descriptions.Item>
                  <Descriptions.Item label="状态">
                    {profileDetail.status === 1 ? '正常' : profileDetail.status === 0 ? '已禁用' : '—'}
                  </Descriptions.Item>
                  <Descriptions.Item label="权限">
                    {profilePermissions.includes('*') ? (
                      <Typography.Text>全部权限（超级管理员）</Typography.Text>
                    ) : profilePermissions.length === 0 ? (
                      '—'
                    ) : (
                      <div
                        style={{
                          maxHeight: 200,
                          overflow: 'auto',
                          display: 'flex',
                          flexWrap: 'wrap',
                          gap: 6,
                        }}
                      >
                        {[...profilePermissions].sort().map((code) => (
                          <Tag key={code}>{code}</Tag>
                        ))}
                      </div>
                    )}
                  </Descriptions.Item>
                </Descriptions>
              </>
            ) : (
              !profileLoading && <Typography.Text type="secondary">暂无数据</Typography.Text>
            )}
          </Spin>
        </Modal>
        <Content className="content">
          <Outlet />
        </Content>
      </AntLayout>

    </AntLayout>
  )
}

export default Layout



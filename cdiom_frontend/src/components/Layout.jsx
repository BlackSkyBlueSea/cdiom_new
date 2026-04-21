import { useState, useEffect, useRef } from 'react'
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
  Result,
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
import {
  clearPermissionCache,
  fetchUserPermissions,
  hasPermission,
} from '../utils/permission'
import {
  APP_MENU_LABELS,
  APP_MENU_ORDER,
  MENU_ITEM_ROLES,
  canAccessFunctionRoute,
  getPrimaryRouteKey,
} from '../config/appAccessPolicy'
import './Layout.css'

const { Header, Sider, Content } = AntLayout

const MENU_ICONS = {
  dashboard: <DashboardOutlined />,
  drugs: <MedicineBoxOutlined />,
  inventory: <DatabaseOutlined />,
  inbound: <InboxOutlined />,
  outbound: <ExportOutlined />,
  'purchase-orders': <ShoppingCartOutlined />,
  suppliers: <ShopOutlined />,
  users: <UserOutlined />,
  roles: <TeamOutlined />,
  configs: <SettingOutlined />,
  notices: <BellOutlined />,
  'operation-logs': <FileTextOutlined />,
  'login-logs': <LoginOutlined />,
  'supplier-dashboard': <DashboardOutlined />,
  'supplier-drugs': <MedicineBoxOutlined />,
  'supplier-orders': <ShoppingCartOutlined />,
}

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
  const warned403PathRef = useRef('')

  // 侧栏：按角色展示该角色全部导航项；细粒度权限由内容区 canAccessFunctionRoute 拦截
  useEffect(() => {
    let cancelled = false
    ;(async () => {
      if (!getUser()) return
      try {
        await fetchUserPermissions()
      } catch {
        /* 忽略 */
      }
      if (cancelled) return

      setUserState(getUser())
      const roleId = getUserRoleId()

      const visible = APP_MENU_ORDER.filter((key) => {
        const allowedRoles = MENU_ITEM_ROLES[key]
        return Array.isArray(allowedRoles) && allowedRoles.includes(roleId)
      })

      setMenuItems(
        visible.map((key) => ({
          key,
          icon: MENU_ICONS[key],
          label: APP_MENU_LABELS[key] || key,
        })),
      )
    })()
    return () => {
      cancelled = true
    }
  }, [location.pathname])

  // 无权限访问功能页时：403 区 + 顶部友好提示（离开无权限页后再次进入会再次提示）
  useEffect(() => {
    const path = location.pathname
    const routeKey = getPrimaryRouteKey(path)
    if (canAccessFunctionRoute(path, hasPermission, getUserRoleId)) {
      warned403PathRef.current = ''
      return
    }
    if (routeKey === 'dashboard') return
    if (warned403PathRef.current === path) return
    warned403PathRef.current = path
    message.warning(
      '当前账号无访问该功能的权限，页面已拦截。如需开通请联系系统管理员。',
    )
  }, [location.pathname])

  const handleMenuClick = ({ key }) => {
    navigate(key)
  }

  const handleLogout = async () => {
    try {
      await request.post('/auth/logout')
      removeToken()
      clearPermissionCache()
      message.success('登出成功')
      navigate('/')
    } catch (error) {
      removeToken()
      clearPermissionCache()
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
          destroyOnHidden
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
          {canAccessFunctionRoute(location.pathname, hasPermission, getUserRoleId) ? (
            <Outlet />
          ) : (
            <Result
              status="403"
              title="403 权限不足"
              subTitle="您没有访问该功能的权限，无法打开此页面。如需开通相应权限，请联系系统管理员。"
              extra={
                <Button type="primary" onClick={() => navigate('dashboard')}>
                  返回仪表盘
                </Button>
              }
            />
          )}
        </Content>
      </AntLayout>

    </AntLayout>
  )
}

export default Layout



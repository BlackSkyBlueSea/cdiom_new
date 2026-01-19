import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout, Card, Row, Col, Typography, Space, Button, Divider, Dropdown } from 'antd';
import { CheckCircleOutlined, RocketOutlined, ApiOutlined, SafetyOutlined, UserOutlined, LogoutOutlined, UserSwitchOutlined } from '@ant-design/icons';
import { getUserInfo, clearAuth } from '../utils/auth';
import request from '../utils/request';
import { message } from 'antd';
import LoginModal from '../components/common/LoginModal';
import './Home.less';

const { Header, Content, Footer } = Layout;
const { Title, Paragraph, Text } = Typography;

function Home() {
  const navigate = useNavigate();
  const [userInfo, setUserInfo] = useState(getUserInfo());
  const [loginModalVisible, setLoginModalVisible] = useState(false);

  // 监听用户信息变化（登录/登出后刷新）
  useEffect(() => {
    setUserInfo(getUserInfo());
  }, []);

  // 组件挂载时允许页面滚动，卸载时恢复
  useEffect(() => {
    document.body.classList.add('home-page');
    const root = document.getElementById('root');
    if (root) {
      root.classList.add('home-page');
    }
    return () => {
      document.body.classList.remove('home-page');
      if (root) {
        root.classList.remove('home-page');
      }
    };
  }, []);

  const handleLogin = () => {
    setLoginModalVisible(true);
  };

  const handleLoginSuccess = (userData) => {
    // 更新用户信息
    setUserInfo(getUserInfo());
    // 登录成功后跳转到主应用
    navigate('/app');
  };

  const handleLogout = async () => {
    try {
      await request.post('/auth/logout');
      clearAuth();
      setUserInfo(null);
      message.success('已退出登录');
      // 刷新页面以确保状态完全清除
      window.location.reload();
    } catch (error) {
      clearAuth();
      setUserInfo(null);
      window.location.reload();
    }
  };

  const handleLoginOtherUser = () => {
    // 在新标签页中打开登录页面，带multiLogin参数
    const loginUrl = `${window.location.origin}/login?multiLogin=true`;
    window.open(loginUrl, '_blank');
    message.info('已在新标签页中打开登录页面');
  };

  const userMenuItems = [
    {
      key: 'userInfo',
      label: (
        <Space>
          <UserOutlined />
          <span>{userInfo?.username || '用户'}</span>
        </Space>
      ),
      disabled: true,
    },
    {
      type: 'divider',
    },
    {
      key: 'loginOther',
      label: (
        <Space>
          <UserSwitchOutlined />
          <span>登录其他用户</span>
        </Space>
      ),
      onClick: handleLoginOtherUser,
    },
    {
      key: 'logout',
      label: (
        <Space>
          <LogoutOutlined />
          <span>退出登录</span>
        </Space>
      ),
      onClick: handleLogout,
    },
  ];

  const features = [
    {
      icon: <SafetyOutlined style={{ fontSize: 32, color: '#1890ff' }} />,
      title: '合规管理',
      description: '完善的药品合规管理流程，确保符合行业标准',
    },
    {
      icon: <ApiOutlined style={{ fontSize: 32, color: '#52c41a' }} />,
      title: '库存管理',
      description: '实时库存监控，智能预警，高效管理',
    },
    {
      icon: <RocketOutlined style={{ fontSize: 32, color: '#faad14' }} />,
      title: '快速响应',
      description: '快速处理采购、出库等业务流程',
    },
    {
      icon: <CheckCircleOutlined style={{ fontSize: 32, color: '#722ed1' }} />,
      title: '数据追踪',
      description: '完整的操作日志，全程可追溯',
    },
  ];

  return (
    <Layout className="home-layout">
      <Header className="home-header">
        <div className="header-content">
          <Title level={3} style={{ margin: 0, color: '#fff' }}>
            CDIOM 医药管理系统
          </Title>
          <Space>
            {userInfo ? (
              <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                <Button type="primary" ghost icon={<UserOutlined />}>
                  {userInfo.username} ({userInfo.roleDesc || userInfo.roleName || '用户'})
                </Button>
              </Dropdown>
            ) : (
              <Button type="primary" ghost onClick={handleLogin}>
                登录
              </Button>
            )}
          </Space>
        </div>
      </Header>

      <Content className="home-content">
        <div className="hero-section">
          <div className="hero-content">
            <Title level={1} className="hero-title">
              欢迎使用 CDIOM 医药管理系统
            </Title>
            <Paragraph className="hero-description">
              专业的医药库存管理系统，提供完整的药品管理、库存监控、合规追踪等功能
            </Paragraph>
            <Space size="large">
              {userInfo ? (
                <Button type="primary" size="large" onClick={() => navigate('/app')}>
                  进入系统
                </Button>
              ) : (
                <Button type="primary" size="large" onClick={handleLogin}>
                  立即开始
                </Button>
              )}
              <Button size="large" onClick={() => navigate('/backend-monitor')}>
                后端监控中心
              </Button>
            </Space>
          </div>
        </div>

        <Divider />

        <div className="features-section">
          <Title level={2} style={{ textAlign: 'center', marginBottom: 48 }}>
            核心功能
          </Title>
          <Row gutter={[24, 24]}>
            {features.map((feature, index) => (
              <Col xs={24} sm={12} lg={6} key={index}>
                <Card
                  hoverable
                  className="feature-card"
                  style={{
                    height: '100%',
                    textAlign: 'center',
                    borderRadius: 8,
                  }}
                >
                  <div style={{ marginBottom: 16 }}>{feature.icon}</div>
                  <Title level={4}>{feature.title}</Title>
                  <Paragraph type="secondary">{feature.description}</Paragraph>
                </Card>
              </Col>
            ))}
          </Row>
        </div>

        <Divider />

        <div className="status-section">
          <Card>
            <Row gutter={[24, 24]}>
              <Col xs={24} md={12}>
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Title level={4}>系统状态</Title>
                  <Space>
                    <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 20 }} />
                    <Text strong>前端服务运行正常</Text>
                  </Space>
                  <Space>
                    <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 20 }} />
                    <Text>后端API服务运行正常</Text>
                  </Space>
                  <Space>
                    <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 20 }} />
                    <Text>数据库连接正常</Text>
                  </Space>
                </Space>
              </Col>
              <Col xs={24} md={12}>
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Title level={4}>技术栈</Title>
                  <Text>前端：React 18.2.0 + Vite 5.4.8 + Ant Design 5.20.6</Text>
                  <Text>后端：Spring Boot 3.2.8 + MyBatis-Plus 3.5.6</Text>
                  <Text>数据库：MySQL 8.0.37</Text>
                </Space>
              </Col>
            </Row>
          </Card>
        </div>
      </Content>

      <Footer className="home-footer" style={{ textAlign: 'center' }}>
        <Text type="secondary">
          CDIOM 医药管理系统 © 2024 Created by CDIOM Team
        </Text>
      </Footer>

      {/* 登录弹窗 */}
      <LoginModal
        open={loginModalVisible}
        onClose={() => setLoginModalVisible(false)}
        onSuccess={handleLoginSuccess}
      />
    </Layout>
  );
}

export default Home;


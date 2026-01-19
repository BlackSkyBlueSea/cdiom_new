import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Layout,
  Card,
  Row,
  Col,
  Typography,
  Space,
  Button,
  Tag,
  Spin,
  Alert,
  Divider,
  Switch,
  Input,
  message,
  Empty,
  Modal,
} from 'antd';
import {
  ArrowLeftOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LoadingOutlined,
  ClearOutlined,
  CopyOutlined,
  ApiOutlined,
  FileTextOutlined,
  DatabaseOutlined,
  HomeOutlined,
  GlobalOutlined,
  ToolOutlined,
  LinkOutlined,
} from '@ant-design/icons';
import request from '../utils/request';
import './BackendMonitor.less';

const { Header, Content } = Layout;
const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

function BackendMonitor() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState(null);
  const [logs, setLogs] = useState([]);
  const [autoRefresh, setAutoRefresh] = useState(false);
  const [logFilter, setLogFilter] = useState('');
  const [wsConnected, setWsConnected] = useState(false);
  const wsRef = useRef(null);
  const logEndRef = useRef(null);
  const [systemInfo, setSystemInfo] = useState(null);
  const [backendUrl, setBackendUrl] = useState('http://localhost:8080');
  const [linkModalVisible, setLinkModalVisible] = useState(false);
  const [linkModalType, setLinkModalType] = useState('config'); // 'config', 'api', 'monitor'
  const reconnectAttemptsRef = useRef(0);
  const healthCheckTimeoutRef = useRef(null);

  // 组件挂载时允许页面滚动，卸载时恢复
  useEffect(() => {
    document.body.classList.add('backend-monitor-page');
    const root = document.getElementById('root');
    if (root) {
      root.classList.add('backend-monitor-page');
    }
    return () => {
      document.body.classList.remove('backend-monitor-page');
      if (root) {
        root.classList.remove('backend-monitor-page');
      }
    };
  }, []);

  // 滚动到底部
  const scrollToBottom = () => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // 获取后端健康状态（带防抖，避免频繁请求）
  const fetchHealthStatus = async () => {
    // 清除之前的定时器
    if (healthCheckTimeoutRef.current) {
      clearTimeout(healthCheckTimeoutRef.current);
    }
    
    // 防抖：如果500ms内有新的请求，取消之前的请求
    return new Promise((resolve) => {
      healthCheckTimeoutRef.current = setTimeout(async () => {
        try {
          // 使用自定义健康检查端点
          const res = await request.get('/health');
          if (res.code === 200 && res.data) {
            setStatus({
              healthy: res.data.status === 'UP',
              status: res.data.status,
              details: res.data.details || res.data,
            });
          } else {
            setStatus({
              healthy: false,
              status: 'DOWN',
              details: { message: '健康检查失败' },
            });
          }
        } catch (error) {
          // 如果自定义端点失败，尝试访问基础API
          try {
            await request.get('/auth/current');
            setStatus({
              healthy: true,
              status: 'UP',
              details: { message: '后端服务运行正常' },
            });
          } catch (err) {
            setStatus({
              healthy: false,
              status: 'DOWN',
              details: { message: error.message || '无法连接到后端服务' },
            });
          }
        }
        resolve();
      }, 500);
    });
  };

  // 获取系统信息
  const fetchSystemInfo = async () => {
    try {
      const res = await request.get('/system/info');
      if (res.code === 200 && res.data) {
        setSystemInfo(res.data);
        if (res.data.backendUrl) {
          setBackendUrl(res.data.backendUrl);
        }
      } else {
        // 如果API失败，使用默认信息
        const defaultUrl = window.location.origin.replace(':5173', ':8080');
        setSystemInfo({
          backendUrl: defaultUrl,
          apiBaseUrl: '/api/v1',
          timestamp: new Date().toISOString(),
        });
        setBackendUrl(defaultUrl);
      }
    } catch (error) {
      // 如果API失败，使用默认信息
      const defaultUrl = window.location.origin.replace(':5173', ':8080');
      setSystemInfo({
        backendUrl: defaultUrl,
        apiBaseUrl: '/api/v1',
        timestamp: new Date().toISOString(),
      });
      setBackendUrl(defaultUrl);
    }
  };

  // WebSocket连接（原生WebSocket）
  const connectWebSocket = () => {
    // 如果已有连接，先关闭
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    
    try {
      const wsUrl = `ws://localhost:8080/api/v1/logs/stream`;
      const ws = new WebSocket(wsUrl);
      
      ws.onopen = () => {
        setWsConnected(true);
        reconnectAttemptsRef.current = 0; // 连接成功，重置重连次数
        console.log('WebSocket连接成功');
        // 只在首次连接成功时显示消息，避免重复提示
        if (reconnectAttemptsRef.current === 0) {
          message.success('日志流连接成功');
        }
      };
      
      ws.onmessage = (event) => {
        try {
          const logData = JSON.parse(event.data);
          // 过滤心跳消息（不显示）
          if (logData.type === 'heartbeat') {
            return;
          }
          // 确保时间戳格式正确
          if (!logData.timestamp) {
            logData.timestamp = new Date().toISOString();
          }
          addLog(logData);
        } catch (error) {
          // 如果不是JSON，直接作为文本日志
          addLog({
            level: 'INFO',
            message: event.data,
            timestamp: new Date().toISOString(),
          });
        }
      };
      
      ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        setWsConnected(false);
      };
      
      ws.onclose = (event) => {
        setWsConnected(false);
        console.log('WebSocket连接关闭', event.code, event.reason);
        
        // 如果自动刷新开启，尝试重连（指数退避策略）
        if (autoRefresh && event.code !== 1000) { // 1000是正常关闭，不重连
          reconnectAttemptsRef.current += 1;
          const reconnectDelay = Math.min(3000 * reconnectAttemptsRef.current, 30000);
          setTimeout(() => {
            if (autoRefresh) { // 再次检查，防止在等待期间关闭了自动刷新
              console.log(`尝试重连WebSocket (第${reconnectAttemptsRef.current}次)...`);
              connectWebSocket();
            }
          }, reconnectDelay);
        } else if (event.code === 1000) {
          // 正常关闭，重置重连次数
          reconnectAttemptsRef.current = 0;
        }
      };
      
      wsRef.current = ws;
    } catch (error) {
      console.error('WebSocket connection failed:', error);
      setWsConnected(false);
      
      // 如果自动刷新开启，尝试重连
      if (autoRefresh) {
        setTimeout(() => {
          if (autoRefresh) {
            connectWebSocket();
          }
        }, 5000);
      }
    }
  };

  // 添加日志
  const addLog = (logData) => {
    setLogs((prevLogs) => {
      const newLogs = [...prevLogs, logData];
      // 限制日志数量，最多保留1000条
      if (newLogs.length > 1000) {
        return newLogs.slice(-1000);
      }
      return newLogs;
    });
    // 自动滚动到底部
    setTimeout(scrollToBottom, 100);
  };

  // 获取日志（轮询方式，如果WebSocket不可用）
  const fetchLogs = async () => {
    try {
      const res = await request.get('/logs/recent', {
        params: {
          limit: 100,
        },
      });
      
      if (res.code === 200 && res.data && Array.isArray(res.data)) {
        // 智能合并日志：只添加新的日志，避免重复和闪烁
        setLogs(prevLogs => {
          if (prevLogs.length === 0) {
            // 如果没有旧日志，直接设置
            return res.data;
          }
          
          // 找到最后一条日志的时间戳
          const lastLog = prevLogs[prevLogs.length - 1];
          const lastTimestamp = lastLog?.timestamp;
          
          // 过滤出新的日志（时间戳大于最后一条）
          const newLogs = res.data.filter(log => {
            if (!lastTimestamp) return true;
            try {
              const logTime = new Date(log.timestamp).getTime();
              const lastTime = new Date(lastTimestamp).getTime();
              return logTime > lastTime;
            } catch (e) {
              // 如果时间戳解析失败，认为是新日志
              return true;
            }
          });
          
          // 如果有新日志，添加到现有日志后面
          if (newLogs.length > 0) {
            const mergedLogs = [...prevLogs, ...newLogs];
            // 限制日志数量，最多保留1000条
            if (mergedLogs.length > 1000) {
              return mergedLogs.slice(-1000);
            }
            return mergedLogs;
          }
          
          // 如果没有新日志，返回原有日志
          return prevLogs;
        });
      }
    } catch (error) {
      console.error('获取日志失败:', error);
      // 如果后端没有日志API，只在首次失败时添加提示日志
      setLogs(prevLogs => {
        if (prevLogs.length === 0) {
          return [{
            level: 'WARN',
            message: '无法从后端获取日志，请检查后端服务是否正常运行。',
            timestamp: new Date().toISOString(),
          }];
        }
        return prevLogs;
      });
    }
  };

  // 初始化 - 页面加载时立即获取一次数据
  useEffect(() => {
    setLoading(true);
    Promise.all([fetchHealthStatus(), fetchSystemInfo(), fetchLogs()]).finally(() => {
      setLoading(false);
    });
  }, []);

  // 后端状态检测 - 即使未开启自动刷新，也定期检测后端是否启动（每30秒）
  // 这样即使后端未启动，前端也能自动检测到后端启动
  useEffect(() => {
    let statusCheckInterval = null;
    let logUpdateInterval = null;
    
    // 每30秒检测一次后端状态（即使未开启自动刷新）
    // 注意：如果开启了自动刷新，这个检测会被自动刷新的健康检查覆盖，避免重复请求
    const checkBackendStatus = async () => {
      // 如果开启了自动刷新，跳过这个检测（避免重复请求）
      if (autoRefresh) {
        return;
      }
      
      try {
        const res = await request.get('/health');
        if (res.code === 200 && res.data && res.data.status === 'UP') {
          // 检测到后端启动，更新状态并获取系统信息和日志
          setStatus({
            healthy: true,
            status: res.data.status,
            details: res.data.details || res.data,
          });
          // 如果之前未获取到系统信息，现在获取
          fetchSystemInfo();
          fetchLogs();
        } else {
          // 后端未启动或状态异常
          setStatus(prevStatus => {
            // 只在状态变化时更新，避免不必要的重渲染
            if (!prevStatus || prevStatus.healthy) {
              return {
                healthy: false,
                status: 'DOWN',
                details: { message: '后端服务未启动或异常' },
              };
            }
            return prevStatus;
          });
        }
      } catch (error) {
        // 后端未启动，连接失败
        setStatus(prevStatus => {
          if (!prevStatus || prevStatus.healthy) {
            return {
              healthy: false,
              status: 'DOWN',
              details: { message: '无法连接到后端服务，请检查后端是否已启动' },
            };
          }
          return prevStatus;
        });
      }
    };

    // 日志自动更新 - 即使未开启自动刷新，也定期更新日志（每10秒）
    // 这样用户可以看到最新的日志，即使WebSocket未连接
    const updateLogs = () => {
      // 只在WebSocket未连接时通过轮询获取日志
      // 如果WebSocket已连接，日志会通过WebSocket实时推送
      if (!wsConnected && !autoRefresh) { // 如果开启了自动刷新，跳过这个更新
        fetchLogs();
      }
    };

    // 只在未开启自动刷新时执行检测
    if (!autoRefresh) {
      // 立即执行一次检测
      checkBackendStatus();
      
      // 每30秒检测一次后端状态
      statusCheckInterval = setInterval(checkBackendStatus, 30000);
      
      // 每10秒更新一次日志（如果WebSocket未连接）
      logUpdateInterval = setInterval(updateLogs, 10000);
    }
    
    return () => {
      if (statusCheckInterval) {
        clearInterval(statusCheckInterval);
      }
      if (logUpdateInterval) {
        clearInterval(logUpdateInterval);
      }
    };
  }, [wsConnected, autoRefresh]); // 依赖wsConnected和autoRefresh

  // 自动刷新 - 定期获取后端状态、系统信息和日志
  useEffect(() => {
    let interval = null;
    
    if (autoRefresh) {
      // 立即执行一次
      fetchHealthStatus();
      fetchSystemInfo();
      // 如果WebSocket未连接，立即获取一次日志
      if (!wsConnected) {
        fetchLogs();
      }
      
      // 每10秒刷新一次健康状态、系统信息和日志（降低频率，避免资源不足）
      interval = setInterval(() => {
        fetchHealthStatus();
        fetchSystemInfo();
        
        // 如果WebSocket未连接，通过轮询方式获取日志
        if (!wsConnected) {
          fetchLogs();
        }
      }, 10000); // 从5秒改为10秒，减少请求频率
    }
    
    return () => {
      if (interval) {
        clearInterval(interval);
      }
      // 清理健康检查的防抖定时器
      if (healthCheckTimeoutRef.current) {
        clearTimeout(healthCheckTimeoutRef.current);
      }
    };
  }, [autoRefresh, wsConnected]);

  // WebSocket连接管理
  useEffect(() => {
    if (autoRefresh) {
      connectWebSocket();
    } else {
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
      setWsConnected(false);
    }
    
    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [autoRefresh]);

  // 日志滚动
  useEffect(() => {
    scrollToBottom();
  }, [logs]);

  // 格式化日志级别颜色
  const getLogLevelColor = (level) => {
    const levelUpper = level?.toUpperCase() || 'INFO';
    switch (levelUpper) {
      case 'ERROR':
        return 'red';
      case 'WARN':
      case 'WARNING':
        return 'orange';
      case 'DEBUG':
        return 'blue';
      case 'INFO':
      default:
        return 'green';
    }
  };

  // 格式化时间
  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    try {
      // 支持多种时间格式
      let date;
      if (typeof timestamp === 'string') {
        // 尝试解析 "yyyy/MM/dd HH:mm:ss.SSS" 格式
        if (timestamp.match(/^\d{4}\/\d{2}\/\d{2} \d{2}:\d{2}:\d{2}\.\d{3}$/)) {
          // 转换为 ISO 格式
          const isoStr = timestamp.replace(/(\d{4})\/(\d{2})\/(\d{2})/, '$1-$2-$3');
          date = new Date(isoStr);
        } else {
          date = new Date(timestamp);
        }
      } else {
        date = new Date(timestamp);
      }
      
      // 如果日期无效，直接返回原始值
      if (isNaN(date.getTime())) {
        return timestamp;
      }
      
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        fractionalSecondDigits: 3,
      });
    } catch (error) {
      return timestamp;
    }
  };

  // 格式化运行时间
  const formatUptime = (seconds) => {
    if (!seconds) return '未知';
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    const parts = [];
    if (days > 0) parts.push(`${days}天`);
    if (hours > 0) parts.push(`${hours}小时`);
    if (minutes > 0) parts.push(`${minutes}分钟`);
    if (secs > 0 || parts.length === 0) parts.push(`${secs}秒`);
    
    return parts.join(' ');
  };

  // 过滤日志
  const filteredLogs = logs.filter((log) => {
    if (!logFilter) return true;
    const filterLower = logFilter.toLowerCase();
    const message = (log.message || '').toLowerCase();
    const level = (log.level || '').toLowerCase();
    return message.includes(filterLower) || level.includes(filterLower);
  });

  // 清空日志
  const clearLogs = () => {
    setLogs([]);
    message.success('日志已清空');
  };

  // 复制日志
  const copyLogs = () => {
    const logText = logs.map((log) => {
      const time = formatTime(log.timestamp);
      const level = log.level || 'INFO';
      const message = log.message || '';
      return `[${time}] [${level}] ${message}`;
    }).join('\n');
    
    navigator.clipboard.writeText(logText).then(() => {
      message.success('日志已复制到剪贴板');
    }).catch(() => {
      message.error('复制失败');
    });
  };

  // 复制信息
  const copyInfo = (text) => {
    navigator.clipboard.writeText(text).then(() => {
      message.success('已复制到剪贴板');
    }).catch(() => {
      message.error('复制失败');
    });
  };

  // 打开链接模态框
  const openLinkModal = (type) => {
    setLinkModalType(type);
    setLinkModalVisible(true);
  };

  // 获取系统配置信息（基础配置、数据库、环境）
  const getSystemConfigInfo = () => {
    const jvmInfo = systemInfo?.jvm || {};
    const memoryInfo = jvmInfo.memory || {};
    
    return `CDIOM 医药管理系统 - 系统配置信息
==========================================

一、基础配置
------------
应用名称: ${systemInfo?.applicationName || 'cdiom-backend'}
后端地址: ${backendUrl}
API基础路径: /api/v1
前端地址: ${window.location.origin}
WebSocket: ws://localhost:8080/api/v1/logs/stream
服务器端口: ${systemInfo?.serverPort || 8080}
上下文路径: /

二、数据库配置
------------
数据库类型: MySQL
数据库驱动: com.mysql.cj.jdbc.Driver
主机地址: localhost
端口: 3306
数据库名: cdiom_db
字符集: utf8mb4
时区: Asia/Shanghai
连接池: HikariCP
连接池名称: CDIOM-HikariPool
最小空闲连接: 10
最大连接数: 50
连接超时: 30000ms
空闲超时: 600000ms
最大生命周期: 1800000ms

三、系统环境
------------
${jvmInfo.javaVersion ? `Java版本: ${jvmInfo.javaVersion}` : ''}
${jvmInfo.javaVendor ? `Java供应商: ${jvmInfo.javaVendor}` : ''}
${jvmInfo.javaHome ? `Java路径: ${jvmInfo.javaHome}` : ''}
${jvmInfo.osName ? `操作系统: ${jvmInfo.osName} ${jvmInfo.osVersion || ''}` : ''}
${jvmInfo.osArch ? `系统架构: ${jvmInfo.osArch}` : ''}
${jvmInfo.uptime ? `运行时间: ${formatUptime(jvmInfo.uptime)}` : ''}
${jvmInfo.startTime ? `启动时间: ${formatTime(jvmInfo.startTime)}` : ''}
${memoryInfo.usagePercent ? `内存使用率: ${parseFloat(memoryInfo.usagePercent).toFixed(2)}%` : ''}
${memoryInfo.total ? `总内存: ${(memoryInfo.total / 1024 / 1024 / 1024).toFixed(2)} GB` : ''}
${memoryInfo.used ? `已用内存: ${(memoryInfo.used / 1024 / 1024 / 1024).toFixed(2)} GB` : ''}
${memoryInfo.free ? `可用内存: ${(memoryInfo.free / 1024 / 1024 / 1024).toFixed(2)} GB` : ''}

四、服务器配置
------------
Tomcat最大线程数: 200
Tomcat最小空闲线程: 10
Tomcat最大连接数: 10000
Tomcat等待队列: 100
连接超时: 20000ms
文件上传最大大小: 10MB
请求最大大小: 10MB

五、前端应用
------------
前端地址: ${window.location.origin}
开发服务器: http://localhost:5173

技术栈:
- React 18.2.0
- Vite 5.4.8
- Ant Design 5.20.6
- React Router DOM
- Axios

主要页面:
- / - 首页
- /app - 主应用（需要登录）
- /backend-monitor - 后端监控页面
- /login - 登录页面（已集成到首页）

六、日志配置
------------
日志格式: [yyyy-MM-dd HH:mm:ss.SSS] [thread] LEVEL logger - message
日志级别: INFO (root), DEBUG (com.cdiom.backend)
日志输出: 控制台 + 文件 + WebSocket
日志文件: logs/cdiom-backend.log
日志滚动: 按日期和大小滚动，最大100MB，保留30天

生成时间: ${new Date().toLocaleString('zh-CN')}`;
  };

  // 获取API接口文档信息（只包含API相关）
  const getApiDocInfo = () => {
    return `CDIOM 医药管理系统 - API 接口文档
==========================================

一、认证相关接口
------------
POST /api/v1/auth/login - 用户登录（无需认证）
GET /api/v1/auth/current - 获取当前用户信息
GET /api/v1/auth/permissions - 获取当前用户权限列表
POST /api/v1/auth/logout - 用户登出

二、系统监控接口（公开接口）
------------
GET /api/v1/system/info - 获取系统信息
GET /api/v1/health - 健康检查
GET /api/v1/logs/recent - 获取最近日志
WebSocket: ws://localhost:8080/api/v1/logs/stream - 实时日志流

三、药品管理接口
------------
GET /api/v1/drugs - 分页查询药品列表
GET /api/v1/drugs/{id} - 获取药品详情
POST /api/v1/drugs - 新增药品
PUT /api/v1/drugs/{id} - 更新药品
DELETE /api/v1/drugs/{id} - 删除药品
GET /api/v1/drugs/search - 搜索药品
GET /api/v1/drugs/search/name - 按名称搜索
GET /api/v1/drugs/search/approval - 按批准文号搜索
GET /api/v1/drugs/export - 导出药品数据

四、库存管理接口
------------
GET /api/v1/inventory - 分页查询库存列表
GET /api/v1/inventory/{id} - 获取库存详情
GET /api/v1/inventory/near-expiry-warning - 过期预警
GET /api/v1/inventory/total - 库存统计
GET /api/v1/inventory/export - 导出库存数据

五、采购订单接口
------------
GET /api/v1/purchase-orders - 分页查询采购订单
GET /api/v1/purchase-orders/{id} - 获取订单详情
POST /api/v1/purchase-orders - 创建采购订单
PUT /api/v1/purchase-orders/{id} - 更新订单
DELETE /api/v1/purchase-orders/{id} - 删除订单
POST /api/v1/purchase-orders/{id}/status - 更新订单状态
POST /api/v1/purchase-orders/{id}/confirm - 确认订单
POST /api/v1/purchase-orders/{id}/reject - 拒绝订单
POST /api/v1/purchase-orders/{id}/ship - 发货
POST /api/v1/purchase-orders/{id}/cancel - 取消订单
GET /api/v1/purchase-orders/{id}/barcode - 生成条形码
GET /api/v1/purchase-orders/{id}/export - 导出订单

六、入库管理接口
------------
GET /api/v1/inbound - 分页查询入库记录
GET /api/v1/inbound/{id} - 获取入库记录详情
POST /api/v1/inbound/from-order - 从订单入库
POST /api/v1/inbound/temporary - 临时入库
POST /api/v1/inbound/check-expiry - 检查过期
GET /api/v1/inbound/today-count - 今日入库数量

七、出库管理接口
------------
GET /api/v1/outbound - 分页查询出库申请
GET /api/v1/outbound/{id} - 获取出库申请详情
POST /api/v1/outbound - 创建出库申请
POST /api/v1/outbound/{id}/approve - 审批通过
POST /api/v1/outbound/{id}/reject - 审批拒绝
POST /api/v1/outbound/{id}/execute - 执行出库
POST /api/v1/outbound/{id}/cancel - 取消申请
GET /api/v1/outbound/pending-count - 待审批数量

八、供应商管理接口
------------
GET /api/v1/suppliers - 分页查询供应商
GET /api/v1/suppliers/{id} - 获取供应商详情
POST /api/v1/suppliers - 新增供应商
PUT /api/v1/suppliers/{id} - 更新供应商
DELETE /api/v1/suppliers/{id} - 删除供应商
POST /api/v1/suppliers/{id}/status - 更新状态
POST /api/v1/suppliers/{id}/audit - 审核供应商
GET /api/v1/suppliers/{id}/drugs - 获取供应商药品

九、供应商审批接口
------------
POST /api/v1/supplier-approvals - 创建审批申请
POST /api/v1/supplier-approvals/{id}/quality-check - 质量检查
POST /api/v1/supplier-approvals/{id}/price-review - 价格审核
POST /api/v1/supplier-approvals/{id}/final-approve - 最终审批
GET /api/v1/supplier-approvals/{id} - 获取审批详情
GET /api/v1/supplier-approvals/{id}/logs - 获取审批日志

十、供应商药品接口
------------
POST /api/v1/supplier-drugs - 添加供应商药品
DELETE /api/v1/supplier-drugs - 删除供应商药品
PUT /api/v1/supplier-drugs/price - 更新价格

十一、供应商药品协议接口
------------
POST /api/v1/supplier-drug-agreements - 创建协议
GET /api/v1/supplier-drug-agreements/{id} - 获取协议详情
GET /api/v1/supplier-drug-agreements/current - 获取当前协议
GET /api/v1/supplier-drug-agreements/list - 协议列表
PUT /api/v1/supplier-drug-agreements/{id} - 更新协议
DELETE /api/v1/supplier-drug-agreements/{id} - 删除协议

十二、用户管理接口
------------
GET /api/v1/users - 分页查询用户
GET /api/v1/users/{id} - 获取用户详情
POST /api/v1/users - 新增用户
PUT /api/v1/users/{id} - 更新用户
DELETE /api/v1/users/{id} - 删除用户
PUT /api/v1/users/{id}/status - 更新用户状态
PUT /api/v1/users/{id}/unlock - 解锁用户
GET /api/v1/users/deleted - 查询已删除用户
PUT /api/v1/users/{id}/permissions - 更新用户权限

十三、角色管理接口
------------
GET /api/v1/roles - 分页查询角色
GET /api/v1/roles/{id} - 获取角色详情
POST /api/v1/roles - 新增角色
PUT /api/v1/roles/{id} - 更新角色
DELETE /api/v1/roles/{id} - 删除角色
PUT /api/v1/roles/{id}/status - 更新角色状态

十四、系统配置接口
------------
GET /api/v1/configs - 分页查询系统配置
GET /api/v1/configs/{id} - 获取配置详情
GET /api/v1/configs/key/{configKey} - 按键获取配置
POST /api/v1/configs - 新增配置
PUT /api/v1/configs/{id} - 更新配置
DELETE /api/v1/configs/{id} - 删除配置

十五、系统通知接口
------------
GET /api/v1/notices - 分页查询通知
GET /api/v1/notices/{id} - 获取通知详情
POST /api/v1/notices - 新增通知
PUT /api/v1/notices/{id} - 更新通知
DELETE /api/v1/notices/{id} - 删除通知
PUT /api/v1/notices/{id}/status - 更新通知状态

十六、日志接口
------------
GET /api/v1/login-logs - 分页查询登录日志
GET /api/v1/login-logs/{id} - 获取登录日志详情
GET /api/v1/operation-logs - 分页查询操作日志
GET /api/v1/operation-logs/{id} - 获取操作日志详情

十七、仪表盘接口
------------
GET /api/v1/dashboard/statistics - 统计数据
GET /api/v1/dashboard/login-trend - 登录趋势
GET /api/v1/dashboard/operation-statistics - 操作统计
GET /api/v1/dashboard/warehouse - 仓库数据
GET /api/v1/dashboard/purchaser - 采购员数据
GET /api/v1/dashboard/medical-staff - 医务人员数据
GET /api/v1/dashboard/supplier - 供应商数据

十八、文件上传接口
------------
POST /api/v1/upload - 上传文件
DELETE /api/v1/upload - 删除文件

十九、超级管理员接口
------------
POST /api/v1/super-admin/send-verification-code - 发送验证码
POST /api/v1/super-admin/enable - 启用超级管理员
POST /api/v1/super-admin/disable - 禁用超级管理员
GET /api/v1/super-admin/status - 获取状态

二十、库存调整接口
------------
GET /api/v1/inventory-adjustments - 分页查询库存调整
GET /api/v1/inventory-adjustments/{id} - 获取调整详情
POST /api/v1/inventory-adjustments - 创建库存调整

二十一、API文档工具
------------
Swagger UI地址:
- ${backendUrl}/swagger-ui/index.html
- ${backendUrl}/swagger-ui.html

使用说明:
1. Swagger UI提供交互式API文档
2. 可以在Swagger UI中直接测试API接口
3. 部分接口需要认证后才能访问

二十二、认证说明
------------
- 登录接口(/api/v1/auth/login)无需认证
- 监控接口(/api/v1/system/info, /api/v1/health, /api/v1/logs/**)无需认证
- 其他接口需要携带JWT Token
- Token格式: Authorization: Bearer <token>
- Token存储在Cookie中: cdiom_token
- Token有效期: 8小时

二十三、权限说明
------------
- 系统使用基于权限的访问控制
- 每个接口可能需要特定权限
- 权限格式: resource:action (如 drug:view, drug:manage)
- 用户权限通过角色分配

生成时间: ${new Date().toLocaleString('zh-CN')}`;
  };

  // 获取监控工具信息（只包含监控相关）
  const getMonitorInfo = () => {
    return `Spring Actuator 监控工具信息
==========================================

一、主要监控端点
------------
${backendUrl}/actuator - Actuator主页（列出所有可用端点）
${backendUrl}/actuator/health - 健康检查（服务状态）
${backendUrl}/actuator/metrics - 系统指标（性能数据）
${backendUrl}/actuator/info - 应用信息（自定义信息）
${backendUrl}/actuator/env - 环境变量（配置信息）
${backendUrl}/actuator/beans - Bean信息（Spring容器中的Bean）
${backendUrl}/actuator/mappings - 请求映射（URL映射关系）
${backendUrl}/actuator/loggers - 日志配置（日志级别管理）
${backendUrl}/actuator/httptrace - HTTP跟踪（请求追踪）
${backendUrl}/actuator/threaddump - 线程转储（线程信息）
${backendUrl}/actuator/heapdump - 堆转储（内存快照）

二、健康检查端点详解
------------
${backendUrl}/actuator/health - 基础健康检查
${backendUrl}/actuator/health/db - 数据库健康检查
${backendUrl}/actuator/health/diskSpace - 磁盘空间检查
${backendUrl}/actuator/health/ping - 简单ping检查

返回状态:
- UP: 服务正常
- DOWN: 服务异常
- UNKNOWN: 状态未知

三、系统指标端点详解
------------
${backendUrl}/actuator/metrics - 列出所有可用指标
${backendUrl}/actuator/metrics/jvm.memory.used - JVM内存使用
${backendUrl}/actuator/metrics/jvm.memory.max - JVM最大内存
${backendUrl}/actuator/metrics/jvm.threads.live - 活动线程数
${backendUrl}/actuator/metrics/jvm.gc.pause - GC暂停时间
${backendUrl}/actuator/metrics/http.server.requests - HTTP请求统计
${backendUrl}/actuator/metrics/system.cpu.usage - CPU使用率
${backendUrl}/actuator/metrics/process.uptime - 进程运行时间

四、日志管理端点
------------
${backendUrl}/actuator/loggers - 获取所有Logger配置
${backendUrl}/actuator/loggers/{loggerName} - 获取特定Logger配置
POST ${backendUrl}/actuator/loggers/{loggerName} - 动态修改日志级别

请求体示例:
{
  "configuredLevel": "DEBUG"
}

支持的日志级别: TRACE, DEBUG, INFO, WARN, ERROR, OFF

五、环境变量端点
------------
${backendUrl}/actuator/env - 显示所有环境变量和配置属性
${backendUrl}/actuator/env/{propertyName} - 获取特定配置属性
POST ${backendUrl}/actuator/env - 动态修改配置（需要重启生效）

六、请求映射端点
------------
${backendUrl}/actuator/mappings - 显示所有URL映射关系
包括:
- 控制器映射
- 过滤器映射
- 拦截器映射
- 异常处理器映射

七、Bean信息端点
------------
${backendUrl}/actuator/beans - 显示Spring容器中所有Bean的信息
包括:
- Bean名称
- Bean类型
- Bean作用域
- Bean依赖关系

八、使用说明
------------
1. 健康检查端点(/actuator/health)可用于监控服务状态
2. 系统指标端点(/actuator/metrics)提供JVM、内存等性能数据
3. 环境变量端点(/actuator/env)显示应用配置信息
4. 日志配置端点(/actuator/loggers)可动态调整日志级别
5. 请求映射端点(/actuator/mappings)查看所有URL映射
6. Bean信息端点(/actuator/beans)查看Spring容器状态

九、安全建议
------------
- 生产环境建议关闭敏感端点（如/env, /beans）
- 或添加认证保护
- 建议只暴露必要的监控端点（/health, /metrics）
- 使用Spring Security配置端点访问权限
- 定期检查端点访问日志

十、配置说明
------------
在application.yml中配置:
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info  # 只暴露这些端点
        exclude: env,beans  # 排除敏感端点

生成时间: ${new Date().toLocaleString('zh-CN')}`;
  };

  // 下载为Word文档
  const downloadAsWord = (content, filename) => {
    const htmlContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <title>${filename}</title>
        <style>
          body { font-family: 'Microsoft YaHei', Arial, sans-serif; padding: 20px; line-height: 1.6; }
          pre { background: #f5f5f5; padding: 15px; border-radius: 5px; white-space: pre-wrap; }
        </style>
      </head>
      <body>
        <pre>${content.replace(/\n/g, '<br>')}</pre>
      </body>
      </html>
    `;
    
    const blob = new Blob([htmlContent], { type: 'application/msword' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${filename}.doc`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    message.success('Word文档下载成功');
  };

  // 下载为PDF（使用浏览器打印功能）
  const downloadAsPDF = (content, filename) => {
    const printWindow = window.open('', '_blank');
    const htmlContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <title>${filename}</title>
        <style>
          body { font-family: 'Microsoft YaHei', Arial, sans-serif; padding: 20px; line-height: 1.6; }
          pre { background: #f5f5f5; padding: 15px; border-radius: 5px; white-space: pre-wrap; }
          @media print {
            body { margin: 0; padding: 15px; }
          }
        </style>
      </head>
      <body>
        <pre>${content}</pre>
        <script>
          window.onload = function() {
            window.print();
          };
        </script>
      </body>
      </html>
    `;
    
    printWindow.document.write(htmlContent);
    printWindow.document.close();
    message.success('正在打开打印对话框，请选择"另存为PDF"');
  };

  return (
    <Layout className="backend-monitor-layout">
      <Header className="monitor-header">
        <div className="header-content">
          <Space>
            <Button
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate('/')}
            >
              返回首页
            </Button>
            <Title level={4} style={{ margin: 0, color: '#fff', fontWeight: 600 }}>
              <Space>
                <DatabaseOutlined />
                <span>
                  {systemInfo?.applicationName || 'cdiom-backend'} - 后端监控中心
                </span>
              </Space>
            </Title>
          </Space>
          <Space>
            <Switch
              checked={autoRefresh}
              onChange={(checked) => {
                setAutoRefresh(checked);
                if (checked) {
                  message.info('已开启自动刷新，每5秒更新一次状态和系统信息');
                } else {
                  message.info('已关闭自动刷新（后端状态仍会每30秒自动检测）');
                }
              }}
              checkedChildren="自动刷新"
              unCheckedChildren="手动刷新"
            />
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                setLoading(true);
                Promise.all([fetchHealthStatus(), fetchSystemInfo(), fetchLogs()]).finally(() => {
                  setLoading(false);
                });
              }}
              loading={loading}
            >
              刷新
            </Button>
          </Space>
        </div>
      </Header>

      <Content className="monitor-content">
        <Row gutter={[16, 16]}>
          {/* 后端状态卡片 */}
          <Col xs={24} lg={8}>
            <Card 
              title={
                <Space>
                  <CheckCircleOutlined style={{ color: status?.healthy ? '#52c41a' : '#ff4d4f' }} />
                  <span>后端运行状态</span>
                </Space>
              }
              loading={loading}
              className="status-card"
            >
              {status ? (
                <Space direction="vertical" size="large" style={{ width: '100%' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Text strong style={{ fontSize: 14 }}>服务状态：</Text>
                    <Tag
                      color={status.healthy ? 'success' : 'error'}
                      icon={status.healthy ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
                      style={{ 
                        marginLeft: 8,
                        fontSize: 13,
                        padding: '4px 12px',
                        borderRadius: 4
                      }}
                    >
                      {status.status}
                    </Tag>
                  </div>
                  
                  {/* 内存使用信息 */}
                  {systemInfo?.jvm?.memory && (
                    <div style={{ 
                      padding: '12px', 
                      background: '#f5f5f5', 
                      borderRadius: '6px',
                      border: '1px solid #e8e8e8'
                    }}>
                      <Text strong style={{ fontSize: 13, display: 'block', marginBottom: 8 }}>
                        内存使用
                      </Text>
                      <Space direction="vertical" size="small" style={{ width: '100%' }}>
                        {systemInfo.jvm.memory.usagePercent && (
                          <div>
                            <Text type="secondary" style={{ fontSize: 12 }}>使用率：</Text>
                            <Text 
                              style={{ 
                                fontSize: 12, 
                                marginLeft: 4,
                                color: parseFloat(systemInfo.jvm.memory.usagePercent) > 90 ? '#ff4d4f' : 
                                       parseFloat(systemInfo.jvm.memory.usagePercent) > 70 ? '#faad14' : '#52c41a'
                              }}
                            >
                              {parseFloat(systemInfo.jvm.memory.usagePercent).toFixed(2)}%
                            </Text>
                          </div>
                        )}
                        {systemInfo.jvm.memory.total && (
                          <div>
                            <Text type="secondary" style={{ fontSize: 12 }}>总内存：</Text>
                            <Text style={{ fontSize: 12, marginLeft: 4 }}>
                              {(systemInfo.jvm.memory.total / 1024 / 1024 / 1024).toFixed(2)} GB
                            </Text>
                          </div>
                        )}
                        {systemInfo.jvm.memory.used && (
                          <div>
                            <Text type="secondary" style={{ fontSize: 12 }}>已用内存：</Text>
                            <Text style={{ fontSize: 12, marginLeft: 4 }}>
                              {(systemInfo.jvm.memory.used / 1024 / 1024 / 1024).toFixed(2)} GB
                            </Text>
                          </div>
                        )}
                        {systemInfo.jvm.memory.free && (
                          <div>
                            <Text type="secondary" style={{ fontSize: 12 }}>可用内存：</Text>
                            <Text style={{ fontSize: 12, marginLeft: 4 }}>
                              {(systemInfo.jvm.memory.free / 1024 / 1024 / 1024).toFixed(2)} GB
                            </Text>
                          </div>
                        )}
                      </Space>
                    </div>
                  )}
                  
                  {/* 运行时间 */}
                  {systemInfo?.jvm?.uptime && (
                    <div>
                      <Text type="secondary" style={{ fontSize: 12 }}>运行时间：</Text>
                      <Text style={{ fontSize: 12, marginLeft: 4 }}>
                        {formatUptime(systemInfo.jvm.uptime)}
                      </Text>
                    </div>
                  )}
                  
                  {status.details && status.details.message && !systemInfo?.jvm?.memory && (
                    <div>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {status.details.message}
                      </Text>
                    </div>
                  )}
                  
                  <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                    {wsConnected && (
                      <Tag 
                        color="success" 
                        icon={<CheckCircleOutlined />}
                        style={{ borderRadius: 4 }}
                      >
                        WebSocket已连接
                      </Tag>
                    )}
                    {autoRefresh && (
                      <Tag 
                        color="processing"
                        style={{ borderRadius: 4 }}
                      >
                        自动刷新中
                      </Tag>
                    )}
                  </div>
                </Space>
              ) : (
                <Empty description="无法获取状态" image={Empty.PRESENTED_IMAGE_SIMPLE} />
              )}
            </Card>
          </Col>

          {/* 系统信息卡片 */}
          <Col xs={24} lg={8}>
            <Card 
              title={
                <Space>
                  <DatabaseOutlined />
                  <span>系统运行信息</span>
                </Space>
              }
              loading={loading}
              className="system-info-card"
            >
              {systemInfo ? (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  {/* JVM信息 */}
                  {systemInfo.jvm && (
                    <>
                      <Divider style={{ margin: '8px 0' }} />
                      <div>
                        <Text strong style={{ fontSize: 13, display: 'block', marginBottom: 8 }}>
                          JVM 信息
                        </Text>
                        <Space direction="vertical" size="small" style={{ width: '100%' }}>
                          <div>
                            <Text type="secondary" style={{ fontSize: 12 }}>Java版本：</Text>
                            <Text style={{ fontSize: 12, marginLeft: 4 }}>
                              {systemInfo.jvm.javaVersion || '未知'}
                            </Text>
                          </div>
                          {systemInfo.jvm.javaVendor && (
                            <div>
                              <Text type="secondary" style={{ fontSize: 12 }}>Java供应商：</Text>
                              <Text style={{ fontSize: 12, marginLeft: 4 }}>
                                {systemInfo.jvm.javaVendor}
                              </Text>
                            </div>
                          )}
                        </Space>
                      </div>
                      
                      {/* 操作系统信息 */}
                      {systemInfo.jvm.osName && (
                        <div style={{ 
                          padding: '12px', 
                          background: '#f5f5f5', 
                          borderRadius: '6px',
                          border: '1px solid #e8e8e8'
                        }}>
                          <Text strong style={{ fontSize: 13, display: 'block', marginBottom: 8 }}>
                            操作系统
                          </Text>
                          <Space direction="vertical" size="small" style={{ width: '100%' }}>
                            <div>
                              <Text type="secondary" style={{ fontSize: 12 }}>系统：</Text>
                              <Text style={{ fontSize: 12, marginLeft: 4 }}>
                                {systemInfo.jvm.osName} {systemInfo.jvm.osVersion || ''}
                              </Text>
                            </div>
                            {systemInfo.jvm.osArch && (
                              <div>
                                <Text type="secondary" style={{ fontSize: 12 }}>架构：</Text>
                                <Text style={{ fontSize: 12, marginLeft: 4 }}>
                                  {systemInfo.jvm.osArch}
                                </Text>
                              </div>
                            )}
                          </Space>
                        </div>
                      )}
                      
                    </>
                  )}
                  
                  {systemInfo.timestamp && (
                    <div>
                      <Text type="secondary" style={{ fontSize: 11 }}>
                        更新时间：{formatTime(systemInfo.timestamp)}
                      </Text>
                    </div>
                  )}
                </Space>
              ) : (
                <Empty description="无法获取系统信息" image={Empty.PRESENTED_IMAGE_SIMPLE} />
              )}
            </Card>
          </Col>

          {/* 相关链接卡片 */}
          <Col xs={24} lg={8}>
            <Card 
              title={
                <Space>
                  <LinkOutlined />
                  <span>快速链接</span>
                </Space>
              }
            >
              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                <Button
                  block
                  icon={<DatabaseOutlined />}
                  onClick={() => openLinkModal('config')}
                >
                  系统配置信息
                </Button>
                <Button
                  block
                  icon={<ApiOutlined />}
                  onClick={() => openLinkModal('api')}
                >
                  API 接口文档
                </Button>
                <Button
                  block
                  icon={<ToolOutlined />}
                  onClick={() => openLinkModal('monitor')}
                >
                  监控工具信息
                </Button>
              </Space>
            </Card>
          </Col>
        </Row>

        <Divider />

        {/* 日志显示区域 */}
        <Card
          title={
            <Space>
              <FileTextOutlined />
              <span>后端日志</span>
              <Tag 
                color={wsConnected ? 'success' : 'default'}
                style={{ borderRadius: 4 }}
              >
                {wsConnected ? '实时推送' : '静态显示'}
              </Tag>
              <Text type="secondary" style={{ fontSize: 12 }}>
                ({filteredLogs.length} / {logs.length} 条)
              </Text>
            </Space>
          }
          extra={
            <Space wrap>
              <Input
                placeholder="过滤日志（关键词/级别）..."
                value={logFilter}
                onChange={(e) => setLogFilter(e.target.value)}
                style={{ width: 220 }}
                allowClear
                prefix={<span style={{ color: '#999' }}>🔍</span>}
              />
              <Button 
                icon={<ClearOutlined />} 
                onClick={clearLogs}
                danger
              >
                清空
              </Button>
              <Button 
                icon={<CopyOutlined />} 
                onClick={copyLogs}
                type="default"
              >
                复制
              </Button>
            </Space>
          }
          className="log-card"
        >
          <div className="log-container">
            {filteredLogs.length === 0 ? (
              <Empty description="暂无日志" />
            ) : (
              <div className="log-content">
                {filteredLogs.map((log, index) => {
                  const logLevel = (log.level || 'INFO').toUpperCase();
                  return (
                    <div 
                      key={index} 
                      className="log-item" 
                      data-level={logLevel}
                    >
                      <Tag color={getLogLevelColor(log.level)} style={{ marginRight: 8, minWidth: 60 }}>
                        {log.level || 'INFO'}
                      </Tag>
                      <Text type="secondary" style={{ marginRight: 8, fontSize: 12, minWidth: 180 }}>
                        {formatTime(log.timestamp)}
                      </Text>
                      <Text 
                        style={{ 
                          flex: 1, 
                          whiteSpace: 'pre-wrap',
                          wordBreak: 'break-word',
                          fontFamily: 'monospace',
                          fontSize: 13,
                          lineHeight: 1.6,
                          color: logLevel === 'ERROR' ? '#ff7875' : 
                                 logLevel === 'WARN' ? '#faad14' : 
                                 logLevel === 'DEBUG' ? '#69c0ff' : '#d4d4d4'
                        }}
                      >
                        {log.message || JSON.stringify(log)}
                      </Text>
                    </div>
                  );
                })}
                <div ref={logEndRef} />
              </div>
            )}
          </div>
        </Card>

        {!wsConnected && logs.length === 0 && (
          <Alert
            message="提示"
            description="后端日志API不可用。如需实时日志功能，请配置后端日志流服务（WebSocket或SSE）。当前显示模拟日志。"
            type="info"
            showIcon
            style={{ marginTop: 16 }}
          />
        )}

        {/* 链接信息模态框 */}
        <Modal
          title={
            <Space>
              {linkModalType === 'config' ? <DatabaseOutlined /> : 
               linkModalType === 'api' ? <ApiOutlined /> : <ToolOutlined />}
              <span>
                {linkModalType === 'config' ? '系统配置信息' : 
                 linkModalType === 'api' ? 'API 接口文档' : '监控工具信息'}
              </span>
            </Space>
          }
          open={linkModalVisible}
          onCancel={() => setLinkModalVisible(false)}
          footer={[
            <Button key="copy" icon={<CopyOutlined />} onClick={() => {
              const text = linkModalType === 'config' ? getSystemConfigInfo() :
                          linkModalType === 'api' ? getApiDocInfo() : getMonitorInfo();
              copyInfo(text);
            }}>
              复制
            </Button>,
            <Button key="word" icon={<FileTextOutlined />} onClick={() => {
              const text = linkModalType === 'config' ? getSystemConfigInfo() :
                          linkModalType === 'api' ? getApiDocInfo() : getMonitorInfo();
              const filename = linkModalType === 'config' ? '系统配置信息' :
                              linkModalType === 'api' ? 'API接口文档' : '监控工具信息';
              downloadAsWord(text, filename);
            }}>
              下载Word
            </Button>,
            <Button key="pdf" type="primary" icon={<FileTextOutlined />} onClick={() => {
              const text = linkModalType === 'config' ? getSystemConfigInfo() :
                          linkModalType === 'api' ? getApiDocInfo() : getMonitorInfo();
              const filename = linkModalType === 'config' ? '系统配置信息' :
                              linkModalType === 'api' ? 'API接口文档' : '监控工具信息';
              downloadAsPDF(text, filename);
            }}>
              下载PDF
            </Button>,
            <Button key="close" onClick={() => setLinkModalVisible(false)}>
              关闭
            </Button>,
          ]}
          width={700}
        >
          <div style={{ marginBottom: 16 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {linkModalType === 'config' 
                ? '以下信息包含系统基础配置、数据库配置、系统环境等完整信息' 
                : linkModalType === 'api'
                ? '以下信息包含API接口端点和Swagger UI文档工具的访问地址及说明'
                : '以下信息包含Spring Actuator监控工具的访问地址及使用说明'}
            </Text>
          </div>
          <TextArea
            value={linkModalType === 'config' ? getSystemConfigInfo() :
                   linkModalType === 'api' ? getApiDocInfo() : getMonitorInfo()}
            readOnly
            rows={linkModalType === 'config' ? 20 : 15}
            style={{
              fontFamily: 'monospace',
              fontSize: 13,
              backgroundColor: '#f5f5f5',
              cursor: 'text',
            }}
            onClick={(e) => {
              e.target.select();
              copyInfo(e.target.value);
            }}
          />
          <div style={{ marginTop: 12 }}>
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                💡 提示：
              </Text>
              <Text type="secondary" style={{ fontSize: 12 }}>
                • 点击文本框可全选并自动复制
              </Text>
              <Text type="secondary" style={{ fontSize: 12 }}>
                • 点击"下载Word"可保存为Word文档
              </Text>
              <Text type="secondary" style={{ fontSize: 12 }}>
                • 点击"下载PDF"会打开打印对话框，选择"另存为PDF"即可保存
              </Text>
            </Space>
          </div>
        </Modal>
      </Content>
    </Layout>
  );
}

export default BackendMonitor;


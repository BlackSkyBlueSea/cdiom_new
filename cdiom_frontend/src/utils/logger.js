/**
 * 统一日志工具
 * 开发环境：输出所有日志
 * 生产环境：禁用console输出，避免泄露敏感信息和影响性能
 */

const isDev = import.meta.env.DEV;

export const logger = {
  /**
   * 普通日志
   */
  log: (...args) => {
    if (isDev) {
      console.log(...args);
    }
  },

  /**
   * 错误日志
   */
  error: (...args) => {
    if (isDev) {
      console.error(...args);
    }
  },

  /**
   * 警告日志
   */
  warn: (...args) => {
    if (isDev) {
      console.warn(...args);
    }
  },

  /**
   * 信息日志
   */
  info: (...args) => {
    if (isDev) {
      console.info(...args);
    }
  },

  /**
   * 调试日志
   */
  debug: (...args) => {
    if (isDev) {
      console.debug(...args);
    }
  },
};

export default logger;


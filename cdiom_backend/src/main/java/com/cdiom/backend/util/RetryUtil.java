package com.cdiom.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

import java.util.concurrent.TimeUnit;

/**
 * 单号生成重试工具类
 * 用于解决并发场景下单号重复的问题
 * 
 * @author cdiom
 */
@Slf4j
public class RetryUtil {
    // 默认重试次数（3次足够应对绝大多数并发场景）
    private static final int DEFAULT_RETRY_TIMES = 3;
    // 默认重试间隔（50毫秒，避免高频重试加剧数据库压力）
    private static final long DEFAULT_RETRY_INTERVAL = 50;

    /**
     * 执行带重试的任务
     * @param task 待执行任务
     * @param <T> 任务返回值类型
     * @return 任务执行结果
     * @throws Exception 超出重试次数后抛出异常
     */
    public static <T> T executeWithRetry(RetryTask<T> task) throws Exception {
        return executeWithRetry(task, DEFAULT_RETRY_TIMES, DEFAULT_RETRY_INTERVAL);
    }

    /**
     * 执行带重试的任务（自定义参数）
     * @param task 待执行任务
     * @param retryTimes 重试次数
     * @param retryInterval 重试间隔（毫秒）
     * @param <T> 任务返回值类型
     * @return 任务执行结果
     * @throws Exception 超出重试次数后抛出异常
     */
    public static <T> T executeWithRetry(RetryTask<T> task, int retryTimes, long retryInterval) throws Exception {
        int currentRetry = 0;
        Exception lastException = null;

        do {
            try {
                // 执行核心任务（生成单号 + 插入数据）
                return task.execute();
            } catch (DuplicateKeyException e) {
                // 捕获唯一索引冲突异常（仅处理单号重复场景）
                lastException = e;
                currentRetry++;
                if (currentRetry > retryTimes) {
                    log.error("超出最大重试次数（{}次），单号生成失败", retryTimes, e);
                    break;
                }
                log.warn("单号重复，正在进行第{}次重试...", currentRetry);
                // 重试间隔，避免高频竞争
                try {
                    TimeUnit.MILLISECONDS.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new Exception("重试过程被中断", ie);
                }
            }
        } while (currentRetry <= retryTimes);

        throw new RuntimeException("单号生成失败，并发冲突过高，已重试" + retryTimes + "次", lastException);
    }

    /**
     * 重试任务接口
     * @param <T> 任务返回值类型
     */
    @FunctionalInterface
    public interface RetryTask<T> {
        T execute() throws Exception;
    }
}



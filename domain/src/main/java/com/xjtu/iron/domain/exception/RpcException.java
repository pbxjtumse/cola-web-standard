package com.xjtu.iron.domain.exception;


/**
 * RPC 调用异常示例。
 *
 * <p>
 * 这个类也属于业务系统或 RPC 客户端，不属于 concurrency-component。
 * </p>
 */
public class RpcException extends RuntimeException {

    /**
     * 远程服务名。
     */
    private final String remoteService;

    public RpcException(String remoteService, String message, Throwable cause) {
        super(message, cause);
        this.remoteService = remoteService;
    }

    public String getRemoteService() {
        return remoteService;
    }
}

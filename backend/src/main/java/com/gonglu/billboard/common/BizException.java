package com.gonglu.billboard.common;

/**
 * 业务异常：消息直接返回给前端展示
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}

package com.gonglu.billboard.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体：code=0 成功，非 0 失败
 */
@Data
public class R<T> implements Serializable {

    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 0;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static R<Void> ok() {
        return ok(null);
    }

    public static <T> R<T> error(String msg) {
        return error(1, msg);
    }

    public static <T> R<T> error(int code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}

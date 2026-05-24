package com.dave.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 项目统一返回结果实体
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功码
     */
    public static final int SUCCESS = 200;

    /**
     * 失败码
     */
    public static final int FAIL = 500;

    /**
     * 状态码
     */
    private int code;

    /**
     * 返回消息
     */
    private String msg;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功返回
     */
    public static <T> R<T> success() {
        return restResult(null, SUCCESS, "操作成功");
    }

    public static <T> R<T> success(T data) {
        return restResult(data, SUCCESS, "操作成功");
    }

    public static <T> R<T> success(T data, String msg) {
        return restResult(data, SUCCESS, msg);
    }

    /**
     * 失败返回
     */
    public static <T> R<T> fail() {
        return restResult(null, FAIL, "操作失败");
    }

    public static <T> R<T> fail(String msg) {
        return restResult(null, FAIL, msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        return restResult(null, code, msg);
    }

    public static <T> R<T> fail(T data) {
        return restResult(data, FAIL, "操作失败");
    }

    public static <T> R<T> fail(T data, String msg) {
        return restResult(data, FAIL, msg);
    }

    private static <T> R<T> restResult(T data, int code, String msg) {
        R<T> apiResult = new R<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMsg(msg);
        return apiResult;
    }

    public boolean isSuccess() {
        return SUCCESS == this.code;
    }
}

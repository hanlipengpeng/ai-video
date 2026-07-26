package com.ai.comic.aigc;

/**
 * Agnes AI HTTP 调用异常。
 * <p>
 * 携带 HTTP 状态码，便于 KeyPool 区分 429（限流）/ 401（鉴权失败）/ 5xx（服务端错误）。
 */
public class AgnesHttpException extends RuntimeException {

    private final int httpStatus;

    public AgnesHttpException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public AgnesHttpException(int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /** 是否限流（429） */
    public boolean isRateLimited() {
        return httpStatus == 429;
    }

    /** 是否鉴权失败（401） */
    public boolean isUnauthorized() {
        return httpStatus == 401;
    }
}

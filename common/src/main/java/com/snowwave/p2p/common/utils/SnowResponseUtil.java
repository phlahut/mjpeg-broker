package com.snowwave.p2p.common.utils;

import com.snowwave.p2p.common.dto.SnowResponse;

/**
 * @author 胖还亮
 * @date 2022/7/27
 */
public class SnowResponseUtil {

    public static <T> SnowResponse<T> buildResponse(int code, String message, T body) {
        SnowResponse<T> response = new SnowResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setBody(body);
        return response;
    }
}

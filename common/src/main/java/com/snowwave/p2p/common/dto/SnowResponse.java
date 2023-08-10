package com.snowwave.p2p.common.dto;

import lombok.Data;

/**
 * @Author  胖还亮
 * @Date 2022/7/27
 *
 */
@Data
public class SnowResponse<T> {
    private int code;
    private String message;
    private T body;
}

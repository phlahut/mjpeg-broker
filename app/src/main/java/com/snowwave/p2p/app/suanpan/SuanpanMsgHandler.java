package com.snowwave.p2p.app.suanpan;

import com.alibaba.fastjson.JSONObject;
import com.xuelang.mqstream.config.EnvUtil;
import com.xuelang.mqstream.handler.annotation.BussinessListener;
import com.xuelang.mqstream.handler.annotation.BussinessListenerMapping;
import com.xuelang.mqstream.message.arguments.CommonType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;


@BussinessListener
@Component
@Slf4j
public class SuanpanMsgHandler {

    @Value("${web.server.port}")
    private String httpPort;

    @BussinessListenerMapping(input = "in1", targets = {"out1"})
    public Object dealCommonTypeMsg1(CommonType commonType) {
        String in = commonType.getMessage().get("in1");
        JSONObject ret = JSONObject.parseObject(in);
        JSONObject body = ret.getJSONObject("body");
        body.put("host", EnvUtil.get("_stream_host"));
        body.put("brokerAddr", getAddr() + ":" + httpPort);
        body.put("brokerNodeId", EnvUtil.get("_stream_node_id"));
        return ret;
    }

    private String getAddr() {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            log.error("get localhost error " + e.getMessage(), e);
            return null;
        }

        return inetAddress.getHostAddress();
    }
}

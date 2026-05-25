package com.tiankong44.tool.websocket;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class WebSocketService {
    private SseEmitter emitter;

    public WebSocketService() {
        // 启动WebSocket客户端
        WebSocketClient client = new StandardWebSocketClient();
        client.doHandshake(new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                // 连接建立后发送一些初始消息（如果需要）
                session.sendMessage(new TextMessage("Hello WebSocket!"));
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                // 每当收到一条消息时，通过SseEmitter发送
                if (emitter != null) {
                    emitter.send(message.getPayload());
                }
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                // 错误处理
                System.err.println("WebSocket transport error: " + exception.getMessage());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                // 连接关闭时的处理
                System.out.println("WebSocket connection closed with status: " + closeStatus);
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, "wss://devcloud.sitechcloud.com/km/search-api/bigModel/websocketNew?userId=oncon100000175488&userName=zhanghao_smeics&cpersonName=zhanghao_smeics&epId=jiusiTest&scene_id=1930f3b15bc64601a54522a4b3898b77&clientType=PC&is_send_hint=Y&chat_source_href=1&appid=dd9942368e4a4d1b9ba0d144832170e8&appkey=6bfc7075b8b4469197569e36943f5a44", new WebSocketHttpHeaders()); // 设置超时时间为60秒
    }

    public SseEmitter startStream() {
        emitter = new SseEmitter(Long.MAX_VALUE); // 设置一个非常长的超时时间
        // 定期发送心跳消息
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().data("Heartbeat"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 30, TimeUnit.SECONDS);
        return emitter;
    }
}
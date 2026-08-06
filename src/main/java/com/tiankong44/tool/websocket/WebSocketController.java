package com.tiankong44.tool.websocket;

import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
public class WebSocketController {
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);


    @GetMapping(value = "/connect-websocket")
    public ResponseEntity<SseEmitter> connectToWebSocket() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_EVENT_STREAM);
        headers.setCacheControl("no-cache");
        headers.setConnection("keep-alive");

        executorService.submit(() -> {
            try {
                StandardWebSocketClient client = new StandardWebSocketClient();

                // 获取WebSocketContainer并设置更大的缓冲区
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024 * 5); // 5MB
                container.setDefaultMaxTextMessageBufferSize(1024 * 1024 * 5);  // 5MB

                StringBuilder stringBuilder = new StringBuilder();
                WebSocketHandler handler = new TextWebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                        // 设置WebSocketSession的缓冲区大小
                        session.setBinaryMessageSizeLimit(1024 * 1024 * 5);
                        session.setTextMessageSizeLimit(1024 * 1024 * 5);

                        session.sendMessage(new TextMessage("{\"question\":\"思特奇有哪些荣誉\"}"));
                    }

                    @Override
                    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                        try {
                            String content = message.getPayload().toString();
                            System.out.println("Received message: " + content);

                            JSONObject jsonObject = JSONObject.parseObject(content);
                            if (!(jsonObject.get("data") instanceof String)) {
                                JSONObject dataNode = jsonObject.getJSONObject("data");

                                boolean finished = dataNode.getBoolean("finished");

                                if (finished) {
                                    System.out.println(stringBuilder);
                                    session.close(CloseStatus.NORMAL.withReason("websocket结束"));
                                    emitter.complete();

                                } else {
                                    stringBuilder.append(dataNode.getString("deltas"));
                                    // Send the message content
                                    emitter.send(SseEmitter.event().data(content));
                                }
                            }
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                        System.out.println("Transport error: " + exception.getMessage());
                        emitter.completeWithError(exception);
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                        System.out.println("Connection closed: " + status.getReason());
                        emitter.complete();
                    }
                };


                String url = "wss://devcloud.sitechcloud.com/km/search-api/bigModel/websocketNew?userId=oncon100000175488&userName=zhanghao_smeics&cpersonName=zhanghao_smeics&epId=jiusiTest&scene_id=1930f3b15bc64601a54522a4b3898b77&clientType=PC&is_send_hint=Y&chat_source_href=1&appid=dd9942368e4a4d1b9ba0d144832170e8&appkey=6bfc7075b8b4469197569e36943f5a44";
                client.doHandshake(handler, url).get(10, TimeUnit.SECONDS);

            } catch (Exception e) {
                e.printStackTrace();
                emitter.completeWithError(e);
            }
        });

        return new ResponseEntity<>(emitter, headers, HttpStatus.OK);
    }
}

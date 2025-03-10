package com.tiankong44.tool.util;

import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpUtils {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    /**
     * 发送 GET 请求
     * @param url 请求的 URL
     * @param jwtToken JWT令牌（可选，传入null则不添加认证头）
     * @return 响应的字符串
     */
    public static String get(String url, String jwtToken) {
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (jwtToken != null && !jwtToken.trim().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + jwtToken);
        }

        Request request = requestBuilder.build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response.code() + ": " + response.body().string());
            }
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 发送 POST 请求
     * @param url 请求的 URL
     * @param body 请求体内容
     * @param mediaType 请求体的媒体类型（如 MediaType.parse("application/json; charset=utf-8")）
     * @param jwtToken JWT令牌（可选，传入null则不添加认证头）
     * @return 响应的字符串
     */
    public static String post(String url, String body, MediaType mediaType, String jwtToken) {
        RequestBody requestBody = RequestBody.create(body, mediaType);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);

        if (jwtToken != null && !jwtToken.trim().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + jwtToken);
        }

        Request request = requestBuilder.build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response.code() + ": " + response.body().string());
            }
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 发送 PUT 请求
     * @param url 请求的 URL
     * @param body 请求体内容
     * @param mediaType 请求体的媒体类型（如 MediaType.parse("application/json; charset=utf-8")）
     * @param jwtToken JWT令牌（可选，传入null则不添加认证头）
     * @return 响应的字符串
     */
    public static String put(String url, String body, MediaType mediaType, String jwtToken) {
        RequestBody requestBody = RequestBody.create(body, mediaType);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .put(requestBody);

        if (jwtToken != null && !jwtToken.trim().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + jwtToken);
        }

        Request request = requestBuilder.build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response.code() + ": " + response.body().string());
            }
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 发送 DELETE 请求
     * @param url 请求的 URL
     * @param jwtToken JWT令牌（可选，传入null则不添加认证头）
     * @return 响应的字符串
     */
    public static String delete(String url, String jwtToken) {
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (jwtToken != null && !jwtToken.trim().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + jwtToken);
        }

        Request request = requestBuilder.delete().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response.code() + ": " + response.body().string());
            }
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
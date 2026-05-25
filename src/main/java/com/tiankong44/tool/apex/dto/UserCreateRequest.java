package com.tiankong44.tool.apex.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserCreateRequest {
    @NotBlank(message = "用户ID不能为空")
    private String userId;
}
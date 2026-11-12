package com.tiankong44.tool.reefjournal.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户主动维护昵称和头像时提交的资料。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Data
public class ReefUserProfileSaveRequest {
    @NotBlank(message = "请填写昵称后保存")
    @Size(max = 64, message = "昵称不能超过64个字符")
    private String nickname;

    @Size(max = 64, message = "头像文件编号无效")
    private String avatarFileKey;
}

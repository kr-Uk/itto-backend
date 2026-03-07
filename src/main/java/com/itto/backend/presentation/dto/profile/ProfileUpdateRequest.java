package com.itto.backend.presentation.dto.profile;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProfileUpdateRequest {

    @Size(min = 2, max = 8, message = "닉네임은 2~8자여야 합니다")
    private String nickname;

    @Size(min = 1, max = 3, message = "취미는 1~3개를 선택해야 합니다")
    private List<Long> hobbyIds;

    @Size(max = 15, message = "한줄소개는 15자 이하여야 합니다")
    private String introduction;
}

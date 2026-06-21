package com.medical.triage.dto.request;

import com.medical.triage.enums.AppointmentSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSyncRequest {

    @NotBlank(message = "客户姓名不能为空")
    private String name;

    @NotBlank(message = "性别不能为空")
    private String gender;

    @NotNull(message = "年龄不能为空")
    private Integer age;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    private String address;

    @NotNull(message = "来源不能为空")
    private AppointmentSource source;

    private String memberLevel;
}

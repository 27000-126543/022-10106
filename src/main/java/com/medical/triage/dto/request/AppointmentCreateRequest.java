package com.medical.triage.dto.request;

import com.medical.triage.enums.AppointmentSource;
import com.medical.triage.enums.ConsultationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentCreateRequest {

    @NotBlank(message = "客户姓名不能为空")
    private String customerName;

    @NotBlank(message = "性别不能为空")
    private String gender;

    @NotNull(message = "年龄不能为空")
    private Integer age;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    @NotNull(message = "预约时间不能为空")
    private LocalDateTime appointmentTime;

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    @NotNull(message = "预约来源不能为空")
    private AppointmentSource source;

    @NotNull(message = "咨询类型不能为空")
    private ConsultationType consultationType;

    private String notes;

    private Boolean isFirstVisit;
}

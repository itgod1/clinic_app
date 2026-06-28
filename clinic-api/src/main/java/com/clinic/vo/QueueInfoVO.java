package com.clinic.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 排队信息VO
 */
@Data
@ApiModel("排队信息")
public class QueueInfoVO {

    @ApiModelProperty("当前叫号")
    private Integer currentNumber;

    @ApiModelProperty("我的排队号")
    private Integer queueNumber;

    @ApiModelProperty("前方等待人数")
    private Integer aheadCount;

    @ApiModelProperty("预计等待时间(分钟)")
    private Integer estimatedWaitTime;

    @ApiModelProperty("医生名称")
    private String doctorName;

    @ApiModelProperty("科室名称")
    private String deptName;

    @ApiModelProperty("就诊日期")
    private String regDate;

    @ApiModelProperty("就诊时间段")
    private String regTime;

    @ApiModelProperty("状态：1排队中 2就诊中 3已完成")
    private Integer status;
}

package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.controller.dto.CreateRevisitRequest;
import com.clinic.controller.dto.MarkRemindRequest;
import com.clinic.controller.dto.MarkAppointRequest;
import com.clinic.controller.dto.MarkCompleteRequest;
import com.clinic.entity.ReturnVisitPlan;
import com.clinic.entity.ReturnVisitRecord;
import com.clinic.entity.ReturnVisitTemplate;
import com.clinic.entity.ReturnVisitRule;
import com.clinic.service.ReturnVisitService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "诊疗管理-回访中心")
@RestController
@RequestMapping("/admin/returnVisit")
@RequiredArgsConstructor
public class ReturnVisitController {

    private final ReturnVisitService returnVisitService;

    @ApiOperation("获取回访计划列表")
    @GetMapping("/plan/list")
    public Result<Map<String, Object>> listPlans(
            @RequestParam(required = false) Long clinicId,
            @RequestParam(required = false) String planType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> data = returnVisitService.listPlans(clinicId, planType, status, startDate, endDate, keyword, assigneeId, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("获取回访计划详情")
    @GetMapping("/plan/{id}")
    public Result<ReturnVisitPlan> getPlanById(@PathVariable Long id) {
        ReturnVisitPlan plan = returnVisitService.getPlanById(id);
        return Result.success(plan);
    }

    @ApiOperation("创建回访计划")
    @OperationLog(module = "诊疗管理-回访中心", type = OperationType.CREATE, desc = "创建回访计划")
    @PostMapping("/plan/create")
    public Result<String> createPlan(@RequestBody ReturnVisitPlan plan) throws Exception {
        returnVisitService.createPlan(plan);
        return Result.success("创建成功");
    }

    @ApiOperation("更新回访计划")
    @OperationLog(module = "诊疗管理-回访中心", type = OperationType.UPDATE, desc = "更新回访计划")
    @PostMapping("/plan/update")
    public Result<String> updatePlan(@RequestBody ReturnVisitPlan plan) throws Exception {
        returnVisitService.updatePlan(plan);
        return Result.success("更新成功");
    }

    @ApiOperation("取消回访计划")
    @OperationLog(module = "诊疗管理-回访中心", type = OperationType.UPDATE, desc = "取消回访计划")
    @PostMapping("/plan/cancel/{id}")
    public Result<String> cancelPlan(@PathVariable Long id) throws Exception {
        returnVisitService.cancelPlan(id);
        return Result.success("取消成功");
    }

    @ApiOperation("获取今日待回访列表")
    @GetMapping("/plan/today")
    public Result<List<ReturnVisitPlan>> getTodayPlans(@RequestParam(required = false) Long clinicId) {
        List<ReturnVisitPlan> plans = returnVisitService.getTodayPlans(clinicId);
        return Result.success(plans);
    }

    @ApiOperation("获取逾期未回访列表")
    @GetMapping("/plan/overdue")
    public Result<List<ReturnVisitPlan>> getOverduePlans(@RequestParam(required = false) Long clinicId) {
        List<ReturnVisitPlan> plans = returnVisitService.getOverduePlans(clinicId);
        return Result.success(plans);
    }

    @ApiOperation("获取回访统计数据")
    @GetMapping("/plan/statistics")
    public Result<Map<String, Object>> getPlanStatistics(@RequestParam(required = false) Long clinicId) {
        Map<String, Object> statistics = returnVisitService.getPlanStatistics(clinicId);
        return Result.success(statistics);
    }

    @ApiOperation("获取回访记录列表")
    @GetMapping("/record/list")
    public Result<Map<String, Object>> listRecords(
            @RequestParam(required = false) Long clinicId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> data = returnVisitService.listRecords(clinicId, patientId, doctorId, startDate, endDate, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("获取回访记录详情")
    @GetMapping("/record/{id}")
    public Result<ReturnVisitRecord> getRecordById(@PathVariable Long id) {
        ReturnVisitRecord record = returnVisitService.getRecordById(id);
        return Result.success(record);
    }

    @ApiOperation("提交回访记录")
    @OperationLog(module = "诊疗管理-回访中心", type = OperationType.CREATE, desc = "提交回访记录")
    @PostMapping("/record/create")
    public Result<String> createRecord(@RequestBody ReturnVisitRecord record) throws Exception {
        returnVisitService.createRecord(record);
        return Result.success("提交成功");
    }

    @ApiOperation("获取回访模板列表")
    @GetMapping("/template/list")
    public Result<List<ReturnVisitTemplate>> listTemplates(@RequestParam(required = false) Long clinicId) {
        List<ReturnVisitTemplate> templates = returnVisitService.listTemplates(clinicId);
        return Result.success(templates);
    }

    @ApiOperation("创建回访模板")
    @OperationLog(module = "诊疗管理-回访中心", type = OperationType.CREATE, desc = "创建回访模板")
    @PostMapping("/template/create")
    public Result<String> createTemplate(@RequestBody ReturnVisitTemplate template) throws Exception {
        returnVisitService.createTemplate(template);
        return Result.success("创建成功");
    }

    @ApiOperation("更新回访模板")
    @OperationLog(module = "诊疗管理-回访中心", type = OperationType.UPDATE, desc = "更新回访模板")
    @PostMapping("/template/update")
    public Result<String> updateTemplate(@RequestBody ReturnVisitTemplate template) throws Exception {
        returnVisitService.updateTemplate(template);
        return Result.success("更新成功");
    }

    @ApiOperation("删除回访模板")
    @OperationLog(module = "诊疗管理-回访中心", type = OperationType.DELETE, desc = "删除回访模板")
    @PostMapping("/template/delete/{id}")
    public Result<String> deleteTemplate(@PathVariable Long id) {
        returnVisitService.deleteTemplate(id);
        return Result.success("删除成功");
    }

    @ApiOperation("获取回访规则列表")
    @GetMapping("/rule/list")
    public Result<List<ReturnVisitRule>> listRules(@RequestParam(required = false) Long clinicId) {
        List<ReturnVisitRule> rules = returnVisitService.listRules(clinicId);
        return Result.success(rules);
    }

    @ApiOperation("创建回访规则")
    @OperationLog(module = "诊疗管理-回访中心", type = OperationType.CREATE, desc = "创建回访规则")
    @PostMapping("/rule/create")
    public Result<String> createRule(@RequestBody ReturnVisitRule rule) throws Exception {
        returnVisitService.createRule(rule);
        return Result.success("创建成功");
    }

    @ApiOperation("更新回访规则")
    @OperationLog(module = "诊疗管理-回访中心", type = OperationType.UPDATE, desc = "更新回访规则")
    @PostMapping("/rule/update")
    public Result<String> updateRule(@RequestBody ReturnVisitRule rule) throws Exception {
        returnVisitService.updateRule(rule);
        return Result.success("更新成功");
    }

    @ApiOperation("删除回访规则")
    @OperationLog(module = "诊疗管理-回访中心", type = OperationType.DELETE, desc = "删除回访规则")
    @PostMapping("/rule/delete/{id}")
    public Result<String> deleteRule(@PathVariable Long id) {
        returnVisitService.deleteRule(id);
        return Result.success("删除成功");
    }

    @ApiOperation("启用/禁用回访规则")
    @OperationLog(module = "诊疗管理-回访中心", type = OperationType.UPDATE, desc = "切换回访规则状态")
    @PostMapping("/rule/toggle/{id}")
    public Result<String> toggleRule(@PathVariable Long id) throws Exception {
        returnVisitService.toggleRule(id);
        return Result.success("操作成功");
    }

    @ApiOperation("根据挂号自动创建回访计划")
    @OperationLog(module = "诊疗管理-回访中心", type = OperationType.CREATE, desc = "自动创建回访计划")
    @PostMapping("/plan/autoCreate")
    public Result<String> autoCreatePlan(@RequestParam Long registrationId,
                                         @RequestParam Long patientId,
                                         @RequestParam String patientName,
                                         @RequestParam(required = false) String visitItem) {
        returnVisitService.applyRulesToRegistration(registrationId, patientId, patientName, visitItem);
        return Result.success("创建成功");
    }

    // ========== 复诊提醒相关接口 ==========

    @ApiOperation("获取复诊提醒列表")
    @GetMapping("/revisit/list")
    public Result<Map<String, Object>> listRevisitPlans(
            @RequestParam(required = false) Long clinicId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> data = returnVisitService.listRevisitPlans(clinicId, status, keyword, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("获取今日到期复诊提醒")
    @GetMapping("/revisit/todayDue")
    public Result<List<ReturnVisitPlan>> getTodayDueRevisits(@RequestParam(required = false) Long clinicId) {
        List<ReturnVisitPlan> plans = returnVisitService.getDueRevisitPlans(clinicId);
        return Result.success(plans);
    }

    @ApiOperation("获取已提醒复诊列表")
    @GetMapping("/revisit/reminded")
    public Result<List<ReturnVisitPlan>> getRemindedRevisits(@RequestParam(required = false) Long clinicId) {
        List<ReturnVisitPlan> plans = returnVisitService.getRemindedRevisitPlans(clinicId);
        return Result.success(plans);
    }

    @ApiOperation("获取已预约复诊列表")
    @GetMapping("/revisit/appointed")
    public Result<List<ReturnVisitPlan>> getAppointedRevisits(@RequestParam(required = false) Long clinicId) {
        List<ReturnVisitPlan> plans = returnVisitService.getAppointedRevisitPlans(clinicId);
        return Result.success(plans);
    }

    @ApiOperation("获取复诊提醒统计")
    @GetMapping("/revisit/statistics")
    public Result<Map<String, Object>> getRevisitStatistics(@RequestParam(required = false) Long clinicId) {
        Map<String, Object> statistics = returnVisitService.getRevisitStatistics(clinicId);
        return Result.success(statistics);
    }

    @ApiOperation("创建复诊提醒计划")
    @OperationLog(module = "诊疗管理-复诊提醒", type = OperationType.CREATE, desc = "创建复诊提醒")
    @PostMapping("/revisit/create")
    public Result<String> createRevisitPlan(@RequestBody ReturnVisitPlan plan) throws Exception {
        plan.setPlanType("REVISIT");
        if (plan.getStatus() == null) {
            plan.setStatus("PENDING");
        }
        returnVisitService.createPlan(plan);
        return Result.success("创建成功");
    }

    @ApiOperation("标记复诊为已提醒")
    @OperationLog(module = "诊疗管理-复诊提醒", type = OperationType.UPDATE, desc = "标记已提醒")
    @PostMapping("/revisit/remind/{id}")
    public Result<String> markRevisitAsReminded(@PathVariable Long id,
                                                 @RequestBody MarkRemindRequest request) throws Exception {
        returnVisitService.markRevisitAsReminded(id, request.getDoctorId());
        return Result.success("标记成功");
    }

    @ApiOperation("标记复诊为已预约")
    @OperationLog(module = "诊疗管理-复诊提醒", type = OperationType.UPDATE, desc = "标记已预约")
    @PostMapping("/revisit/appoint/{id}")
    public Result<String> markRevisitAsAppointed(@PathVariable Long id,
                                                  @RequestBody MarkAppointRequest request) throws Exception {
        returnVisitService.markRevisitAsAppointed(id, request.getDoctorId(), request.getDoctorName(), request.getNote());
        return Result.success("预约成功");
    }

    @ApiOperation("标记复诊为已完成")
    @OperationLog(module = "诊疗管理-复诊提醒", type = OperationType.UPDATE, desc = "标记已完成")
    @PostMapping("/revisit/complete/{id}")
    public Result<String> markRevisitAsCompleted(@PathVariable Long id,
                                                  @RequestBody MarkCompleteRequest request) throws Exception {
        returnVisitService.markRevisitAsCompleted(id, request.getDoctorId(), request.getNote());
        return Result.success("标记成功");
    }

    @ApiOperation("从病历创建复诊提醒")
    @OperationLog(module = "诊疗管理-复诊提醒", type = OperationType.CREATE, desc = "从病历创建复诊提醒")
    @PostMapping("/revisit/createFromRecord")
    public Result<String> createRevisitFromRecord(@RequestBody CreateRevisitRequest request) throws Exception {
        LocalDate date = LocalDate.parse(request.getPlanDate());
        returnVisitService.createRevisitPlan(request.getMedicalRecordId(), request.getPatientId(),
                request.getPatientName(), request.getPatientPhone(),
                request.getOriginalTreatment(), request.getFollowUpItem(),
                request.getFollowUpDesc(), request.getRecoveryDays(), date);
        return Result.success("创建成功");
    }

    @ApiOperation("获取患者的复诊计划")
    @GetMapping("/revisit/patient/{patientId}")
    public Result<List<ReturnVisitPlan>> getPatientRevisitPlans(@RequestParam Long clinicId,
                                                                 @PathVariable Long patientId) {
        List<ReturnVisitPlan> plans = returnVisitService.getPatientRevisitPlans(clinicId, patientId);
        return Result.success(plans);
    }

    // ========== 工作台复诊提醒接口 ==========

    @ApiOperation("工作台-获取复诊提醒概览")
    @GetMapping("/revisit/dashboard")
    public Result<Map<String, Object>> getRevisitDashboard(@RequestParam Long clinicId) {
        Map<String, Object> result = new HashMap<>();

        // 待处理的（包括今天到期、未来到期的）
        List<ReturnVisitPlan> pending = returnVisitService.getPendingRevisitPlans(clinicId);
        // 今日到期
        List<ReturnVisitPlan> todayDue = returnVisitService.getDueRevisitPlans(clinicId);
        // 已提醒
        List<ReturnVisitPlan> reminded = returnVisitService.getRemindedRevisitPlans(clinicId);
        // 已预约
        List<ReturnVisitPlan> appointed = returnVisitService.getAppointedRevisitPlans(clinicId);
        // 统计
        Map<String, Object> statistics = returnVisitService.getRevisitStatistics(clinicId);

        result.put("pending", pending);
        result.put("todayDue", todayDue);
        result.put("reminded", reminded);
        result.put("appointed", appointed);
        result.put("statistics", statistics);

        return Result.success(result);
    }
}

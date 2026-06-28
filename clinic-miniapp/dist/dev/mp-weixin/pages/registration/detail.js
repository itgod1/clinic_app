"use strict";
const common_vendor = require("../../common/vendor.js");
const api_registration = require("../../api/registration.js");
const utils_constants = require("../../utils/constants.js");
const utils_index = require("../../utils/index.js");
require("../../utils/request.js");
const _sfc_main = {
  __name: "detail",
  setup(__props) {
    const detail = common_vendor.ref({});
    const queueInfo = common_vendor.ref({});
    const loading = common_vendor.ref(false);
    const refreshTimer = common_vendor.ref(null);
    const registrationId = common_vendor.ref(null);
    const isQueuing = common_vendor.computed(() => {
      return detail.value.status === utils_constants.REGISTRATION_STATUS.PENDING || detail.value.status === utils_constants.REGISTRATION_STATUS.CHECKED_IN;
    });
    const statusText = common_vendor.computed(() => utils_constants.REGISTRATION_STATUS_TEXT[detail.value.status] || "未知");
    const statusColor = common_vendor.computed(() => utils_constants.REGISTRATION_STATUS_COLOR[detail.value.status] || "#999");
    const statusIcon = common_vendor.computed(() => {
      const map = {
        [utils_constants.REGISTRATION_STATUS.PENDING]: "⏳",
        [utils_constants.REGISTRATION_STATUS.IN_PROGRESS]: "🔍",
        [utils_constants.REGISTRATION_STATUS.COMPLETED]: "✓",
        [utils_constants.REGISTRATION_STATUS.CANCELLED]: "✕"
      };
      return map[detail.value.status] || "?";
    });
    const statusDesc = common_vendor.computed(() => {
      const map = {
        [utils_constants.REGISTRATION_STATUS.PENDING]: "请按时前往就诊",
        [utils_constants.REGISTRATION_STATUS.IN_PROGRESS]: "正在就诊中",
        [utils_constants.REGISTRATION_STATUS.COMPLETED]: "就诊已完成",
        [utils_constants.REGISTRATION_STATUS.CANCELLED]: "挂号已取消"
      };
      return map[detail.value.status] || "";
    });
    const canCancel = common_vendor.computed(() => {
      return detail.value.status === utils_constants.REGISTRATION_STATUS.PENDING;
    });
    common_vendor.onMounted(() => {
      var _a;
      console.log("详情页面 onMounted");
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      console.log("当前页面:", currentPage);
      console.log("页面参数:", currentPage.options);
      const id = (_a = currentPage.options) == null ? void 0 : _a.id;
      console.log("获取到的ID:", id);
      if (id) {
        registrationId.value = id;
        loadDetail(id);
      } else {
        console.error("没有获取到挂号ID");
        common_vendor.index.showToast({ title: "参数错误", icon: "none" });
      }
    });
    common_vendor.onShow(() => {
      if (isQueuing.value) {
        startAutoRefresh();
      }
    });
    common_vendor.onHide(() => {
      stopAutoRefresh();
    });
    common_vendor.onUnmounted(() => {
      stopAutoRefresh();
    });
    const startAutoRefresh = () => {
      stopAutoRefresh();
      refreshTimer.value = setInterval(() => {
        loadQueueInfo();
      }, 3e4);
    };
    const stopAutoRefresh = () => {
      if (refreshTimer.value) {
        clearInterval(refreshTimer.value);
        refreshTimer.value = null;
      }
    };
    const loadDetail = async (id) => {
      loading.value = true;
      console.log("加载挂号详情, ID:", id);
      try {
        const res = await api_registration.getRegistrationDetail(id);
        console.log("挂号详情响应:", res);
        detail.value = res.data || {};
        console.log("挂号详情数据:", detail.value);
        console.log("挂号状态:", detail.value.status);
        if (detail.value.status === utils_constants.REGISTRATION_STATUS.PENDING || detail.value.status === utils_constants.REGISTRATION_STATUS.CHECKED_IN) {
          console.log("开始加载排队信息");
          loadQueueInfo();
          startAutoRefresh();
        } else {
          console.log("不需要加载排队信息, 状态:", detail.value.status);
        }
      } catch (error) {
        console.error("加载详情失败:", error);
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      } finally {
        loading.value = false;
      }
    };
    const loadQueueInfo = async () => {
      if (!registrationId.value)
        return;
      try {
        const res = await api_registration.getRegistrationQueueInfo(registrationId.value);
        queueInfo.value = res.data || {};
      } catch (error) {
        console.log("获取排队信息失败，使用模拟数据");
        const currentNum = Math.floor(Math.random() * 10) + 1;
        const myNumber = currentNum + Math.floor(Math.random() * 8) + 2;
        queueInfo.value = {
          currentNumber: currentNum,
          queueNumber: myNumber,
          aheadCount: myNumber - currentNum - 1,
          estimatedWaitTime: (myNumber - currentNum) * 10
        };
      }
    };
    const refreshQueueInfo = () => {
      common_vendor.index.showLoading({ title: "刷新中..." });
      loadQueueInfo().finally(() => {
        common_vendor.index.hideLoading();
        common_vendor.index.showToast({ title: "已更新", icon: "success", duration: 1e3 });
      });
    };
    const handleCancel = async () => {
      const confirmed = await utils_index.showConfirm("确定取消该挂号吗？", "取消挂号");
      if (!confirmed)
        return;
      try {
        await api_registration.cancelRegistration({ id: detail.value.id });
        utils_index.showSuccess("取消成功");
        stopAutoRefresh();
        loadDetail(detail.value.id);
      } catch (error) {
        console.error("取消失败:", error);
      }
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.unref(isQueuing)
      }, common_vendor.unref(isQueuing) ? common_vendor.e({
        b: common_vendor.t(detail.value.doctorName),
        c: common_vendor.t(detail.value.departmentName),
        d: common_vendor.t(queueInfo.value.currentNumber || "-"),
        e: common_vendor.t(queueInfo.value.queueNumber || "-"),
        f: common_vendor.t(queueInfo.value.aheadCount || 0),
        g: queueInfo.value.estimatedWaitTime
      }, queueInfo.value.estimatedWaitTime ? {
        h: common_vendor.t(queueInfo.value.estimatedWaitTime)
      } : {}, {
        i: queueInfo.value.aheadCount <= 3 && queueInfo.value.aheadCount > 0 ? 1 : "",
        j: queueInfo.value.aheadCount <= 2 && queueInfo.value.aheadCount > 0
      }, queueInfo.value.aheadCount <= 2 && queueInfo.value.aheadCount > 0 ? {} : {}, {
        k: common_vendor.o(refreshQueueInfo)
      }) : {
        l: common_vendor.t(common_vendor.unref(statusIcon)),
        m: common_vendor.unref(statusColor),
        n: common_vendor.unref(statusColor) + "20",
        o: common_vendor.t(common_vendor.unref(statusText)),
        p: common_vendor.unref(statusColor),
        q: common_vendor.t(common_vendor.unref(statusDesc))
      }, {
        r: common_vendor.t(detail.value.regNo),
        s: common_vendor.t(detail.value.createTime),
        t: common_vendor.t(detail.value.regDate),
        v: common_vendor.t(detail.value.regTime),
        w: common_vendor.t(detail.value.regFee),
        x: common_vendor.t(detail.value.visitType === 1 ? "初诊" : "复诊"),
        y: detail.value.doctorAvatar || "/static/default-avatar.png",
        z: common_vendor.t(detail.value.doctorName),
        A: common_vendor.t(detail.value.doctorTitle),
        B: common_vendor.t(detail.value.departmentName),
        C: common_vendor.t(detail.value.patientName),
        D: common_vendor.t(detail.value.patientPhone),
        E: common_vendor.unref(canCancel)
      }, common_vendor.unref(canCancel) ? {
        F: common_vendor.o(handleCancel)
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-8985dcc5"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/registration/detail.vue"]]);
wx.createPage(MiniProgramPage);

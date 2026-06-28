"use strict";
const common_vendor = require("../../common/vendor.js");
const api_registration = require("../../api/registration.js");
const utils_constants = require("../../utils/constants.js");
const utils_index = require("../../utils/index.js");
require("../../utils/request.js");
if (!Array) {
  const _component_uni_load_more = common_vendor.resolveComponent("uni-load-more");
  _component_uni_load_more();
}
if (!Math) {
  EmptyData();
}
const EmptyData = () => "../../components/empty-data.js";
const _sfc_main = {
  __name: "list",
  setup(__props) {
    const loading = common_vendor.ref(false);
    const refreshing = common_vendor.ref(false);
    const registrationList = common_vendor.ref([]);
    const pageNum = common_vendor.ref(1);
    const pageSize = 10;
    const loadStatus = common_vendor.ref("more");
    const autoRefreshTimer = common_vendor.ref(null);
    const hasPendingRegistration = common_vendor.computed(() => {
      return registrationList.value.some((item) => item.status === utils_constants.REGISTRATION_STATUS.PENDING);
    });
    common_vendor.onMounted(() => {
      console.log("挂号列表页面 onMounted");
      const clinicId = common_vendor.index.getStorageSync("clinicId");
      console.log("当前诊所ID:", clinicId);
      if (clinicId) {
        loadRegistrations();
      } else {
        console.log("没有诊所ID，不加载数据");
        common_vendor.index.showToast({ title: "请先选择诊所", icon: "none" });
      }
    });
    common_vendor.onShow(() => {
      console.log("挂号列表页面 onShow");
      const clinicId = common_vendor.index.getStorageSync("clinicId");
      if (clinicId && registrationList.value.length > 0) {
        startAutoRefresh();
      }
    });
    common_vendor.onHide(() => {
      stopAutoRefresh();
    });
    common_vendor.onUnmounted(() => {
      stopAutoRefresh();
    });
    common_vendor.onPullDownRefresh(() => {
      pageNum.value = 1;
      loadRegistrations().finally(() => {
        common_vendor.index.stopPullDownRefresh();
      });
    });
    const startAutoRefresh = () => {
      stopAutoRefresh();
      autoRefreshTimer.value = setInterval(() => {
        loadRegistrations(false);
      }, 3e4);
    };
    const stopAutoRefresh = () => {
      if (autoRefreshTimer.value) {
        clearInterval(autoRefreshTimer.value);
        autoRefreshTimer.value = null;
      }
    };
    const onRefresh = () => {
      refreshing.value = true;
      pageNum.value = 1;
      loadRegistrations().finally(() => {
        refreshing.value = false;
      });
    };
    const manualRefresh = () => {
      common_vendor.index.showLoading({ title: "刷新中..." });
      pageNum.value = 1;
      loadRegistrations(false).finally(() => {
        common_vendor.index.hideLoading();
        common_vendor.index.showToast({ title: "已更新", icon: "success", duration: 1e3 });
      });
    };
    const getProgressWidth = (item) => {
      if (!item.queueNumber || !item.currentNumber)
        return 0;
      const progress = item.currentNumber / item.queueNumber * 100;
      return Math.min(progress, 95);
    };
    const getStatusText = (status) => {
      return utils_constants.REGISTRATION_STATUS_TEXT[status] || "未知";
    };
    const getStatusColor = (status) => {
      const map = {
        [utils_constants.REGISTRATION_STATUS.PENDING]: "linear-gradient(135deg, #00b894 0%, #00cec9 100%)",
        [utils_constants.REGISTRATION_STATUS.CHECKED_IN]: "linear-gradient(135deg, #1890ff 0%, #36cfc9 100%)",
        [utils_constants.REGISTRATION_STATUS.IN_PROGRESS]: "linear-gradient(135deg, #52c41a 0%, #73d13d 100%)",
        [utils_constants.REGISTRATION_STATUS.COMPLETED]: "#999",
        [utils_constants.REGISTRATION_STATUS.CANCELLED]: "#ff4d4f",
        [utils_constants.REGISTRATION_STATUS.REFUNDED]: "#ff7875",
        [utils_constants.REGISTRATION_STATUS.MISSED]: "#faad14"
      };
      return map[status] || "#999";
    };
    const loadRegistrations = async (showLoading = true) => {
      console.log("开始加载挂号列表, showLoading:", showLoading);
      if (loading.value && showLoading) {
        console.log("正在加载中，跳过");
        return;
      }
      if (showLoading)
        loading.value = true;
      try {
        const params = {
          pageNum: pageNum.value,
          pageSize
        };
        console.log("请求参数:", params);
        const res = await api_registration.getMyRegistrationList(params);
        console.log("挂号列表响应:", res);
        const list = res.data.list || [];
        const listWithQueueInfo = list.map((item) => {
          if (item.status === utils_constants.REGISTRATION_STATUS.PENDING || item.status === utils_constants.REGISTRATION_STATUS.CHECKED_IN) {
            return {
              ...item,
              currentNumber: item.currentNumber ?? 0,
              queueNumber: item.queueNo,
              aheadCount: item.aheadCount ?? 0,
              estimatedWaitTime: item.expectedWaitTime ?? 0
            };
          }
          return item;
        });
        listWithQueueInfo.sort((a, b) => {
          if (a.status === utils_constants.REGISTRATION_STATUS.PENDING && b.status !== utils_constants.REGISTRATION_STATUS.PENDING)
            return -1;
          if (a.status !== utils_constants.REGISTRATION_STATUS.PENDING && b.status === utils_constants.REGISTRATION_STATUS.PENDING)
            return 1;
          return /* @__PURE__ */ new Date(b.regDate + " " + b.regTime) - /* @__PURE__ */ new Date(a.regDate + " " + a.regTime);
        });
        if (pageNum.value === 1) {
          registrationList.value = listWithQueueInfo;
        } else {
          registrationList.value.push(...listWithQueueInfo);
        }
        loadStatus.value = list.length < pageSize ? "noMore" : "more";
      } catch (error) {
        console.error("加载挂号列表失败:", error);
        if (showLoading) {
          common_vendor.index.showToast({ title: "加载失败", icon: "none" });
        }
      } finally {
        if (showLoading)
          loading.value = false;
      }
    };
    const loadMore = () => {
      if (loadStatus.value === "noMore" || loading.value)
        return;
      pageNum.value++;
      loadRegistrations();
    };
    const handleCancel = async (item) => {
      const confirmed = await utils_index.showConfirm(`确定取消 ${item.doctorName} 医生的挂号吗？`, "取消挂号");
      if (!confirmed)
        return;
      try {
        await api_registration.cancelRegistration({ id: item.id });
        utils_index.showSuccess("取消成功");
        loadRegistrations();
      } catch (error) {
        console.error("取消挂号失败:", error);
      }
    };
    const handleView = (item) => {
      console.log("点击查看详情:", item);
      console.log("跳转URL:", `/pages/registration/detail?id=${item.id}`);
      if (!item.id) {
        common_vendor.index.showToast({ title: "挂号ID不存在", icon: "none" });
        return;
      }
      common_vendor.index.navigateTo({
        url: `/pages/registration/detail?id=${item.id}`,
        success: () => {
          console.log("页面跳转成功");
        },
        fail: (err) => {
          console.error("页面跳转失败:", err);
          common_vendor.index.showToast({ title: "跳转失败", icon: "none" });
        }
      });
    };
    const goToDoctor = () => {
      common_vendor.index.switchTab({ url: "/pages/doctor/list" });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(manualRefresh),
        b: common_vendor.unref(hasPendingRegistration)
      }, common_vendor.unref(hasPendingRegistration) ? {} : {}, {
        c: common_vendor.f(registrationList.value, (item, index, i0) => {
          return common_vendor.e({
            a: item.doctorAvatar || "/static/default-avatar.png",
            b: common_vendor.t(item.doctorName),
            c: common_vendor.t(item.doctorTitle),
            d: common_vendor.t(item.deptName),
            e: common_vendor.t(getStatusText(item.status)),
            f: getStatusColor(item.status),
            g: item.status === common_vendor.unref(utils_constants.REGISTRATION_STATUS).PENDING || item.status === common_vendor.unref(utils_constants.REGISTRATION_STATUS).CHECKED_IN
          }, item.status === common_vendor.unref(utils_constants.REGISTRATION_STATUS).PENDING || item.status === common_vendor.unref(utils_constants.REGISTRATION_STATUS).CHECKED_IN ? common_vendor.e({
            h: common_vendor.t(item.regTime),
            i: common_vendor.t(item.currentNumber || "-"),
            j: common_vendor.t(item.queueNumber || "-"),
            k: getProgressWidth(item) + "%",
            l: common_vendor.t(item.aheadCount || 0),
            m: item.estimatedWaitTime
          }, item.estimatedWaitTime ? {
            n: common_vendor.t(item.estimatedWaitTime)
          } : {}, {
            o: item.aheadCount <= 3 && item.aheadCount > 0 ? 1 : "",
            p: item.aheadCount <= 2 && item.aheadCount > 0
          }, item.aheadCount <= 2 && item.aheadCount > 0 ? {} : {}, {
            q: common_vendor.o(($event) => handleCancel(item), item.id),
            r: common_vendor.o(($event) => handleView(item), item.id)
          }) : {
            s: common_vendor.t(item.regDate),
            t: common_vendor.t(item.regTime),
            v: common_vendor.t(item.patientName),
            w: common_vendor.t(item.regFee),
            x: common_vendor.o(($event) => handleView(item), item.id)
          }, {
            y: item.id,
            z: item.status === common_vendor.unref(utils_constants.REGISTRATION_STATUS).PENDING ? 1 : ""
          });
        }),
        d: common_vendor.p({
          status: loadStatus.value
        }),
        e: registrationList.value.length === 0 && !loading.value
      }, registrationList.value.length === 0 && !loading.value ? {
        f: common_vendor.o(goToDoctor),
        g: common_vendor.p({
          text: "暂无挂号记录",
          subtext: "可以去预约医生哦",
          ["show-button"]: true,
          ["button-text"]: "去挂号"
        })
      } : {}, {
        h: common_vendor.o(loadMore),
        i: refreshing.value,
        j: common_vendor.o(onRefresh)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-c7017abc"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/registration/list.vue"]]);
wx.createPage(MiniProgramPage);

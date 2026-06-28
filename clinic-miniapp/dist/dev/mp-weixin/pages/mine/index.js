"use strict";
const common_vendor = require("../../common/vendor.js");
const api_auth = require("../../api/auth.js");
const utils_index = require("../../utils/index.js");
require("../../utils/request.js");
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const userInfo = common_vendor.ref({});
    const clinicName = common_vendor.ref("");
    const cacheSize = common_vendor.ref("0KB");
    common_vendor.computed(() => {
      try {
        const list = JSON.parse(common_vendor.index.getStorageSync("clinicList") || "[]");
        return list.length > 1;
      } catch {
        return false;
      }
    });
    common_vendor.onMounted(() => {
      loadUserInfo();
      refreshClinicName();
      calculateCacheSize();
    });
    common_vendor.onShow(() => {
      refreshClinicName();
    });
    const refreshClinicName = () => {
      clinicName.value = common_vendor.index.getStorageSync("clinicName") || "";
    };
    const loadUserInfo = () => {
      try {
        const info = common_vendor.index.getStorageSync("userInfo");
        if (info) {
          userInfo.value = JSON.parse(info);
        }
      } catch (e) {
        console.error("加载用户信息失败:", e);
      }
    };
    const calculateCacheSize = () => {
      cacheSize.value = "1.2MB";
    };
    const goToPatients = () => {
      common_vendor.index.navigateTo({ url: "/pages/patient/list" });
    };
    const goToPayments = () => {
      common_vendor.index.navigateTo({ url: "/pages/payment/outpatient" });
    };
    const goToMedicalRecords = () => {
      common_vendor.index.navigateTo({ url: "/pages/medical/list" });
    };
    const selectClinic = () => {
      common_vendor.index.navigateTo({ url: "/pages/clinic/select" });
    };
    const clearCache = async () => {
      const confirmed = await utils_index.showConfirm("确定清除缓存吗？", "提示");
      if (!confirmed)
        return;
      common_vendor.index.showLoading({ title: "清除中..." });
      setTimeout(() => {
        common_vendor.index.hideLoading();
        common_vendor.index.showToast({ title: "清除成功", icon: "success" });
        cacheSize.value = "0KB";
      }, 1e3);
    };
    const contactService = () => {
      common_vendor.index.makePhoneCall({
        phoneNumber: "400-123-4567"
      });
    };
    const aboutUs = () => {
      common_vendor.index.navigateTo({ url: "/pages/mine/about" });
    };
    const handleUserCardClick = async () => {
      const confirmed = await utils_index.showConfirm("确定退出登录吗？", "退出登录");
      if (!confirmed)
        return;
      await logout();
    };
    const logout = async () => {
      try {
        await api_auth.logout();
      } catch (e) {
      }
      common_vendor.index.removeStorageSync("token");
      common_vendor.index.removeStorageSync("userInfo");
      common_vendor.index.removeStorageSync("clinicId");
      common_vendor.index.removeStorageSync("clinicName");
      common_vendor.index.removeStorageSync("clinicList");
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
    };
    return (_ctx, _cache) => {
      return {
        a: userInfo.value.avatar || "/static/default-avatar.png",
        b: common_vendor.t(userInfo.value.realName || userInfo.value.username || "用户"),
        c: common_vendor.t(common_vendor.unref(utils_index.formatPhone)(userInfo.value.phone)),
        d: common_vendor.o(handleUserCardClick),
        e: common_vendor.o(goToPatients),
        f: common_vendor.o(goToPayments),
        g: common_vendor.o(goToMedicalRecords),
        h: common_vendor.t(clinicName.value || "未选择"),
        i: common_vendor.o(selectClinic),
        j: common_vendor.t(cacheSize.value),
        k: common_vendor.o(clearCache),
        l: common_vendor.o(contactService),
        m: common_vendor.o(aboutUs),
        n: common_vendor.o(logout)
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-9023ef44"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/mine/index.vue"]]);
wx.createPage(MiniProgramPage);

"use strict";
const common_vendor = require("../../common/vendor.js");
const api_auth = require("../../api/auth.js");
require("../../utils/request.js");
if (!Array) {
  const _component_uni_popup = common_vendor.resolveComponent("uni-popup");
  _component_uni_popup();
}
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const agreed = common_vendor.ref(false);
    const showAccountLogin = common_vendor.ref(false);
    const accountForm = common_vendor.ref({
      username: "",
      password: ""
    });
    const accountPopup = common_vendor.ref(null);
    const isDevTools = common_vendor.ref(false);
    common_vendor.onMounted(() => {
      const systemInfo = common_vendor.index.getSystemInfoSync();
      isDevTools.value = systemInfo.platform === "devtools";
    });
    const handleDevLogin = async () => {
      if (!agreed.value) {
        common_vendor.index.showToast({ title: "请先同意用户协议", icon: "none" });
        return;
      }
      common_vendor.index.showLoading({ title: "模拟登录中..." });
      try {
        const mockUserInfo = {
          id: 1,
          username: "test_user",
          realName: "测试用户",
          phone: "13800138000",
          avatar: ""
        };
        common_vendor.index.setStorageSync("token", "mock_token_" + Date.now());
        common_vendor.index.setStorageSync("userInfo", JSON.stringify(mockUserInfo));
        common_vendor.index.showToast({ title: "模拟登录成功", icon: "success" });
        setTimeout(() => {
          common_vendor.index.switchTab({ url: "/pages/index/index" });
        }, 1500);
      } catch (error) {
        common_vendor.index.showToast({ title: "登录失败", icon: "none" });
      } finally {
        common_vendor.index.hideLoading();
      }
    };
    const handleGetPhoneNumber = async (e) => {
      if (!agreed.value) {
        common_vendor.index.showToast({ title: "请先同意用户协议", icon: "none" });
        return;
      }
      if (e.detail.errMsg !== "getPhoneNumber:ok") {
        common_vendor.index.showToast({ title: "请授权手机号", icon: "none" });
        return;
      }
      common_vendor.index.showLoading({ title: "登录中..." });
      try {
        const loginRes = await common_vendor.index.login({ provider: "weixin" });
        const res = await api_auth.miniappLogin({
          code: loginRes.code,
          encryptedData: e.detail.encryptedData,
          iv: e.detail.iv
        });
        console.log("登录返回数据:", res);
        console.log("token:", res.data.token);
        common_vendor.index.setStorageSync("token", res.data.token);
        common_vendor.index.setStorageSync("userInfo", JSON.stringify(res.data.userInfo));
        const savedToken = common_vendor.index.getStorageSync("token");
        console.log("保存后的token:", savedToken);
        handleLoginSuccess(res.data);
      } catch (error) {
        common_vendor.index.showToast({ title: error.message || "登录失败", icon: "none" });
      } finally {
        common_vendor.index.hideLoading();
      }
    };
    const handleAccountLogin = async () => {
      if (!accountForm.value.username || !accountForm.value.password) {
        common_vendor.index.showToast({ title: "请输入用户名和密码", icon: "none" });
        return;
      }
      common_vendor.index.showLoading({ title: "登录中..." });
      try {
        const res = await api_auth.login({
          username: accountForm.value.username,
          password: accountForm.value.password
        });
        common_vendor.index.setStorageSync("token", res.data.token);
        common_vendor.index.setStorageSync("userInfo", JSON.stringify(res.data.userInfo));
        handleLoginSuccess(res.data);
        closeAccountLogin();
      } catch (error) {
        common_vendor.index.showToast({ title: error.message || "登录失败", icon: "none" });
      } finally {
        common_vendor.index.hideLoading();
      }
    };
    const handleLoginSuccess = (data) => {
      common_vendor.index.switchTab({ url: "/pages/index/index" });
    };
    const openAgreement = () => {
      common_vendor.index.navigateTo({ url: "/pages/webview/index?url=agreement" });
    };
    const openPrivacy = () => {
      common_vendor.index.navigateTo({ url: "/pages/webview/index?url=privacy" });
    };
    const closeAccountLogin = () => {
      var _a;
      (_a = accountPopup.value) == null ? void 0 : _a.close();
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(handleGetPhoneNumber),
        b: isDevTools.value
      }, isDevTools.value ? {
        c: common_vendor.o(handleDevLogin)
      } : {}, {
        d: common_vendor.o(($event) => showAccountLogin.value = true),
        e: agreed.value,
        f: common_vendor.o(($event) => agreed.value = !agreed.value),
        g: common_vendor.o(openAgreement),
        h: common_vendor.o(openPrivacy),
        i: common_vendor.o(closeAccountLogin),
        j: accountForm.value.username,
        k: common_vendor.o(($event) => accountForm.value.username = $event.detail.value),
        l: accountForm.value.password,
        m: common_vendor.o(($event) => accountForm.value.password = $event.detail.value),
        n: common_vendor.o(handleAccountLogin),
        o: common_vendor.sr(accountPopup, "45258083-0", {
          "k": "accountPopup"
        }),
        p: common_vendor.p({
          type: "center"
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-45258083"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/login/index.vue"]]);
wx.createPage(MiniProgramPage);

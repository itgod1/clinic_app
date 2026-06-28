"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
if (!Math) {
  "./pages/index/index.js";
  "./pages/login/index.js";
  "./pages/clinic/select.js";
  "./pages/doctor/list.js";
  "./pages/doctor/detail.js";
  "./pages/registration/list.js";
  "./pages/registration/dept-select.js";
  "./pages/registration/confirm.js";
  "./pages/registration/detail.js";
  "./pages/medical/list.js";
  "./pages/patient/list.js";
  "./pages/patient/edit.js";
  "./pages/mine/index.js";
  "./pages/mine/about.js";
  "./pages/payment/detail.js";
  "./pages/payment/outpatient.js";
  "./pages/ai/consult.js";
  "./pages/ai/return-visit.js";
}
const _sfc_main = {
  __name: "App",
  setup(__props) {
    common_vendor.onLaunch(() => {
      console.log("App Launch");
    });
    common_vendor.onShow(() => {
      console.log("App Show");
    });
    common_vendor.onHide(() => {
      console.log("App Hide");
    });
    return () => {
    };
  }
};
const App = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__file", "F:/work-code/agent-dev/clinic-miniapp/src/App.vue"]]);
function createApp() {
  const app = common_vendor.createSSRApp(App);
  return {
    app
  };
}
createApp().app.mount("#app");
exports.createApp = createApp;

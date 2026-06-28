"use strict";
const common_vendor = require("../common/vendor.js");
const BASE_URL = "http://10.13.34.148:8080/api";
const NO_CLINIC_ID_URLS = [
  "/auth/",
  "/public/",
  "/clinic/list",
  "/miniapp/clinic/",
  "/miniapp/clinics",
  "/miniapp/auth/",
  "/miniapp/schedule/",
  "/miniapp/doctor/",
  "/ai/return-visit/",
  "/ai/dify/"
];
const PUBLIC_URLS = [
  "/auth/",
  "/public/",
  "/miniapp/"
];
const AI_RETURN_VISIT_URLS = [
  "/ai/return-visit/"
];
const isAIReturnVisitUrl = (url) => {
  return AI_RETURN_VISIT_URLS.some((u) => url.includes(u));
};
const isPublicUrl = (url) => {
  return PUBLIC_URLS.some((u) => url.includes(u));
};
const needClinicId = (url) => {
  return !NO_CLINIC_ID_URLS.some((u) => url.includes(u));
};
const request = (options) => {
  return new Promise((resolve, reject) => {
    const url = options.url;
    const method = (options.method || "GET").toUpperCase();
    let data = options.data || {};
    let params = options.params || {};
    const token = common_vendor.index.getStorageSync("token");
    const clinicId = common_vendor.index.getStorageSync("clinicId");
    const userInfoStr = common_vendor.index.getStorageSync("userInfo");
    let userId = null;
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        userId = userInfo.userId || userInfo.id;
        console.log("获取到userId:", userId);
      } catch (e) {
        console.error("解析userInfo失败:", e);
      }
    }
    if (needClinicId(url)) {
      if (!clinicId) {
        common_vendor.index.showToast({ title: "请先选择诊所", icon: "none" });
        reject(new Error("请先选择诊所"));
        return;
      }
      if (method === "GET") {
        params = { ...params, clinicId };
      } else {
        data = { ...data, clinicId };
      }
    }
    console.log("检查就诊人接口:", url, "userId:", userId, "method:", method);
    const needUserIdUrls = ["/miniapp/patient/", "/miniapp/payments/"];
    const needUserId = needUserIdUrls.some((u) => url.includes(u));
    if (needUserId && userId) {
      if (method === "GET") {
        params = { ...params, userId };
        console.log("GET请求添加userId到params:", params);
      } else {
        data = { ...data, userId };
        console.log("POST请求添加userId到data:", data);
      }
    }
    let fullUrl = BASE_URL + url;
    if (method === "GET" && Object.keys(params).length > 0) {
      const queryString = Object.keys(params).map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`).join("&");
      fullUrl += (fullUrl.includes("?") ? "&" : "?") + queryString;
    }
    const requestConfig = {
      url: fullUrl,
      method,
      data: method !== "GET" ? data : void 0,
      header: {
        "Content-Type": "application/json"
      }
    };
    if (method !== "GET" && data) {
      console.log("POST请求data:", JSON.stringify(data));
    }
    if (token) {
      const authValue = "Bearer " + token;
      requestConfig.header["Authorization"] = authValue;
      console.log("设置Authorization:", authValue.substring(0, 30) + "...");
    }
    if (clinicId) {
      requestConfig.header["X-Tenant-Id"] = clinicId;
    }
    if (isAIReturnVisitUrl(url)) {
      requestConfig.header["X-API-Key"] = "sk-clinic-ai-2025-a1b2c3d4e5f6";
    }
    console.log("====== 请求调试 ======");
    console.log("请求URL:", fullUrl);
    console.log("请求header:", JSON.stringify(requestConfig.header));
    common_vendor.index.request({
      ...requestConfig,
      success: (res) => {
        console.log("响应状态:", res.statusCode);
        if (res.statusCode === 200) {
          const result = res.data;
          if (result.code === 200) {
            resolve(result);
          } else if (result.code === 401) {
            if (isPublicUrl(url)) {
              reject(result);
              return;
            }
            common_vendor.index.removeStorageSync("token");
            common_vendor.index.removeStorageSync("userInfo");
            common_vendor.index.showToast({ title: "登录已过期", icon: "none" });
            setTimeout(() => {
              common_vendor.index.reLaunch({ url: "/pages/login/index" });
            }, 1500);
            reject(result);
          } else if (result.code === 403) {
            common_vendor.index.showModal({
              title: "提示",
              content: "您没有权限访问该诊所",
              showCancel: false,
              success: () => {
                common_vendor.index.reLaunch({ url: "/pages/clinic/select" });
              }
            });
            reject(result);
          } else {
            common_vendor.index.showToast({
              title: result.message || "请求失败",
              icon: "none"
            });
            reject(result);
          }
        } else {
          common_vendor.index.showToast({ title: "网络错误", icon: "none" });
          reject(res);
        }
      },
      fail: (err) => {
        console.log("请求失败:", err);
        common_vendor.index.showToast({ title: "网络请求失败", icon: "none" });
        reject(err);
      }
    });
  });
};
exports.request = request;

"use strict";
const common_vendor = require("../common/vendor.js");
const DIFY_CONFIG = {
  // Dify API 基础地址
  baseURL: "http://192.168.1.9/v1",
  // 你的 Dify Chat 应用 API Key
  apiKey: "your-dify-api-key-here",
  // 应用类型: 'chat' 或 'completion'
  appType: "chat"
};
const sendMessage = (query, conversationId = "", inputs = {}) => {
  return new Promise((resolve, reject) => {
    const data = {
      inputs,
      query,
      response_mode: "blocking",
      // blocking 或 streaming
      conversation_id: conversationId,
      user: getUserId()
      // 用户标识
    };
    common_vendor.index.request({
      url: `${DIFY_CONFIG.baseURL}/chat-messages`,
      method: "POST",
      header: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${DIFY_CONFIG.apiKey}`
      },
      data,
      success: (res) => {
        var _a;
        if (res.statusCode === 200) {
          resolve(res.data);
        } else {
          reject(new Error(((_a = res.data) == null ? void 0 : _a.message) || "请求失败"));
        }
      },
      fail: reject
    });
  });
};
function getUserId() {
  const userInfo = common_vendor.index.getStorageSync("userInfo");
  if (userInfo) {
    try {
      const user = JSON.parse(userInfo);
      return `user_${user.userId || user.id || "anonymous"}`;
    } catch (e) {
      return "user_anonymous";
    }
  }
  let anonymousId = common_vendor.index.getStorageSync("anonymousUserId");
  if (!anonymousId) {
    anonymousId = `anon_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    common_vendor.index.setStorageSync("anonymousUserId", anonymousId);
  }
  return anonymousId;
}
exports.sendMessage = sendMessage;

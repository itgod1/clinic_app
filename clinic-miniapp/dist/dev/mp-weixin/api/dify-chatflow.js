"use strict";
const common_vendor = require("../common/vendor.js");
const utils_request = require("../utils/request.js");
const sendChatflowMessage = (query, conversationId = "", inputs = {}) => {
  const data = {
    query,
    conversation_id: conversationId,
    inputs,
    response_mode: "blocking",
    user: getUserId()
  };
  console.log("发送 Chatflow 请求(后端转发):", {
    conversation_id: conversationId,
    inputs: Object.keys(inputs)
  });
  return utils_request.request({
    url: "/ai/dify/chat",
    method: "POST",
    data
  });
};
function getUserId() {
  const userInfo = common_vendor.index.getStorageSync("userInfo");
  if (userInfo) {
    try {
      const user = JSON.parse(userInfo);
      return `user_${user.userId || user.id || user.patientId || "anonymous"}`;
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
exports.sendChatflowMessage = sendChatflowMessage;

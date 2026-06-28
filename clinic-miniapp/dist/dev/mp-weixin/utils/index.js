"use strict";
const common_vendor = require("../common/vendor.js");
const formatPhone = (phone) => {
  if (!phone || phone.length !== 11)
    return phone;
  return phone.replace(/(\d{3})\d{4}(\d{4})/, "$1****$2");
};
const debounce = (fn, delay = 300) => {
  let timer = null;
  return function(...args) {
    if (timer)
      clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
};
const generateDateList = (days = 7) => {
  const list = [];
  const today = /* @__PURE__ */ new Date();
  for (let i = 0; i < days; i++) {
    const date = new Date(today);
    date.setDate(today.getDate() + i);
    list.push({
      date: common_vendor.dayjs(date).format("YYYY-MM-DD"),
      day: i === 0 ? "今天" : i === 1 ? "明天" : ["周日", "周一", "周二", "周三", "周四", "周五", "周六"][date.getDay()],
      weekDay: date.getDay()
    });
  }
  return list;
};
const showSuccess = (title = "操作成功", duration = 1500) => {
  common_vendor.index.showToast({ title, icon: "success", duration });
};
const showError = (title = "操作失败", duration = 1500) => {
  common_vendor.index.showToast({ title, icon: "none", duration });
};
const showConfirm = (content, title = "提示") => {
  return new Promise((resolve) => {
    common_vendor.index.showModal({
      title,
      content,
      success: (res) => {
        resolve(res.confirm);
      }
    });
  });
};
exports.debounce = debounce;
exports.formatPhone = formatPhone;
exports.generateDateList = generateDateList;
exports.showConfirm = showConfirm;
exports.showError = showError;
exports.showSuccess = showSuccess;

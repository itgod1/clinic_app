"use strict";
const utils_request = require("../utils/request.js");
const getUnpaidList = (params) => {
  return utils_request.request({
    url: "/miniapp/payments/unpaid",
    method: "GET",
    params
  });
};
const getPaymentHistory = (params) => {
  return utils_request.request({
    url: "/miniapp/payments/history",
    method: "GET",
    params
  });
};
const getPaymentDetail = (id) => {
  return utils_request.request({
    url: `/miniapp/payments/${id}`,
    method: "GET"
  });
};
const payPrescription = (data) => {
  return utils_request.request({
    url: "/miniapp/payments/pay",
    method: "POST",
    data
  });
};
exports.getPaymentDetail = getPaymentDetail;
exports.getPaymentHistory = getPaymentHistory;
exports.getUnpaidList = getUnpaidList;
exports.payPrescription = payPrescription;

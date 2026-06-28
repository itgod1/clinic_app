"use strict";
const utils_request = require("../utils/request.js");
const getMyRegistrationList = (params) => {
  return utils_request.request({
    url: "/miniapp/registration/my-list",
    method: "GET",
    params
  });
};
const getRegistrationDetail = (id) => {
  return utils_request.request({
    url: `/admin/registration/${id}`,
    method: "GET"
  });
};
const createRegistration = (data) => {
  return utils_request.request({
    url: "/admin/registration/create",
    method: "POST",
    data
  });
};
const cancelRegistration = (data) => {
  return utils_request.request({
    url: "/admin/registration/cancel",
    method: "POST",
    data
  });
};
const getRegistrationQueueInfo = (id) => {
  return utils_request.request({
    url: `/miniapp/registration/${id}/queue`,
    method: "GET"
  });
};
exports.cancelRegistration = cancelRegistration;
exports.createRegistration = createRegistration;
exports.getMyRegistrationList = getMyRegistrationList;
exports.getRegistrationDetail = getRegistrationDetail;
exports.getRegistrationQueueInfo = getRegistrationQueueInfo;

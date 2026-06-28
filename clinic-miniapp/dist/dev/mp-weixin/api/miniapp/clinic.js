"use strict";
const utils_request = require("../../utils/request.js");
const getClinicList = (params) => {
  return utils_request.request({
    url: "/miniapp/clinics",
    method: "GET",
    params
  });
};
const getClinicDetail = (id) => {
  return utils_request.request({
    url: `/miniapp/clinics/${id}`,
    method: "GET"
  });
};
const getDepartmentList = (params) => {
  return utils_request.request({
    url: "/miniapp/departments",
    method: "GET",
    params
  });
};
const switchClinic = (clinicId) => {
  return utils_request.request({
    url: "/miniapp/clinic/switch",
    method: "POST",
    params: { clinicId }
  });
};
exports.getClinicDetail = getClinicDetail;
exports.getClinicList = getClinicList;
exports.getDepartmentList = getDepartmentList;
exports.switchClinic = switchClinic;

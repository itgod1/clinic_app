"use strict";
const utils_request = require("../utils/request.js");
const getPatientList = (params) => {
  return utils_request.request({
    url: "/miniapp/patient/list",
    method: "GET",
    params
  });
};
const getPatientDetail = (id) => {
  return utils_request.request({
    url: `/miniapp/patient/${id}`,
    method: "GET"
  });
};
const createPatient = (data) => {
  return utils_request.request({
    url: "/miniapp/patient/create",
    method: "POST",
    data
  });
};
const updatePatient = (data) => {
  return utils_request.request({
    url: "/miniapp/patient/update",
    method: "POST",
    data
  });
};
exports.createPatient = createPatient;
exports.getPatientDetail = getPatientDetail;
exports.getPatientList = getPatientList;
exports.updatePatient = updatePatient;

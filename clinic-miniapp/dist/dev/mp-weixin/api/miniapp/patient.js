"use strict";
const utils_request = require("../../utils/request.js");
const getPatientList = (params) => {
  return utils_request.request({
    url: "/miniapp/patient/list",
    method: "GET",
    params
  });
};
exports.getPatientList = getPatientList;

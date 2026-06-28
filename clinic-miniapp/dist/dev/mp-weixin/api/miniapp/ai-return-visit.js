"use strict";
const utils_request = require("../../utils/request.js");
const recordTaskOpen = (taskId) => {
  return utils_request.request({
    url: "/ai/return-visit/task/open",
    method: "POST",
    params: { taskId }
  });
};
const getPatientProfile = (patientId) => {
  return utils_request.request({
    url: `/ai/return-visit/patient/${patientId}/profile`,
    method: "GET"
  });
};
exports.getPatientProfile = getPatientProfile;
exports.recordTaskOpen = recordTaskOpen;

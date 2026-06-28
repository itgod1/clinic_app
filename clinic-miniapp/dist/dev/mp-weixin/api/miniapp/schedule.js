"use strict";
const utils_request = require("../../utils/request.js");
const getDoctorSchedule = (params) => {
  return utils_request.request({
    url: "/miniapp/schedules",
    method: "GET",
    params
  });
};
exports.getDoctorSchedule = getDoctorSchedule;

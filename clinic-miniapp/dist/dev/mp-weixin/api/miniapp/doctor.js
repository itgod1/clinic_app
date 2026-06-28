"use strict";
const utils_request = require("../../utils/request.js");
const getDoctorList = (params) => {
  return utils_request.request({
    url: "/miniapp/doctors",
    method: "GET",
    params
  });
};
const getDoctorDetail = (id) => {
  return utils_request.request({
    url: `/miniapp/doctors/${id}`,
    method: "GET"
  });
};
exports.getDoctorDetail = getDoctorDetail;
exports.getDoctorList = getDoctorList;

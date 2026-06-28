"use strict";
const utils_request = require("../utils/request.js");
const getMedicalRecordList = (params) => {
  return utils_request.request({
    url: "/miniapp/medical/record/list",
    method: "GET",
    params
  });
};
exports.getMedicalRecordList = getMedicalRecordList;

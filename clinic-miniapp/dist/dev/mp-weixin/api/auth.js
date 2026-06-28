"use strict";
const utils_request = require("../utils/request.js");
const miniappLogin = (data) => {
  return utils_request.request({
    url: "/auth/miniapp/login",
    method: "POST",
    data
  });
};
const login = (data) => {
  return utils_request.request({
    url: "/auth/login",
    method: "POST",
    data
  });
};
const logout = () => {
  return utils_request.request({
    url: "/auth/logout",
    method: "POST"
  });
};
exports.login = login;
exports.logout = logout;
exports.miniappLogin = miniappLogin;

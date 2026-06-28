"use strict";
const common_vendor = require("../../common/vendor.js");
const api_miniapp_doctor = require("../../api/miniapp/doctor.js");
const api_miniapp_schedule = require("../../api/miniapp/schedule.js");
const utils_index = require("../../utils/index.js");
require("../../utils/request.js");
const _sfc_main = {
  __name: "detail",
  setup(__props) {
    const doctor = common_vendor.ref({});
    const dateList = common_vendor.ref([]);
    const selectedDate = common_vendor.ref("");
    const selectedTime = common_vendor.ref("");
    const scheduleData = common_vendor.ref({});
    const morningSlots = ["08:00-08:30", "08:30-09:00", "09:00-09:30", "09:30-10:00", "10:00-10:30", "10:30-11:00", "11:00-11:30"];
    const afternoonSlots = ["14:00-14:30", "14:30-15:00", "15:00-15:30", "15:30-16:00", "16:00-16:30", "16:30-17:00"];
    const specialtyList = common_vendor.computed(() => {
      if (!doctor.value.specialty)
        return [];
      return doctor.value.specialty.split(/[,，、]/).filter(Boolean);
    });
    const canSubmit = common_vendor.computed(() => {
      return selectedDate.value && selectedTime.value && isTimeAvailable(selectedTime.value);
    });
    common_vendor.onMounted(() => {
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      const doctorId = currentPage.options.id;
      if (doctorId) {
        loadDoctorDetail(doctorId);
        initDateList();
      }
    });
    const initDateList = () => {
      dateList.value = utils_index.generateDateList(7);
      if (dateList.value.length > 0) {
        selectDate(dateList.value[0]);
      }
    };
    const loadDoctorDetail = async (id) => {
      try {
        const res = await api_miniapp_doctor.getDoctorDetail(id);
        doctor.value = res.data || {};
      } catch (error) {
        console.error("加载医生详情失败:", error);
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      }
    };
    const loadSchedule = async () => {
      if (!doctor.value.id || !selectedDate.value)
        return;
      try {
        const res = await api_miniapp_schedule.getDoctorSchedule({
          doctorId: doctor.value.id,
          scheduleDate: selectedDate.value
        });
        scheduleData.value = res.data || {};
      } catch (error) {
        console.error("加载排班失败:", error);
      }
    };
    const selectDate = (date) => {
      selectedDate.value = date.date;
      selectedTime.value = "";
      loadSchedule();
    };
    const selectTime = (time) => {
      selectedTime.value = time;
    };
    const isTimeAvailable = (time) => {
      return true;
    };
    const submitRegistration = () => {
      const params = {
        doctorId: doctor.value.id,
        doctorName: doctor.value.doctorName,
        doctorTitle: doctor.value.position,
        departmentName: doctor.value.deptName,
        deptId: doctor.value.deptId,
        regDate: selectedDate.value,
        regTime: selectedTime.value,
        regFee: doctor.value.regFee
      };
      common_vendor.index.navigateTo({
        url: `/pages/registration/confirm?data=${encodeURIComponent(JSON.stringify(params))}`
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: doctor.value.avatarUrl || "/static/default-avatar.png",
        b: common_vendor.t(doctor.value.doctorName),
        c: common_vendor.t(doctor.value.position),
        d: common_vendor.t(doctor.value.deptName),
        e: common_vendor.t(doctor.value.registrationCount || 0),
        f: common_vendor.t(doctor.value.goodRate || "100%"),
        g: common_vendor.t(doctor.value.introduction || "暂无简介"),
        h: doctor.value.specialty
      }, doctor.value.specialty ? {
        i: common_vendor.f(common_vendor.unref(specialtyList), (tag, index, i0) => {
          return {
            a: common_vendor.t(tag),
            b: index
          };
        })
      } : {}, {
        j: common_vendor.f(dateList.value, (date, k0, i0) => {
          return {
            a: common_vendor.t(date.day),
            b: common_vendor.t(date.date.slice(5)),
            c: date.date,
            d: common_vendor.n(selectedDate.value === date.date ? "active" : ""),
            e: common_vendor.o(($event) => selectDate(date), date.date)
          };
        }),
        k: common_vendor.f(morningSlots, (time, k0, i0) => {
          return {
            a: common_vendor.t(time),
            b: time,
            c: common_vendor.n(selectedTime.value === time ? "active" : ""),
            d: common_vendor.n(""),
            e: common_vendor.o(($event) => selectTime(time), time)
          };
        }),
        l: common_vendor.f(afternoonSlots, (time, k0, i0) => {
          return {
            a: common_vendor.t(time),
            b: time,
            c: common_vendor.n(selectedTime.value === time ? "active" : ""),
            d: common_vendor.n(""),
            e: common_vendor.o(($event) => selectTime(time), time)
          };
        }),
        m: common_vendor.t(doctor.value.regFee || 0),
        n: !common_vendor.unref(canSubmit),
        o: common_vendor.o(submitRegistration)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-842e07c6"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/doctor/detail.vue"]]);
wx.createPage(MiniProgramPage);

"use strict";
const common_vendor = require("../../common/vendor.js");
const api_registration = require("../../api/registration.js");
const api_patient = require("../../api/patient.js");
const api_miniapp_schedule = require("../../api/miniapp/schedule.js");
const utils_index = require("../../utils/index.js");
require("../../utils/request.js");
if (!Array) {
  const _component_uni_popup = common_vendor.resolveComponent("uni-popup");
  _component_uni_popup();
}
const _sfc_main = {
  __name: "confirm",
  setup(__props) {
    const regData = common_vendor.ref({});
    const patientList = common_vendor.ref([]);
    const selectedPatient = common_vendor.ref({});
    const visitType = common_vendor.ref(1);
    const chiefComplaint = common_vendor.ref("");
    const submitting = common_vendor.ref(false);
    const patientPopup = common_vendor.ref(null);
    const dateList = common_vendor.ref([]);
    const selectedDate = common_vendor.ref("");
    const selectedTime = common_vendor.ref("");
    const morningSlots = ["08:00-08:30", "08:30-09:00", "09:00-09:30", "09:30-10:00", "10:00-10:30", "10:30-11:00", "11:00-11:30"];
    const afternoonSlots = ["14:00-14:30", "14:30-15:00", "15:00-15:30", "15:30-16:00", "16:00-16:30", "16:30-17:00"];
    const timeSlots = common_vendor.ref([
      { name: "上午", range: "08:00-12:00", value: "08:00-12:00", available: false },
      { name: "下午", range: "14:00-18:00", value: "14:00-18:00", available: false }
    ]);
    const scheduleData = common_vendor.ref([]);
    const isTimeAvailable = (time) => {
      if (!scheduleData.value || scheduleData.value.length === 0) {
        return false;
      }
      const startTime = time.split("-")[0];
      const [hour, minute] = startTime.split(":").map(Number);
      const slotStartMinutes = hour * 60 + minute;
      const schedule = scheduleData.value.find((s) => {
        if (!s.timeSlotStart || !s.timeSlotEnd)
          return false;
        const scheduleStartHour = parseInt(s.timeSlotStart.substring(0, 2));
        const scheduleStartMinute = parseInt(s.timeSlotStart.substring(3, 5));
        const scheduleStartMinutes = scheduleStartHour * 60 + scheduleStartMinute;
        const scheduleEndHour = parseInt(s.timeSlotEnd.substring(0, 2));
        const scheduleEndMinute = parseInt(s.timeSlotEnd.substring(3, 5));
        const scheduleEndMinutes = scheduleEndHour * 60 + scheduleEndMinute;
        return slotStartMinutes >= scheduleStartMinutes && slotStartMinutes < scheduleEndMinutes;
      });
      return schedule && schedule.remainingQuota > 0;
    };
    const canSubmit = common_vendor.computed(() => {
      return selectedPatient.value.id && selectedDate.value && selectedTime.value && !submitting.value;
    });
    common_vendor.onMounted(() => {
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      const data = currentPage.options.data;
      if (data) {
        try {
          regData.value = JSON.parse(decodeURIComponent(data));
        } catch (e) {
          console.error("解析参数失败:", e);
        }
      }
      generateDateList();
      loadPatients();
    });
    const generateDateList = () => {
      var _a;
      const weekDays = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
      const list = [];
      const today = /* @__PURE__ */ new Date();
      for (let i = 0; i < 7; i++) {
        const date = new Date(today);
        date.setDate(today.getDate() + i);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        const dateStr = `${year}-${month}-${day}`;
        list.push({
          value: dateStr,
          week: i === 0 ? "今天" : weekDays[date.getDay()],
          day: `${month}-${day}`
        });
      }
      dateList.value = list;
      selectedDate.value = ((_a = list[0]) == null ? void 0 : _a.value) || "";
      loadSchedule();
    };
    const selectDate = (date) => {
      selectedDate.value = date;
      selectedTime.value = "";
      loadSchedule();
    };
    const selectTime = (time) => {
      selectedTime.value = time;
    };
    const loadSchedule = async () => {
      if (!regData.value.doctorId || !selectedDate.value)
        return;
      try {
        const clinicId = common_vendor.index.getStorageSync("clinicId");
        const res = await api_miniapp_schedule.getDoctorSchedule({
          clinicId,
          doctorId: regData.value.doctorId,
          startDate: selectedDate.value,
          endDate: selectedDate.value
        });
        const schedules = res.data || [];
        scheduleData.value = schedules;
        timeSlots.value[0].available = false;
        timeSlots.value[1].available = false;
        schedules.forEach((schedule) => {
          var _a;
          const startHour = parseInt(((_a = schedule.timeSlotStart) == null ? void 0 : _a.substring(0, 2)) || "0");
          const remaining = schedule.remainingQuota || 0;
          if (startHour < 12) {
            timeSlots.value[0].available = remaining > 0;
          } else {
            timeSlots.value[1].available = remaining > 0;
          }
        });
        console.log("排班数据:", schedules);
      } catch (error) {
        console.error("加载排班失败:", error);
        scheduleData.value = [];
      }
    };
    const loadPatients = async () => {
      try {
        const clinicId = common_vendor.index.getStorageSync("clinicId");
        if (!clinicId) {
          common_vendor.index.showToast({ title: "请先选择诊所", icon: "none" });
          return;
        }
        const res = await api_patient.getPatientList({ pageSize: 100, clinicId });
        patientList.value = Array.isArray(res.data) ? res.data : res.data.list || [];
        if (patientList.value.length > 0 && !selectedPatient.value.id) {
          selectedPatient.value = patientList.value[0];
        }
      } catch (error) {
        console.error("加载就诊人列表失败:", error);
      }
    };
    const selectPatient = () => {
      var _a;
      if (patientList.value.length === 0) {
        addPatient();
        return;
      }
      (_a = patientPopup.value) == null ? void 0 : _a.open();
    };
    const confirmSelectPatient = (patient) => {
      selectedPatient.value = patient;
      closePatientPopup();
    };
    const closePatientPopup = () => {
      var _a;
      (_a = patientPopup.value) == null ? void 0 : _a.close();
    };
    const addPatient = () => {
      common_vendor.index.navigateTo({
        url: "/pages/patient/edit?from=registration"
      });
    };
    const submit = async () => {
      if (!canSubmit.value)
        return;
      submitting.value = true;
      common_vendor.index.showLoading({ title: "提交中..." });
      console.log("selectedPatient:", selectedPatient.value);
      console.log("patientName:", selectedPatient.value.patientName);
      console.log("phone:", selectedPatient.value.phone);
      try {
        await api_registration.createRegistration({
          doctorId: regData.value.doctorId,
          deptId: regData.value.deptId,
          patientId: selectedPatient.value.id,
          patientName: selectedPatient.value.patientName,
          phone: selectedPatient.value.phone,
          regDate: selectedDate.value,
          regTime: selectedTime.value,
          visitType: visitType.value,
          chiefComplaint: chiefComplaint.value
        });
        common_vendor.index.showToast({
          title: "预约成功",
          icon: "success",
          duration: 1500,
          success: () => {
            setTimeout(() => {
              common_vendor.index.switchTab({
                url: "/pages/registration/list",
                success: () => {
                  console.log("跳转成功");
                },
                fail: (err) => {
                  console.error("跳转失败:", err);
                  common_vendor.index.showToast({ title: "跳转失败", icon: "none" });
                }
              });
            }, 1500);
          }
        });
      } catch (error) {
        utils_index.showError(error.message || "预约失败");
      } finally {
        submitting.value = false;
        common_vendor.index.hideLoading();
      }
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(regData.value.doctorName),
        b: common_vendor.t(regData.value.doctorTitle),
        c: common_vendor.t(regData.value.departmentName),
        d: common_vendor.t(regData.value.regFee),
        e: common_vendor.f(dateList.value, (date, k0, i0) => {
          return {
            a: common_vendor.t(date.week),
            b: common_vendor.t(date.day),
            c: date.value,
            d: common_vendor.n(selectedDate.value === date.value ? "active" : ""),
            e: common_vendor.o(($event) => selectDate(date.value), date.value)
          };
        }),
        f: common_vendor.f(morningSlots, (time, k0, i0) => {
          return {
            a: common_vendor.t(time),
            b: time,
            c: common_vendor.n(selectedTime.value === time ? "active" : ""),
            d: common_vendor.n(!isTimeAvailable(time) ? "disabled" : ""),
            e: common_vendor.o(($event) => isTimeAvailable(time) && selectTime(time), time)
          };
        }),
        g: common_vendor.f(afternoonSlots, (time, k0, i0) => {
          return {
            a: common_vendor.t(time),
            b: time,
            c: common_vendor.n(selectedTime.value === time ? "active" : ""),
            d: common_vendor.n(!isTimeAvailable(time) ? "disabled" : ""),
            e: common_vendor.o(($event) => isTimeAvailable(time) && selectTime(time), time)
          };
        }),
        h: common_vendor.o(addPatient),
        i: selectedPatient.value.id
      }, selectedPatient.value.id ? {
        j: common_vendor.t(selectedPatient.value.patientName || "未获取"),
        k: common_vendor.t(selectedPatient.value.phone || "未获取"),
        l: common_vendor.t(selectedPatient.value.gender === 1 ? "男" : "女"),
        m: common_vendor.t(selectedPatient.value.age),
        n: common_vendor.t(selectedPatient.value.phone)
      } : {
        o: common_vendor.o(selectPatient)
      }, {
        p: common_vendor.n(visitType.value === 1 ? "active" : ""),
        q: common_vendor.o(($event) => visitType.value = 1),
        r: common_vendor.n(visitType.value === 2 ? "active" : ""),
        s: common_vendor.o(($event) => visitType.value = 2),
        t: chiefComplaint.value,
        v: common_vendor.o(($event) => chiefComplaint.value = $event.detail.value),
        w: common_vendor.t(chiefComplaint.value.length),
        x: common_vendor.t(regData.value.regFee),
        y: !common_vendor.unref(canSubmit),
        z: submitting.value,
        A: common_vendor.o(submit),
        B: common_vendor.o(closePatientPopup),
        C: common_vendor.f(patientList.value, (patient, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(patient.patientName),
            b: common_vendor.t(patient.gender === 1 ? "男" : "女"),
            c: common_vendor.t(patient.age),
            d: common_vendor.t(patient.phone),
            e: selectedPatient.value.id === patient.id
          }, selectedPatient.value.id === patient.id ? {} : {}, {
            f: patient.id,
            g: common_vendor.n(selectedPatient.value.id === patient.id ? "selected" : ""),
            h: common_vendor.o(($event) => confirmSelectPatient(patient), patient.id)
          });
        }),
        D: common_vendor.o(addPatient),
        E: common_vendor.sr(patientPopup, "f960a9f2-0", {
          "k": "patientPopup"
        }),
        F: common_vendor.p({
          type: "bottom"
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-f960a9f2"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/registration/confirm.vue"]]);
wx.createPage(MiniProgramPage);

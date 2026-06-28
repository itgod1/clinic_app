"use strict";
const common_vendor = require("../../common/vendor.js");
const api_miniapp_clinic = require("../../api/miniapp/clinic.js");
const api_miniapp_doctor = require("../../api/miniapp/doctor.js");
require("../../utils/request.js");
const _sfc_main = {
  __name: "dept-select",
  setup(__props) {
    const departmentList = common_vendor.ref([]);
    const doctorList = common_vendor.ref([]);
    const selectedDeptId = common_vendor.ref(null);
    const loading = common_vendor.ref(false);
    const clinicName = common_vendor.ref("");
    common_vendor.onMounted(() => {
      const clinicId = common_vendor.index.getStorageSync("clinicId");
      clinicName.value = common_vendor.index.getStorageSync("clinicName") || "未知诊所";
      if (clinicId) {
        loadDepartments(clinicId);
      } else {
        common_vendor.index.showToast({ title: "请先选择诊所", icon: "none" });
        setTimeout(() => {
          common_vendor.index.switchTab({ url: "/pages/index/index" });
        }, 1500);
      }
    });
    const loadDepartments = async (clinicId) => {
      var _a, _b;
      try {
        const res = await api_miniapp_clinic.getDepartmentList({ clinicId, pageSize: 100 });
        departmentList.value = ((_a = res.data) == null ? void 0 : _a.list) || ((_b = res.data) == null ? void 0 : _b.records) || [];
        if (departmentList.value.length > 0) {
          selectDept(departmentList.value[0]);
        }
      } catch (error) {
        console.error("加载科室失败:", error);
        common_vendor.index.showToast({ title: "加载科室失败", icon: "none" });
      }
    };
    const selectDept = async (dept) => {
      selectedDeptId.value = dept.id;
      await loadDoctors(dept.id);
    };
    const loadDoctors = async (deptId) => {
      var _a, _b;
      loading.value = true;
      try {
        const clinicId = common_vendor.index.getStorageSync("clinicId");
        const res = await api_miniapp_doctor.getDoctorList({
          clinicId,
          deptId,
          pageSize: 100
        });
        doctorList.value = ((_a = res.data) == null ? void 0 : _a.list) || ((_b = res.data) == null ? void 0 : _b.records) || [];
      } catch (error) {
        console.error("加载医生失败:", error);
        common_vendor.index.showToast({ title: "加载医生失败", icon: "none" });
      } finally {
        loading.value = false;
      }
    };
    const selectDoctor = (doctor) => {
      if (!doctor.hasSchedule) {
        common_vendor.index.showToast({ title: "该医生暂无号源", icon: "none" });
        return;
      }
      const regData = {
        doctorId: doctor.id,
        doctorName: doctor.doctorName,
        doctorTitle: doctor.position,
        deptId: selectedDeptId.value,
        departmentName: doctor.deptName,
        regDate: (/* @__PURE__ */ new Date()).toISOString().split("T")[0],
        regTime: "08:00-12:00",
        regFee: doctor.regFee || 0
      };
      common_vendor.index.navigateTo({
        url: `/pages/registration/confirm?data=${encodeURIComponent(JSON.stringify(regData))}`
      });
    };
    const goBack = () => {
      common_vendor.index.navigateBack();
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(goBack),
        b: clinicName.value
      }, clinicName.value ? {
        c: common_vendor.t(clinicName.value)
      } : {}, {
        d: common_vendor.f(departmentList.value, (dept, k0, i0) => {
          return common_vendor.e({
            a: selectedDeptId.value === dept.id
          }, selectedDeptId.value === dept.id ? {} : {}, {
            b: common_vendor.t(dept.deptName),
            c: dept.id,
            d: common_vendor.n(selectedDeptId.value === dept.id ? "active" : ""),
            e: common_vendor.o(($event) => selectDept(dept), dept.id)
          });
        }),
        e: loading.value
      }, loading.value ? {} : doctorList.value.length === 0 ? {} : {
        g: common_vendor.f(doctorList.value, (doctor, k0, i0) => {
          return common_vendor.e({
            a: doctor.avatarUrl || "/static/default-avatar.png",
            b: doctor.hasSchedule
          }, doctor.hasSchedule ? {} : {}, {
            c: common_vendor.t(doctor.doctorName),
            d: common_vendor.t(doctor.position),
            e: doctor.specialty
          }, doctor.specialty ? {
            f: common_vendor.f(doctor.specialty.split(",").slice(0, 2), (tag, idx, i1) => {
              return {
                a: common_vendor.t(tag),
                b: idx
              };
            })
          } : {}, {
            g: common_vendor.t(doctor.introduction || "专业口腔医师，经验丰富"),
            h: common_vendor.t(doctor.regFee || 0),
            i: common_vendor.t(doctor.hasSchedule ? "立即预约" : "已满"),
            j: !doctor.hasSchedule ? 1 : "",
            k: common_vendor.o(($event) => selectDoctor(doctor), doctor.id),
            l: doctor.id,
            m: common_vendor.o(($event) => selectDoctor(doctor), doctor.id)
          });
        })
      }, {
        f: doctorList.value.length === 0
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-993c260a"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/registration/dept-select.vue"]]);
wx.createPage(MiniProgramPage);

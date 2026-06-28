"use strict";
const common_vendor = require("../../common/vendor.js");
const api_patient = require("../../api/patient.js");
require("../../utils/request.js");
if (!Math) {
  EmptyData();
}
const EmptyData = () => "../../components/empty-data.js";
const _sfc_main = {
  __name: "list",
  setup(__props) {
    const loading = common_vendor.ref(false);
    const patientList = common_vendor.ref([]);
    common_vendor.onMounted(() => {
      loadPatients();
    });
    common_vendor.onShow(() => {
      loadPatients();
    });
    common_vendor.onPullDownRefresh(() => {
      loadPatients().finally(() => {
        common_vendor.index.stopPullDownRefresh();
      });
    });
    const loadPatients = async () => {
      loading.value = true;
      try {
        const clinicId = common_vendor.index.getStorageSync("clinicId");
        if (!clinicId) {
          common_vendor.index.showToast({ title: "请先选择诊所", icon: "none" });
          return;
        }
        const res = await api_patient.getPatientList({ pageSize: 100, clinicId });
        patientList.value = Array.isArray(res.data) ? res.data : res.data.list || [];
      } catch (error) {
        console.error("加载就诊人列表失败:", error);
      } finally {
        loading.value = false;
      }
    };
    const formatIdCard = (idCard) => {
      if (!idCard || idCard.length !== 18)
        return idCard;
      return idCard.replace(/(\d{6})\d{8}(\d{4})/, "$1********$2");
    };
    const addPatient = () => {
      common_vendor.index.navigateTo({ url: "/pages/patient/edit" });
    };
    const editPatient = (patient) => {
      common_vendor.index.navigateTo({
        url: `/pages/patient/edit?id=${patient.id}`
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(patientList.value, (patient, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(patient.name),
            b: common_vendor.t(patient.gender === 1 ? "男" : "女"),
            c: common_vendor.n(patient.gender === 1 ? "male" : "female"),
            d: common_vendor.t(patient.age),
            e: patient.isDefault
          }, patient.isDefault ? {} : {}, {
            f: common_vendor.t(patient.phone),
            g: common_vendor.t(formatIdCard(patient.idCard)),
            h: patient.id,
            i: common_vendor.o(($event) => editPatient(patient), patient.id)
          });
        }),
        b: patientList.value.length === 0 && !loading.value
      }, patientList.value.length === 0 && !loading.value ? {
        c: common_vendor.o(addPatient),
        d: common_vendor.p({
          text: "暂无就诊人",
          subtext: "请添加就诊人",
          ["show-button"]: true,
          ["button-text"]: "添加就诊人"
        })
      } : {}, {
        e: common_vendor.o(addPatient)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-35cb59a8"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/patient/list.vue"]]);
wx.createPage(MiniProgramPage);

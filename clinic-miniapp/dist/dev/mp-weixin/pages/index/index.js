"use strict";
const common_vendor = require("../../common/vendor.js");
const api_miniapp_doctor = require("../../api/miniapp/doctor.js");
const api_miniapp_clinic = require("../../api/miniapp/clinic.js");
const api_miniapp_patient = require("../../api/miniapp/patient.js");
require("../../utils/request.js");
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const clinicInfo = common_vendor.ref({});
    const doctorList = common_vendor.ref([]);
    const loading = common_vendor.ref(false);
    const selectedClinicId = common_vendor.ref(null);
    const bannerList = common_vendor.ref([
      {
        bgGradient: "linear-gradient(135deg, #00b894 0%, #00d4a8 100%)",
        pattern: "radial-gradient(circle at 80% 20%, rgba(255,255,255,0.1) 0%, transparent 50%)",
        iconBg: "rgba(255,255,255,0.15)",
        emoji: "👨‍⚕️",
        title: "专家坐诊",
        subtitle: "三甲医院专家定期坐诊",
        btnText: "立即预约",
        url: "/pages/doctor/list"
      },
      {
        bgGradient: "linear-gradient(135deg, #00a884 0%, #00b894 100%)",
        pattern: "radial-gradient(circle at 20% 80%, rgba(255,255,255,0.1) 0%, transparent 50%)",
        iconBg: "rgba(255,255,255,0.15)",
        emoji: "📅",
        title: "在线挂号",
        subtitle: "提前预约 无需排队",
        btnText: "去挂号",
        url: "/pages/registration/dept-select"
      },
      {
        bgGradient: "linear-gradient(135deg, #00c8a0 0%, #00d4b8 100%)",
        pattern: "radial-gradient(circle at 70% 70%, rgba(255,255,255,0.1) 0%, transparent 50%)",
        iconBg: "rgba(255,255,255,0.15)",
        emoji: "🎁",
        title: "优惠活动",
        subtitle: "洁牙套餐限时特惠中",
        btnText: "查看详情",
        url: "/pages/index/index"
      }
    ]);
    const quickActions = common_vendor.ref([
      { icon: "📋", label: "预约挂号", bgColor: "linear-gradient(135deg, #e8f5f0 0%, #d4edda 100%)", needLogin: true, needClinic: true, url: "/pages/registration/dept-select" },
      { icon: "💳", label: "门诊缴费", bgColor: "linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%)", needLogin: true, needClinic: true, url: "/pages/payment/outpatient" },
      { icon: "📄", label: "就诊记录", bgColor: "linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%)", needLogin: true, needClinic: true, url: "/pages/medical/list" },
      { icon: "📍", label: "来院导航", bgColor: "linear-gradient(135deg, #fce4ec 0%, #f8bbd9 100%)", needLogin: false, needClinic: true, url: "" },
      { icon: "🤖", label: "AI问诊", bgColor: "linear-gradient(135deg, #f3e5f5 0%, #e1bee7 100%)", needLogin: false, needClinic: false, url: "/pages/ai/consult", type: "consult" },
      { icon: "📋", label: "AI回访", bgColor: "linear-gradient(135deg, #e8f5f0 0%, #d4edda 100%)", needLogin: true, needClinic: false, url: "/pages/ai/return-visit", type: "return-visit" },
      { icon: "💬", label: "联系客服", bgColor: "linear-gradient(135deg, #e0f7fa 0%, #b2ebf2 100%)", needLogin: false, needClinic: false, url: "" },
      { icon: "🏷️", label: "优惠活动", bgColor: "linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%)", needLogin: false, needClinic: false, url: "" },
      { icon: "📰", label: "健康资讯", bgColor: "linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%)", needLogin: false, needClinic: false, url: "" }
    ]);
    const clinicName = common_vendor.computed(() => {
      if (selectedClinicId.value) {
        return common_vendor.index.getStorageSync("clinicName") || "选择诊所";
      }
      return "选择诊所";
    });
    const isLoggedIn = common_vendor.computed(() => {
      return !!common_vendor.index.getStorageSync("token");
    });
    common_vendor.onMounted(() => {
      updateClinicStatus();
      loadPublicData();
      if (isLoggedIn.value) {
        loadPrivateData();
      }
    });
    common_vendor.onShow(() => {
      const oldClinicId = selectedClinicId.value;
      updateClinicStatus();
      const newClinicId = selectedClinicId.value;
      if (oldClinicId !== newClinicId) {
        loadDoctors();
      }
    });
    common_vendor.onPullDownRefresh(() => {
      loadPublicData().finally(() => {
        common_vendor.index.stopPullDownRefresh();
      });
    });
    const loadPublicData = async () => {
      loading.value = true;
      try {
        const clinicId = common_vendor.index.getStorageSync("clinicId");
        if (clinicId) {
          const res = await api_miniapp_clinic.getClinicDetail(clinicId);
          clinicInfo.value = res.data || {};
        }
        await loadDoctors();
      } catch (error) {
        console.error("加载公开数据失败:", error);
      } finally {
        loading.value = false;
      }
    };
    const loadPrivateData = async () => {
    };
    const loadDoctors = async () => {
      var _a, _b;
      try {
        const clinicId = common_vendor.index.getStorageSync("clinicId");
        const params = { pageSize: 6 };
        if (clinicId) {
          params.clinicId = clinicId;
        }
        const res = await api_miniapp_doctor.getDoctorList(params);
        doctorList.value = ((_a = res.data) == null ? void 0 : _a.records) || ((_b = res.data) == null ? void 0 : _b.list) || res.data || [];
      } catch (error) {
        console.error("加载医生列表失败:", error);
        doctorList.value = [];
      }
    };
    const hasSelectedClinic = common_vendor.computed(() => {
      return !!selectedClinicId.value;
    });
    const updateClinicStatus = () => {
      const clinicId = common_vendor.index.getStorageSync("clinicId");
      selectedClinicId.value = clinicId ? parseInt(clinicId) : null;
    };
    const handleActionClick = (item) => {
      if (item.needClinic !== false && !hasSelectedClinic.value) {
        common_vendor.index.showModal({
          title: "提示",
          content: "请先选择就诊诊所",
          confirmText: "去选择",
          cancelText: "取消",
          success: (res) => {
            if (res.confirm) {
              common_vendor.index.navigateTo({ url: "/pages/clinic/select" });
            }
          }
        });
        return;
      }
      const token = common_vendor.index.getStorageSync("token");
      common_vendor.index.getStorageSync("userInfo");
      if (item.needLogin && !token) {
        common_vendor.index.showModal({
          title: "提示",
          content: "你还没有登录哦...",
          confirmText: "马上登录",
          cancelText: "取消",
          success: (res) => {
            if (res.confirm) {
              common_vendor.index.navigateTo({ url: "/pages/login/index" });
            }
          }
        });
        return;
      }
      if (item.type === "return-visit") {
        handleAIReturnVisit();
        return;
      }
      if (item.url) {
        if (item.url.startsWith("/pages")) {
          common_vendor.index.navigateTo({ url: item.url });
        } else {
          common_vendor.index.switchTab({ url: item.url });
        }
      } else {
        common_vendor.index.showToast({ title: "功能开发中", icon: "none" });
      }
    };
    const onBannerClick = (item) => {
      if (!hasSelectedClinic.value) {
        common_vendor.index.showModal({
          title: "提示",
          content: "请先选择就诊诊所",
          confirmText: "去选择",
          cancelText: "取消",
          success: (res) => {
            if (res.confirm) {
              common_vendor.index.navigateTo({ url: "/pages/clinic/select" });
            }
          }
        });
        return;
      }
      if (item.url) {
        common_vendor.index.navigateTo({ url: item.url });
      }
    };
    const goToSearch = () => {
      common_vendor.index.showToast({ title: "搜索功能开发中", icon: "none" });
    };
    const switchClinic = () => {
      common_vendor.index.navigateTo({ url: "/pages/clinic/select" });
    };
    const goToDoctors = () => {
      common_vendor.index.switchTab({ url: "/pages/doctor/list" });
    };
    const goToDoctorDetail = (doctor) => {
      if (!hasSelectedClinic.value) {
        common_vendor.index.showModal({
          title: "提示",
          content: "请先选择就诊诊所",
          confirmText: "去选择",
          cancelText: "取消",
          success: (res) => {
            if (res.confirm) {
              common_vendor.index.navigateTo({ url: "/pages/clinic/select" });
            }
          }
        });
        return;
      }
      common_vendor.index.navigateTo({ url: `/pages/doctor/detail?id=${doctor.id}` });
    };
    const handleAIReturnVisit = async () => {
      var _a;
      common_vendor.index.showLoading({ title: "加载中..." });
      try {
        const userInfoStr = common_vendor.index.getStorageSync("userInfo");
        const userInfo = userInfoStr ? JSON.parse(userInfoStr) : null;
        let patientId = (userInfo == null ? void 0 : userInfo.patientId) || ((_a = userInfo == null ? void 0 : userInfo.patient) == null ? void 0 : _a.id);
        if (!patientId) {
          const res = await api_miniapp_patient.getPatientList();
          const patients = res.data || [];
          if (patients.length === 0) {
            common_vendor.index.hideLoading();
            common_vendor.index.showModal({
              title: "提示",
              content: "您还没有绑定就诊人，请先绑定",
              confirmText: "去绑定",
              cancelText: "取消",
              success: (res2) => {
                if (res2.confirm) {
                  common_vendor.index.navigateTo({ url: "/pages/patient/list" });
                }
              }
            });
            return;
          }
          if (patients.length > 1) {
            common_vendor.index.hideLoading();
            showPatientSelector(patients);
            return;
          }
          patientId = patients[0].id;
        }
        common_vendor.index.hideLoading();
        common_vendor.index.navigateTo({
          url: `/pages/ai/return-visit?patient_id=${patientId}&from=home`
        });
      } catch (error) {
        common_vendor.index.hideLoading();
        console.error("获取就诊人信息失败:", error);
        common_vendor.index.showToast({ title: "加载失败，请重试", icon: "none" });
      }
    };
    const showPatientSelector = (patients) => {
      const patientNames = patients.map(
        (p, index) => `${p.patientName || p.name} (${p.gender === "MALE" || p.gender === "男" ? "男" : "女"} ${p.age || ""}岁)`
      );
      common_vendor.index.showActionSheet({
        title: "请选择要回访的就诊人",
        itemList: patientNames,
        success: (res) => {
          const selectedPatient = patients[res.tapIndex];
          const patientId = selectedPatient.id;
          common_vendor.index.navigateTo({
            url: `/pages/ai/return-visit?patient_id=${patientId}&patientName=${selectedPatient.patientName || selectedPatient.name}&patientGender=${selectedPatient.gender}&patientAge=${selectedPatient.age}&from=home`
          });
        },
        fail: (res) => {
          console.log("用户取消选择");
        }
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(common_vendor.unref(clinicName) || "选择诊所"),
        b: common_vendor.o(switchClinic),
        c: common_vendor.o(goToSearch),
        d: common_vendor.f(bannerList.value, (item, index, i0) => {
          return {
            a: item.pattern,
            b: common_vendor.t(item.title),
            c: common_vendor.t(item.subtitle),
            d: common_vendor.t(item.btnText),
            e: common_vendor.t(item.emoji),
            f: item.iconBg,
            g: item.bgGradient,
            h: index,
            i: common_vendor.o(($event) => onBannerClick(item), index)
          };
        }),
        e: common_vendor.f(quickActions.value, (item, index, i0) => {
          return {
            a: common_vendor.t(item.icon),
            b: item.bgColor,
            c: common_vendor.t(item.label),
            d: index,
            e: common_vendor.o(($event) => handleActionClick(item), index)
          };
        }),
        f: common_vendor.o(goToDoctors),
        g: common_vendor.f(doctorList.value, (doctor, k0, i0) => {
          return common_vendor.e({
            a: doctor.avatarUrl || "/static/default-avatar.png",
            b: doctor.hasSchedule
          }, doctor.hasSchedule ? {} : {}, {
            c: common_vendor.t(doctor.doctorName),
            d: common_vendor.t(doctor.position),
            e: common_vendor.t(doctor.deptName),
            f: doctor.id,
            g: common_vendor.o(($event) => goToDoctorDetail(doctor), doctor.id)
          });
        }),
        h: clinicInfo.value.name
      }, clinicInfo.value.name ? {
        i: common_vendor.t(clinicInfo.value.name),
        j: common_vendor.t(clinicInfo.value.slogan || "专业口腔医疗服务")
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-83a5a03c"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/index/index.vue"]]);
wx.createPage(MiniProgramPage);

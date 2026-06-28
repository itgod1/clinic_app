"use strict";
const common_vendor = require("../../common/vendor.js");
const api_patient = require("../../api/patient.js");
const utils_index = require("../../utils/index.js");
require("../../utils/request.js");
const _sfc_main = {
  __name: "edit",
  setup(__props) {
    const form = common_vendor.ref({
      patientName: "",
      gender: 1,
      age: "",
      phone: "",
      idCard: "",
      birthDate: "",
      address: "",
      isDefault: false
    });
    const patientId = common_vendor.ref(null);
    const submitting = common_vendor.ref(false);
    const isEdit = common_vendor.computed(() => !!patientId.value);
    const canSubmit = common_vendor.computed(() => {
      return form.value.patientName && form.value.gender && form.value.age && form.value.phone && !submitting.value;
    });
    common_vendor.onMounted(() => {
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      const id = currentPage.options.id;
      if (id) {
        patientId.value = id;
        loadPatientDetail(id);
      }
    });
    const loadPatientDetail = async (id) => {
      try {
        const res = await api_patient.getPatientDetail(id);
        const data = res.data;
        form.value = {
          patientName: data.patientName || data.name,
          gender: data.gender || 1,
          age: data.age ? String(data.age) : "",
          phone: data.phone,
          idCard: data.idCard || "",
          birthDate: data.birthDate || "",
          address: data.address || "",
          isDefault: data.isDefault || false
        };
      } catch (error) {
        console.error("加载就诊人详情失败:", error);
        utils_index.showError("加载失败");
      }
    };
    const onBirthDateChange = (e) => {
      form.value.birthDate = e.detail.value;
    };
    const submit = async () => {
      if (!canSubmit.value)
        return;
      if (!/^1[3-9]\d{9}$/.test(form.value.phone)) {
        utils_index.showError("请输入正确的手机号");
        return;
      }
      submitting.value = true;
      common_vendor.index.showLoading({ title: "保存中..." });
      try {
        const data = {
          ...form.value,
          age: parseInt(form.value.age)
        };
        if (isEdit.value) {
          await api_patient.updatePatient({ ...data, id: patientId.value });
        } else {
          await api_patient.createPatient(data);
        }
        utils_index.showSuccess(isEdit.value ? "保存成功" : "添加成功");
        setTimeout(() => {
          common_vendor.index.navigateBack();
        }, 1500);
      } catch (error) {
        utils_index.showError(error.message || "保存失败");
      } finally {
        submitting.value = false;
        common_vendor.index.hideLoading();
      }
    };
    return (_ctx, _cache) => {
      return {
        a: form.value.patientName,
        b: common_vendor.o(($event) => form.value.patientName = $event.detail.value),
        c: common_vendor.t(form.value.gender === 1 ? "●" : "○"),
        d: common_vendor.n(form.value.gender === 1 ? "active" : ""),
        e: common_vendor.o(($event) => form.value.gender = 1),
        f: common_vendor.t(form.value.gender === 2 ? "●" : "○"),
        g: common_vendor.n(form.value.gender === 2 ? "active" : ""),
        h: common_vendor.o(($event) => form.value.gender = 2),
        i: form.value.age,
        j: common_vendor.o(($event) => form.value.age = $event.detail.value),
        k: form.value.phone,
        l: common_vendor.o(($event) => form.value.phone = $event.detail.value),
        m: form.value.idCard,
        n: common_vendor.o(($event) => form.value.idCard = $event.detail.value),
        o: common_vendor.t(form.value.birthDate || "请选择出生日期（选填）"),
        p: common_vendor.n(!form.value.birthDate ? "placeholder" : ""),
        q: form.value.birthDate,
        r: common_vendor.o(onBirthDateChange),
        s: form.value.address,
        t: common_vendor.o(($event) => form.value.address = $event.detail.value),
        v: form.value.isDefault,
        w: common_vendor.o(($event) => form.value.isDefault = $event.detail.value),
        x: common_vendor.t(common_vendor.unref(isEdit) ? "保存" : "添加"),
        y: !common_vendor.unref(canSubmit),
        z: submitting.value,
        A: common_vendor.o(submit)
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-aa01721d"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/patient/edit.vue"]]);
wx.createPage(MiniProgramPage);

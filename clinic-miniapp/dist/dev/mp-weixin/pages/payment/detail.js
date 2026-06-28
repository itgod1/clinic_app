"use strict";
const common_vendor = require("../../common/vendor.js");
const api_payment = require("../../api/payment.js");
require("../../utils/request.js");
if (!Math) {
  (EmptyData + PaymentCashier)();
}
const EmptyData = () => "../../components/empty-data.js";
const PaymentCashier = () => "../../components/payment-cashier.js";
const _sfc_main = {
  __name: "detail",
  setup(__props) {
    const loading = common_vendor.ref(true);
    const prescription = common_vendor.ref({});
    const prescriptionId = common_vendor.ref(null);
    const showCashier = common_vendor.ref(false);
    const selectedPayMethod = common_vendor.ref(null);
    common_vendor.onMounted(() => {
      var _a;
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      const { id } = ((_a = currentPage.$page) == null ? void 0 : _a.options) || currentPage.options || {};
      if (id) {
        prescriptionId.value = id;
        loadDetail();
      } else {
        loading.value = false;
        common_vendor.index.showToast({ title: "参数错误", icon: "none" });
      }
    });
    const loadDetail = async () => {
      loading.value = true;
      try {
        const res = await api_payment.getPaymentDetail(prescriptionId.value);
        prescription.value = res.data || {};
      } catch (error) {
        console.error("加载处方详情失败:", error);
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      } finally {
        loading.value = false;
      }
    };
    const getStatusText = (status) => {
      const map = {
        0: "待缴费",
        1: "已支付",
        2: "已退款"
      };
      return map[status] || "未知";
    };
    const getStatusClass = (status) => {
      const map = {
        0: "status-unpaid",
        1: "status-paid",
        2: "status-refund"
      };
      return map[status] || "";
    };
    const getStatusIcon = (status) => {
      const map = {
        0: "⏰",
        1: "✅",
        2: "↩️"
      };
      return map[status] || "❓";
    };
    const getPayMethodText = (method) => {
      const map = {
        1: "现金",
        2: "微信支付",
        3: "支付宝",
        4: "银行卡",
        5: "会员卡"
      };
      return map[method] || "未知";
    };
    const formatTime = (timeStr) => {
      if (!timeStr)
        return "-";
      const date = new Date(timeStr);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
    };
    const handlePay = () => {
      console.log("点击立即支付");
      console.log("处方金额:", prescription.value.totalAmount);
      if (!prescription.value.totalAmount || prescription.value.totalAmount <= 0) {
        common_vendor.index.showToast({ title: "支付金额错误", icon: "none" });
        return;
      }
      console.log("显示收银台");
      showCashier.value = true;
    };
    const closeCashier = () => {
      showCashier.value = false;
      selectedPayMethod.value = null;
    };
    const confirmPay = async (paymentMethod) => {
      selectedPayMethod.value = paymentMethod;
      showCashier.value = false;
      if (paymentMethod === 1) {
        common_vendor.index.showModal({
          title: "现金支付",
          content: "您选择了现金支付，请到医院前台缴费",
          confirmText: "我知道了",
          showCancel: false
        });
        return;
      }
      common_vendor.index.showLoading({ title: "支付中..." });
      try {
        const clinicId = common_vendor.index.getStorageSync("clinicId");
        await api_payment.payPrescription({
          prescriptionId: prescription.value.id,
          clinicId,
          paymentMethod
        });
        common_vendor.index.hideLoading();
        common_vendor.index.showToast({ title: "支付成功", icon: "success" });
        setTimeout(() => {
          loadDetail();
        }, 1500);
      } catch (error) {
        common_vendor.index.hideLoading();
        console.error("支付失败:", error);
        common_vendor.index.showToast({ title: error.message || "支付失败", icon: "none" });
      }
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: loading.value
      }, loading.value ? {} : prescription.value.id ? common_vendor.e({
        c: common_vendor.t(getStatusIcon(prescription.value.paymentStatus)),
        d: common_vendor.t(getStatusText(prescription.value.paymentStatus)),
        e: common_vendor.n(getStatusClass(prescription.value.paymentStatus)),
        f: common_vendor.t(prescription.value.prescriptionNo),
        g: common_vendor.t(prescription.value.patientName),
        h: common_vendor.t(prescription.value.doctorName),
        i: common_vendor.t(prescription.value.departmentName),
        j: common_vendor.t(prescription.value.diagnosis || "-"),
        k: common_vendor.t(formatTime(prescription.value.createdAt)),
        l: common_vendor.t(prescription.value.totalAmount || "0.00"),
        m: prescription.value.discountAmount && prescription.value.discountAmount > 0
      }, prescription.value.discountAmount && prescription.value.discountAmount > 0 ? {
        n: common_vendor.t(prescription.value.discountAmount)
      } : {}, {
        o: common_vendor.t(prescription.value.actualAmount || prescription.value.totalAmount || "0.00"),
        p: prescription.value.paymentStatus === 1
      }, prescription.value.paymentStatus === 1 ? common_vendor.e({
        q: common_vendor.t(getPayMethodText(prescription.value.paymentMethod)),
        r: prescription.value.payTime
      }, prescription.value.payTime ? {
        s: common_vendor.t(formatTime(prescription.value.payTime))
      } : {}) : {}, {
        t: prescription.value.paymentStatus === 0
      }, prescription.value.paymentStatus === 0 ? {
        v: common_vendor.t(prescription.value.totalAmount || "0.00"),
        w: common_vendor.o(handlePay)
      } : {}) : {
        x: common_vendor.p({
          text: "处方不存在",
          subtext: "请检查处方信息"
        })
      }, {
        b: prescription.value.id,
        y: common_vendor.o(closeCashier),
        z: common_vendor.o(confirmPay),
        A: common_vendor.p({
          visible: showCashier.value,
          amount: prescription.value.actualAmount || prescription.value.totalAmount || "0.00",
          ["default-method"]: 2
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-540e2f19"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/payment/detail.vue"]]);
wx.createPage(MiniProgramPage);

"use strict";
const common_vendor = require("../../common/vendor.js");
const api_payment = require("../../api/payment.js");
require("../../utils/request.js");
if (!Array) {
  const _component_uni_load_more = common_vendor.resolveComponent("uni-load-more");
  _component_uni_load_more();
}
if (!Math) {
  EmptyData();
}
const EmptyData = () => "../../components/empty-data.js";
const _sfc_main = {
  __name: "outpatient",
  setup(__props) {
    const loading = common_vendor.ref(false);
    const paymentList = common_vendor.ref([]);
    const activeTab = common_vendor.ref(0);
    const pageNum = common_vendor.ref(1);
    const pageSize = 10;
    const loadStatus = common_vendor.ref("more");
    const tabs = [
      { label: "待缴费", value: 0 },
      { label: "已缴费", value: 1 }
    ];
    const emptyText = common_vendor.computed(() => {
      const map = {
        0: "暂无待缴费项目",
        1: "暂无缴费记录"
      };
      return map[activeTab.value];
    });
    common_vendor.onMounted(() => {
      loadPayments();
    });
    common_vendor.onShow(() => {
      pageNum.value = 1;
      loadPayments();
    });
    common_vendor.onPullDownRefresh(() => {
      pageNum.value = 1;
      loadPayments().finally(() => {
        common_vendor.index.stopPullDownRefresh();
      });
    });
    const loadPayments = async () => {
      var _a;
      if (loading.value)
        return;
      loading.value = true;
      try {
        const clinicId = common_vendor.index.getStorageSync("clinicId");
        const userInfo = common_vendor.index.getStorageSync("userInfo");
        if (!clinicId || !userInfo) {
          common_vendor.index.showToast({ title: "请先登录", icon: "none" });
          return;
        }
        const patientInfo = JSON.parse(userInfo);
        const params = {
          clinicId,
          userId: patientInfo.id,
          pageNum: pageNum.value,
          pageSize
        };
        let res;
        if (activeTab.value === 0) {
          res = await api_payment.getUnpaidList(params);
        } else {
          res = await api_payment.getPaymentHistory(params);
        }
        const list = ((_a = res.data) == null ? void 0 : _a.list) || [];
        if (pageNum.value === 1) {
          paymentList.value = list;
        } else {
          paymentList.value.push(...list);
        }
        loadStatus.value = list.length < pageSize ? "noMore" : "more";
      } catch (error) {
        console.error("加载缴费列表失败:", error);
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      } finally {
        loading.value = false;
      }
    };
    const selectTab = (value) => {
      activeTab.value = value;
      pageNum.value = 1;
      loadPayments();
    };
    const loadMore = () => {
      if (loadStatus.value === "noMore" || loading.value)
        return;
      pageNum.value++;
      loadPayments();
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
    const goToPay = (item) => {
      common_vendor.index.navigateTo({
        url: `/pages/payment/detail?id=${item.id}&action=pay`
      });
    };
    const goToDetail = (item) => {
      common_vendor.index.navigateTo({
        url: `/pages/payment/detail?id=${item.id}`
      });
    };
    const goToDoctor = () => {
      common_vendor.index.switchTab({ url: "/pages/doctor/list" });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(tabs, (tab, k0, i0) => {
          return {
            a: common_vendor.t(tab.label),
            b: tab.value,
            c: common_vendor.n(activeTab.value === tab.value ? "active" : ""),
            d: common_vendor.o(($event) => selectTab(tab.value), tab.value)
          };
        }),
        b: common_vendor.f(paymentList.value, (item, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(item.prescriptionNo),
            b: common_vendor.t(item.statusName || getStatusText(item.paymentStatus)),
            c: common_vendor.n(getStatusClass(item.paymentStatus)),
            d: common_vendor.t(item.patientName),
            e: common_vendor.t(item.doctorName),
            f: common_vendor.t(item.departmentName),
            g: common_vendor.t(item.diagnosis || "-"),
            h: common_vendor.t(item.totalAmount || "0.00"),
            i: item.paymentStatus === 0
          }, item.paymentStatus === 0 ? {
            j: common_vendor.o(($event) => goToPay(item), item.id)
          } : {
            k: common_vendor.o(($event) => goToDetail(item), item.id)
          }, {
            l: item.id,
            m: common_vendor.o(($event) => goToDetail(item), item.id)
          });
        }),
        c: common_vendor.p({
          status: loadStatus.value
        }),
        d: paymentList.value.length === 0 && !loading.value
      }, paymentList.value.length === 0 && !loading.value ? {
        e: common_vendor.o(goToDoctor),
        f: common_vendor.p({
          text: common_vendor.unref(emptyText),
          subtext: "暂无相关费用记录",
          ["show-button"]: activeTab.value === 0,
          ["button-text"]: "去挂号"
        })
      } : {}, {
        g: common_vendor.o(loadMore)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-9808683b"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/payment/outpatient.vue"]]);
wx.createPage(MiniProgramPage);

"use strict";
const common_vendor = require("../../common/vendor.js");
const api_miniapp_clinic = require("../../api/miniapp/clinic.js");
require("../../utils/request.js");
const _sfc_main = {
  __name: "select",
  setup(__props) {
    const clinicList = common_vendor.ref([]);
    const currentClinic = common_vendor.ref({});
    const searchKeyword = common_vendor.ref("");
    const loading = common_vendor.ref(false);
    const loadingMore = common_vendor.ref(false);
    const page = common_vendor.ref(1);
    const pageSize = common_vendor.ref(10);
    const hasMore = common_vendor.ref(true);
    const gradients = [
      "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
      "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)",
      "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)",
      "linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)",
      "linear-gradient(135deg, #fa709a 0%, #fee140 100%)",
      "linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)",
      "linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)",
      "linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)"
    ];
    const getGradient = (id) => {
      return gradients[id % gradients.length];
    };
    const getFirstChar = (name) => {
      return name ? name.charAt(0) : "医";
    };
    const getStatusClass = (status) => {
      if (status === 1)
        return "open";
      return "close";
    };
    const getStatusText = (status) => {
      switch (status) {
        case 1:
          return "营业中";
        case 2:
          return "休息中";
        case 3:
          return "停业";
        default:
          return "休息中";
      }
    };
    common_vendor.onMounted(() => {
      const clinicId = common_vendor.index.getStorageSync("clinicId");
      const clinicName = common_vendor.index.getStorageSync("clinicName");
      if (clinicId && clinicName) {
        currentClinic.value = { id: parseInt(clinicId), clinicName };
      }
      loadClinicList();
    });
    const loadClinicList = async (isLoadMore = false) => {
      var _a, _b, _c;
      if (loading.value || loadingMore.value)
        return;
      if (!isLoadMore) {
        loading.value = true;
        page.value = 1;
      } else {
        if (!hasMore.value)
          return;
        loadingMore.value = true;
      }
      try {
        const res = await api_miniapp_clinic.getClinicList({
          page: page.value,
          pageSize: pageSize.value,
          keyword: searchKeyword.value || void 0
        });
        const list = ((_a = res.data) == null ? void 0 : _a.records) || ((_b = res.data) == null ? void 0 : _b.list) || res.data || [];
        const total = ((_c = res.data) == null ? void 0 : _c.total) || list.length;
        if (isLoadMore) {
          clinicList.value = [...clinicList.value, ...list];
        } else {
          clinicList.value = list;
        }
        hasMore.value = clinicList.value.length < total;
        page.value++;
      } catch (error) {
        console.error("加载诊所列表失败:", error);
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      } finally {
        loading.value = false;
        loadingMore.value = false;
      }
    };
    const loadMore = () => {
      loadClinicList(true);
    };
    const goBack = () => {
      const pages = getCurrentPages();
      if (pages.length > 1) {
        common_vendor.index.navigateBack();
      } else {
        common_vendor.index.switchTab({ url: "/pages/index/index" });
      }
    };
    const handleSearch = () => {
      loadClinicList(false);
    };
    const clearSearch = () => {
      searchKeyword.value = "";
      loadClinicList(false);
    };
    const selectClinic = async (clinic) => {
      if (currentClinic.value.id === clinic.id) {
        common_vendor.index.showToast({ title: "已经是当前诊所", icon: "none" });
        return;
      }
      common_vendor.index.showModal({
        title: "确认选择",
        content: `确定选择「${clinic.clinicName}」作为就诊诊所吗？`,
        success: async (res) => {
          if (res.confirm) {
            common_vendor.index.showLoading({ title: "保存中..." });
            try {
              common_vendor.index.setStorageSync("clinicId", clinic.id);
              common_vendor.index.setStorageSync("clinicName", clinic.clinicName);
              const token = common_vendor.index.getStorageSync("token");
              if (token) {
                await api_miniapp_clinic.switchClinic(clinic.id);
              }
              common_vendor.index.hideLoading();
              common_vendor.index.showToast({
                title: "选择成功",
                icon: "success",
                success: () => {
                  setTimeout(() => {
                    common_vendor.index.switchTab({ url: "/pages/index/index" });
                  }, 1500);
                }
              });
            } catch (error) {
              common_vendor.index.hideLoading();
              console.error("切换诊所失败:", error);
              common_vendor.index.showToast({
                title: "选择成功",
                icon: "success",
                success: () => {
                  setTimeout(() => {
                    common_vendor.index.switchTab({ url: "/pages/index/index" });
                  }, 1500);
                }
              });
            }
          }
        }
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(goBack),
        b: common_vendor.o(handleSearch),
        c: searchKeyword.value,
        d: common_vendor.o(($event) => searchKeyword.value = $event.detail.value),
        e: searchKeyword.value
      }, searchKeyword.value ? {
        f: common_vendor.o(clearSearch)
      } : {}, {
        g: currentClinic.value.id
      }, currentClinic.value.id ? common_vendor.e({
        h: common_vendor.t(getFirstChar(currentClinic.value.clinicName)),
        i: getGradient(currentClinic.value.id),
        j: common_vendor.t(currentClinic.value.clinicName),
        k: common_vendor.t(currentClinic.value.address || "暂无地址信息"),
        l: currentClinic.value.contactPhone
      }, currentClinic.value.contactPhone ? {
        m: common_vendor.t(currentClinic.value.contactPhone)
      } : {}) : {}, {
        n: common_vendor.t(searchKeyword.value ? "搜索结果" : "推荐诊所"),
        o: loading.value && clinicList.value.length === 0
      }, loading.value && clinicList.value.length === 0 ? {} : clinicList.value.length === 0 ? common_vendor.e({
        q: searchKeyword.value
      }, searchKeyword.value ? {} : {}) : {
        r: common_vendor.f(clinicList.value, (clinic, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(getFirstChar(clinic.clinicName)),
            b: getGradient(clinic.id),
            c: common_vendor.t(clinic.clinicName),
            d: common_vendor.t(getStatusText(clinic.businessStatus)),
            e: common_vendor.n(getStatusClass(clinic.businessStatus)),
            f: common_vendor.t(clinic.address || "暂无地址信息"),
            g: clinic.contactPhone
          }, clinic.contactPhone ? {
            h: common_vendor.t(clinic.contactPhone)
          } : {}, {
            i: currentClinic.value.id === clinic.id
          }, currentClinic.value.id === clinic.id ? {} : {}, {
            j: clinic.id,
            k: common_vendor.n(currentClinic.value.id === clinic.id ? "disabled" : ""),
            l: common_vendor.o(($event) => selectClinic(clinic), clinic.id)
          });
        })
      }, {
        p: clinicList.value.length === 0,
        s: loadingMore.value
      }, loadingMore.value ? {} : {}, {
        t: common_vendor.o(loadMore)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e8bdac7f"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/clinic/select.vue"]]);
wx.createPage(MiniProgramPage);

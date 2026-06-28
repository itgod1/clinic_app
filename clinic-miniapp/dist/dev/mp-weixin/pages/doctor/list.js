"use strict";
const common_vendor = require("../../common/vendor.js");
const api_miniapp_doctor = require("../../api/miniapp/doctor.js");
const api_miniapp_clinic = require("../../api/miniapp/clinic.js");
const utils_index = require("../../utils/index.js");
require("../../utils/request.js");
if (!Array) {
  const _component_uni_load_more = common_vendor.resolveComponent("uni-load-more");
  _component_uni_load_more();
}
if (!Math) {
  (DoctorCard + EmptyData)();
}
const DoctorCard = () => "../../components/doctor-card.js";
const EmptyData = () => "../../components/empty-data.js";
const _sfc_main = {
  __name: "list",
  setup(__props) {
    const loading = common_vendor.ref(false);
    const doctorList = common_vendor.ref([]);
    const departmentList = common_vendor.ref([{ id: 0, name: "全部" }]);
    const activeDeptId = common_vendor.ref(0);
    const searchKeyword = common_vendor.ref("");
    const pageNum = common_vendor.ref(1);
    const pageSize = 10;
    const loadStatus = common_vendor.ref("more");
    common_vendor.onMounted(() => {
      loadDepartments();
      loadDoctors();
    });
    common_vendor.onPullDownRefresh(() => {
      pageNum.value = 1;
      Promise.all([
        loadDepartments(),
        loadDoctors()
      ]).finally(() => {
        common_vendor.index.stopPullDownRefresh();
      });
    });
    const loadDepartments = async () => {
      var _a, _b;
      try {
        const params = { pageSize: 100 };
        const clinicId = common_vendor.index.getStorageSync("clinicId");
        if (clinicId) {
          params.clinicId = clinicId;
        }
        const res = await api_miniapp_clinic.getDepartmentList(params);
        const list = ((_a = res.data) == null ? void 0 : _a.list) || ((_b = res.data) == null ? void 0 : _b.records) || [];
        const formattedList = list.map((item) => ({
          ...item,
          name: item.deptName || item.name
        }));
        departmentList.value = [{ id: 0, name: "全部" }, ...formattedList];
      } catch (error) {
        console.error("加载科室列表失败:", error);
      }
    };
    const loadDoctors = async () => {
      var _a, _b;
      if (loading.value)
        return;
      loading.value = true;
      try {
        const params = {
          page: pageNum.value,
          pageSize
        };
        if (searchKeyword.value && searchKeyword.value.trim()) {
          params.keyword = searchKeyword.value.trim();
        }
        if (activeDeptId.value > 0) {
          params.deptId = activeDeptId.value;
        }
        const clinicId = common_vendor.index.getStorageSync("clinicId");
        if (clinicId) {
          params.clinicId = clinicId;
        }
        const res = await api_miniapp_doctor.getDoctorList(params);
        const list = ((_a = res.data) == null ? void 0 : _a.records) || ((_b = res.data) == null ? void 0 : _b.list) || [];
        if (pageNum.value === 1) {
          doctorList.value = list;
        } else {
          doctorList.value.push(...list);
        }
        loadStatus.value = list.length < pageSize ? "noMore" : "more";
      } catch (error) {
        console.error("加载医生列表失败:", error);
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      } finally {
        loading.value = false;
      }
    };
    const selectDept = (id) => {
      activeDeptId.value = id;
      pageNum.value = 1;
      loadDoctors();
    };
    const handleSearch = utils_index.debounce(() => {
      pageNum.value = 1;
      loadDoctors();
    }, 500);
    const clearSearch = () => {
      searchKeyword.value = "";
      pageNum.value = 1;
      loadDoctors();
    };
    const loadMore = () => {
      if (loadStatus.value === "noMore" || loading.value)
        return;
      pageNum.value++;
      loadDoctors();
    };
    const goToDetail = (doctor) => {
      common_vendor.index.navigateTo({
        url: `/pages/doctor/detail?id=${doctor.id}`
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o((...args) => common_vendor.unref(handleSearch) && common_vendor.unref(handleSearch)(...args)),
        b: searchKeyword.value,
        c: common_vendor.o(($event) => searchKeyword.value = $event.detail.value),
        d: searchKeyword.value
      }, searchKeyword.value ? {
        e: common_vendor.o(clearSearch)
      } : {}, {
        f: common_vendor.f(departmentList.value, (dept, k0, i0) => {
          return {
            a: common_vendor.t(dept.name),
            b: dept.id,
            c: common_vendor.n(activeDeptId.value === dept.id ? "active" : ""),
            d: common_vendor.o(($event) => selectDept(dept.id), dept.id)
          };
        }),
        g: common_vendor.f(doctorList.value, (doctor, k0, i0) => {
          return {
            a: doctor.id,
            b: common_vendor.o(($event) => goToDetail(doctor), doctor.id),
            c: "8115aa71-0-" + i0,
            d: common_vendor.p({
              doctor
            })
          };
        }),
        h: common_vendor.p({
          status: loadStatus.value
        }),
        i: doctorList.value.length === 0 && !loading.value
      }, doctorList.value.length === 0 && !loading.value ? {
        j: common_vendor.p({
          text: "暂无医生信息",
          subtext: "换个条件试试"
        })
      } : {}, {
        k: common_vendor.o(loadMore)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-8115aa71"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/doctor/list.vue"]]);
wx.createPage(MiniProgramPage);

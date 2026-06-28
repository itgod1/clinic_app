"use strict";
const common_vendor = require("../../common/vendor.js");
const api_medical = require("../../api/medical.js");
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
  __name: "list",
  setup(__props) {
    const loading = common_vendor.ref(false);
    const recordList = common_vendor.ref([]);
    const pageNum = common_vendor.ref(1);
    const pageSize = 10;
    const loadStatus = common_vendor.ref("more");
    common_vendor.onMounted(() => {
      loadRecords();
    });
    common_vendor.onPullDownRefresh(() => {
      pageNum.value = 1;
      loadRecords().finally(() => {
        common_vendor.index.stopPullDownRefresh();
      });
    });
    const loadRecords = async () => {
      if (loading.value)
        return;
      loading.value = true;
      try {
        const clinicId = common_vendor.index.getStorageSync("clinicId");
        const res = await api_medical.getMedicalRecordList({
          clinicId,
          pageNum: pageNum.value,
          pageSize
        });
        const list = res.data.list || [];
        const formattedList = list.map((item) => ({
          id: item.id,
          recordNo: item.recordNo || "",
          visitDate: item.visitDate || (item.createdAt ? item.createdAt.split("T")[0] : ""),
          status: 2,
          doctorName: item.doctorName || "",
          doctorTitle: "",
          departmentName: item.deptName || "",
          diagnosis: item.diagnosis || "",
          chiefComplaint: item.chiefComplaint || "",
          treatment: item.treatment || "",
          medicalAdvice: item.medicalAdvice || "",
          visitTypeName: item.visitTypeName || "门诊",
          prescriptionId: null
        }));
        if (pageNum.value === 1) {
          recordList.value = formattedList;
        } else {
          recordList.value.push(...formattedList);
        }
        loadStatus.value = list.length < pageSize ? "noMore" : "more";
      } catch (error) {
        console.error("加载就诊记录失败:", error);
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      } finally {
        loading.value = false;
      }
    };
    const loadMore = () => {
      if (loadStatus.value === "noMore" || loading.value)
        return;
      pageNum.value++;
      loadRecords();
    };
    const getStatusText = (status) => {
      const map = {
        0: "待就诊",
        1: "就诊中",
        2: "已完成",
        3: "已取消"
      };
      return map[status] || "已完成";
    };
    const getStatusClass = (status) => {
      const map = {
        0: "pending",
        1: "in-progress",
        2: "completed",
        3: "cancelled"
      };
      return map[status] || "completed";
    };
    const getDay = (dateStr) => {
      if (!dateStr)
        return "--";
      const date = new Date(dateStr);
      return date.getDate().toString().padStart(2, "0");
    };
    const getMonth = (dateStr) => {
      if (!dateStr)
        return "--";
      const date = new Date(dateStr);
      return (date.getMonth() + 1).toString();
    };
    const getCurrentYearCount = () => {
      const currentYear = (/* @__PURE__ */ new Date()).getFullYear();
      return recordList.value.filter((r) => {
        if (!r.visitDate)
          return false;
        return new Date(r.visitDate).getFullYear() === currentYear;
      }).length;
    };
    const getLastVisitDate = () => {
      if (recordList.value.length === 0)
        return "-";
      const lastRecord = recordList.value[0];
      if (!lastRecord.visitDate)
        return "-";
      const date = new Date(lastRecord.visitDate);
      return `${date.getMonth() + 1}/${date.getDate()}`;
    };
    const viewDetail = (record) => {
      common_vendor.index.navigateTo({
        url: `/pages/medical/detail?id=${record.id}`
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(recordList.value.length),
        b: common_vendor.t(getCurrentYearCount()),
        c: common_vendor.t(getLastVisitDate()),
        d: common_vendor.f(recordList.value, (record, index, i0) => {
          return common_vendor.e({
            a: common_vendor.t(getDay(record.visitDate)),
            b: common_vendor.t(getMonth(record.visitDate)),
            c: common_vendor.t(record.recordNo),
            d: common_vendor.t(record.visitTypeName || "门诊"),
            e: common_vendor.t(getStatusText(record.status)),
            f: common_vendor.n(getStatusClass(record.status)),
            g: common_vendor.t(record.doctorName || "未知医生"),
            h: common_vendor.t(record.departmentName || "未知科室"),
            i: record.chiefComplaint
          }, record.chiefComplaint ? {
            j: common_vendor.t(record.chiefComplaint)
          } : {}, {
            k: record.diagnosis && record.diagnosis !== "暂无诊断"
          }, record.diagnosis && record.diagnosis !== "暂无诊断" ? {
            l: common_vendor.t(record.diagnosis)
          } : {}, {
            m: record.treatment
          }, record.treatment ? {
            n: common_vendor.t(record.treatment)
          } : {}, {
            o: record.medicalAdvice
          }, record.medicalAdvice ? {
            p: common_vendor.t(record.medicalAdvice)
          } : {}, {
            q: record.id,
            r: common_vendor.o(($event) => viewDetail(record), record.id)
          });
        }),
        e: common_vendor.p({
          status: loadStatus.value
        }),
        f: recordList.value.length === 0 && !loading.value
      }, recordList.value.length === 0 && !loading.value ? {
        g: common_vendor.p({
          text: "暂无就诊记录",
          subtext: "您还没有就诊记录，请先预约挂号"
        })
      } : {}, {
        h: common_vendor.o(loadMore)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-90e36fa3"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/medical/list.vue"]]);
wx.createPage(MiniProgramPage);

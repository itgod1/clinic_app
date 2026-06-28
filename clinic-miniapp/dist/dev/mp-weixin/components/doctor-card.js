"use strict";
const common_vendor = require("../common/vendor.js");
const _sfc_main = {
  __name: "doctor-card",
  props: {
    doctor: {
      type: Object,
      required: true
    }
  },
  emits: ["click"],
  setup(__props, { emit }) {
    const props = __props;
    const statusClass = common_vendor.computed(() => {
      if (props.doctor.hasSchedule === true)
        return "online";
      return "offline";
    });
    const statusText = common_vendor.computed(() => {
      if (props.doctor.hasSchedule === true)
        return "可预约";
      return "休息中";
    });
    const handleClick = () => {
      emit("click", props.doctor);
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.doctor.avatarUrl || "/static/default-avatar.png",
        b: common_vendor.t(__props.doctor.doctorName),
        c: common_vendor.t(__props.doctor.position),
        d: common_vendor.t(__props.doctor.deptName),
        e: __props.doctor.introduction
      }, __props.doctor.introduction ? {
        f: common_vendor.t(__props.doctor.introduction)
      } : {}, {
        g: common_vendor.t(__props.doctor.regFee || 0),
        h: common_vendor.t(common_vendor.unref(statusText)),
        i: common_vendor.n(common_vendor.unref(statusClass)),
        j: common_vendor.o(handleClick)
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-83c945d3"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/components/doctor-card.vue"]]);
wx.createComponent(Component);

"use strict";
const common_vendor = require("../common/vendor.js");
const _sfc_main = {
  __name: "empty-data",
  props: {
    text: {
      type: String,
      default: "暂无数据"
    },
    subtext: {
      type: String,
      default: ""
    },
    image: {
      type: String,
      default: ""
    },
    showButton: {
      type: Boolean,
      default: false
    },
    buttonText: {
      type: String,
      default: "去逛逛"
    },
    top: {
      type: Number,
      default: 100
    },
    bottom: {
      type: Number,
      default: 100
    }
  },
  emits: ["click"],
  setup(__props, { emit }) {
    const handleClick = () => {
      emit("click");
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.image || "/static/empty.png",
        b: common_vendor.t(__props.text),
        c: __props.subtext
      }, __props.subtext ? {
        d: common_vendor.t(__props.subtext)
      } : {}, {
        e: __props.showButton
      }, __props.showButton ? {
        f: common_vendor.t(__props.buttonText),
        g: common_vendor.o(handleClick)
      } : {}, {
        h: __props.top + "rpx",
        i: __props.bottom + "rpx"
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-26d5031f"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/components/empty-data.vue"]]);
wx.createComponent(Component);

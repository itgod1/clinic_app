"use strict";
const common_vendor = require("../common/vendor.js");
const _sfc_main = {
  __name: "payment-cashier",
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    amount: {
      type: [Number, String],
      default: "0.00"
    },
    defaultMethod: {
      type: Number,
      default: 2
      // 默认微信支付
    }
  },
  emits: ["close", "confirm"],
  setup(__props, { emit }) {
    const props = __props;
    const selectedMethod = common_vendor.ref(props.defaultMethod);
    const payMethods = [
      {
        value: 2,
        name: "微信支付",
        icon: "💬",
        bgColor: "#07c160",
        desc: "推荐使用微信支付"
      },
      {
        value: 3,
        name: "支付宝",
        icon: "🔷",
        bgColor: "#1677ff",
        desc: ""
      },
      {
        value: 4,
        name: "银行卡",
        icon: "💳",
        bgColor: "#ff9500",
        desc: "支持储蓄卡/信用卡"
      },
      {
        value: 5,
        name: "会员卡",
        icon: "💎",
        bgColor: "#ffd700",
        desc: "使用会员卡余额支付"
      },
      {
        value: 1,
        name: "现金支付",
        icon: "💵",
        bgColor: "#ff6b6b",
        desc: "到院后现场支付"
      }
    ];
    common_vendor.watch(() => props.visible, (newVal) => {
      if (newVal) {
        selectedMethod.value = props.defaultMethod;
      }
    });
    const close = () => {
      emit("close");
    };
    const selectMethod = (value) => {
      selectedMethod.value = value;
    };
    const confirmPay = () => {
      if (!selectedMethod.value) {
        common_vendor.index.showToast({ title: "请选择支付方式", icon: "none" });
        return;
      }
      emit("confirm", selectedMethod.value);
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.visible
      }, __props.visible ? {
        b: common_vendor.o(close),
        c: common_vendor.o(close),
        d: common_vendor.t(__props.amount),
        e: common_vendor.f(payMethods, (method, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(method.icon),
            b: method.bgColor,
            c: common_vendor.t(method.name),
            d: method.desc
          }, method.desc ? {
            e: common_vendor.t(method.desc)
          } : {}, {
            f: selectedMethod.value === method.value
          }, selectedMethod.value === method.value ? {} : {}, {
            g: method.value,
            h: common_vendor.n(selectedMethod.value === method.value ? "active" : ""),
            i: common_vendor.o(($event) => selectMethod(method.value), method.value)
          });
        }),
        f: common_vendor.t(__props.amount),
        g: !selectedMethod.value,
        h: common_vendor.o(confirmPay)
      } : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-9b61654f"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/components/payment-cashier.vue"]]);
wx.createComponent(Component);

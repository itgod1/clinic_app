"use strict";
const common_vendor = require("../../common/vendor.js");
const api_dify = require("../../api/dify.js");
const _sfc_main = {
  __name: "consult",
  setup(__props) {
    const messages = common_vendor.ref([]);
    const inputMessage = common_vendor.ref("");
    const isLoading = common_vendor.ref(false);
    const scrollTop = common_vendor.ref(0);
    const conversationId = common_vendor.ref("");
    const safeBottom = common_vendor.ref(0);
    const quickQuestions = [
      "头痛是什么原因？",
      "感冒发热怎么办？",
      "口腔溃疡怎么缓解？",
      "牙痛需要看医生吗？",
      "皮肤过敏怎么处理？"
    ];
    const showQuickQuestions = common_vendor.computed(() => messages.value.length === 0);
    common_vendor.onLoad(() => {
      var _a;
      const systemInfo = common_vendor.index.getSystemInfoSync();
      safeBottom.value = ((_a = systemInfo.safeAreaInsets) == null ? void 0 : _a.bottom) || 0;
    });
    const sendMessageHandler = async () => {
      const message = inputMessage.value.trim();
      if (!message || isLoading.value)
        return;
      addMessage("user", message);
      inputMessage.value = "";
      await sendToAI(message);
    };
    const sendQuickQuestion = (question) => {
      inputMessage.value = question;
      sendMessageHandler();
    };
    const sendToAI = async (message) => {
      isLoading.value = true;
      const loadingIndex = messages.value.length;
      addMessage("assistant", "", true);
      try {
        const response = await api_dify.sendMessage(message, conversationId.value);
        if (response.conversation_id) {
          conversationId.value = response.conversation_id;
        }
        messages.value[loadingIndex] = {
          role: "assistant",
          content: response.answer || "抱歉，我没有理解您的问题，请重新描述一下。",
          time: formatTime()
        };
      } catch (error) {
        console.error("AI 请求失败:", error);
        messages.value[loadingIndex] = {
          role: "assistant",
          content: "抱歉，服务暂时不可用，请稍后再试。",
          time: formatTime()
        };
      } finally {
        isLoading.value = false;
        scrollToBottom();
      }
    };
    const addMessage = (role, content, loading = false) => {
      messages.value.push({
        role,
        content,
        loading,
        time: formatTime()
      });
      scrollToBottom();
    };
    const scrollToBottom = () => {
      common_vendor.nextTick$1(() => {
        const query = common_vendor.index.createSelectorQuery();
        query.select(".chat-list").boundingClientRect();
        query.exec((res) => {
          if (res[0]) {
            scrollTop.value = res[0].height;
          }
        });
      });
    };
    const loadMoreHistory = async () => {
      if (!conversationId.value)
        return;
    };
    const formatTime = (date = /* @__PURE__ */ new Date()) => {
      const hours = date.getHours().toString().padStart(2, "0");
      const minutes = date.getMinutes().toString().padStart(2, "0");
      return `${hours}:${minutes}`;
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(messages.value, (msg, index, i0) => {
          return common_vendor.e({
            a: msg.role === "assistant"
          }, msg.role === "assistant" ? {} : {}, {
            b: msg.loading
          }, msg.loading ? {} : {
            c: common_vendor.t(msg.content)
          }, {
            d: msg.time
          }, msg.time ? {
            e: common_vendor.t(msg.time)
          } : {}, {
            f: msg.role === "user"
          }, msg.role === "user" ? {} : {}, {
            g: index,
            h: common_vendor.n(msg.role)
          });
        }),
        b: scrollTop.value,
        c: common_vendor.o(loadMoreHistory),
        d: common_vendor.unref(showQuickQuestions)
      }, common_vendor.unref(showQuickQuestions) ? {
        e: common_vendor.f(quickQuestions, (q, index, i0) => {
          return {
            a: common_vendor.t(q),
            b: index,
            c: common_vendor.o(($event) => sendQuickQuestion(q), index)
          };
        })
      } : {}, {
        f: isLoading.value,
        g: common_vendor.o(sendMessageHandler),
        h: inputMessage.value,
        i: common_vendor.o(($event) => inputMessage.value = $event.detail.value),
        j: inputMessage.value.trim() && !isLoading.value ? 1 : "",
        k: common_vendor.o(sendMessageHandler),
        l: safeBottom.value + "px"
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-c667d17b"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/ai/consult.vue"]]);
wx.createPage(MiniProgramPage);

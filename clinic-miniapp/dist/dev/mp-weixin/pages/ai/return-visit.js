"use strict";
const common_vendor = require("../../common/vendor.js");
const api_miniapp_aiReturnVisit = require("../../api/miniapp/ai-return-visit.js");
const api_difyChatflow = require("../../api/dify-chatflow.js");
const api_miniapp_patient = require("../../api/miniapp/patient.js");
require("../../utils/request.js");
const _sfc_main = {
  __name: "return-visit",
  setup(__props) {
    const messages = common_vendor.ref([]);
    const inputMessage = common_vendor.ref("");
    const isLoading = common_vendor.ref(false);
    const scrollTop = common_vendor.ref(0);
    const conversationId = common_vendor.ref("");
    const safeBottom = common_vendor.ref(0);
    const patientInfo = common_vendor.ref({});
    const visitItem = common_vendor.ref("");
    const visitDate = common_vendor.ref("");
    const pageParams = common_vendor.ref({
      patientId: "",
      planId: "",
      taskId: "",
      from: ""
    });
    common_vendor.onLoad((options) => {
      var _a;
      console.log("AI回访页面参数:", options);
      pageParams.value = {
        patientId: options.patient_id || options.patientId || "",
        planId: options.plan_id || options.planId || "",
        taskId: options.task_id || options.taskId || "",
        from: options.from || "miniapp"
      };
      const systemInfo = common_vendor.index.getSystemInfoSync();
      safeBottom.value = ((_a = systemInfo.safeAreaInsets) == null ? void 0 : _a.bottom) || 0;
      initPage();
    });
    const initPage = async () => {
      var _a;
      try {
        if (!pageParams.value.patientId) {
          const userInfoStr = common_vendor.index.getStorageSync("userInfo");
          const userInfo = userInfoStr ? JSON.parse(userInfoStr) : null;
          console.log("userInfo:", userInfo);
          let patientId = (userInfo == null ? void 0 : userInfo.patientId) || ((_a = userInfo == null ? void 0 : userInfo.patient) == null ? void 0 : _a.id);
          console.log("从缓存获取的 patientId:", patientId);
          if (!patientId) {
            try {
              const patientRes = await api_miniapp_patient.getPatientList({ page: 1, pageSize: 1 });
              console.log("getPatientList 响应:", patientRes);
              if (patientRes.code === 200 && patientRes.data && patientRes.data.length > 0) {
                console.log("第一个患者数据:", patientRes.data[0]);
                patientId = patientRes.data[0].id || patientRes.data[0].patientId;
                console.log("通过接口获取默认患者ID:", patientId);
              }
            } catch (e) {
              console.error("获取患者列表失败:", e);
            }
          }
          if (patientId) {
            pageParams.value.patientId = patientId;
            console.log("最终使用的 patientId:", pageParams.value.patientId);
          } else {
            common_vendor.index.showToast({ title: "缺少患者信息", icon: "none" });
            return;
          }
        }
        await loadPatientInfo();
        if (pageParams.value.taskId) {
          recordTaskOpened();
        }
      } catch (error) {
        console.error("初始化页面失败:", error);
      }
    };
    const loadPatientInfo = async () => {
      try {
        const res = await api_miniapp_aiReturnVisit.getPatientProfile(pageParams.value.patientId);
        console.log("获取患者档案响应:", res);
        if (res.data) {
          patientInfo.value = {
            name: res.data.patientName || res.data.patient_name || "患者",
            gender: res.data.genderText || res.data.gender || "女",
            age: res.data.age || 35
          };
          visitItem.value = res.data.visitItem || res.data.visit_item || "牙科治疗";
          visitDate.value = res.data.visitDate || res.data.visit_date || "";
        }
      } catch (error) {
        console.error("获取患者信息失败:", error);
      }
    };
    const sendMessageHandler = async () => {
      const message = inputMessage.value.trim();
      if (!message || isLoading.value)
        return;
      addMessage("user", message);
      inputMessage.value = "";
      await sendToAI(message);
    };
    const sendToAI = async (message) => {
      isLoading.value = true;
      const loadingIndex = messages.value.length;
      addMessage("assistant", "", true);
      try {
        const inputs = {
          patient_id: pageParams.value.patientId,
          patient_name: patientInfo.value.name,
          patient_gender: patientInfo.value.gender,
          patient_age: patientInfo.value.age,
          plan_id: pageParams.value.planId,
          task_id: pageParams.value.taskId,
          visit_item: visitItem.value,
          visit_date: visitDate.value
        };
        const res = await api_difyChatflow.sendChatflowMessage(message, conversationId.value, inputs);
        console.log("AI 响应:", JSON.stringify(res));
        const responseData = res && res.code === 200 && res.data ? res.data : res;
        console.log("解析后:", JSON.stringify(responseData));
        console.log("answer:", responseData ? responseData.answer : "undefined");
        if (responseData && responseData.conversation_id) {
          conversationId.value = responseData.conversation_id;
        }
        messages.value[loadingIndex] = {
          role: "assistant",
          content: responseData && responseData.answer ? responseData.answer : "抱歉，我没有理解您的回复，请重新描述一下。",
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
    const recordTaskOpened = async () => {
      try {
        await api_miniapp_aiReturnVisit.recordTaskOpen(pageParams.value.taskId);
      } catch (error) {
        console.error("记录任务打开失败:", error);
      }
    };
    const formatTime = (date = /* @__PURE__ */ new Date()) => {
      const hours = date.getHours().toString().padStart(2, "0");
      const minutes = date.getMinutes().toString().padStart(2, "0");
      return `${hours}:${minutes}`;
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: patientInfo.value.name
      }, patientInfo.value.name ? {
        b: common_vendor.t(patientInfo.value.gender === "男" ? "👨" : "👩"),
        c: common_vendor.t(patientInfo.value.name),
        d: common_vendor.t(patientInfo.value.gender),
        e: common_vendor.t(patientInfo.value.age),
        f: common_vendor.t(visitItem.value || "牙科治疗"),
        g: common_vendor.t(visitDate.value)
      } : {}, {
        h: common_vendor.f(messages.value, (msg, index, i0) => {
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
        i: scrollTop.value,
        j: isLoading.value,
        k: common_vendor.o(sendMessageHandler),
        l: inputMessage.value,
        m: common_vendor.o(($event) => inputMessage.value = $event.detail.value),
        n: inputMessage.value.trim() && !isLoading.value ? 1 : "",
        o: common_vendor.o(sendMessageHandler),
        p: safeBottom.value + "px"
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-eb778b3f"], ["__file", "F:/work-code/agent-dev/clinic-miniapp/src/pages/ai/return-visit.vue"]]);
wx.createPage(MiniProgramPage);

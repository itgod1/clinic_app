"""
工具模块
提供 AI 回访工作流所需的各种工具：API客户端、知识库、意图分类、满意度收集
"""
import os
import re
import requests
from typing import Dict, List, Optional, Any
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()


class ClinicAPIClient:
    """诊所 API 客户端 - 与 Java 后端通信（对应 AiReturnVisitController）"""

    def __init__(self, base_url: str = None):
        self.base_url = base_url or os.getenv("CLINIC_API_URL", "http://localhost:8080")
        self.api_key = os.getenv("CLINIC_API_KEY", "")

    def _headers(self) -> Dict[str, str]:
        return {
            "Content-Type": "application/json",
            "X-API-Key": self.api_key
        }

    def _get(self, path: str, params: dict = None) -> Dict[str, Any]:
        try:
            url = f"{self.base_url}{path}"
            print(f"[HTTP GET] {url}")
            resp = requests.get(url, headers=self._headers(), params=params, timeout=10)
            print(f"[HTTP GET] 状态码: {resp.status_code}")
            if resp.status_code == 200:
                result = resp.json()
                print(f"[HTTP GET] 返回数据: {result}")
                data = result.get("data", result)
                print(f"[HTTP GET] 提取data: {data}")
                return data
            else:
                print(f"[HTTP GET] 错误响应: {resp.text}")
            return {}
        except Exception as e:
            print(f"GET {path} 失败: {e}")
            return {}

    def _post(self, path: str, data: dict = None) -> bool:
        try:
            resp = requests.post(f"{self.base_url}{path}", json=data, headers=self._headers(), timeout=10)
            return resp.status_code == 200
        except Exception as e:
            print(f"POST {path} 失败: {e}")
            return False

    # ---- 患者接口 ----

    def get_patient_profile(self, patient_id: int) -> Dict[str, Any]:
        """GET /api/ai/return-visit/patient/{patientId}/profile"""
        return self._get(f"/api/ai/return-visit/patient/{patient_id}/profile")

    # ---- 回访计划接口 ----

    def get_plan_detail(self, plan_id: int) -> Dict[str, Any]:
        """GET /api/ai/return-visit/plan/{planId}"""
        return self._get(f"/api/ai/return-visit/plan/{plan_id}")

    # ---- 对话保存（LangGraph 原生格式） ----

    def save_conversation(self, data: Dict[str, Any]) -> bool:
        """POST /api/ai/return-visit/conversation/save — 直接接收 LangGraph 状态格式"""
        return self._post("/api/ai/return-visit/conversation/save", data)

    # ---- 任务接口 ----

    def get_pending_tasks(self, limit: int = 50) -> List[Dict[str, Any]]:
        """GET /api/ai/return-visit/task/pending"""
        result = self._get("/api/ai/return-visit/task/pending", params={"limit": limit})
        if isinstance(result, list):
            return result
        return []

    def trigger_task(self, task_id: int) -> bool:
        """POST /api/ai/return-visit/task/trigger"""
        return self._post("/api/ai/return-visit/task/trigger?taskId=" + str(task_id))


class KnowledgeBase:
    """知识库 - 口腔治疗术后护理指南"""

    def __init__(self):
        self.treatment_guidelines = {
            "拔牙": {
                "术后注意事项": [
                    "术后24小时内不要漱口或刷牙，保护血凝块",
                    "避免用吸管喝水，防止血凝块脱落",
                    "术后2小时可进食，吃温凉软食",
                    "不要用舌头舔伤口或手指触摸",
                    "如有出血不止、剧烈疼痛请及时就诊"
                ],
                "常见问题": {
                    "出血": "轻微渗血是正常的，可用消毒纱布轻咬30-60分钟止血。如出血量大或持续不止，请及时就诊。",
                    "疼痛": "麻药消退后会有轻微疼痛，可按医嘱服用止痛药。如疼痛持续加重，请及时就诊。",
                    "肿胀": "术后48小时内可冰敷（每次15-20分钟），48小时后可改为热敷帮助消肿。",
                    "饮食": "术后2小时可进食，建议吃温凉软食如粥、面条、酸奶等，避免过热过硬食物。",
                    "干槽症": "术后3-4天出现剧烈疼痛、口腔异味可能是干槽症，需要及时复诊处理。"
                }
            },
            "种植牙": {
                "术后注意事项": [
                    "术后2小时内不要进食",
                    "术后24小时内不要漱口刷牙",
                    "避免吸烟和饮酒（影响骨结合）",
                    "按医嘱服用抗生素和漱口水",
                    "术后7-10天拆线",
                    "避免用种植区域咀嚼，直到医生确认可以负重"
                ],
                "常见问题": {
                    "肿胀": "术后轻微肿胀是正常的，一般3-5天消退。可冰敷帮助消肿。",
                    "疼痛": "术后有轻微疼痛，可按医嘱服用止痛药。如疼痛持续加重，请及时就诊。",
                    "饮食": "术后一周内吃软食，避免咀嚼硬物。温度适中，不宜过热。",
                    "口腔卫生": "拆线后可用软毛牙刷轻刷种植区域，配合漱口水保持清洁。"
                }
            },
            "根管治疗": {
                "术后注意事项": [
                    "治疗后牙齿可能有轻微敏感或不适",
                    "避免用治疗侧咀嚼硬物",
                    "按时复诊完成后续治疗（根管治疗通常需要2-4次）",
                    "如临时充填物脱落，请及时复诊",
                    "治疗完成后建议做牙冠保护"
                ],
                "常见问题": {
                    "疼痛": "治疗后轻微疼痛或咬合不适是正常的，一般2-3天缓解。可服用止痛药。",
                    "咬合不适": "如感觉牙齿有高点，可能需要调整咬合，请联系医生。",
                    "临时充填物脱落": "请及时复诊重新充填，避免细菌再次感染根管。",
                    "肿胀": "如出现明显肿胀，可能是感染加重，请及时就诊。"
                }
            },
            "正畸": {
                "注意事项": [
                    "保持口腔卫生，每次进食后刷牙",
                    "使用正畸专用牙刷和牙缝刷",
                    "避免吃太硬（坚果、骨头）或太粘（口香糖、年糕）的食物",
                    "按时复诊调整（通常4-6周一次）",
                    "按医生要求佩戴橡皮筋或头帽"
                ],
                "常见问题": {
                    "疼痛": "每次调整后2-3天会有轻微酸痛，这是牙齿移动的正常反应。可吃软食缓解。",
                    "口腔溃疡": "托槽或弓丝摩擦可能导致溃疡，可用正畸蜡保护。一般1-2周适应。",
                    "托槽脱落": "保存好脱落的托槽，尽快预约复诊重新粘贴。",
                    "弓丝扎嘴": "可用正畸蜡覆盖扎嘴处，如弓丝过长请预约修剪。"
                }
            },
            "洗牙": {
                "术后注意事项": [
                    "洗牙后30分钟内不要进食饮水",
                    "洗牙后牙齿可能有轻微敏感，一般1-2天缓解",
                    "避免食用过冷过热食物",
                    "使用抗敏感牙膏有助于缓解不适",
                    "建议每6-12个月洗牙一次"
                ],
                "常见问题": {
                    "牙齿敏感": "洗牙后短暂敏感是正常的，一般1-2天缓解。可使用抗敏感牙膏。",
                    "牙龈出血": "洗牙后轻微出血正常，说明牙龈有炎症。保持口腔清洁，出血会逐渐减少。",
                    "牙缝变大": "洗牙清除牙结石后，之前被结石填塞的牙缝暴露出来，这是正常的。"
                }
            },
            "补牙": {
                "术后注意事项": [
                    "树脂充填后即可正常使用，但建议2小时内不要进食",
                    "避免用补过的牙齿咬硬物",
                    "如有咬合高点，请及时复诊调整",
                    "保持良好的口腔卫生习惯"
                ],
                "常见问题": {
                    "敏感": "补牙后牙齿可能对冷热敏感，一般1-2周缓解。如持续敏感请复诊。",
                    "咬合不适": "如咬合时感觉有高点，请联系医生调整。",
                    "充填物脱落": "如充填物脱落，请及时复诊重新充填。"
                }
            }
        }

    def get_treatment_guidelines(self, treatment_type: str) -> Dict[str, Any]:
        """获取指定治疗类型的指南"""
        # 精确匹配
        if treatment_type in self.treatment_guidelines:
            return self.treatment_guidelines[treatment_type]
        # 模糊匹配
        for key in self.treatment_guidelines:
            if key in treatment_type or treatment_type in key:
                return self.treatment_guidelines[key]
        return {}

    def search_faq(self, query: str, treatment_type: Optional[str] = None) -> List[Dict[str, str]]:
        """搜索相关的FAQ"""
        results = []

        # 优先搜索指定治疗类型
        search_keys = [treatment_type] if treatment_type and treatment_type in self.treatment_guidelines else self.treatment_guidelines.keys()

        for t_type in search_keys:
            guidelines = self.treatment_guidelines.get(t_type)
            if not guidelines:
                continue
            faqs = guidelines.get("常见问题", {})
            for question, answer in faqs.items():
                if any(keyword in query for keyword in [question, question[:2]]):
                    results.append({
                        "question": question,
                        "answer": answer,
                        "treatment_type": t_type
                    })

        # 如果没找到，搜索所有类型
        if not results:
            for t_type, guidelines in self.treatment_guidelines.items():
                faqs = guidelines.get("常见问题", {})
                for question, answer in faqs.items():
                    if any(keyword in query for keyword in question):
                        results.append({
                            "question": question,
                            "answer": answer,
                            "treatment_type": t_type
                        })

        return results[:3]


class IntentClassifier:
    """基于关键字的意图分类器"""

    KEYWORDS = {
        "end": ["谢谢", "好的", "没有了", "没问题了", "再见", "拜拜", "行了", "可以了", "没事了", "就这样"],
        "greeting": ["你好", "您好", "在吗", "嗨", "hello", "hi", "早上好", "下午好", "晚上好"],
        "satisfied": ["满意", "很好", "不错", "挺好的", "没问题", "舒服", "不疼", "不痛", "好了", "恢复了"],
        "dissatisfied": ["不满意", "不好", "疼", "痛", "难受", "不舒服", "还疼", "还痛", "没有好转"],
        "complaint": ["投诉", "举报", "太差", "服务不好", "医生态度差", "骗人", "坑人"],
        "appointment": ["预约", "挂号", "想去看", "什么时候能", "时间", "复诊", "再来"],
        "medical": ["出血", "肿", "发炎", "化脓", "发烧", "松动", "脱落", "裂开"],
        "inquiry": ["请问", "咨询", "想了解", "怎么回事", "为什么", "可以吗", "能不能"]
    }

    def classify(self, text: str) -> str:
        """分类用户意图，返回 intent 字符串"""
        text_lower = text.lower()

        scores = {}
        for intent, keywords in self.KEYWORDS.items():
            score = sum(1 for kw in keywords if kw in text_lower)
            if score > 0:
                scores[intent] = score

        if not scores:
            return "other"

        # 投诉优先级最高
        if "complaint" in scores:
            return "complaint"
        # 医疗问题次之
        if "medical" in scores:
            return "medical"

        return max(scores, key=scores.get)


class SatisfactionCollector:
    """满意度评分收集器"""

    SATISFACTION_PATTERNS = {
        5: ["非常满意", "很好", "完美", "5分", "五星", "好评"],
        4: ["满意", "不错", "挺好", "4分", "四星", "比较好"],
        3: ["一般", "还可以", "凑合", "还行", "3分", "三星"],
        2: ["不太满意", "不太好", "有点差", "2分", "二星"],
        1: ["非常不满意", "很差", "太差", "不好", "1分", "一星", "差评"]
    }

    def extract_score(self, text: str) -> Optional[int]:
        """从文本中提取满意度评分 1-5"""
        if not text:
            return None

        # 匹配明确的数字评分
        numbers = re.findall(r'(\d+)\s*分', text)
        for num in numbers:
            score = int(num)
            if 1 <= score <= 5:
                return score

        # 关键词匹配
        text_lower = text.lower()
        for score, patterns in self.SATISFACTION_PATTERNS.items():
            for pattern in patterns:
                if pattern in text_lower:
                    return score

        return None


# 模块级工具实例
api_client = ClinicAPIClient()
knowledge_base = KnowledgeBase()
intent_classifier = IntentClassifier()
satisfaction_collector = SatisfactionCollector()

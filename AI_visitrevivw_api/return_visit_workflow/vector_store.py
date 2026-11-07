"""
向量数据库模块 - RAG知识库核心
使用ChromaDB + Sentence Transformers实现语义检索
"""
import os
import hashlib
from typing import List, Dict, Any, Optional
from dataclasses import dataclass

import chromadb
from chromadb.config import Settings
from sentence_transformers import SentenceTransformer


@dataclass
class KnowledgeDocument:
    """知识文档结构"""
    id: str
    content: str
    metadata: Dict[str, Any]
    treatment_type: str
    category: str  # '术后注意事项', '常见问题', '治疗指南'等


class ChromaManager:
    """
    ChromaDB向量数据库管理器
    负责知识库的存储、检索和维护
    """

    def __init__(self, collection_name: str = "
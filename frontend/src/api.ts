import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/chat',
  headers: {
    'Content-Type': 'application/json',
  },
});

// 统一响应类型
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

// 会话类型
export interface Conversation {
  id: number;
  title: string;
  createdAt: string;
}

// 消息类型
export interface Message {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
}



// API 方法
export const chatApi = {
  // 获取所有会话
  getConversations: (): Promise<ApiResponse<Conversation[]>> => {
    return api.get('/conversations').then(res => res.data);
  },

  // 创建会话
  createConversation: (title: string): Promise<ApiResponse<Conversation>> => {
    return api.post('/conversations', { title }).then(res => res.data);
  },

  // 获取会话消息
  getMessages: (conversationId: number): Promise<ApiResponse<Message[]>> => {
    return api.get(`/conversations/${conversationId}/messages`).then(res => res.data);
  },

  // 发送消息
  sendMessage: (
    conversationId: number,
    content: string
  ): Promise<ApiResponse<Message>> => {
    return api
      .post(`/conversations/${conversationId}/messages`, {
        role: 'user',
        content,
      })
      .then(res => res.data);
  },
  // 新增：获取会话最新的用户消息
    getLatestUserMessage: (
      conversationId: number,
      content: string
    ): Promise<ApiResponse<Message>> => {
      return api.get(`/conversations/${conversationId}/latestUserMessage`, {
        params: { message: content }
      }).then(res => res.data);
    },

    // 新增：获取会话最新的AI助手消息
    getLatestAssistantMessage: (
      conversationId: number,
      content: string
    ): Promise<ApiResponse<Message>> => {
      return api.get(`/conversations/${conversationId}/latestAssistantMessage`, {
        params: { content }
      }).then(res => res.data);
    },

    // RAG 对话（非流式）
    ragChat: (
      conversationId: number,
      content: string
    ): Promise<ApiResponse<Message>> => {
      return api
        .post(`/conversations/${conversationId}/rag`, { content })
        .then(res => res.data);
    },
    // RAG 对话-流式
    ragStreamChat: (
      conversationId: number,
      message: string,
      onChunk: (chunk: string) => void,
      onComplete: () => void
    ) => {
      // 使用 fetch + ReadableStream 实现 POST SSE
      let abortController: AbortController | null = new AbortController();
      
      fetch(`http://localhost:8080/api/chat/conversations/${conversationId}/rag-stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ message }),
        signal: abortController.signal,
      })
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const reader = response.body?.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        
        if (!reader) {
          onComplete();
          return;
        }
        
        const readStream = () => {
          reader.read().then(({ done, value }) => {
            if (done) {
              // 处理剩余的 buffer
              if (buffer.trim()) {
                processBuffer();
              }
              onComplete();
              return;
            }
            
            // 立即解码并添加到buffer
            if (value) {
              buffer += decoder.decode(value, { stream: true });
            }
            
            // SSE 格式：每个事件以 \n\n 结束
            // 立即处理所有完整的事件（不等待，不缓冲）
            let eventEndIndex;
            while ((eventEndIndex = buffer.indexOf('\n\n')) !== -1) {
              const eventText = buffer.substring(0, eventEndIndex);
              buffer = buffer.substring(eventEndIndex + 2);
              
              // 立即处理这个事件（同步处理，不延迟）
              processEvent(eventText);
            }
            
            // 继续读取下一个数据块（递归调用，不等待）
            readStream();
          }).catch(err => {
            console.error('读取流失败:', err);
            onComplete();
          });
        };
        
        // 处理单个 SSE 事件（模拟原生 EventSource 的行为）
        const processEvent = (eventText: string) => {
          // 查找 data: 行（支持多行 data:，Spring 会在内容包含换行时自动格式化为多行）
          const lines = eventText.split(/\r?\n/);
          const dataParts: string[] = [];
          
          for (const line of lines) {
            // 只处理以 "data: " 开头的行
            if (line.startsWith('data: ')) {
              const data = line.substring(6); // 提取 data: 后面的内容
              
              // 检查是否是结束标记
              if (data === '[DONE]' || data.trim() === '[DONE]') {
                onComplete();
                return;
              }
              
              // 收集数据（保留原始内容，包括空字符串）
              dataParts.push(data);
            }
            // 忽略其他行（空行、event:、id: 等 SSE 协议字段）
          }
          
          // 如果有数据，合并并立即发送（多行 data: 用换行符连接）
          // 注意：即使 dataParts 只有一个元素，也要发送
          if (dataParts.length > 0) {
            const mergedData = dataParts.join('\n');
            // 立即调用 onChunk，不延迟
            onChunk(mergedData);
          }
        };
        
        // 处理剩余的 buffer（流结束时）
        const processBuffer = () => {
          const lines = buffer.split(/\r?\n/);
          const dataParts: string[] = [];
          
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.substring(6);
              if (data === '[DONE]' || data.trim() === '[DONE]') {
                return;
              }
              dataParts.push(data);
            }
          }
          
          // 合并并发送（多行 data: 用换行符连接）
          if (dataParts.length > 0) {
            const mergedData = dataParts.join('\n');
            onChunk(mergedData);
          }
        };
        
        readStream();
      })
      .catch(err => {
        if (err.name !== 'AbortError') {
          console.error('请求失败:', err);
        }
        onComplete();
      });
      
      // 返回一个可以关闭的对象
      return {
        close: () => {
          if (abortController) {
            abortController.abort();
            abortController = null;
          }
        }
      };
    },

    // Agent 流式对话
    agentStreamChat: (
      conversationId: number,
      message: string,
      onChunk: (chunk: string) => void,
      onComplete: () => void
    ) => {
      const eventSource = new EventSource(
        `http://localhost:8080/api/chat/conversations/${conversationId}/agent-stream?message=${encodeURIComponent(message)}`
      );

      eventSource.onmessage = (event) => {
        if (event.data === '[DONE]') {
          eventSource.close();
          onComplete();
        } else {
          onChunk(event.data);
        }
      };

      eventSource.onerror = () => {
        eventSource.close();
        onComplete();
      };

      return eventSource;
    },
};

// 流式发送消息
export const streamMessage = async (
  conversationId: number,
  message: string,
  onChunk: (chunk: string) => void,
  onComplete: () => void
) => {
  const eventSource = new EventSource(
    `http://localhost:8080/api/chat/conversations/${conversationId}/stream?message=${encodeURIComponent(message)}`
  );

  eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
      eventSource.close();
      onComplete();
    } else {
      onChunk(event.data);
    }
  };

  eventSource.onerror = () => {
    eventSource.close();
    onComplete();
  };
};

// 文档类型（使用 type 避免与 DOM Document 冲突）
export type Document = {
  id: number;
  filename: string;
  fileType: string;
  fileSize: number;
  description?: string;
  createdAt: string;
  createdBy?: string;
};

// 文档引用信息
export interface DocumentReference {
  content: string;
  docId: number;
  docName: string;
  chunkId: number;
}

// 文档 API
export const documentApi = {
  // 上传文档
  upload: (file: File, description?: string): Promise<ApiResponse<Document>> => {
    const formData = new FormData();
    formData.append('file', file);
    if (description) {
      formData.append('description', description);
    }

    return axios.post('http://localhost:8080/api/document/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }).then(res => res.data);
  },

  // 获取文档列表
  list: (): Promise<ApiResponse<Document[]>> => {
    return axios.get('http://localhost:8080/api/document/list').then(res => res.data);
  },

  // 获取文档详情
  getById: (id: number): Promise<ApiResponse<Document>> => {
    return axios.get(`http://localhost:8080/api/document/${id}`).then(res => res.data);
  },

  // 删除文档
  delete: (id: number): Promise<ApiResponse<string>> => {
    return axios.delete(`http://localhost:8080/api/document/${id}`).then(res => res.data);
  },
};
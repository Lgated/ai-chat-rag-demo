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
    ragStreamChat: (
      conversationId: number,
      message: string,
      onChunk: (chunk: string) => void,
      onComplete: () => void
    ) => {
      const eventSource = new EventSourcePolyfill(
        `http://localhost:8080/api/chat/conversations/${conversationId}/rag-stream`,
        { method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ message }) }
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

      return eventSource; // 返回 EventSource，方便关闭
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
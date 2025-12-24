import { useState, useEffect, useRef } from 'react';
import type { Conversation, Message } from '../api';
import { chatApi, streamMessage } from '../api';
import DocumentPage from './DocumentPage';
import './ChatPage.css';

const ChatPage = () => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [currentConversationId, setCurrentConversationId] = useState<number | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputText, setInputText] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingConversations, setLoadingConversations] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);  // 修复：添加加载消息状态
  const [error, setError] = useState<string | null>(null);
  // 新增：对话模式（普通 / RAG / Agent）
  const [chatMode, setChatMode] = useState<'normal' | 'rag' | 'agent'>('normal');
  // 新增：页面切换（聊天 / 文档）
  const [currentPage, setCurrentPage] = useState<'chat' | 'document'>('chat');
  //当前流式连接管理（支持 EventSource 或任何有 close 方法的对象）
  const currentEventSourceRef = useRef<EventSource | { close: () => void } | null>(null);
  //用于取消过期的请求
  const abortControllerRef = useRef<AbortController | null>(null);
  //用于消息错误
  const [messagesError, setMessagesError] = useState<string | null>(null);
  //用于消息容器的引用，实现滚动到底部
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);

  // 加载会话列表
  useEffect(() => {
    setLoadingConversations(true);
    setError(null);

    chatApi.getConversations()
      .then((res) => {
        if (res.code === 200) {
          setConversations(res.data);
          // 自动选择第一个会话
          if (res.data.length > 0) {
              // 如果当前会话ID仍然存在于新列表中，保持不变
              const currentStillExists = res.data.some(conv => conv.id === currentConversationId);
              if (!currentStillExists) {
                 // 当前会话不存在了（可能被删除），选择第一个
                 setCurrentConversationId(res.data[0].id);
              }
          } else {
                // 没有会话，清空当前会话ID和消息
                setCurrentConversationId(null);
                setMessages([]);
          }
        } else {
           setError(res.message || '加载会话列表失败');
           //加载失败时也清空当前会话
           setCurrentConversationId(null);
           setMessages([]);
        }
      })
      .catch((err) => {
        console.error('加载会话列表失败:', err);
        setError('无法连接到服务器，请确保后端服务已启动（http://localhost:8080）');
        // 网络错误时清空状态
        setCurrentConversationId(null);
        setMessages([]);
      })
      .finally(() => {
        setLoadingConversations(false);
      });
  }, []);//只在组件挂载时执行一次

  // 滚动到底部的函数
  const scrollToBottom = () => {
    // 使用 setTimeout 确保 DOM 更新后再滚动
    setTimeout(() => {
      if (messagesEndRef.current) {
        messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
      } else if (messagesContainerRef.current) {
        messagesContainerRef.current.scrollTop = messagesContainerRef.current.scrollHeight;
      }
    }, 100);
  };

  // 加载消息列表 -- 解决竞态条件
  useEffect(() => {

   // ✅ 改进1：清空之前的状态
   setMessages([]);
   setMessagesError(null);

   // ✅ 改进2：关闭之前的流式连接
   if (currentEventSourceRef.current) {
     currentEventSourceRef.current.close();
     currentEventSourceRef.current = null;
   }

   // ✅ 改进3：取消之前的请求
   if (abortControllerRef.current) {
     abortControllerRef.current.abort();
   }

   // ✅ 改进4：如果没有会话ID，直接返回
   if (!currentConversationId) {
     setLoadingMessages(false);
     return;
   }

   // ✅ 改进5：创建新的 AbortController 用于取消请求
   const abortController = new AbortController();
   abortControllerRef.current = abortController;

   setLoadingMessages(true);
// ✅ 改进6：保存当前的会话ID，用于验证响应是否有效
    const targetConversationId = currentConversationId;

    chatApi.getMessages(targetConversationId)
      .then((res) => {
        // ✅ 改进7：检查请求是否被取消
        if (abortController.signal.aborted) {
          return;  // 请求已被取消，忽略响应
        }

        // ✅ 改进8：检查会话ID是否仍然匹配（防止竞态条件）
        if (currentConversationId !== targetConversationId) {
          console.log('会话已切换，忽略过期响应');
          return;
        }

        if (res.code === 200) {
          setMessages(res.data || []);
          setMessagesError(null);
          // 加载消息后滚动到底部
          scrollToBottom();
        } else {
          setMessagesError(res.message || '加载消息失败');
          setMessages([]);
        }
      })
      .catch((err) => {
        // ✅ 改进9：如果是取消的请求，不显示错误
        if (abortController.signal.aborted) {
          return;
        }

        console.error('加载消息失败:', err);
        setMessagesError('加载消息失败，请稍后重试');
        setMessages([]);
      })
      .finally(() => {
        // ✅ 改进10：只有当前请求有效时才更新 loading 状态
        if (!abortController.signal.aborted && currentConversationId === targetConversationId) {
          setLoadingMessages(false);
        }
      });

    // ✅ 改进11：清理函数：组件卸载或依赖变化时取消请求
    return () => {
      abortController.abort();
    };
  }, [currentConversationId]); //依赖 currentConversationId，切换会话时重新执行

  // 组件卸载时清理 EventSource
  useEffect(() => {
    return () => {
      if (currentEventSourceRef.current) {
        currentEventSourceRef.current.close();
      }
    };
  }, []);

  // 发送消息 - 根据模式选择不同的流式接口
  const handleSend = async () => {
    if (!inputText.trim() || !currentConversationId || loading || loadingMessages) return;

    const userMessage = inputText.trim();
    setInputText('');
    setLoading(true);

    const tempUserId = Date.now();
    // 先添加用户消息到界面
    const tempUserMessage: Message = {
      id: tempUserId,
      role: 'user',
      content: userMessage,
      createdAt: new Date().toISOString(),
    };
    setMessages(prev => [...prev, tempUserMessage]);

    // 生成AI消息临时ID
    const tempAssistantId = Date.now() + 1;
    const tempAssistantMessage: Message = {
      id: tempAssistantId,
      role: 'assistant',
      content: '',
      createdAt: new Date().toISOString(),
    };
    setMessages(prev => [...prev, tempAssistantMessage]);

    try {
      let fullContent = '';
      const conversationId = currentConversationId; // 保存当前会话ID

      // 关闭之前的流式连接
      if (currentEventSourceRef.current) {
        currentEventSourceRef.current.close();
        currentEventSourceRef.current = null;
      }

      // 统一的流式处理函数
      const handleStreamComplete = async () => {
        // 流结束：重新加载完整消息列表（更可靠）
        try {
          // 检查会话是否仍然是当前会话
          if (currentConversationId !== conversationId) {
            console.log('会话已切换，跳过加载消息');
            setLoading(false);
            return;
          }

          // 等待一小段时间，确保后端已保存消息
          await new Promise(resolve => setTimeout(resolve, 300));
          
          // 重新加载消息列表
          const messagesRes = await chatApi.getMessages(conversationId);
          if (messagesRes.code === 200 && currentConversationId === conversationId) {
            setMessages(messagesRes.data);
            scrollToBottom();
          }
        } catch (e) {
          console.warn('重新加载消息失败:', e);
          // 如果重新加载失败，尝试通过内容匹配查找（备用方案）
          try {
            const userRes = await chatApi.getLatestUserMessage(conversationId, userMessage);
            const assistantRes = await chatApi.getLatestAssistantMessage(conversationId, fullContent);
            
            if (userRes.code === 200 && assistantRes.code === 200 && currentConversationId === conversationId) {
              setMessages(prev =>
                prev.map(msg => {
                  if (msg.id === tempUserId) return userRes.data;
                  if (msg.id === tempAssistantId) return assistantRes.data;
                  return msg;
                })
              );
              scrollToBottom();
            }
          } catch (e2) {
            console.warn('替换临时消息ID也失败:', e2);
          }
        } finally {
          setLoading(false);
        }
      };

      // 流式输出缓冲和节流机制
      let chunkBuffer = '';
      let rafId: number | null = null;
      
      const flushBuffer = () => {
        if (chunkBuffer && currentConversationId === conversationId) {
          fullContent += chunkBuffer;
          const contentToUpdate = fullContent;
          setMessages(prev =>
            prev.map(msg =>
              msg.id === tempAssistantId
                ? { ...msg, content: contentToUpdate }
                : msg
            )
          );
          chunkBuffer = '';
          scrollToBottom();
        }
        rafId = null;
      };
      
      const scheduleUpdate = (chunk: string) => {
        if (currentConversationId !== conversationId) {
          return; // 会话已切换，忽略
        }
        
        chunkBuffer += chunk;
        // 如果buffer达到一定大小（20个字符），立即更新
        if (chunkBuffer.length >= 20) {
          if (rafId) {
            cancelAnimationFrame(rafId);
            rafId = null;
          }
          flushBuffer();
        } else {
          // 否则使用requestAnimationFrame节流更新（约16ms一次，最多60fps）
          if (!rafId) {
            rafId = requestAnimationFrame(() => {
              flushBuffer();
            });
          }
        }
      };

      // 根据模式选择不同的流式接口
      let eventSource: EventSource | { close: () => void } | null = null;

      if (chatMode === 'rag') {
        // RAG 流式对话
        eventSource = chatApi.ragStreamChat(
          conversationId,
          userMessage,
          (chunk) => {
            // 检查是否还是当前会话
            if (currentConversationId === conversationId) {
              scheduleUpdate(chunk);
            }
          },
          () => {
            // 流结束时，确保所有缓冲的数据都被更新
            if (rafId) {
              cancelAnimationFrame(rafId);
            }
            flushBuffer();
            handleStreamComplete();
          }
        );
      } else if (chatMode === 'agent') {
        // Agent 流式对话
        eventSource = chatApi.agentStreamChat(
          conversationId,
          userMessage,
          (chunk) => {
            if (currentConversationId === conversationId) {
              scheduleUpdate(chunk);
            }
          },
          () => {
            // 流结束时，确保所有缓冲的数据都被更新
            if (rafId) {
              cancelAnimationFrame(rafId);
            }
            flushBuffer();
            handleStreamComplete();
          }
        );
      } else {
        // 普通流式对话
        await streamMessage(
          conversationId,
          userMessage,
          (chunk) => {
            if (currentConversationId === conversationId) {
              scheduleUpdate(chunk);
            }
          },
          () => {
            // 流结束时，确保所有缓冲的数据都被更新
            if (rafId) {
              cancelAnimationFrame(rafId);
            }
            flushBuffer();
            handleStreamComplete();
          }
        );
        return; // 普通模式不需要保存 EventSource
      }

      // 保存 EventSource 引用（RAG 和 Agent 模式）
      if (eventSource) {
        currentEventSourceRef.current = eventSource;
      }

    } catch (error) {
      console.error('发送消息失败:', error);
      alert('发送消息失败，请重试');
      const currentTempUserId = tempUserId;
      const currentTempAssistantId = tempAssistantId;
      setMessages(prev =>
        prev.filter(msg => msg.id !== currentTempUserId && msg.id !== currentTempAssistantId)
      );
      setLoading(false);
    }
  };

  // 创建新会话
  const handleNewConversation = async () => {
    const title = prompt('请输入会话标题:', '新会话');
    if (!title) return;

    const res = await chatApi.createConversation(title);
    if (res.code === 200) {
      setConversations([...conversations, res.data]);
      setCurrentConversationId(res.data.id);
    }
  };

  return (
    <div className="chat-container">
      {/* 顶部标签页切换 */}
      <div className="page-tabs">
        <button
          className={`page-tab ${currentPage === 'chat' ? 'active' : ''}`}
          onClick={() => setCurrentPage('chat')}
        >
          💬 对话
        </button>
        <button
          className={`page-tab ${currentPage === 'document' ? 'active' : ''}`}
          onClick={() => setCurrentPage('document')}
        >
          📚 文档管理
        </button>
      </div>

      {currentPage === 'document' ? (
        <div className="document-page-wrapper">
          <DocumentPage />
        </div>
      ) : (
        <div className="chat-content-wrapper">
          {/* 左侧：会话列表 */}
          <div className="sidebar">
        <button className="new-chat-btn" onClick={handleNewConversation}>
          + 新会话
        </button>
        <div className="conversation-list">
          {loadingConversations ? (
            <div style={{ padding: '20px', textAlign: 'center', color: '#666' }}>
              加载中...
            </div>
          ) : error ? (
            <div style={{ padding: '20px', color: '#d32f2f', fontSize: '14px' }}>
              {error}
            </div>
          ) : conversations.length === 0 ? (
            <div style={{ padding: '20px', textAlign: 'center', color: '#666' }}>
              暂无会话，点击上方按钮创建新会话
            </div>
          ) : (
            conversations.map((conv) => (
              <div
                key={conv.id}
                className={`conversation-item ${
                  conv.id === currentConversationId ? 'active' : ''
                }`}
                onClick={() => setCurrentConversationId(conv.id)}
              >
                {conv.title}
              </div>
            ))
          )}
        </div>
      </div>

      {/* 右侧：聊天窗口 */}
      <div className="chat-main">
        {/* 模式切换器 */}
        <div className="chat-mode-selector">
          <button
            className={`mode-btn ${chatMode === 'normal' ? 'active' : ''}`}
            onClick={() => setChatMode('normal')}
            disabled={loading || loadingMessages}
          >
            💬 普通对话
          </button>
          <button
            className={`mode-btn ${chatMode === 'rag' ? 'active' : ''}`}
            onClick={() => setChatMode('rag')}
            disabled={loading || loadingMessages}
          >
            📚 RAG知识库
          </button>
          <button
            className={`mode-btn ${chatMode === 'agent' ? 'active' : ''}`}
            onClick={() => setChatMode('agent')}
            disabled={loading || loadingMessages}
          >
            🔧 Agent工具
          </button>
        </div>

        {/* 消息加载状态和错误提示 */}
        {loadingMessages && (
          <div style={{ padding: '20px', textAlign: 'center', color: '#666' }}>
            加载消息中...
          </div>
        )}
        {messagesError && (
          <div style={{ padding: '20px', color: '#d32f2f', fontSize: '14px' }}>
            {messagesError}
          </div>
        )}

        <div className="messages-container" ref={messagesContainerRef}>
          {(messages || [])
            .filter((msg): msg is Message => !!msg && !!msg.role)
            .map((msg) => (
            <div key={msg.id} className={`message ${msg.role}`}>
              <div className="message-content">{msg.content}</div>
            </div>
          ))}
          {loading && <div className="message assistant">思考中...</div>}
          <div ref={messagesEndRef} />
        </div>

        <div className="input-container">
          <input
            type="text"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSend()}
            placeholder={`输入消息... (${chatMode === 'rag' ? 'RAG模式' : chatMode === 'agent' ? 'Agent模式' : '普通模式'})`}
            disabled={loading || loadingMessages}
          />
          <button 
            onClick={handleSend} 
            disabled={loading || loadingMessages || !inputText.trim()}
          >
            发送
          </button>
        </div>
      </div>
        </div>
      )}
    </div>
  );
};

export default ChatPage;
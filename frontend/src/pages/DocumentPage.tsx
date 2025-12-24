import { useState, useEffect } from 'react';
import { documentApi, type Document } from '../api';
import './DocumentPage.css';

const DocumentPage = () => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [description, setDescription] = useState('');

  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await documentApi.list();
      if (res.code === 200) {
        setDocuments(res.data);
      } else {
        setError(res.message || '加载文档列表失败');
      }
    } catch (err) {
      console.error('加载文档列表失败:', err);
      setError('无法连接到服务器');
    } finally {
      setLoading(false);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setError(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('请选择文件');
      return;
    }

    setUploading(true);
    setError(null);

    try {
      const res = await documentApi.upload(selectedFile, description);
      if (res.code === 200) {
        await loadDocuments();
        setSelectedFile(null);
        setDescription('');
        const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
        if (fileInput) fileInput.value = '';
      } else {
        setError(res.message || '上传失败');
      }
    } catch (err) {
      console.error('上传文档失败:', err);
      setError('上传文档失败，请稍后重试');
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (docId: number) => {
    if (!window.confirm('确定要删除这个文档吗？')) {
      return;
    }

    try {
      const res = await documentApi.delete(docId);
      if (res.code === 200) {
        await loadDocuments();
      } else {
        setError(res.message || '删除失败');
      }
    } catch (err) {
      console.error('删除文档失败:', err);
      setError('删除文档失败，请稍后重试');
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  return (
    <div className="document-page">
      <h1>文档管理</h1>

      <div className="upload-section">
        <h2>上传文档</h2>
        <div className="upload-form">
          <div className="form-group">
            <label>选择文件（PDF、Word、Excel、PPT、TXT、Markdown）</label>
            <input
              type="file"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md"
              onChange={handleFileSelect}
              disabled={uploading}
            />
            {selectedFile && (
              <div className="file-info">
                已选择: {selectedFile.name} ({formatFileSize(selectedFile.size)})
              </div>
            )}
          </div>

          <div className="form-group">
            <label>文档描述（可选）</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="输入文档描述..."
              rows={3}
              disabled={uploading}
            />
          </div>

          <button
            onClick={handleUpload}
            disabled={!selectedFile || uploading}
            className="upload-btn"
          >
            {uploading ? '上传中...' : '上传文档'}
          </button>
        </div>

        {error && <div className="error-message">{error}</div>}
      </div>

      <div className="document-list-section">
        <h2>文档列表</h2>
        {loading ? (
          <div className="loading">加载中...</div>
        ) : documents.length === 0 ? (
          <div className="empty">暂无文档</div>
        ) : (
          <table className="document-table">
            <thead>
              <tr>
                <th>文件名</th>
                <th>类型</th>
                <th>大小</th>
                <th>描述</th>
                <th>上传时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {documents.map((doc) => (
                <tr key={doc.id}>
                  <td>{doc.filename}</td>
                  <td>{doc.fileType}</td>
                  <td>{formatFileSize(doc.fileSize)}</td>
                  <td>{doc.description || '-'}</td>
                  <td>{new Date(doc.createdAt).toLocaleString()}</td>
                  <td>
                    <button
                      onClick={() => handleDelete(doc.id)}
                      className="delete-btn"
                    >
                      删除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default DocumentPage;
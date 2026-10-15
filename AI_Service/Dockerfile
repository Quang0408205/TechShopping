FROM python:3.11-slim

WORKDIR /app

# PyTorch bản CPU (nhỏ hơn nhiều bản CUDA) cho mô hình embedding của RAG
RUN pip install --no-cache-dir torch==2.8.0 --index-url https://download.pytorch.org/whl/cpu
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Tải sẵn mô hình embedding vào image để container khởi động không cần mạng
ENV HF_HOME=/opt/hf
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('intfloat/multilingual-e5-small')"

COPY app ./app
COPY scripts ./scripts
COPY knowledge ./knowledge
COPY evals ./evals

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

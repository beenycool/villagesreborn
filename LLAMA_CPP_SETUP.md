# Llama.cpp Integration Setup Guide

This guide explains how to set up llama.cpp with VillagersReborn for local AI-powered villager dialogue.

## Overview

The llama.cpp integration allows you to run local Large Language Models (LLMs) for villager dialogue without relying on external APIs. This provides:

- **Privacy**: All processing happens locally
- **Cost**: No API fees
- **Customization**: Use any compatible model
- **Offline**: Works without internet connection

## Quick Start

### 1. Install llama.cpp

#### Option A: Pre-built binaries
Download the latest release from [llama.cpp releases](https://github.com/ggerganov/llama.cpp/releases)

#### Option B: Build from source
```bash
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp
make
```

### 2. Download a Model

Choose a model optimized for conversation. Recommended models:

- **Small (fast)**: `llama-2-7b-chat.Q4_K_M.gguf`
- **Medium**: `llama-2-13b-chat.Q4_K_M.gguf`
- **Large**: `mistral-7b-instruct-v0.2.Q4_K_M.gguf`

Download from [Hugging Face](https://huggingface.co/models?library=gguf&sort=trending)

### 3. Start llama.cpp Server

```bash
# Basic server start
./server -m models/llama-2-7b-chat.Q4_K_M.gguf -c 2048 --host 0.0.0.0 --port 8080

# With optimized settings
./server -m models/llama-2-7b-chat.Q4_K_M.gguf \
  -c 2048 \
  -n 512 \
  --host 0.0.0.0 \
  --port 8080 \
  --threads 4 \
  --batch-size 512 \
  --gpu-layers 20
```

### 4. Configure VillagersReborn

Update your configuration:

```java
// In VillagersRebornConfig.java or config file
```java
LLM_PROVIDER = "local";
LLM_LOCAL_URL = "http://localhost:8080";
LLM_MODEL = "llama-2-7b-chat"; // Model name for reference
LLM_TEMPERATURE = 0.7;
LLM_MAX_TOKENS = 150;
LLM_REQUEST_TIMEOUT = 15000; // 15 seconds for local processing
```

## Advanced Configuration

### Model Parameters

```java
// Temperature (creativity vs consistency)
// 0.1 = very consistent, 1.0 = creative, 1.5+ = chaotic
LLM_TEMPERATURE = 0.7;

// Response length
LLM_MAX_TOKENS = 150;

// Local server settings
LLM_LOCAL_URL = "http://localhost:8080";
LLM_REQUEST_TIMEOUT = 15000; // 15 seconds for local processing
```
```

### Server Optimization

#### For CPU-only systems:
```bash
./server -m models/llama-2-7b-chat.Q4_K_M.gguf \
  -c 2048 \
  --threads $(nproc) \
  --batch-size 512
```

#### For GPU systems:
```bash
./server -m models/llama-2-7b-chat.Q4_K_M.gguf \
  -c 2048 \
  --gpu-layers 35 \
  --threads 4 \
  --batch-size 512
```

## Testing the Setup

### 1. Test Server Health
```bash
curl http://localhost:8080/health
```

### 2. Test API Response
```bash
curl -X POST http://localhost:8080/completion \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "You are a Minecraft villager. Say hello to the player.",
    "max_tokens": 50,
    "temperature": 0.7
  }'
```

### 3. In-Game Test
1. Start Minecraft with VillagersReborn
2. Interact with any villager
3. Check server logs for API calls

## Troubleshooting

### Common Issues

#### "Connection refused"
- Ensure llama.cpp server is running
- Check firewall settings
- Verify URL and port in config

#### "Model not found"
- Ensure model file exists and is readable
- Check file permissions
- Verify model path in server command

#### "Out of memory"
- Use a smaller model (Q4_K_M quantization)
- Reduce context size (`-c` parameter)
- Add more system RAM or use GPU

#### "Slow responses"
- Use smaller models
- Enable GPU acceleration
- Increase server threads
- Reduce max tokens

### Performance Tips

1. **Model Selection**: Smaller models respond faster
2. **Quantization**: Use Q4_K_M for good balance
3. **Context Size**: 2048 is usually sufficient
4. **Batch Size**: 512 works well for most systems
5. **GPU Layers**: Set to ~80% of model layers for GPU

### Debug Mode

Enable verbose logging:
```bash
./server -m models/llama-2-7b-chat.Q4_K_M.gguf -c 2048 --verbose
```

## Model Recommendations

### Beginner Friendly
- **llama-2-7b-chat.Q4_K_M.gguf** (~3.8GB)
- **mistral-7b-instruct-v0.2.Q4_K_M.gguf** (~4.1GB)

### Advanced Users
- **llama-2-13b-chat.Q4_K_M.gguf** (~7.3GB)
- **codellama-13b-instruct.Q4_K_M.gguf** (~7.3GB)

### System Requirements

| Model Size | RAM Required | VRAM (GPU) | Response Time |
|------------|--------------|------------|---------------|
| 7B Q4      | 4GB          | 4GB        | 1-3s         |
| 13B Q4     | 8GB          | 8GB        | 2-5s         |
| 30B Q4     | 16GB         | 16GB       | 5-10s        |

## Integration Features

The llama.cpp provider supports:
- ✅ Context-aware dialogue
- ✅ Conversation memory
- ✅ Personality-based responses
- ✅ Weather/time awareness
- ✅ Reputation system
- ✅ Caching for performance
- ✅ Fallback to static dialogue

## Security Notes

- Local processing ensures privacy
- No data leaves your system
- Models run entirely offline
- No API keys required

## Getting Help

- [llama.cpp Documentation](https://github.com/ggerganov/llama.cpp/blob/master/README.md)
- [VillagersReborn Issues](https://github.com/your-repo/issues)
- [Hugging Face Models](https://huggingface.co/models?library=gguf)
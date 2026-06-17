# 🧠 DocuMind AI

Local AI assistant for project documentation and repository analysis.

## 🛠 Tech Stack
* **Backend:** Java 21, Spring Boot 3.x, LangChain4j
* **Frontend:** HTMX, Tailwind CSS
* **LLM:** Ollama

## 🚀 Quick Start

### 1. Ollama Setup
1. Install Ollama.
2. Set environment variable OLLAMA_MODELS to your preferred path (e.g., D:\Ollama).
3. Launch Ollama and pull Llama 3:
```bash
ollama run llama3
```
### 2. Backend Configuration & Launch
* Check configuration in src/main/resources/application.properties:
```properties
langchain4j.ollama.chat-model.base-url=http://localhost:11434
langchain4j.ollama.chat-model.model-name=llama3
langchain4j.ollama.activation-flag=true
```
* Build and run the project using Maven (JDK 21 required):
```bash
mvn clean compile spring-boot:run
```
### 3. Usage
* Open in browser: http://localhost:8080.
* Step 1: Specify absolute path to documentation folder and click "Scan Folder".
* Step 2: Interact with the AI assistant in the chat.

## 🏗Architectural Features

* Stateful Chat: Implements MessageWindowChatMemory (20-message buffer) for context retention.
* On-the-Fly Prompt Injection: Proxy services are built dynamically per request via systemMessageProvider.
* UI Responsiveness: Powered by HTMX; input fields are disabled during generation to prevent spam.
* Clean Context Isolation: Repository loader wraps content in strict [FILE: filename] markers to maintain document boundaries.

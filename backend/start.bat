@echo off
echo ========================================
echo   Starting RAG Chatbot Backend
echo ========================================
echo.
echo Prerequisites:
echo   - Java 17+ installed
echo   - PostgreSQL running with pgvector
echo   - Ollama running (ollama serve)
echo.
echo Starting Spring Boot...
echo.

mvn spring-boot:run

pause

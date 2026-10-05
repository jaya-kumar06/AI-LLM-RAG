@echo off
echo ========================================
echo   Starting RAG Chatbot Frontend
echo ========================================
echo.
echo Installing dependencies...
call npm install

echo.
echo Starting React dev server...
echo Frontend will be available at: http://localhost:3000
echo.

npm run dev

pause

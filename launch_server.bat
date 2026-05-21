@echo off
cd /d C:\Users\ruddls030\jvision
start "" /MIN python -m uvicorn web:app --host 0.0.0.0 --port 8000

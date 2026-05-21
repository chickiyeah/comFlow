content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

old = '''@app.post("/api/lms/login")
def lms_login(req: AttendRequest):
    """LMS 로그인 + 초기 데이터 탐색"""
    try:
        result = fetch_lms_all(req)
        return JSONResponse(content=result)
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)'''

new = '''@app.post("/api/lms/login")
def lms_login(req: LmsCourseRequest):
    """LMS 완전 출결 조회 (로그인 + eclass_room2 + attendance)"""
    try:
        result = fetch_lms_all(req)
        return JSONResponse(content=result)
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)'''

if old in content:
    content = content.replace(old, new)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("FIXED lms_login route")
else:
    print("NOT FOUND")

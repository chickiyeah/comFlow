content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Find where lms_login is
login_idx = content.find('@app.post("/api/lms/login")')
if login_idx < 0:
    print("lms_login not found")
    exit()

# Check if LmsCourseRequest is defined before lms_login
lms_req_idx = content.find('class LmsCourseRequest(BaseModel):')
print(f"LmsCourseRequest at {lms_req_idx}, lms_login at {login_idx}")

if lms_req_idx > login_idx or lms_req_idx < 0:
    print("LmsCourseRequest defined AFTER lms_login - adding definition before it")

    # Add class definition right before @app.post("/api/lms/login")
    lms_req_class = '''class LmsCourseRequest(BaseModel):
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]
    year: str = "2026"
    term: str = "1"   # 1=1학기, 2=2학기


'''
    content = content[:login_idx] + lms_req_class + content[login_idx:]
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)

    import py_compile
    try:
        py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
        print("FIXED - SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"ERROR: {e}")
else:
    print("LmsCourseRequest already defined before lms_login - other issue")

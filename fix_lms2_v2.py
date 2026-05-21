content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Fix the attendance endpoint to use ud/ky parameters
old_attend_payload = '''        payload = {"encoding": "utf-8"}
        if req.course_id:
            payload["course_id"] = req.course_id
        if req.year:
            payload["YEAR"] = req.year
        if req.term:
            payload["TERM"] = req.term

        resp = sess.post(f"{LMS_URL}/ilos/st/course/attendance_list.acl",
            data=payload, headers=headers, verify=False, timeout=15)'''

new_attend_payload = '''        # payload: ud=학번&ky=과목키(klass_id)&encoding=utf-8
        payload = {"ud": req.user_id, "encoding": "utf-8"}
        if req.course_id:
            payload["ky"] = req.course_id  # ky = klass_id (N2026B182940c00 형식)

        resp = sess.post(f"{LMS_URL}/ilos/st/course/attendance_list.acl",
            data=payload, headers=headers, verify=False, timeout=15)'''

if old_attend_payload in content:
    content = content.replace(old_attend_payload, new_attend_payload)
    print("Fixed attendance payload to use ud/ky")

# Fix LmsCourseRequest and LmsAttendRequest to include course_id properly
# Also update the Pydantic model for LmsAttendRequest
old_lms_model = '''class LmsAttendRequest(BaseModel):
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]
    year: str = "2026"
    term: str = "1"
    # 출결 조회 시 course_id 필요 (과목목록에서 추출)
    course_id: str = ""'''

new_lms_model = '''class LmsAttendRequest(BaseModel):
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]
    year: str = "2026"
    term: str = "1"
    course_id: str = ""  # ky 파라미터값 (예: N2026B182940c00)'''

if old_lms_model in content:
    content = content.replace(old_lms_model, new_lms_model)
    print("Updated LmsAttendRequest model")

open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
import py_compile
try:
    py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
    print("SYNTAX OK")
except py_compile.PyCompileError as e:
    print(f"ERROR: {e}")

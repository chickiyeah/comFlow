content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Find the problem: fetch_portal_schedule uses ScheduleRequest but it's defined elsewhere or missing
# Check if ScheduleRequest is defined
if 'class ScheduleRequest(BaseModel):' not in content:
    print("ScheduleRequest NOT defined - adding it")
    # Add before fetch_portal_schedule
    old = '@app.post("/api/schedule/portal")'
    new = '''class ScheduleRequest(BaseModel):
    user_id: str
    access_token: str
    session_cookies: Union[List[Any], dict]
    year: str = "2026"
    smr: str = "SU002001"   # SU002001=1학기, SU002002=2학기


@app.post("/api/schedule/portal")'''
    if old in content:
        content = content.replace(old, new, 1)
        print("Added ScheduleRequest before schedule endpoint")
    else:
        print("Could not find schedule endpoint!")
else:
    # ScheduleRequest exists but might be after the function - check order
    sr_idx = content.find('class ScheduleRequest(BaseModel):')
    fn_idx = content.find('def fetch_portal_schedule(')
    if fn_idx < sr_idx:
        print(f"ScheduleRequest ({sr_idx}) is AFTER fetch_portal_schedule ({fn_idx}) - reordering")
        # Remove existing ScheduleRequest definition
        # Find its end (next class or @app.post)
        sr_end = content.find('\n\n\n', sr_idx + 10)
        if sr_end < 0:
            sr_end = content.find('\n@app.', sr_idx + 10)
        sr_block = content[sr_idx:sr_end]
        content = content[:sr_idx] + content[sr_end:]
        # Re-insert before fetch_portal_schedule
        fn_idx2 = content.find('def fetch_portal_schedule(')
        # Find the @app.post before it
        app_post_idx = content.rfind('@app.post', 0, fn_idx2)
        content = content[:app_post_idx] + sr_block + '\n\n' + content[app_post_idx:]
        print("Moved ScheduleRequest before fetch_portal_schedule")
    else:
        print(f"Order is correct: ScheduleRequest ({sr_idx}) before fetch_portal_schedule ({fn_idx})")
        # Maybe the issue is something else - check for async def
        if 'async def fetch_portal_schedule' in content:
            print("Found async def - schedule still uses async")

# Also remove async from fetch_portal_schedule if present
content = content.replace('async def fetch_portal_schedule', 'def fetch_portal_schedule')
content = content.replace('async def fetch_all_attendance', 'def fetch_all_attendance')
content = content.replace('async def fetch_grade_terms', 'def fetch_grade_terms')
content = content.replace('async def fetch_grade_detail', 'def fetch_grade_detail')
content = content.replace('async def fetch_shuttle', 'def fetch_shuttle')
content = content.replace('async def attend_login', 'def attend_login')
content = content.replace('async def fetch_course_attendance', 'def fetch_course_attendance')
content = content.replace('async def get_login_datasets', 'def get_login_datasets')
content = content.replace('async def get_layout', 'def get_layout')
content = content.replace('async def explore_menus', 'def explore_menus')
print("Converted all blocking endpoints from async def to def")

open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)

# Verify
import py_compile, sys
try:
    py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
    print("SYNTAX OK")
except py_compile.PyCompileError as e:
    print(f"SYNTAX ERROR: {e}")

content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# All sync-blocking endpoints must be 'def' not 'async def'
# FastAPI runs 'def' routes in thread pool automatically
replacements = [
    ('async def fetch_all_attendance', 'def fetch_all_attendance'),
    ('async def fetch_grade_terms', 'def fetch_grade_terms'),
    ('async def fetch_grade_detail', 'def fetch_grade_detail'),
    ('async def fetch_portal_schedule', 'def fetch_portal_schedule'),
    ('async def fetch_shuttle', 'def fetch_shuttle'),
    ('async def fetch_shuttle', 'def fetch_shuttle'),  # might have dupe
    ('async def attend_login', 'def attend_login'),
    ('async def fetch_course_attendance', 'def fetch_course_attendance'),
    ('async def get_login_datasets', 'def get_login_datasets'),
    ('async def get_layout', 'def get_layout'),
    ('async def explore_menus', 'def explore_menus'),
]

patched = 0
for old, new in replacements:
    if old in content:
        content = content.replace(old, new)
        patched += 1
        print(f"Patched: {old} -> {new}")

open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
print(f"Done: {patched} replacements")

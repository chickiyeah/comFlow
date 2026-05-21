content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Fix course list payloads - user confirmed: YEAR=2021&TERM=1&num=1&encoding=utf-8 for list.acl
old_course = '''        # 현재 학기 과목
        r1 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list.acl",
            data={"encoding": "utf-8"}, headers=headers, verify=False, timeout=10)

        # 특정 연도/학기 과목
        r2 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list2.acl",
            data={"YEAR": req.year, "TERM": req.term, "num": "2", "encoding": "utf-8"},
            headers=headers, verify=False, timeout=10)

        # 비정규 과목
        r3 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list_b.acl",
            data={"YEAR": "", "TERM": "", "NON_TERM": "B", "encoding": "utf-8"},
            headers=headers, verify=False, timeout=10)'''

new_course = '''        # 과목목록1: YEAR+TERM+num=1+encoding (유저 확인 포맷)
        r1 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list.acl",
            data={"YEAR": req.year, "TERM": req.term, "num": "1", "encoding": "utf-8"},
            headers=headers, verify=False, timeout=10)

        # 과목목록2: num=2
        r2 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list2.acl",
            data={"YEAR": req.year, "TERM": req.term, "num": "2", "encoding": "utf-8"},
            headers=headers, verify=False, timeout=10)

        # 비정규 과목
        r3 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list_b.acl",
            data={"YEAR": "", "TERM": "", "NON_TERM": "B", "encoding": "utf-8"},
            headers=headers, verify=False, timeout=10)'''

found = False
if old_course in content:
    content = content.replace(old_course, new_course)
    found = True
else:
    # Try alternative (previous fix already changed it)
    old2 = '''        # 현재/특정 학기 과목 목록 (Content-Length 31바이트 = YEAR+TERM+encoding)
        r1 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list.acl",
            data={"YEAR": req.year, "TERM": req.term, "encoding": "utf-8"},
            headers=headers, verify=False, timeout=10)'''
    new2 = '''        # 과목목록1: YEAR+TERM+num=1+encoding (유저 확인 포맷)
        r1 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list.acl",
            data={"YEAR": req.year, "TERM": req.term, "num": "1", "encoding": "utf-8"},
            headers=headers, verify=False, timeout=10)'''
    if old2 in content:
        content = content.replace(old2, new2)
        found = True

if found:
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    import py_compile
    try:
        py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
        print("FIXED - SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"ERROR: {e}")
else:
    print("Pattern not found")
    idx = content.find('course_register_list.acl')
    print(repr(content[idx-20:idx+200]))

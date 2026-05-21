content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Find and replace the inner attendance loop in fetch_lms_all
old_inner = '''        r_sub = sess.get(f"{LMS_URL}{return_url}",
            headers={"Referer":f"{LMS_URL}/ilos/mp/course_register_list_form.acl"},
            verify=False, timeout=10, allow_redirects=True)

        n_ky_vals = list(set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_sub.text)))

        for ky in n_ky_vals:
            sess.get(f"{LMS_URL}/ilos/st/course/attendance_list_form.acl",
                params={"ky":ky,"ud":req.user_id}, verify=False, timeout=10)
            r_at = sess.post(f"{LMS_URL}/ilos/st/course/attendance_list.acl",
                data={"ud":req.user_id,"ky":ky,"encoding":"utf-8"},
                headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                         "X-Requested-With":"XMLHttpRequest",
                         "Referer":f"{LMS_URL}/ilos/st/course/attendance_list_form.acl?ky={ky}","Origin":LMS_URL},
                verify=False, timeout=15)
            courses_attend.append({
                "kjkey": kjkey,
                "ky": ky,
                "success": len(r_at.content) > 200,
                "html": r_at.text[:5000] if len(r_at.content) > 200 else None,
                "error": r_at.text[:100] if len(r_at.content) <= 200 else None,
            })'''

new_inner = '''        # 서브메인 진입 (course context 확립)
        sess.get(f"{LMS_URL}{return_url}",
            headers={"Referer":f"{LMS_URL}/ilos/mp/course_register_list_form.acl"},
            verify=False, timeout=10, allow_redirects=True)

        # 출결 폼 GET (ky 불필요 — 세션 컨텍스트로 자동 처리)
        sess.get(f"{LMS_URL}/ilos/st/course/attendance_list_form.acl",
            headers={"Referer":f"{LMS_URL}/ilos/st/course/submain_form.acl"},
            verify=False, timeout=10)

        # 출결 데이터 POST — ky는 A-포맷 klass_id 그대로 사용 가능
        r_at = sess.post(f"{LMS_URL}/ilos/st/course/attendance_list.acl",
            data={"ud":req.user_id,"ky":kjkey,"encoding":"utf-8"},
            headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                     "X-Requested-With":"XMLHttpRequest",
                     "Referer":f"{LMS_URL}/ilos/st/course/attendance_list_form.acl","Origin":LMS_URL},
            verify=False, timeout=15)

        courses_attend.append({
            "kjkey": kjkey,
            "success": len(r_at.content) > 200,
            "html": r_at.text[:5000] if len(r_at.content) > 200 else None,
            "error": r_at.text[:100] if len(r_at.content) <= 200 else None,
        })'''

if old_inner in content:
    content = content.replace(old_inner, new_inner)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    import py_compile
    try:
        py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
        print("FIXED attendance flow - SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"ERROR: {e}")
else:
    print("Pattern NOT found")
    idx = content.find('n_ky_vals')
    print(f"n_ky_vals at: {idx}")
    if idx > 0:
        print(repr(content[idx-200:idx+200]))

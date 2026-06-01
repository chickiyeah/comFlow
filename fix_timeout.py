path = 'C:/Users/ruddls030/jvision/web.py'
content = open(path, encoding='utf-8').read()

# combined_attendance 블록 내 iwin_st_chulseokbu timeout=8 → 30
old = 'iwin_st_chulseokbu",\n                data={"ikey": json.dumps({"dclass": sc, "duser_id": req.user_id})},\n                headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",\n                         "X-Requested-With": "XMLHttpRequest",\n                         "Referer": f"{chk_url}/jvision/student/", "Origin": chk_url},\n                verify=False, timeout=8)'
new = 'iwin_st_chulseokbu",\n                data={"ikey": json.dumps({"dclass": sc, "duser_id": req.user_id})},\n                headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",\n                         "X-Requested-With": "XMLHttpRequest",\n                         "Referer": f"{chk_url}/jvision/student/", "Origin": chk_url},\n                verify=False, timeout=30)'

if old in content:
    content = content.replace(old, new, 1)
    open(path, 'w', encoding='utf-8').write(content)
    import py_compile
    try:
        py_compile.compile(path, doraise=True)
        print("FIXED timeout=8 -> 30, SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"SYNTAX ERROR: {e}")
else:
    # fallback: 단순 문자열 치환
    count = content.count('timeout=8')
    print(f"Exact pattern not found. timeout=8 appears {count} times in file")
    # combined_attendance 함수 내 첫 timeout=8만 바꿈
    idx = content.find('combined_attendance')
    if idx >= 0:
        chunk = content[idx:idx+3000]
        new_chunk = chunk.replace('timeout=8', 'timeout=30', 1)
        if new_chunk != chunk:
            content = content[:idx] + new_chunk + content[idx+3000:]
            open(path, 'w', encoding='utf-8').write(content)
            import py_compile
            try:
                py_compile.compile(path, doraise=True)
                print("FIXED timeout via fallback, SYNTAX OK")
            except py_compile.PyCompileError as e:
                print(f"SYNTAX ERROR: {e}")
        else:
            print("No timeout=8 in combined_attendance block")

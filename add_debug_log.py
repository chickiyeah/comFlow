path = 'C:/Users/ruddls030/jvision/web.py'
content = open(path, encoding='utf-8').read()

# fetch_check_course 내 except 블록에 print 추가
old = '''            try:
                raw = chul_r.json()
            except Exception:
                raw = {}
            parsed = _parse_check_attend(raw)'''

new = '''            try:
                raw = chul_r.json()
            except Exception as je:
                print(f"[check] JSON parse error for {sc}: {je}")
                raw = {}
            rbu = raw.get("rollbookuser", []) if isinstance(raw, dict) else []
            wl  = rbu[0].get("attendCountResultList", []) if rbu else []
            print(f"[check] {sc[:30]} rbu={len(rbu)} wl={len(wl)} attend={sum(w.get('attend_count',0) for w in wl)}")
            parsed = _parse_check_attend(raw)'''

if old in content:
    content = content.replace(old, new, 1)
    open(path, 'w', encoding='utf-8').write(content)
    import py_compile
    try:
        py_compile.compile(path, doraise=True)
        print("ADDED debug logging, SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"SYNTAX ERROR: {e}")
else:
    print("Pattern not found, looking for similar...")
    idx = content.find('chul_r.json()')
    if idx >= 0:
        print(repr(content[idx-50:idx+200]))

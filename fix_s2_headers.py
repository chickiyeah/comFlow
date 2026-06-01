path = 'C:/Users/ruddls030/jvision/web.py'
content = open(path, encoding='utf-8').read()

old = '''            s2 = requests.Session()
            for c in sess_chk.cookies:
                s2.cookies.set(c.name, c.value, domain=c.domain, path=c.path)'''

new = '''            s2 = requests.Session()
            s2.headers.update({"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
            for c in sess_chk.cookies:
                s2.cookies.set(c.name, c.value, domain=c.domain, path=c.path)'''

if old in content:
    content = content.replace(old, new, 1)
    open(path, 'w', encoding='utf-8').write(content)
    import py_compile
    try:
        py_compile.compile(path, doraise=True)
        print("FIXED s2 User-Agent, SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"SYNTAX ERROR: {e}")
else:
    print("Pattern not found")
    idx = content.find('s2 = requests.Session()')
    print(repr(content[idx:idx+200]))

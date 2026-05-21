content = open('C:/Users/ruddls030/jvision/intranet.py', encoding='utf-8').read()
old = '    session_cookies = {k: v for k, v in session.cookies.items()}'
new = '    session_cookies = [{"name": c.name, "value": c.value, "domain": c.domain, "path": c.path} for c in session.cookies]'
if old in content:
    content = content.replace(old, new)
    open('C:/Users/ruddls030/jvision/intranet.py', 'w', encoding='utf-8').write(content)
    print("PATCHED OK")
else:
    print("NOT FOUND")
    print(repr(content[content.find('session_cookies = {'):content.find('session_cookies = {')+80]))

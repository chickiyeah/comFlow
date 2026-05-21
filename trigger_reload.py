content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()
# Add a harmless comment to trigger --reload
content = '# reload trigger\n' + content
open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
print("Triggered reload")

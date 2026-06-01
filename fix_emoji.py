"""intranet.py의 이모지 제거 — cp949 인코딩 오류 방지"""
import re

path = r'C:/Users/ruddls030/jvision/intranet.py'
content = open(path, encoding='utf-8').read()

# 이모지 문자 제거 (유니코드 이모지 범위)
emoji_pattern = re.compile(
    "[\U0001F600-\U0001F64F"   # emoticons
    "\U0001F300-\U0001F5FF"   # symbols & pictographs
    "\U0001F680-\U0001F6FF"   # transport & map
    "\U0001F700-\U0001F77F"   # alchemical
    "\U0001F780-\U0001F7FF"   # geometric
    "\U0001F800-\U0001F8FF"   # supplemental arrows
    "\U0001F900-\U0001F9FF"   # supplemental symbols
    "\U0001FA00-\U0001FA6F"   # chess symbols
    "\U0001FA70-\U0001FAFF"   # symbols and pictographs extended
    "☀-⛿"            # misc symbols
    "✀-➿"            # dingbats
    "❗"                   # ❗
    "✅"                   # ✅
    "⚠"                   # ⚠
    "✨"                   # ✨
    "]+",
    flags=re.UNICODE
)

cleaned = emoji_pattern.sub('', content)
removed = len(content) - len(cleaned)
print(f"제거된 문자 수: {removed}")

open(path, 'w', encoding='utf-8').write(cleaned)
import py_compile
try:
    py_compile.compile(path, doraise=True)
    print("SYNTAX OK")
except py_compile.PyCompileError as e:
    print(f"SYNTAX ERROR: {e}")

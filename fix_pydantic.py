content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()
old = '''class PortalRequest(BaseModel):
    """저장된 토큰 + 쿠키로 학교 포털 API 호출"""
    user_id: str
    access_token: str
    session_cookies: dict   # 로그인 시 직렬화된 세션 쿠키 (필수)'''
new = '''from typing import Union, List, Any

class PortalRequest(BaseModel):
    """저장된 토큰 + 쿠키로 학교 포털 API 호출"""
    user_id: str
    access_token: str
    session_cookies: Union[List[Any], dict]  # 리스트(도메인 포함) 또는 flat dict 모두 허용'''
if old in content:
    content = content.replace(old, new)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("PATCHED OK")
else:
    print("NOT FOUND")
    idx = content.find('class PortalRequest')
    if idx >= 0:
        print(repr(content[idx:idx+200]))

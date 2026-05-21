content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

new_endpoint = '''

@app.post("/api/login-datasets")
async def get_login_datasets(req: LoginRequest, db: DbSession = Depends(get_db)):
    """NMain LoginAction 응답의 전체 데이터셋 반환 - SQL_ID 탐색용"""
    import requests as req_lib
    import urllib3
    urllib3.disable_warnings()

    session = req_lib.Session()
    common_headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept-Language": "ko,en-US;q=0.9",
    }

    # SSO login
    auth_url = "https://portal.jvision.ac.kr/user/loginAuth.face"
    session.get(auth_url, params={"langKnd": "ko", "userId": req.user_id, "password": req.user_pw},
                headers=common_headers, verify=False)
    login_url = "https://portal.jvision.ac.kr/user/loginProcess.face"
    session.post(login_url, data={"langKnd": "ko", "userId": req.user_id, "username": req.user_id, "password": req.user_pw},
                 headers=common_headers, verify=False)

    portal_sid = session.cookies.get('JSESSIONID', domain='portal.jvision.ac.kr')
    if portal_sid:
        session.cookies.set('EnviewSessionId', portal_sid, domain='my.jvision.ac.kr', path='/')

    sso_res = session.get(
        "http://sso.jvision.ac.kr/enpass/login?gateway=client&epAppId=jvisionCommon&service=https://my.jvision.ac.kr/MyVisionApp/index.jsp",
        headers=common_headers, verify=False, allow_redirects=True
    )

    session.cookies.set('JSESSIONID', '', domain='my.jvision.ac.kr', path='/')

    nmain_headers = {
        "Accept": "application/xml, text/xml, */*",
        "Content-Type": "text/xml",
        "X-Requested-With": "XMLHttpRequest",
        "Referer": "https://my.jvision.ac.kr/MyVisionApp/index.jsp",
        "Origin": "https://my.jvision.ac.kr",
        "Host": "my.jvision.ac.kr",
        "Cache-Control": "no-cache",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    }

    login_payload = f"""<?xml version="1.0" encoding="UTF-8"?>
<Root xmlns="http://www.nexacroplatform.com/platform/dataset">
    <Parameters>
        <Parameter id="fsp_action">LoginAction</Parameter>
        <Parameter id="fsp_cmd">execute</Parameter>
        <Parameter id="fsp_logId">all</Parameter>
        <Parameter id="fsp_logging">true</Parameter>
        <Parameter id="LOGIN_METHOD">nexacro</Parameter>
        <Parameter id="OVER_LOGIN">Y</Parameter>
    </Parameters>
    <Dataset id="ds_user">
        <ColumnInfo>
            <Column id="USER_ID" type="string" size="256"/>
            <Column id="USER_PW" type="string" size="256"/>
            <Column id="REMEM_ID" type="string" size="256"/>
        </ColumnInfo>
        <Rows>
            <Row><Col id="USER_ID">{req.user_id}</Col><Col id="USER_PW">{req.user_pw}</Col></Row>
        </Rows>
    </Dataset>
    <Dataset id="fsp_ds_cmd"><ColumnInfo></ColumnInfo><Rows></Rows></Dataset>
    <Dataset id="fsp_ds_prog">
        <ColumnInfo>
            <Column id="MENU_ID" type="string" size="256"/>
            <Column id="PROG_ID" type="string" size="256"/>
            <Column id="LOCALE" type="string" size="256"/>
        </ColumnInfo>
        <Rows><Row><Col id="MENU_ID">unknown</Col><Col id="PROG_ID">frame::login_frame.xfdl</Col><Col id="LOCALE">ko_KR</Col></Row></Rows>
    </Dataset>
    <Dataset id="fsp_ds_user"><ColumnInfo><Column id="USER_ID" type="STRING" size="32"/></ColumnInfo><Rows></Rows></Dataset>
</Root>"""

    nmain_res = session.post("https://my.jvision.ac.kr/NMain", data=login_payload.encode('utf-8'),
                              headers=nmain_headers, verify=False)

    return JSONResponse(content={"success": True, "status": nmain_res.status_code, "raw": nmain_res.text})

'''

old_main = "if __name__ == \"__main__\":"
if old_main in content:
    content = content.replace(old_main, new_endpoint + old_main)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("ADDED login-datasets endpoint")
else:
    print("Could not find main block")

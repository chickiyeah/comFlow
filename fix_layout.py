content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

new_endpoint = '''

@app.post("/api/layout")
async def get_layout(req: PortalRequest, sql_id: str = "sys/com:com_layout_s01"):
    """메뉴/레이아웃 데이터 조회 - 최소 fsp_ds_cmd 사용"""
    try:
        import urllib3
        urllib3.disable_warnings()
        sess = _restore_session(req.access_token, req.session_cookies)
        headers = _nmain_headers(req.access_token)

        # 최소한의 fsp_ds_cmd (EXEC 컬럼 제외)
        payload = f"""<?xml version="1.0" encoding="UTF-8"?>
<Root xmlns="http://www.nexacroplatform.com/platform/dataset">
    <Parameters>
        <Parameter id="fsp_action">JVCAction</Parameter>
        <Parameter id="fsp_cmd">execute</Parameter>
        <Parameter id="fsp_logId">all</Parameter>
        <Parameter id="fsp_logging">true</Parameter>
        <Parameter id="UI_CD">0010</Parameter>
    </Parameters>
    <Dataset id="fsp_ds_cmd">
        <ColumnInfo>
            <Column id="TYPE" type="string" size="10"/>
            <Column id="SQL_ID" type="string" size="200"/>
            <Column id="KEY_ZERO_LEN" type="INT" size="10"/>
            <Column id="EXEC_TYPE" type="STRING" size="2"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="TYPE">N</Col>
                <Col id="SQL_ID">{sql_id}</Col>
                <Col id="KEY_ZERO_LEN">0</Col>
                <Col id="EXEC_TYPE">B</Col>
            </Row>
        </Rows>
    </Dataset>
    <Dataset id="fsp_ds_prog">
        <ColumnInfo>
            <Column id="MENU_ID" type="string" size="256"/>
            <Column id="PROG_ID" type="string" size="256"/>
            <Column id="LOCALE" type="string" size="256"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="MENU_ID">system</Col>
                <Col id="PROG_ID">system</Col>
                <Col id="LOCALE">ko_KR</Col>
            </Row>
        </Rows>
    </Dataset>
    <Dataset id="fsp_ds_user">
        <ColumnInfo>
            <Column id="USER_ID" type="string" size="100"/>
        </ColumnInfo>
        <Rows>
            <Row><Col id="USER_ID">{req.user_id}</Col></Row>
        </Rows>
    </Dataset>
</Root>"""
        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        return JSONResponse(content={"success": True, "raw": res.text})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)

'''

old_main = "if __name__ == \"__main__\":"
if old_main in content:
    content = content.replace(old_main, new_endpoint + old_main, 1)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("ADDED layout endpoint")
else:
    print("Could not find main block")

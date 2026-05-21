content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Add a debug endpoint to explore available SQL_IDs
new_endpoint = '''

@app.post("/api/explore")
async def explore_menus(req: PortalRequest, sql_id: str = "sys/com:com_layout_s01"):
    """메뉴 구조 탐색 - 사용 가능한 SQL_ID 찾기"""
    try:
        import urllib3
        urllib3.disable_warnings()
        sess = _restore_session(req.access_token, req.session_cookies)
        headers = _nmain_headers(req.access_token)

        payload = f"""<?xml version="1.0" encoding="UTF-8"?>
<Root xmlns="http://www.nexacroplatform.com/platform/dataset">
    <Parameters>
        <Parameter id="fsp_action">JVCActionNoSession</Parameter>
        <Parameter id="fsp_cmd">execute</Parameter>
        <Parameter id="fsp_logId">all</Parameter>
        <Parameter id="fsp_logging">true</Parameter>
        <Parameter id="UI_CD">0010</Parameter>
    </Parameters>
    <Dataset id="fsp_ds_cmd">
        <ColumnInfo>
            <Column id="TX_NAME" type="string" size="100"/>
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
        <Rows>
            <Row>
                <Col id="MENU_ID">system</Col>
                <Col id="PROG_ID">system</Col>
                <Col id="LOCALE">ko_KR</Col>
            </Row>
        </Rows>
    </Dataset>
</Root>"""
        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        return JSONResponse(content={"success": True, "raw": res.text[:20000]})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)

'''

# Insert before if __name__
old_main = "if __name__ == \"__main__\":"
if old_main in content:
    content = content.replace(old_main, new_endpoint + old_main)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("ADDED explore endpoint")
else:
    print("Could not find main block")

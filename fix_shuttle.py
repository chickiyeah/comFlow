content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

new_endpoint = '''

@app.post("/api/shuttle")
async def fetch_shuttle(req: PortalRequest):
    """
    통학버스 정보 조회 (sdsb9001m_s01 + s02 + s03 동시 실행)
    s01: 노선 목록, s02: 정류장 목록, s03: 시간표
    """
    try:
        import urllib3
        urllib3.disable_warnings()
        sess = _restore_session(req.access_token, req.session_cookies)
        headers = _nmain_headers(req.access_token)

        # fsp_ds_cmd에 3개 SQL_ID 동시 실행 (원본 요청과 동일)
        payload = f"""<?xml version="1.0" encoding="UTF-8"?>
<Root xmlns="http://www.nexacroplatform.com/platform/dataset">
    <Parameters>
        <Parameter id="fsp_action">JVCAction</Parameter>
        <Parameter id="fsp_cmd">execute</Parameter>
        <Parameter id="fsp_logId">all</Parameter>
        <Parameter id="fsp_logging">true</Parameter>
    </Parameters>
    <Dataset id="fsp_ds_cmd">
        <ColumnInfo>
            <Column id="TX_NAME" type="string" size="100"/>
            <Column id="TYPE" type="string" size="10"/>
            <Column id="SQL_ID" type="string" size="200"/>
            <Column id="KEY_SQL_ID" type="string" size="200"/>
            <Column id="KEY_INCREMENT" type="INT" size="10"/>
            <Column id="CALLBACK_SQL_ID" type="STRING" size="200"/>
            <Column id="INSERT_SQL_ID" type="STRING" size="200"/>
            <Column id="UPDATE_SQL_ID" type="STRING" size="200"/>
            <Column id="DELETE_SQL_ID" type="STRING" size="200"/>
            <Column id="SAVE_FLAG_COLUMN" type="STRING" size="200"/>
            <Column id="USE_INPUT" type="STRING" size="1"/>
            <Column id="USE_ORDER" type="STRING" size="1"/>
            <Column id="KEY_ZERO_LEN" type="INT" size="10"/>
            <Column id="BIZ_NAME" type="STRING" size="100"/>
            <Column id="PAGE_NO" type="INT" size="10"/>
            <Column id="PAGE_SIZE" type="INT" size="10"/>
            <Column id="READ_ALL" type="STRING" size="1"/>
            <Column id="EXEC_TYPE" type="STRING" size="2"/>
            <Column id="EXEC" type="STRING" size="1"/>
            <Column id="FAIL" type="STRING" size="1"/>
            <Column id="FAIL_MSG" type="STRING" size="200"/>
            <Column id="EXEC_CNT" type="INT" size="1"/>
            <Column id="MSG" type="STRING" size="200"/>
        </ColumnInfo>
        <Rows>
            <Row><Col id="TYPE">N</Col><Col id="SQL_ID">sd/sb/sb90:sdsb9001m_s01</Col><Col id="KEY_ZERO_LEN">0</Col><Col id="EXEC_TYPE">B</Col></Row>
            <Row><Col id="TYPE">N</Col><Col id="SQL_ID">sd/sb/sb90:sdsb9001m_s02</Col><Col id="KEY_ZERO_LEN">0</Col><Col id="EXEC_TYPE">B</Col></Row>
            <Row><Col id="TYPE">N</Col><Col id="SQL_ID">sd/sb/sb90:sdsb9001m_s03</Col><Col id="KEY_ZERO_LEN">0</Col><Col id="EXEC_TYPE">B</Col></Row>
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
                <Col id="MENU_ID">1992</Col>
                <Col id="PROG_ID">sdsb90::sdsb9001m.xfdl</Col>
                <Col id="LOCALE">ko_KR</Col>
            </Row>
        </Rows>
    </Dataset>
{_full_ds_user(req.user_id)}
</Root>"""

        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        raw = res.text

        # 멀티 데이터셋 파싱 (ds_out_1, ds_out_2, ds_out_3 또는 다른 이름)
        from xml.etree import ElementTree as ET
        try:
            ns = {"ns": "http://www.nexacroplatform.com/platform/dataset"}
            root_el = ET.fromstring(raw)
            datasets = {}
            for ds in root_el.findall("ns:Dataset", ns):
                ds_id = ds.get("id")
                if ds_id and ds_id.startswith("fsp_ds"):
                    continue
                cols = [c.get("id") for c in ds.findall("ns:ColumnInfo/ns:Column", ns)]
                rows = []
                for row in ds.findall("ns:Rows/ns:Row", ns):
                    record = {{col.get("id"): col.text or "" for col in row.findall("ns:Col", ns)}}
                    rows.append(record)
                datasets[ds_id] = rows
        except Exception:
            datasets = {{}}

        return JSONResponse(content={{"success": True, "datasets": datasets, "raw": raw[:1000]}})
    except Exception as e:
        return JSONResponse(content={{"success": False, "message": str(e)}}, status_code=500)

'''

old_main = "if __name__ == \"__main__\":"
if 'fetch_shuttle' not in content and old_main in content:
    content = content.replace(old_main, new_endpoint + old_main, 1)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("ADDED shuttle endpoint")
else:
    print("Shuttle already exists or main not found")

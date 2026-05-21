content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Remove broken shuttle endpoint and replace with fixed version
old_shuttle = """@app.post("/api/shuttle")
async def fetch_shuttle(req: PortalRequest):"""

new_shuttle_func = '''@app.post("/api/shuttle")
async def fetch_shuttle(req: PortalRequest):
    """
    통학버스 정보 조회 (sdsb9001m_s01 + s02 + s03 동시 실행)
    """
    try:
        import urllib3
        urllib3.disable_warnings()
        sess = _restore_session(req.access_token, req.session_cookies)
        headers = _nmain_headers(req.access_token)

        shuttle_rows = """
            <Row><Col id="TYPE">N</Col><Col id="SQL_ID">sd/sb/sb90:sdsb9001m_s01</Col><Col id="KEY_ZERO_LEN">0</Col><Col id="EXEC_TYPE">B</Col></Row>
            <Row><Col id="TYPE">N</Col><Col id="SQL_ID">sd/sb/sb90:sdsb9001m_s02</Col><Col id="KEY_ZERO_LEN">0</Col><Col id="EXEC_TYPE">B</Col></Row>
            <Row><Col id="TYPE">N</Col><Col id="SQL_ID">sd/sb/sb90:sdsb9001m_s03</Col><Col id="KEY_ZERO_LEN">0</Col><Col id="EXEC_TYPE">B</Col></Row>"""

        col_info = """            <Column id="TX_NAME" type="string" size="100"/>
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
            <Column id="MSG" type="STRING" size="200"/>"""

        payload = (
            '<?xml version="1.0" encoding="UTF-8"?>'
            '<Root xmlns="http://www.nexacroplatform.com/platform/dataset">'
            '<Parameters>'
            '<Parameter id="fsp_action">JVCAction</Parameter>'
            '<Parameter id="fsp_cmd">execute</Parameter>'
            '<Parameter id="fsp_logId">all</Parameter>'
            '<Parameter id="fsp_logging">true</Parameter>'
            '</Parameters>'
            '<Dataset id="fsp_ds_cmd">'
            '<ColumnInfo>' + col_info + '</ColumnInfo>'
            '<Rows>' + shuttle_rows + '</Rows>'
            '</Dataset>'
            '<Dataset id="fsp_ds_prog">'
            '<ColumnInfo>'
            '<Column id="MENU_ID" type="string" size="256"/>'
            '<Column id="PROG_ID" type="string" size="256"/>'
            '<Column id="LOCALE" type="string" size="256"/>'
            '</ColumnInfo>'
            '<Rows><Row>'
            '<Col id="MENU_ID">1992</Col>'
            '<Col id="PROG_ID">sdsb90::sdsb9001m.xfdl</Col>'
            '<Col id="LOCALE">ko_KR</Col>'
            '</Row></Rows>'
            '</Dataset>'
            '<Dataset id="fsp_ds_user">'
            '<ColumnInfo><Column id="USER_ID" type="string" size="100"/></ColumnInfo>'
            '<Rows><Row><Col id="USER_ID">' + req.user_id + '</Col></Row></Rows>'
            '</Dataset>'
            '</Root>'
        )

        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        raw = res.text

        from xml.etree import ElementTree as ET
        ns = {"ns": "http://www.nexacroplatform.com/platform/dataset"}
        try:
            root_el = ET.fromstring(raw)
            datasets = {}
            for ds in root_el.findall("ns:Dataset", ns):
                ds_id = ds.get("id")
                if ds_id and ds_id.startswith("fsp_ds"):
                    continue
                rows_out = []
                for row in ds.findall("ns:Rows/ns:Row", ns):
                    record = {col.get("id"): (col.text or "") for col in row.findall("ns:Col", ns)}
                    rows_out.append(record)
                datasets[ds_id] = rows_out
        except Exception as parse_err:
            datasets = {"parse_error": str(parse_err)}

        return JSONResponse(content={"success": True, "datasets": datasets, "raw": raw[:500]})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)

'''

# Find and replace the broken shuttle function
# Find start of old function
start_idx = content.find(old_shuttle)
if start_idx < 0:
    print("Could not find shuttle function start")
    exit()

# Find the next @app.post or end of file
next_endpoint = content.find('\n@app.', start_idx + 10)
if next_endpoint < 0:
    next_endpoint = content.find('\nif __name__', start_idx + 10)
if next_endpoint < 0:
    print("Could not find end of shuttle function")
    exit()

# Find the blank line + decorator start
end_idx = next_endpoint  # Keep the newline
old_func = content[start_idx:end_idx]
print(f"Replacing shuttle function ({len(old_func)} chars)")

content = content[:start_idx] + new_shuttle_func + content[end_idx:]
open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
print("FIXED shuttle endpoint")

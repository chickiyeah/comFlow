content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Fix the grades endpoint payload - add fsp_ds_prog and full fsp_ds_cmd
old_grades_payload = '''        # 성적 조회 XML (SQL_ID 실험 필요)
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
            <Column id="EXEC_TYPE" type="STRING" size="2"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="TYPE">N</Col>
                <Col id="SQL_ID">hs/gr/gr01:gr_hakjum_list_s00</Col>
                <Col id="EXEC_TYPE">B</Col>
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
    </Dataset>'''

new_grades_payload = '''        # 성적 조회 XML - fsp_ds_prog 포함 (insertLog가 PROG_ID 필요)
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
            <Row>
                <Col id="TYPE">N</Col>
                <Col id="SQL_ID">hs/gr/gr01:gr_hakjum_list_s00</Col>
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
                <Col id="MENU_ID">2001</Col>
                <Col id="PROG_ID">gr90::usgr9001m.xfdl</Col>
                <Col id="LOCALE">ko_KR</Col>
            </Row>
        </Rows>
    </Dataset>
    <Dataset id="fsp_ds_user">
        <ColumnInfo>
            <Column id="USER_ID" type="string" size="100"/>
            <Column id="USER_NM" type="string" size="200"/>
            <Column id="DEPT_CD" type="string" size="10"/>
            <Column id="USER_IP" type="string" size="32"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="USER_ID">{req.user_id}</Col>
                <Col id="USER_NM"></Col>
                <Col id="DEPT_CD"></Col>
                <Col id="USER_IP"></Col>
            </Row>
        </Rows>
    </Dataset>'''

# Fix the portal endpoint payload too
old_portal_payload = '''        payload = f"""<?xml version="1.0" encoding="UTF-8"?>
<Root xmlns="http://www.nexacroplatform.com/platform/dataset">
    <Parameters>
        <Parameter id="fsp_action">JVCAction</Parameter>
        <Parameter id="fsp_cmd">execute</Parameter>
    </Parameters>
    <Dataset id="fsp_ds_cmd">
        <ColumnInfo>
            <Column id="TYPE" type="string" size="10"/>
            <Column id="SQL_ID" type="string" size="200"/>
            <Column id="EXEC_TYPE" type="STRING" size="2"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="TYPE">N</Col>
                <Col id="SQL_ID">{sql_id}</Col>
                <Col id="EXEC_TYPE">B</Col>
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
</Root>"""'''

new_portal_payload = '''        payload = f"""<?xml version="1.0" encoding="UTF-8"?>
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
                <Col id="PROG_ID">system::main.xfdl</Col>
                <Col id="LOCALE">ko_KR</Col>
            </Row>
        </Rows>
    </Dataset>
    <Dataset id="fsp_ds_user">
        <ColumnInfo>
            <Column id="USER_ID" type="string" size="100"/>
            <Column id="USER_NM" type="string" size="200"/>
            <Column id="DEPT_CD" type="string" size="10"/>
            <Column id="USER_IP" type="string" size="32"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="USER_ID">{req.user_id}</Col>
                <Col id="USER_NM"></Col>
                <Col id="DEPT_CD"></Col>
                <Col id="USER_IP"></Col>
            </Row>
        </Rows>
    </Dataset>
</Root>"""'''

patched = 0
if old_grades_payload in content:
    content = content.replace(old_grades_payload, new_grades_payload)
    patched += 1
    print("Grades payload patched")
else:
    print("Grades payload NOT FOUND")
    idx = content.find('fsp_ds_cmd')
    print(f"fsp_ds_cmd found at {idx}")

if old_portal_payload in content:
    content = content.replace(old_portal_payload, new_portal_payload)
    patched += 1
    print("Portal payload patched")
else:
    print("Portal payload NOT FOUND")

if patched > 0:
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print(f"Saved ({patched} patches)")

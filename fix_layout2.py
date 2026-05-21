content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

old = '''    # 최소한의 fsp_ds_cmd (EXEC 컬럼 제외)
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
    </Dataset>'''

new = '''    # intranet.py 정리 호출과 동일한 JVCActionNoSession 사용
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
    </Dataset>'''

if old in content:
    content = content.replace(old, new)
    open('/'.join(['C:', 'Users', 'ruddls030', 'jvision', 'web.py']), 'w', encoding='utf-8').write(content)
    print("PATCHED layout to use JVCActionNoSession")
else:
    print("NOT FOUND - search for unique string")
    idx = content.find('최소한의 fsp_ds_cmd')
    print(f"Found at: {idx}")
    if idx > 0:
        print(repr(content[idx:idx+100]))

content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Full fsp_ds_cmd with all required columns
full_ds_cmd = '''    <Dataset id="fsp_ds_cmd">
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
            <Row>
                <Col id="TYPE">N</Col>
                <Col id="SQL_ID">PLACEHOLDER</Col>
                <Col id="KEY_ZERO_LEN">0</Col>
                <Col id="EXEC_TYPE">B</Col>
            </Row>
        </Rows>
    </Dataset>'''

# Fix the layout endpoint - find the partial Dataset block and replace with full version
old_partial = '''    <Dataset id="fsp_ds_cmd">
        <ColumnInfo>
            <Column id="TX_NAME" type="string" size="100"/>
            <Column id="TYPE" type="string" size="10"/>
            <Column id="SQL_ID" type="string" size="200"/>
            <Column id="KEY_ZERO_LEN" type="INT" size="10"/>
            <Column id="EXEC_TYPE" type="STRING" size="2"/>
            <Column id="EXEC" type="STRING" size="1"/>
            <Column id="FAIL" type="STRING" size="1"/>
            <Column id="FAIL_MSG" type="STRING" size="200"/>
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

new_full = full_ds_cmd.replace('PLACEHOLDER', '{sql_id}')

if old_partial in content:
    content = content.replace(old_partial, new_full)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("PATCHED - added all required fsp_ds_cmd columns for layout")
else:
    print("NOT FOUND")
    # Show what we have around fsp_ds_cmd in layout
    idx = content.find('async def get_layout')
    if idx > 0:
        end = content.find('async def', idx + 1)
        section = content[idx:end] if end > 0 else content[idx:idx+3000]
        ds_idx = section.find('Dataset id="fsp_ds_cmd"')
        if ds_idx > 0:
            print(repr(section[ds_idx:ds_idx+500]))

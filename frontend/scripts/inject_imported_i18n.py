# -*- coding: utf-8 -*-
"""채용공고 실시간 수집/마감필터 i18n 키를 career 블록에 주입. 1회용."""
import json, io, os
BASE = os.path.join(os.path.dirname(__file__), '..', 'src', 'locales')

KEYS = {
 'ko': {'source_imported': '실시간수집', 'hideExpired': '마감 지난 공고 숨기기',
        'importNow': '지금 수집', 'importing': '수집 중…', 'importDone': '수집 완료', 'importFail': '수집에 실패했습니다.'},
 'en': {'source_imported': 'Auto-collected', 'hideExpired': 'Hide expired',
        'importNow': 'Collect now', 'importing': 'Collecting…', 'importDone': 'Collected', 'importFail': 'Collection failed.'},
 'zh': {'source_imported': '实时采集', 'hideExpired': '隐藏已截止',
        'importNow': '立即采集', 'importing': '采集中…', 'importDone': '采集完成', 'importFail': '采集失败。'},
 'ja': {'source_imported': '自動収集', 'hideExpired': '締切済みを非表示',
        'importNow': '今すぐ収集', 'importing': '収集中…', 'importDone': '収集完了', 'importFail': '収集に失敗しました。'},
 'vi': {'source_imported': 'Tự thu thập', 'hideExpired': 'Ẩn tin hết hạn',
        'importNow': 'Thu thập ngay', 'importing': 'Đang thu thập…', 'importDone': 'Đã thu thập', 'importFail': 'Thu thập thất bại.'},
}

for lang, kv in KEYS.items():
    path = os.path.join(BASE, lang + '.json')
    with io.open(path, encoding='utf-8') as f:
        d = json.load(f)
    d.setdefault('career', {}).update(kv)
    with io.open(path, 'w', encoding='utf-8') as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
        f.write('\n')
    print('updated', lang, '+', len(kv), 'career keys')
